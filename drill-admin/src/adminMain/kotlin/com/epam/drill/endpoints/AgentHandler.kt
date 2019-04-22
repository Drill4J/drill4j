@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.common.AgentBuildVersion
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.storage.MongoClient
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory


class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val mc: MongoClient by instance()
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
                            val message = Message::class fromJson frame.readText() ?: return@webSocket
                            when (message.type) {
                                MessageType.AGENT_REGISTER -> {
                                    agentInfo = AgentInfo::class fromJson message.message
                                    agentStorage.put(agentInfo!!, this)
                                    send(agentWsMessage("/plugins/agent-attached", ""))
                                    val collection =
                                        mc.client!!
                                        .getDatabase(agentInfo!!.id)
                                        .getCollection<AgentBuildVersion>("agent-build-versions")
                                    val buildVersion = AgentBuildVersion(agentInfo!!.buildVersion)
                                    val buildVersions = collection.find()
                                    if (!buildVersions.any { it == buildVersion })
                                        collection.insertOne(buildVersion)

                                    println("Build version's count: ${collection.find().count()}")
                                    println("Agent registered")
                                }
                                MessageType.PLUGIN_DATA -> {
                                    agLog.info(message.message)
                                    pd.processPluginData(message.message, agentInfo!!)
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
