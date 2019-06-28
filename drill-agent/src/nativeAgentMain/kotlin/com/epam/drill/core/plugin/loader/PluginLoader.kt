package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.Family
import com.epam.drill.core.exceptions.PluginLoadException
import com.epam.drill.core.exec
import com.epam.drill.jvmapi.AttachNativeThreadToJvm
import com.epam.drill.logger.DLogger
import com.epam.drill.pluginConfig
import jvmapi.GetMethodID
import jvmapi.NewGlobalRef
import jvmapi.NewObjectA
import jvmapi.NewStringUTF
import jvmapi.jobject
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap

val plLogger
    get() = DLogger("plLogger")

@ExperimentalUnsignedTypes
suspend fun loadPlugin(pluginFile: DrillPluginFile) {
    AttachNativeThreadToJvm()
    pluginFile.addPluginsToSystemClassLoader()
    try {
        val pluginConfig = pluginFile.pluginConfig()
        val pluginApiClass = pluginFile.retrievePluginApiClass()
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
                plLogger.warn { "Can't load the plugin file ${pluginFile.absolutePath}. Error: ${ex.message}" }
            else -> plLogger.error { "something terrible happened at the time of processing of ${pluginFile.absolutePath} jar... Error: ${ex.message} ${ex.printStackTrace()}" }
        }
    }
}

