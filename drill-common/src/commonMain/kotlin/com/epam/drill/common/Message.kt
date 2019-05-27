package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class Message(var type: MessageType, var destination: WsUrl = "", var message: String = "")

typealias WsUrl = String


@Serializable
data class PluginMessage(
    val event: DrillEvent,
    val pluginName: String,
    val pluginFile: PluginFileBytes = emptyList(),
    val pl: PluginBean? = null,
    val counString: String = ""
)

typealias PluginFileBytes = List<Byte>


