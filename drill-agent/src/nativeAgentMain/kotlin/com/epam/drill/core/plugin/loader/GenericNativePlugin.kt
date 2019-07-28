package com.epam.drill.core.plugin.loader

import com.epam.drill.common.*
import com.epam.drill.core.plugin.*
import com.epam.drill.jvmapi.*
import com.epam.drill.jvmapi.gen.*
import com.epam.drill.logger.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*

@Suppress("LeakingThis")
open class GenericNativePlugin(
    val pluginApiClass: jclass,
    val userPlugin: jobject,
    pluginConfig: PluginBean
) : PluginRepresenter() {
    private val pluginLogger = DLogger("GenericNativePlugin")

    init {
        updateRawConfig(pluginConfig.toPluginConfig())
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

    override fun updateRawConfig(config: PluginConfig) {
        notifyJavaPart(config)
    }

    private fun notifyJavaPart(config: PluginConfig) {
        CallVoidMethodA(
            userPlugin,
            GetMethodID(pluginApiClass, AgentPart<*, *>::updateRawConfig.name, "(Ljava/lang/String;)V"),
            nativeHeap.allocArray(1.toLong()) {
                val newStringUTF =
                    NewStringUTF(config.data)
                l = newStringUTF
            })
    }
}
