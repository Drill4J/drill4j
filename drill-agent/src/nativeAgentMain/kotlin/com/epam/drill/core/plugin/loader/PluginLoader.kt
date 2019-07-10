package com.epam.drill.core.plugin.loader

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.exceptions.*
import com.epam.drill.jvmapi.*
import com.epam.drill.logger.*
import jvmapi.*
import kotlinx.cinterop.*
import kotlin.collections.set


@SharedImmutable
val plLogger = DLogger("plLogger")

fun loadPlugin(pluginFilePath: String, pluginConfig: PluginBean) {
    AttachNativeThreadToJvm()
    AddToSystemClassLoaderSearch(pluginFilePath)
    plLogger.warn { "System classLoader extends by '$pluginFilePath' path" }
    try {
        val initializerClass = FindClass("com/epam/drill/ws/ClassLoadingUtil")
        val selfMethodId: jfieldID? =
            GetStaticFieldID(initializerClass, "INSTANCE", "Lcom/epam/drill/ws/ClassLoadingUtil;")
        val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
        val calculateBuild: jmethodID? =
            GetMethodID(initializerClass, "retrieveApiClass", "(Ljava/lang/String;)Ljava/lang/Class;")
        val pluginApiClass: jclass = CallObjectMethod(initializer, calculateBuild, NewStringUTF(pluginFilePath))!!

        val userPlugin: jobject =
            NewGlobalRef(
                NewObjectA(
                    pluginApiClass,
                    GetMethodID(pluginApiClass, "<init>", "(Ljava/lang/String;)V"),
                    nativeHeap.allocArray(1.toLong()) {
                        l = NewStringUTF(pluginConfig.id)
                    })
            )!!

        when (pluginConfig.family) {
            Family.INSTRUMENTATION -> {
                val inst = InstrumentationNativePlugin(pluginApiClass, userPlugin, pluginConfig)
                exec {
                    pstorage[pluginConfig.id] = inst
                }
                inst.retransform()
            }
            Family.GENERIC -> {
                GenericNativePlugin(pluginApiClass, userPlugin, pluginConfig).apply {
                    exec {
                        pstorage[this@apply.id] = this@apply
                    }
                }
            }
        }
    } catch (ex: Exception) {
        when (ex) {
            is PluginLoadException ->
                plLogger.warn { "Can't load the plugin file $pluginFilePath. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of $pluginFilePath jar... Error: ${ex.message} ${ex.printStackTrace()}" }
        }
    }
}

