package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.common.PluginBean
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.hasNativePart
import com.epam.drill.jvmapi.jniName
import com.epam.drill.jvmapi.jniParamName
import com.epam.drill.logger.DLogger
import com.epam.drill.nativePart
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.PluginRepresenter
import com.epam.drill.plugin.api.processing.UnloadReason
import com.epam.drill.pluginConfig
import com.epam.drill.pluginId
import jvmapi.CallVoidMethod
import jvmapi.CallVoidMethodA
import jvmapi.FindClass
import jvmapi.GetMethodID
import jvmapi.GetStaticFieldID
import jvmapi.GetStaticObjectField
import jvmapi.NewGlobalRef
import jvmapi.NewObjectA
import jvmapi.NewStringUTF
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap

@ExperimentalUnsignedTypes
open class GenericNativePlugin(private val pf: DrillPluginFile) : PluginRepresenter() {
    override fun doRawAction(action: String) {
        CallVoidMethod(
            userPlugin,
            GetMethodID(pluginApiClass, ::doRawAction.name, "(Ljava/lang/String;)V"),
            NewStringUTF(action)
        )
    }

    private val natPluginLogger
        get() = DLogger("NativePluginController")

    lateinit var pluginApiClass: jclass
    private lateinit var pluginConfig: PluginBean
    override val id: String = pf.pluginId()

    override suspend fun isEnabled() = pluginConfigById(id).enabled

    override suspend fun setEnabled(value: Boolean) {
        javaEnabled(value)
        if (value) on() else off()
        val pluginConfigById = pluginConfigById(id)
        pluginConfigById.enabled = value

    }

    private fun javaEnabled(value: Boolean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, "setEnabled", "(Z)V"),
            nativeHeap.allocArray(1.toLong()) {
                z = if (value) 1.toUByte() else 0.toUByte()
            })
    }

    val userPlugin: jobject? by lazy {
        NewGlobalRef(
            NewObjectA(
                pluginApiClass,
                GetMethodID(pluginApiClass, "<init>", "(Ljava/lang/String;)V"),
                nativeHeap.allocArray(1.toLong()) {
                    l = NewStringUTF(id)
                })
        )
    }

    open suspend fun connect() {
        pluginApiClass = pf.retrievePluginApiClass()
        pluginConfig = pf.pluginConfig()
        updateRawConfig(pluginConfig)
        javaEnabled(pluginConfig.enabled)
        fullLoad(pf)
    }

    override fun on() {
        natPluginLogger.debug { "on" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*, *>::on.name, "()V"), null
        )
        np?.on()
    }

    override fun off() {
        natPluginLogger.debug { "off" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*, *>::off.name, "()V"), null
        )
        np?.off()
    }


    @ExperimentalUnsignedTypes
    override fun load(onImmediately: Boolean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*, *>::load.name, "(Z)V"),
            nativeHeap.allocArray(1.toLong()) {
                z = if (onImmediately) 1.toUByte() else 0.toUByte()
            })

    }

    override fun unload(unloadReason: UnloadReason) = memScoped<Unit> {
        val findClass = FindClass(UnloadReason::class.jniName())
        val getStaticFieldID =
            GetStaticFieldID(findClass, unloadReason.name, UnloadReason::class.jniParamName())
        val getStaticObjectField = GetStaticObjectField(findClass, getStaticFieldID)
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*, *>::unload.name, "(${UnloadReason::class.jniParamName()})V"),
            allocArray(1.toLong()) {
                l = getStaticObjectField
            }
        )
        np?.unload(unloadReason)
    }

    @ExperimentalUnsignedTypes
    suspend fun fullLoad(jar: DrillPluginFile) {
        //fixme move to global?
        val agentIsEnabled = true
        load(pluginConfig.enabled && agentIsEnabled)
        initNativePart(jar)
        try {
            np?.updateRawConfig(pluginConfig)
        } catch (ex: Exception) {
            natPluginLogger.error { "Can't update the config for $id. Config: $pluginConfig" }
        }
        try {
            np?.load(pluginConfig.enabled && agentIsEnabled)
        } catch (ex: Exception) {
            natPluginLogger.error { "Can't Load the native part for $id. Immedeatly: ${pluginConfig.enabled && agentIsEnabled}" }
        }
    }

    private suspend fun initNativePart(jar: DrillPluginFile) {
        if (jar.hasNativePart())
            CallVoidMethodA(
                userPlugin,
                GetMethodID(pluginApiClass, "init", "(Ljava/lang/String;)V"),
                nativeHeap.allocArray(1.toLong()) {

                    val newStringUTF =
                        NewStringUTF(jar.nativePart().absolutePath)
                    l = newStringUTF

                })
    }

    override fun updateRawConfig(config: PluginBean) {
        notifyJavaPart(config)
    }

    private fun notifyJavaPart(config: PluginBean) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*, *>::updateRawConfig.name, "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                val newStringUTF =
                    NewStringUTF(config.config)
                l = newStringUTF

            })
    }


}
