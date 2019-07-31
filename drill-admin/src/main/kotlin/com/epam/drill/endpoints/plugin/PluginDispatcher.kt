package com.epam.drill.endpoints.plugin

import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import com.epam.drill.plugins.*
import com.epam.drill.router.*
import com.epam.drill.util.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.*
import org.kodein.di.*
import org.kodein.di.generic.*
import kotlin.collections.set

class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by instance()
    private val agentManager: AgentManager by instance()
    private val wsService: Sender by kodein.instance()

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
                pluginClass.getConstructor(Sender::class.java, AgentInfo::class.java, String::class.java)
            val plugin = constructor.newInstance(wsService, agentEntry.agent, pluginId)
            agentEntry.instance[pluginId] = plugin
            plugin
        }
    }

    init {
        app.routing {
            authenticate {
                patch<Routes.Api.Agent.UpdatePlugin> { ll ->
                    val pc = call.parse(PluginConfig.serializer())
                    agentManager.agentSession(ll.agentId)
                        ?.send(PluginConfig.serializer().agentWsMessage("/plugins/updatePluginConfig", pc))
                    if (agentManager.updateAgentPluginConfig(ll.agentId, pc)) {
                        call.respond(HttpStatusCode.OK, "")
                    } else call.respond(HttpStatusCode.NotFound, "")
                }
            }
            authenticate {
                get<Routes.Api.Agent.GetPluginConfig> { ll ->
                    val agentInfo = agentManager[ll.agentId]
                    val pluginBean = agentInfo?.plugins?.find { it.id == ll.pluginId }
                    when {
                        agentInfo == null -> call.respond(
                            HttpStatusCode.NotFound,
                            "Agent with id ${ll.agentId} not found"
                        )
                        pluginBean == null -> call.respond(
                            HttpStatusCode.NotFound,
                            "Plugin with id ${ll.pluginId} not found"
                        )
                        else -> call.respond(pluginBean.config)
                    }
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
                            val agentEntry = agentManager.full(agentId)
                            val adminPart: AdminPluginPart<*> = fillPluginInstance(agentEntry, dp.pluginClass, pluginId)
                            val adminActionResult = adminPart.doRawAction(action)
                            val agentPartMsg = when(adminActionResult) {
                                is String -> adminActionResult
                                is Unit -> action
                                else -> Unit
                            }
                            if (agentPartMsg is String) {
                                agentManager.agentSession(agentId)?.apply {
                                    val agentAction = PluginAction(pluginId, agentPartMsg)
                                    val agentPluginMsg = PluginAction.serializer() stringify agentAction
                                    val agentMsg = Message(MessageType.MESSAGE, "/plugins/action", agentPluginMsg)
                                    val agentFrame = Frame.Text(Message.serializer() stringify agentMsg)
                                    send(agentFrame)
                                }
                            }
                            HttpStatusCode.OK to when(adminActionResult) {
                                is String -> adminActionResult
                                else -> EmptyContent
                            }
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
