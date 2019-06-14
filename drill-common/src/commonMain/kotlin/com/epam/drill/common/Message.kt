package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class Message(var type: MessageType, var destination: WsUrl = "", var message: String = "")

typealias WsUrl = String


@Serializable
data class PluginMessage(
    val event: DrillEvent,
    val pluginFile: PluginFileBytes = emptyList(),
    val nativePart: NativePlugin? = null,
    val pl: PluginBean
)

typealias PluginFileBytes = List<Byte>

@Serializable
data class NativePlugin(
    val windowsPlugin: PluginFileBytes = emptyList(),
    val linuxPluginFileBytes: PluginFileBytes = emptyList()
)

