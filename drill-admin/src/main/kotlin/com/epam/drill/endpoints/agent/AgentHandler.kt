@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints.agent

import com.epam.drill.common.AgentConfig
import com.epam.drill.common.AgentConfigParam
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.NeedSyncParam
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.endpoints.fromJson
import com.epam.drill.endpoints.plugin.PluginDispatcher
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.loads
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory

class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()
    private val pd: PluginDispatcher by kodein.instance()

    private val agLog = LoggerFactory.getLogger(AgentHandler::class.java)

    init {
        app.routing {
            webSocket("/agent/attach") {

                val agentConfig = Cbor.loads(
                    AgentConfig.serializer(), call.request.headers[AgentConfigParam]!!
                )

                val agentInfo = agentManager.agentConfiguration(agentConfig.id, agentConfig.buildVersion)
                agentInfo.ipAddress = call.request.local.remoteHost

                agentManager.put(agentInfo, this)

                println("Agent registered")
                agLog.info("Agent WS is connected. Client's address is ${call.request.local.remoteHost}")

                if (call.request.headers[NeedSyncParam]!!.toBoolean()) {
                    agentManager.updateAgentConfig(agentInfo)
                }

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val message = Message::class fromJson frame.readText() ?: return@webSocket
                            when (message.type) {
                                MessageType.PLUGIN_DATA -> {
                                    agLog.debug(message.message)
                                    pd.processPluginData(message.message, agentInfo)
                                }
                                MessageType.DEBUG -> {
//                                    send(frame)
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
                    agLog.error("agentDisconnected!")
                    agentManager.remove(agentInfo)
                }

            }
        }
    }
}