package com.epam.drill.endpoints.openapi

import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.locations.post
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.Routing
import io.ktor.routing.routing
import kotlinx.serialization.*
import org.kodein.di.*
import org.kodein.di.generic.*

/**
 * Swagger DrillAdmin
 *
 * This is a drill-ktor-admin-server
 */
class SwaggerDrillAdminServer(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val agentManager: AgentManager by instance()
    private val plugins: Plugins by kodein.instance()

    @Serializable
    data class PluginId(val pluginId: String)

    init {
        app.routing {
            registerAgent()
            registerDrillAdmin()
        }
    }

    private fun Routing.registerAgent() {

        authenticate {
            post<Routes.Api.Agent.UnloadPlugin> { up ->
                val pluginId = call.receive<PluginId>()
                val drillAgent = agentManager.agentSession(up.agentId)
                if (drillAgent == null) {
                    call.respond("can't find the agent '${up.agentId}'")
                    return@post
                }
                val agentPluginPartFile = plugins[pluginId.pluginId]?.agentPluginPart
                if (agentPluginPartFile == null) {
                    call.respond("can't find the plugin '${pluginId.pluginId}' in the agent '${up.agentId}'")
                    return@post
                }

                drillAgent.send(
                    Frame.Text(
                        Message.serializer() stringify Message(
                            MessageType.MESSAGE,
                            "/plugins/unload",
                            pluginId.pluginId
                        )
                    )
                )
//            drillAgent.agentInfo.plugins.removeIf { x -> x.id == up.pluginName }
                call.respond("event 'unload' was sent to AGENT")
            }
        }

        authenticate {
            get<Routes.Api.Agent.Agent> { up ->
                call.respond(agentManager[up.agentId] ?: "can't find")
            }
        }

        authenticate {
            post<Routes.Api.Agent.AgentToggleStandby> { agent ->
                val agentId = agent.agentId
                agentManager[agentId]?.let { agentInfo ->
                    agentInfo.status = when (agentInfo.status) {
                        AgentStatus.DISABLED -> AgentStatus.READY
                        AgentStatus.READY -> AgentStatus.DISABLED
                        else -> {
                            return@let
                        }
                    }

                    val agentSession = agentManager.agentSession(agentId)
                    agentInfo.plugins.forEach {
                        if (it.enabled) {
                            agentSession?.send(
                                Frame.Text(
                                    Message.serializer() stringify
                                            Message(MessageType.MESSAGE, "/plugins/togglePlugin", it.id)
                                )
                            )
                        }
                    }
                    agentInfo.update(agentManager)


                    call.respond(HttpStatusCode.OK, "toggled")
                }
            }
        }

    }

    /**
     * drill-admin
     */
    private fun Routing.registerDrillAdmin() {
        authenticate {
            get<Routes.Api.AllPlugins> {
                call.respond(plugins.values.map { dp -> dp.pluginBean.id })
            }
        }
    }

}
