package com.epam.drill.endpoints


import com.epam.drill.agentmanager.byId
import com.epam.drill.agentmanager.toAgentInfoWebSocket
import com.epam.drill.agentmanager.toAgentInfosWebSocket
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.toAllPluginsWebSocket
import com.epam.drill.router.WsRoutes
import com.epam.drill.storage.CassandraConnector
import io.ktor.application.Application
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance


class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val agentManager: AgentManager by instance()
    private val plugins: Plugins by instance()
    private val app: Application by instance()
    private val cc: CassandraConnector by instance()
    private val sessionStorage: MutableSet<DrillWsSession> by instance()

    init {

        runBlocking {
            agentManager.agentStorage.onUpdate += update(mutableSetOf()) {
                val destination = app.toLocation(WsRoutes.GetAllAgents())
                sessionStorage.sendTo(
                    it.values.map { it.first }.sortedWith(compareBy(AgentInfo::id)).toMutableSet().toAgentInfosWebSocket().messageEvent(
                        destination
                    )
                )
            }
            agentManager.agentStorage.onAdd += add(mutableSetOf()) { k, v ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(v.first.toAgentInfoWebSocket().messageEvent(destination))
            }

            agentManager.agentStorage.onRemove += remove(mutableSetOf()) { k ->
                val destination = app.toLocation(WsRoutes.GetAgent(k))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(Message(MessageType.DELETE, destination, ""))
            }

            wsTopic {
                topic<WsRoutes.GetAllAgents> { _, _ ->
                    agentManager.agentStorage.values.map { it.first }.sortedWith(compareBy(AgentInfo::id))
                        .toMutableSet()
                        .toAgentInfosWebSocket()

                }

                topic<WsRoutes.GetAgent> { x, _ ->
                    agentManager.agentStorage.byId(x.agentId)?.toAgentInfoWebSocket()
                }

                topic<WsRoutes.GetAgentBuilds> { agent, _ ->
                    agentManager.agentStorage.byId(agent.agentId)?.let { agInfo ->
                        val cm = cc.addEntityManager(agInfo.id)
                        val q = cm.createQuery("Select a from AgentBuildVersion a")
                        val results = q.getResultList()
                        val versions = results as List<AgentBuildVersion>
                        println(versions.stringify())
                        if (versions.isEmpty()) null
                        else versions
                    }
                }

                topic<WsRoutes.GetAllPlugins> { _, _ ->
                    plugins.pluginBeans.toAllPluginsWebSocket(agentManager.agentStorage.values.map { it.first }.toMutableSet())
                }
            }

        }
    }

}