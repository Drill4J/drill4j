package com.epam.drill.endpoints


import com.epam.drill.agentmanager.AgentStorage
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
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table


class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val agentStorage: AgentStorage by instance()
    private val plugins: Plugins by instance()
    private val app: Application by instance()
    private val cc: CassandraConnector by instance()
    private val sessionStorage: MutableSet<DrillWsSession> by instance()

    init {

        runBlocking {
            agentStorage.onUpdate += update(mutableSetOf()) {
                val destination = app.toLocation(WsRoutes.GetAllAgents())
                sessionStorage.sendTo(
                    it.keys.sortedWith(compareBy(AgentInfo::id)).toMutableSet().toAgentInfosWebSocket().messageEvent(
                        destination
                    )
                )
            }
            agentStorage.onAdd += add(mutableSetOf()) { k, _ ->
                val destination = app.toLocation(WsRoutes.GetAgent(k.id))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(k.toAgentInfoWebSocket().messageEvent(destination))
            }

            agentStorage.onRemove += remove(mutableSetOf()) { k ->
                val destination = app.toLocation(WsRoutes.GetAgent(k.id))
                if (sessionStorage.exists(destination))
                    sessionStorage.sendTo(Message(MessageType.DELETE, destination, ""))
            }

            wsTopic {
                topic<WsRoutes.GetAllAgents> { _, _ ->
                    agentStorage.keys.sortedWith(compareBy(AgentInfo::id)).toMutableSet().toAgentInfosWebSocket()

                }

                topic<WsRoutes.GetAgent> { x, _ ->
                    agentStorage.byId(x.agentId)?.toAgentInfoWebSocket()
                }

                topic<WsRoutes.GetAgentBuilds> { agent, _ ->
                    agentStorage.byId(agent.agentId)?.let {agInfo->
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
                    plugins.pluginBeans.toAllPluginsWebSocket(agentStorage.keys)
                }
            }

        }
    }

}