@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.storage.MongoClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.litote.kmongo.getCollection
import java.util.concurrent.ConcurrentHashMap


class DrillServerWs(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val storage: AgentStorage by instance()
    val sessionStorage: MutableMap<String, MutableSet<DefaultWebSocketServerSession>> = ConcurrentHashMap()

    init {
        app.routing {
            webSocket("/drill-server-socket") {

                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val event = Gson().fromJson(frame.readText(), Message::class.java)
                            when (event.type) {
                                MessageType.REGISTER -> {
                                    if (sessionStorage[event.destination] == null) {
                                        sessionStorage[event.destination] = mutableSetOf(this)
                                    }
                                    sessionStorage[event.destination]?.add(this)
                                    val mapper = ObjectMapper()
                                    send(Frame.Text(Gson().toJson(Message(MessageType.MESSAGE, event.destination, mapper.writeValueAsString(storage.agents.values.map { it.agentInfo })))))

                                }
                                MessageType.MESSAGE -> {
                                }

                                else -> {
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}
