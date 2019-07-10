@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints.plugin

import com.epam.drill.cache.*
import com.epam.drill.cache.type.*
import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.plugin.api.end.*
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import mu.*
import org.kodein.di.*
import org.kodein.di.generic.*
import java.util.*
import java.util.concurrent.*

private val logger = KotlinLogging.logger {}

class DrillPluginWs(override val kodein: Kodein) : KodeinAware, Sender {

    private val app: Application by instance()
    private val cacheService: CacheService by instance()
    private val eventStorage: Cache<String, String> by cacheService
    private val sessionStorage: ConcurrentMap<String, MutableSet<DefaultWebSocketServerSession>> = ConcurrentHashMap()


    override suspend fun send(agentInfo: AgentInfo, destination: String, message: String) {
        val messageForSend = Message.serializer() stringify Message(MessageType.MESSAGE, destination, message)

        val id = "${agentInfo.id}:$destination:${agentInfo.buildVersion}"
        logger.debug { "send data to $id destination" }
        eventStorage[id] = messageForSend

        sessionStorage[destination]?.let { sessionSet ->
            for (session in sessionSet) {
                try {
                    session.send(Frame.Text(messageForSend))
                } catch (ex: Exception) {
                    sessionSet.remove(session)
                }
            }
        }
    }

    init {
        app.routing {
            webSocket("/ws/drill-plugin-socket") {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val event = Message.serializer() parse frame.readText()
                            when (event.type) {
                                MessageType.SUBSCRIBE -> {
                                    val subscribeInfo = SubscribeInfo.serializer() parse event.message
                                    saveSession(event)
                                    val buildVersion = subscribeInfo.buildVersion


                                    val message =
                                        eventStorage[
                                                subscribeInfo.agentId + ":" +
                                                        event.destination + ":" +
                                                        if (buildVersion.isNullOrEmpty()) subscribeInfo.agentId
                                                        else buildVersion
                                        ]

                                    if (message.isNullOrEmpty()) {
                                        this.send(
                                            (Message.serializer() stringify
                                                    Message(
                                                        MessageType.MESSAGE,
                                                        event.destination,
                                                        ""
                                                    )).textFrame()
                                        )
                                    } else this.send(Frame.Text(message))

                                }
                                MessageType.UNSUBSCRIBE -> {
                                    sessionStorage[event.destination]?.let {
                                        it.removeIf { ses -> ses == this }
                                    }
                                }
                                else -> {
                                    close(RuntimeException("Event '${event.type}' is not implemented yet"))
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun DefaultWebSocketServerSession.saveSession(event: Message) {
        val sessionSet = sessionStorage.getOrPut(event.destination) {
            Collections.newSetFromMap(ConcurrentHashMap())
        }
        sessionSet.add(this)
    }

}

@Serializable
data class SubscribeInfo(val agentId: String, val buildVersion: String? = null)
