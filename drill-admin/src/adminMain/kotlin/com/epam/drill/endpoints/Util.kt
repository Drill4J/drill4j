package com.epam.drill.endpoints

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.google.gson.Gson
import io.ktor.http.cio.websocket.Frame
import kotlin.reflect.KClass

fun agentWsMessage(destination: String, message: Any): Frame.Text {
    val toJson = Gson().toJson(
        Message(MessageType.MESSAGE, destination, if (message is String) message else Gson().toJson(message))
    )
    println(toJson)
    return Frame.Text(toJson)
}

inline infix fun <reified T : Any> KClass<T>.fromJson(json: String) = Gson().fromJson(json, this.java)
