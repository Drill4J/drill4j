package com.epam.drill.endpoints

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.locations.locations
import io.ktor.websocket.DefaultWebSocketServerSession

fun Application.toLocation(rout: Any): String {
    return this.locations.href(rout)
}

suspend fun MutableSet<DrillWsSession>.sendTo(message: Message) {
    val iter = this.iterator()
    while (iter.hasNext()) {
        try {
            val it = iter.next()
            if (it.url == message.destination) {
                it.send(Frame.Text(message.stringify()))
            }
        } catch (ex: Exception) {
            iter.remove()
        }
    }
}

fun MutableSet<DrillWsSession>.exists(destination: String) = this.firstOrNull { it.url == destination } != null

fun MutableSet<DrillWsSession>.removeTopic(destination: String) {
    if (this.removeIf { it.url == destination })
        println("$destination unsubscribe")
}


fun Any?.stringify() = Gson().toJson(this) ?: ""
fun Any?.textFrame() = Frame.Text(this.stringify())
fun Any.messageEvent(destination: String) = Message(MessageType.MESSAGE, destination, this.stringify())

data class DrillWsSession(var url: String? = null, val sourceSession: DefaultWebSocketServerSession) :
    DefaultWebSocketServerSession by sourceSession

