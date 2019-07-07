package com.epam.drill.core.plugin.loader

import com.epam.drill.common.PluginBean
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.jvmapi.jniName
import com.epam.drill.jvmapi.jniParamName
import com.epam.drill.logger.DLogger
import com.epam.drill.plugin.api.processing.AgentPart
import com.epam.drill.plugin.api.processing.PluginRepresenter
import com.epam.drill.plugin.api.processing.UnloadReason
import jvmapi.CallVoidMethod
import jvmapi.CallVoidMethodA
import jvmapi.FindClass
import jvmapi.GetMethodID
import jvmapi.GetStaticFieldID
import jvmapi.GetStaticObjectField
import jvmapi.NewStringUTF
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap

@Suppress("LeakingThis")
open class GenericNativePlugin(
    val pluginApiClass: jclass,
    val userPlugin: jobject,
    pluginConfig: PluginBean
) : PluginRepresenter() {
    private val pluginLogger = DLogger("GenericNativePlugin")

    init {
        updateRawConfig(pluginConfig)
        javaEnabled(pluginConfig.enabled)
        val agentIsEnabled = true
        load(pluginConfig.enabled && agentIsEnabled)
    }

    override suspend fun doRawAction(rawAction: String) {
        CallVoidMethod(
            userPlugin,
            GetMethodID(pluginApiClass, "doRawActionBlocking", "(Ljava/lang/String;)V"),
            NewStringUTF(rawAction)
        )
    }

    override val id: String = pluginConfig.id

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


    override fun on() {
        pluginLogger.debug { "on" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*, *>::on.name, "()V"), null
        )
        np?.on()
    }

    override fun off() {
        pluginLogger.debug { "off" }
        CallVoidMethodA(
            userPlugin, GetMethodID(pluginApiClass, AgentPart<*, *>::off.name, "()V"), null
        )
        np?.off()
    }


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
