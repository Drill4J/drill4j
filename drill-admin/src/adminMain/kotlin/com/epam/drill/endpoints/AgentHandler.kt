@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.DrillAgent
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.util.DJSON
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
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass


class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentStorage: AgentStorage by instance()
    private val pd: PluginDispatcher by kodein.instance()
    private val agLog = LoggerFactory.getLogger(AgentHandler::class.java)


    init {
        app.routing {
            webSocket("/agent/attach") {

                agLog.info("Agent WS is connected. Client's address is ${call.request.local.remoteHost}")
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
                                    send(agentWsMessage("/plugins/agent-attached", ""))
                                    agLog.info("agent registered.")
                                    agLog.info("AgentInfo: $agentInfo")

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
