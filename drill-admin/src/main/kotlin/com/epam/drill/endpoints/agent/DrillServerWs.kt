@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints.agent

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.endpoints.DrillWsSession
import com.epam.drill.endpoints.WsTopic
import com.epam.drill.endpoints.messageEvent
import com.epam.drill.endpoints.removeTopic
import com.epam.drill.endpoints.sendTo
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance


class DrillServerWs(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val wsTopic: WsTopic by instance()
    private val sessionStorage: MutableSet<DrillWsSession> by instance()

    init {

        app.routing {
            webSocket("/ws/drill-admin-socket") {
                val rawWsSession = this
                try {
                    incoming.consumeEach { frame ->

                        val json = (frame as Frame.Text).readText()
                        val event = Gson().fromJson(json, Message::class.java)
                        when (event.type) {
                            MessageType.SUBSCRIBE -> {
                                val wsSession = DrillWsSession(event.destination, rawWsSession)
                                subscribe(wsSession, event)
                            }
                            MessageType.MESSAGE -> {
                                TODO("NOT IMPLEMENTED YET")
                            }
                            MessageType.UNSUBSCRIBE -> {
                                sessionStorage.removeTopic(event.destination)
                            }

                            else -> {
                            }

                        }
                    }
                } catch (ex: Throwable) {
                    println("Session was removed")
                    sessionStorage.remove(rawWsSession)
                }

            }
        }
    }

    private suspend fun subscribe(
        wsSession: DrillWsSession,
        event: Message
    ) {
        sessionStorage += (wsSession)
        println("${event.destination} is subscribed")
        app.run {
            wsTopic {
                sessionStorage.sendTo(
                    resolve(event.destination, sessionStorage)!!
                        .messageEvent(event.destination)
                )
            }
        }
    }


}

