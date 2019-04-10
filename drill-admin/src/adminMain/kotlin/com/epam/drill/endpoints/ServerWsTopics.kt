package com.epam.drill.endpoints


import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.byId
import com.epam.drill.agentmanager.toAgentInfoWebSocket
import com.epam.drill.agentmanager.toAgentInfosWebSocket
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.toAllPluginsWebSocket
import com.epam.drill.router.WsRoutes
import io.ktor.application.Application
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance


class ServerWsTopics(override val kodein: Kodein) : KodeinAware {
    private val wsTopic: WsTopic by instance()
    private val agentStorage: AgentStorage by instance()
    private val plugins: Plugins by instance()
    private val app: Application by instance()
    private val sessionStorage: MutableSet<DrillWsSession> by instance()

    init {

        runBlocking {
            agentStorage.onUpdate += update(mutableSetOf()) {
                val destination = app.toLocation(WsRoutes.GetAllAgents())
                sessionStorage.sendTo(it.keys.toAgentInfosWebSocket().messageEvent(destination))
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
                    agentStorage.keys.toAgentInfosWebSocket()
                }

                topic<WsRoutes.GetAgent> { x, _ ->
                    agentStorage.byId(x.agentId)?.toAgentInfoWebSocket()
                }

                topic<WsRoutes.GetAllPlugins> { _, _ ->
                    plugins.pluginBeans.toAllPluginsWebSocket(agentStorage.keys)
                }
            }

        }
    }

}

