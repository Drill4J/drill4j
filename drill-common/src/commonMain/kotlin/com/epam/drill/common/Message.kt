package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class Message(var type: MessageType, var destination: WsUrl = "", var message: String = "")

typealias WsUrl = String