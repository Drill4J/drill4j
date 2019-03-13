@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints

import com.epam.drill.AgentStorage
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
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


class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentStorage: AgentStorage by instance()
    private val pd: PluginDispatcher by kodein.instance()
    private val agLog = LoggerFactory.getLogger(AgentHandler::class.java)

    init {
        app.routing {
            webSocket("/agent/attach") {

                agLog.info("Agent WS is connected. Client's address is ${call.request.local.remoteHost}")
                var agentInfo: AgentInfo? = null
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
                                    agentInfo = AgentInfo::class.fromJson(message.message)
                                    agentStorage.put(agentInfo!!, this)
                                    send(agentWsMessage("/plugins/agent-attached", ""))
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
                } finally {
                    if (agentInfo != null)
                        agentStorage.remove(agentInfo!!)
                }

            }
        }
    }
}
