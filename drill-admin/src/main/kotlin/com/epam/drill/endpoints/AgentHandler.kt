@file:Suppress("EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")

package com.epam.drill.endpoints

import com.epam.drill.common.*
import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.dataclasses.AgentBuildVersions
import com.epam.drill.dataclasses.JsonMessages
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.agentPluginPart
import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.cbor.Cbor
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory

class AgentHandler(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()
    private val pd: PluginDispatcher by kodein.instance()
    private val plugins: Plugins by kodein.instance()

    private val agLog = LoggerFactory.getLogger(AgentHandler::class.java)

    init {
        Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PluginBeans, AgentInfos, ConnectedTable, AgentBuildVersions, JsonMessages)
        }

        app.routing {
            webSocket("/agent/attach") {
                val agentId = call.request.headers[AgentIdParam]!!

                val agentInfo = agentManager.agentConfiguration(agentId)
                agentInfo.ipAddress = call.request.local.remoteHost
                agentManager.put(agentInfo, this)

                transaction {
                    addLogger(StdOutSqlLogger)
                    AgentBuildVersion.findById(agentInfo.buildVersion)?.apply {
                        name = agentInfo.name
                    } ?: AgentBuildVersion.new(agentInfo.buildVersion) {
                        name = agentInfo.name
                    }
                }

                println("Agent registered")
                agLog.info("Agent WS is connected. Client's address is ${call.request.local.remoteHost}")

                if (call.request.headers[NeedSyncParam]!!.toBoolean()) {
                    send(
                        Frame.Binary(
                            false,
                            Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_STARTED, ""))
                        )
                    )
                    agentInfo.rawPluginNames.forEach { pb ->
                        val pluginId = pb.id
                        val agentPluginPart = plugins.plugins[pluginId]?.agentPluginPart!!
                        val pluginMessage =
                            PluginMessage(
                                DrillEvent.LOAD_PLUGIN,
                                pluginId,
                                agentPluginPart.readBytes().toList(),
                                pb,
                                "-"
                            )
                        send(Frame.Binary(false, Cbor.dump(PluginMessage.serializer(), pluginMessage)))
                    }


                    send(
                        Frame.Binary(
                            false,
                            Cbor.dump(PluginMessage.serializer(), PluginMessage(DrillEvent.SYNC_FINISHED, ""))
                        )
                    )
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
