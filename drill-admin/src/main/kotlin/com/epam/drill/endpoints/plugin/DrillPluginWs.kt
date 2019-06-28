@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints.plugin

import com.epam.drill.cache.CacheService
import com.epam.drill.cache.type.Cache
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.endpoints.textFrame
import com.epam.drill.plugin.api.end.WsService
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val logger = KotlinLogging.logger {}

class DrillPluginWs(override val kodein: Kodein) : KodeinAware, WsService {

    private val pluginStorage: ConcurrentMap<String, Any> = ConcurrentHashMap()
    private val app: Application by instance()
    private val cacheService: CacheService by instance()
    private val eventStorage: Cache<String, String> by cacheService
    private val sessionStorage: ConcurrentMap<String, MutableSet<DefaultWebSocketServerSession>> = ConcurrentHashMap()


    override fun getPlWsSession(): Set<String> {
        return sessionStorage.keys
    }

    override suspend fun convertAndSend(agentInfo: AgentInfo, destination: String, message: String) {
        val messageForSend = Message.serializer() stringify Message(MessageType.MESSAGE, destination, message)

        val id = destination + ":" + agentInfo.buildVersion
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

    override fun storeData(key: String, obj: Any) {
        pluginStorage[key] = obj
    }

    override fun retrieveData(key: String) = pluginStorage[key]

    override fun getEntityBy(agentId: String, clazz: Class<Any>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

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
                                        eventStorage[(event.destination + ":" + (if (buildVersion.isNullOrEmpty()) {
                                            subscribeInfo.agentId
                                        } else buildVersion))]

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
