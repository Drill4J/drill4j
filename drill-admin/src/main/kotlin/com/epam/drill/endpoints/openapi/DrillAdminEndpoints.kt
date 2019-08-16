package com.epam.drill.endpoints.openapi

import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.post
import io.ktor.response.*
import io.ktor.routing.routing
import org.kodein.di.*
import org.kodein.di.generic.*

class DrillAdminEndpoints(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()
    private val plugins: Plugins by kodein.instance()

    init {
        app.routing {
            authenticate {
                post<Routes.Api.Agent.UnloadPlugin> { (agentId, pluginId) ->
                    val drillAgent = agentManager.agentSession(agentId)
                    if (drillAgent == null) {
                        call.respond("can't find the agent '$agentId'")
                        return@post
                    }
                    val agentPluginPartFile = plugins[pluginId]?.agentPluginPart
                    if (agentPluginPartFile == null) {
                        call.respond("can't find the plugin '$pluginId' in the agent '$agentId'")
                        return@post
                    }
                    drillAgent.send(
                        Frame.Text(
                            Message.serializer() stringify Message(
                                MessageType.MESSAGE,
                                "/plugins/unload",
                                pluginId
                            )
                        )
                    )
                    //TODO: implement the agent-side plugin unloading, remove plugin from AgentInfo
                    call.respond("event 'unload' was sent to AGENT")
                }
            }

            authenticate {
                post<Routes.Api.Agent.AgentToggleStandby> { (agentId) ->
                    agentManager[agentId]?.let { agentInfo ->
                        agentInfo.status = when (agentInfo.status) {
                            AgentStatus.DISABLED -> AgentStatus.READY
                            AgentStatus.READY -> AgentStatus.DISABLED
                            else -> {
                                return@let
                            }
                        }
                        val agentSession = agentManager.agentSession(agentId)
                        agentInfo.plugins.filter { it.enabled }.forEach {
                            agentSession?.send(
                                Frame.Text(
                                    Message.serializer() stringify
                                            Message(MessageType.MESSAGE, "/plugins/togglePlugin", it.id)
                                )
                            )
                        }
                        agentInfo.update(agentManager)
                        call.respond(HttpStatusCode.OK, "toggled")
                    }
                }
            }

            authenticate {
                post<Routes.Api.ResetPlugin> { (agentId, pluginId) ->
                    val agentEntry = agentManager.full(agentId)
                    val (statusCode, response) = when {
                        agentEntry == null -> HttpStatusCode.NotFound to "agent with id $agentId not found"
                        plugins[pluginId] == null -> HttpStatusCode.NotFound to "plugin with id $pluginId not found"
                        else -> {
                            val pluginInstance = agentEntry.instance[pluginId]
                            if (pluginInstance == null) {
                                HttpStatusCode.NotFound to
                                        "plugin with id $pluginId not installed to agent with id $agentId"
                            } else {
                                pluginInstance.dropData()
                                HttpStatusCode.OK to "reset plugin with id $pluginId for agent with id $agentId"
                            }
                        }
                    }
                    call.respond(statusCode, response)
                }
            }

            authenticate {
                post<Routes.Api.ResetAgent> { (agentId) ->
                    val agentEntry = agentManager.full(agentId)
                    val (statusCode, response) = when {
                        agentEntry == null -> HttpStatusCode.NotFound to "agent with id $agentId not found"
                        else -> {
                            agentEntry.instance.values.forEach { pluginInstance -> pluginInstance.dropData() }
                            HttpStatusCode.OK to "reset agent with id $agentId"
                        }
                    }
                    call.respond(statusCode, response)
                }
            }

            authenticate {
                post<Routes.Api.ResetAllAgents> {
                    agentManager.getAllAgents().forEach { agentEntry ->
                        agentEntry.instance.values.forEach { pluginInstance -> pluginInstance.dropData() }
                    }
                    call.respond(HttpStatusCode.OK, "reset drill admin app")
                }
            }
        }
    }
}
