package com.epam.drill.endpoints


import com.epam.drill.agentmanager.AgentInfoWebSocket
import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.agentmanager.toAgentInfoWebSocket
import com.epam.drill.agentmanager.toAgentInfosWebSocket
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.AgentInfoDb
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.stringify
import com.epam.drill.dataclasses.toAgentBuildVersionJson
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.pluginBean
import com.epam.drill.plugins.toAllPluginsWebSocket
import com.epam.drill.router.WsRoutes
import com.epam.drill.storage.add
import com.epam.drill.storage.remove
import com.epam.drill.storage.update
import io.ktor.application.Application
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.list
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance


class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val agentManager: AgentManager by instance()
    private val plugins: Plugins by instance()
    private val app: Application by instance()
    private val sessionStorage: MutableSet<DrillWsSession> by instance()

    init {

        runBlocking {
            agentManager.agentStorage.onUpdate += update(mutableSetOf()) { storage ->
                val destination = app.toLocation(WsRoutes.GetAllAgents())
                sessionStorage.sendTo(

                    Message(
                        MessageType.MESSAGE, destination,
                        AgentInfoWebSocket.serializer().list stringify storage.values.map { it.agent }.sortedWith(
                            compareBy(AgentInfo::id)
                        ).toMutableSet().toAgentInfosWebSocket()
                    )


                )

            }
            agentManager.agentStorage.onAdd += add(mutableSetOf()) { k, v ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination)) {
                    sessionStorage.sendTo(
                        Message(
                            MessageType.MESSAGE,
                            destination,
                            AgentInfoWebSocketSingle.serializer() stringify v.agent.toAgentInfoWebSocket()
                        )
                    )
                }
            }

            agentManager.agentStorage.onRemove += remove(mutableSetOf()) { k ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(Message(MessageType.DELETE, destination, ""))
            }

            wsTopic {
                topic<WsRoutes.GetAllAgents> { _, _ ->
                    agentManager.agentStorage.values.map { it.agent }.sortedWith(compareBy(AgentInfo::id))
                        .toMutableSet()
                        .toAgentInfosWebSocket()

                }

                topic<WsRoutes.GetAgent> { x, _ ->
                    agentManager[x.agentId]?.toAgentInfoWebSocket()
                }

                topic<WsRoutes.GetAgentBuilds> { agent, _ ->
                    agentManager[agent.agentId]?.let {
                        transaction {
                            AgentInfoDb.findById(agent.agentId)?.buildVersions?.map {
                                it.toAgentBuildVersionJson()
                            }?.toList()
                        }
                    }
                }

                topic<WsRoutes.GetAllPlugins> { _, _ ->
                    plugins.map { (_, dp) -> dp.pluginBean }
                        .toAllPluginsWebSocket(agentManager.agentStorage.values.map { it.agent }.toMutableSet())
                }
            }

        }
    }

}