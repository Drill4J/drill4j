@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.DrillAgent
import com.epam.drill.common.*
import com.epam.drill.common.util.DJSON
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import kotlin.reflect.KClass


class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentStorage: AgentStorage by instance()
    private val pd: PluginDispatcher by kodein.instance()


    init {
        app.routing {
            webSocket("/agent/attach") {
                //fixme log
                println("Agent connected")
                var agentId: String? = null
                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val readText = frame.readText()
                            if (readText.isEmpty())
                                return@webSocket
                            val message =
                                Gson().fromJson<Message>(readText, Message::class.javaObjectType) ?: return@webSocket
                            when (message.type) {
                                MessageType.AGENT_REGISTER -> {
                                    val jsonInString = message.message
                                    val agentInfo =
                                        DJSON.parse(jsonInString, AgentInfo::class as KClass<Any>) as AgentInfo
                                    agentId = agentInfo.agentAddress
                                    val drillAgent = DrillAgent(agentInfo, agentStorage, this)
                                    agentStorage.addAgent(drillAgent)
                                    send(
                                        Gson().toJson(
                                            Message(
                                                MessageType.MESSAGE,
                                                "/",
                                                Gson().toJson(AgentEvent(DrillEvent.AGENT_LOAD_SUCCESSFULLY))
                                            )
                                        )
                                    )
                                    println("agent registered.")
                                    println("AgentInfo: $agentInfo")

                                }
                                MessageType.PLUGIN_DATA -> {
                                    logger.info(message.message)
                                    pd.processPluginData(message.message)
                                }
                                else -> {
                                    //fixme log
//                                    logWarn("Not implemented YET!!")
                                }
                            }

                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
//                    fixme log
//                    logError(ex)
                } finally {
                    if (agentId != null)
                        agentStorage.removeAgent(agentId!!)
                }

            }
        }
    }
}
