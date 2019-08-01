@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints.agent

import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.swagger.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import org.kodein.di.*
import org.kodein.di.generic.*


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
                        val event = Message.serializer() parse json
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

    @UseExperimental(ImplicitReflectionSerializer::class)
    private suspend fun subscribe(
        wsSession: DrillWsSession,
        event: Message
    ) {
        sessionStorage += (wsSession)
        println("${event.destination} is subscribed")
        app.run {
            wsTopic {
                val resolve = resolve(event.destination, sessionStorage)!!
                if (resolve is Collection<*>) {
                    sessionStorage.sendTo(
                        Message(
                            MessageType.MESSAGE,
                            event.destination,
                            Json.stringify(resolve)
                        )
                    )
                } else {
                    val message = if (resolve !is String) {
                        @Suppress("UNCHECKED_CAST")
                        resolve::class.serializer() as KSerializer<Any> stringify resolve
                    } else resolve
                    sessionStorage.sendTo(
                        Message(
                            MessageType.MESSAGE,
                            event.destination,
                            message
                        )
                    )
                }
            }
        }
    }


}

