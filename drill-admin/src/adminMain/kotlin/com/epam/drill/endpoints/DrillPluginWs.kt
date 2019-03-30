@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.plugin.api.end.WsService
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
import org.litote.kmongo.eq
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

val logger = LoggerFactory.getLogger(DrillPluginWs::class.java)

class DrillPluginWs(override val kodein: Kodein) : KodeinAware, WsService {
    override fun getPlWsSession(): Set<String> {
        return sessionStorage.keys
    }

    override suspend fun convertAndSend(destination: String, message: Any) {

        sessionStorage[destination]?.apply {
            //            val ogs = message as SeqMessage
//            val messageAsString = getMessageForSocket(ogs)
            if (message is String) {
                val iterator = this.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    try {
                        next.send(Frame.Text(Gson().toJson(Message(MessageType.MESSAGE, destination, message))))
                    } catch (ex: Exception) {
                        iterator.remove()
                        //fixme log
//                    logDebug("Old WS session was removed")
                    }
                }
            }
        }

    }

    private fun getMessageForSocket(ogs: SeqMessage): String {
        val content = ogs.drillMessage.content
        return try {
            val map: Map<*, *>? = ObjectMapper().readValue(content, Map::class.java)
            val hashMap = HashMap<Any, Any>(map)
            hashMap["id"] = ogs.id ?: ""
            Gson().toJson(hashMap)
        } catch (ignored: Exception) {
            content ?: ""
        }
    }

    private val app: Application by instance()
    private val mc: MongoClient by instance()
    private val sessionStorage: MutableMap<String, MutableSet<DefaultWebSocketServerSession>> = ConcurrentHashMap()

    init {
        app.routing {

            webSocket("/ws/drill-plugin-socket") {

                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            val event = Gson().fromJson(frame.readText(), Message::class.java)
                            when (event.type) {
                                MessageType.SUBSCRIBE -> {
                                    if (sessionStorage[event.destination] == null) {
                                        sessionStorage[event.destination] = mutableSetOf(this)
                                    }
                                    sessionStorage[event.destination]?.add(this)
//                                    logDebug("${event.destination} was subscribe")

                                    val objects =
                                        mc.client!!.getDatabase("test").getCollection<SeqMessage>(event.destination)
                                    for (ogs in objects.find()) {
                                        val message = getMessageForSocket(ogs)
                                        this.send(
                                            Frame.Text(
                                                Gson().toJson(
                                                    Message(
                                                        MessageType.MESSAGE,
                                                        event.destination,
                                                        message
                                                    )
                                                )
                                            )
                                        )
                                    }


                                }
                                MessageType.MESSAGE -> {
                                }
                                MessageType.DELETE -> {
                                    val id = event.message
                                    val collection =
                                        mc.client!!.getDatabase("test").getCollection<SeqMessage>(event.destination)
                                    val deleteOneById = collection.deleteOne(SeqMessage::id eq id)
                                    println(deleteOneById)
                                    if (deleteOneById.deletedCount > 0) {
                                        this.send(
                                            Frame.Text(
                                                Gson().toJson(
                                                    Message(
                                                        MessageType.DELETE,
                                                        event.destination,
                                                        id
                                                    )
                                                )
                                            )
                                        )
                                    }
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
