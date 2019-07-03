package com.epam.drill.endpoints.plugin

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.PluginAction
import com.epam.drill.common.PluginBean
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.endpoints.AgentEntry
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.endpoints.agentWsMessage
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.MessageWrapper
import com.epam.drill.plugins.Plugin
import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import com.epam.drill.util.parse
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.locations.get
import io.ktor.locations.patch
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.serialization.Serializable
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by instance()
    private val agentManager: AgentManager by instance()
    private val wsService: WsService by kodein.instance()

    suspend fun processPluginData(pluginData: String, agentInfo: AgentInfo) {
        val message = MessageWrapper.serializer().parse(pluginData)
        val pluginId = message.pluginId
        try {
            val dp: Plugin = plugins[pluginId] ?: return
            val pluginClass = dp.pluginClass
            val agentEntry = agentManager.full(agentInfo.id)
            val plugin: AdminPluginPart<*> = fillPluginInstance(agentEntry, pluginClass, pluginId)
            plugin.processData(message.drillMessage)
        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }

    private fun fillPluginInstance(
        agentEntry: AgentEntry?,
        pluginClass: Class<AdminPluginPart<*>>,
        pluginId: String
    ): AdminPluginPart<*> {
        return agentEntry?.instance!![pluginId] ?: run {
            val constructor =
                pluginClass.getConstructor(WsService::class.java, AgentInfo::class.java, String::class.java)
            val plugin = constructor.newInstance(wsService, agentEntry.agent, pluginId)
            agentEntry.instance[pluginId] = plugin
            plugin
        }
    }

    init {
        app.routing {
            authenticate {
                patch<Routes.Api.Agent.UpdatePlugin> { ll ->
                    val pb = call.parse(PluginBean.serializer())
                    val pluginId = pb.id
                    agentManager.agentSession(ll.agentId)
                        ?.send(PluginBean.serializer().agentWsMessage("/plugins/updatePluginConfig", pb))
                    val pluginBean = agentManager[ll.agentId]?.plugins?.first { it.id == pluginId }
                    if (pluginBean != null) call.respond(HttpStatusCode.OK, "")
                    else call.respond(HttpStatusCode.NotFound, "")
                }
            }
            authenticate {
                post<Routes.Api.Agent.DispatchPluginAction> { ll ->
                    val action = call.receive<String>()
                    val agentId = ll.agentId
                    val pluginId = ll.pluginId
                    val dp: Plugin? = plugins[pluginId]
                    val agentInfo = agentManager[ll.agentId]
                    val (statusCode, response) = when {
                        (dp == null) -> HttpStatusCode.NotFound to "plugin with id $pluginId not found"
                        (agentInfo == null) -> HttpStatusCode.NotFound to "agent with id $agentId not found"
                        else -> {
                            val message = PluginAction.serializer() stringify
                                    PluginAction(pluginId, action)
                            agentManager.agentSession(agentId)
                                ?.send(
                                    Frame.Text(
                                        Message.serializer() stringify Message(
                                            MessageType.MESSAGE,
                                            "/plugins/action",
                                            message
                                        )
                                    )
                                )
                            val agentEntry = agentManager.full(agentId)
                            val plugin: AdminPluginPart<*> = fillPluginInstance(
                                agentEntry, dp.pluginClass, pluginId
                            )
                            plugin.doRawAction(action)
                            HttpStatusCode.OK to ""
                        }
                    }
                    call.respond(statusCode, response)
                }
            }

            authenticate {
                post<Routes.Api.Agent.AddNewPlugin> { ll ->
                    val agentId = ll.agentId
                    val pluginId = call.parse(PluginId.serializer()).pluginId
                    val (status, msg) = when (pluginId) {
                        null -> HttpStatusCode.BadRequest to "Plugin id is null for agent '$agentId'"
                        in plugins.keys -> {
                            if (agentId in agentManager) {
                                val agentInfo = agentManager[agentId]!!
                                if (agentInfo.plugins.any { it.id == pluginId }) {
                                    HttpStatusCode.BadRequest to "Plugin '$pluginId' is already in agent '$agentId'"
                                } else {
                                    agentManager.addPluginFromLib(agentId, pluginId)
                                    val dp: Plugin = plugins[pluginId]!!
                                    val pluginClass = dp.pluginClass
                                    val agentEntry = agentManager.full(agentInfo.id)
                                    fillPluginInstance(agentEntry, pluginClass, pluginId)
                                    HttpStatusCode.OK to "Plugin '$pluginId' was added to agent '$agentId'"
                                }
                            } else {
                                HttpStatusCode.BadRequest to "Agent '$agentId' not found"
                            }
                        }
                        else -> HttpStatusCode.BadRequest to "Plugin $pluginId not found."
                    }
                    call.respond(status, msg)
                }
            }

            authenticate {
                post<Routes.Api.Agent.TogglePlugin> { ll ->
                    agentManager.agentSession(ll.agentId)

                        ?.send(
                            Frame.Text(
                                Message.serializer() stringify
                                        Message(
                                            MessageType.MESSAGE,
                                            "/plugins/togglePlugin", ll.pluginId
                                        )
                            )
                        )
                    call.respond(HttpStatusCode.OK, "OK")
                }
            }
            authenticate {
                get<Routes.Api.PluginConfiguration> {

                    call.respond(plugins.keys.toTypedArray())

                }
            }
        }
    }
}

@Serializable
data class PluginId(val pluginId: String?)
