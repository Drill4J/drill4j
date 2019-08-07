package com.epam.drill.endpoints


import com.epam.drill.agentmanager.*
import com.epam.drill.common.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import com.epam.drill.storage.*
import io.ktor.application.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.kodein.di.*
import org.kodein.di.generic.*


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

                topic<WsRoutes.GetAgent> { payload, _ ->
                    agentManager[payload.agentId]?.toAgentInfoWebSocket()
                }

                topic<WsRoutes.GetAgentBuilds> { payload, _ ->
                    agentManager[payload.agentId]?.buildVersions
                }

                topic<WsRoutes.GetAllPlugins> { _, _ ->
                    plugins.map { (_, dp) -> dp.pluginBean }
                        .toAllPluginsWebSocket(agentManager.agentStorage.values.map { it.agent }.toMutableSet())
                }

                topic<WsRoutes.GetPluginInfo> { payload, _ ->
                    val installedPluginBeanIds = agentManager
                        .getAllInstalledPluginBeanIds(payload.agentId)
                    plugins.getAllPluginBeans().map { plug ->
                        val pluginWebSocket = plug.toPluginWebSocket()
                        if (plug partOf installedPluginBeanIds) {
                            pluginWebSocket.relation = "Installed"
                        }
                        pluginWebSocket
                    }
                }

                topic<WsRoutes.GetPluginConfig> { payload, _ ->
                    agentManager[payload.agent]?.plugins?.find { it.id == payload.plugin }?.config
                }
            }

        }
    }

}