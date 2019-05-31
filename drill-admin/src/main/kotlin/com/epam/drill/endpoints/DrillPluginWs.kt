@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.dataclasses.JsonMessage
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.storage.CassandraConnector
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class DrillPluginWs(override val kodein: Kodein) : KodeinAware, WsService {
    private val logger = LoggerFactory.getLogger(DrillPluginWs::class.java)
    private val app: Application by instance()
    private val cc: CassandraConnector by instance()
    private val sessionStorage: ConcurrentMap<String, MutableSet<DefaultWebSocketServerSession>> = ConcurrentHashMap()
    private val agentManager: AgentManager by instance()

    override fun getPlWsSession(): Set<String> {
        return sessionStorage.keys
    }

    override suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String) {
        val messageForSend = Gson().toJson(Message(MessageType.MESSAGE, destination, message))

        val cm = cc.addEntityManager(agentInfo.id)
        val message = JsonMessage(destination + ":" + agentInfo.buildVersion, messageForSend)
        cm.persist(message)

        println("PLUGIN MEASSAGE: $messageForSend")

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

    override fun storeData(agentId: String, obj: Any) {
        val cm = cc.addEntityManager(agentId)
        cm.persist(obj)
    }

    override fun getEntityBy(agentId: String, clazz: Class<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    init {
        app.routing {
            webSocket("/ws/drill-plugin-socket") {
                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val event = Message::class fromJson frame.readText()
                                ?: closeConnection(CloseReason.Codes.PROTOCOL_ERROR, "wrong input")
                            when (event.type) {
                                MessageType.SUBSCRIBE -> {
                                    val subscribeInfo = SubscribeInfo::class fromJson event.message ?: closeConnection(
                                        CloseReason.Codes.PROTOCOL_ERROR,
                                        "wrong subs info"
                                    )
                                    saveSession(event)
                                    val buildVersion = subscribeInfo.buildVersion

                                    val cm = cc.addEntityManager(subscribeInfo.agentId)
                                    val message = cm.find(
                                        JsonMessage::class.java,
                                        event.destination + ":" + (if (buildVersion.isNullOrEmpty()) {
                                            (agentManager.self(subscribeInfo.agentId))//?.buildVersion
                                        } else buildVersion)
                                    ).message

                                    if (message.isNullOrEmpty()) {
                                        this.send(
                                            Message(
                                                MessageType.MESSAGE,
                                                event.destination,
                                                ""
                                            ).textFrame()
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

    private suspend fun DefaultWebSocketServerSession.closeConnection(
        reason: CloseReason.Codes,
        message: String
    ): Nothing {
        this.close(CloseReason(reason, message))
        throw java.lang.RuntimeException()
    }

    private fun DefaultWebSocketServerSession.saveSession(event: Message) {
        val sessionSet = sessionStorage.getOrPut(event.destination) {
            Collections.newSetFromMap(ConcurrentHashMap())
        }
        sessionSet.add(this)
    }

}

data class SubscribeInfo(val agentId: String, val buildVersion: String? = null)
