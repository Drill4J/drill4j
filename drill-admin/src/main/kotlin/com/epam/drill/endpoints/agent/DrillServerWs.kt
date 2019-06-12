@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.endpoints.agent

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.endpoints.DrillWsSession
import com.epam.drill.endpoints.WsTopic
import com.epam.drill.endpoints.removeTopic
import com.epam.drill.endpoints.sendTo
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.swagger.Json
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
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
                    @Suppress("UNCHECKED_CAST") val serializer = resolve::class.serializer() as KSerializer<Any>
                    sessionStorage.sendTo(
                        Message(
                            MessageType.MESSAGE,
                            event.destination,
                            serializer stringify resolve
                        )
                    )
                }
            }
        }
    }


}

