package com.epam.drill.endpoints.plugin

import com.epam.drill.common.*
import com.epam.drill.endpoints.*
import com.epam.drill.endpoints.agent.*
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

class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by instance()
    private val agentManager: AgentManager by instance()
    private val wsService: Sender by kodein.instance()
    private val serverWs: DrillServerWs by instance()

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
                patch<Routes.Api.Agent.UpdatePlugin> { (agentId, pluginId) ->
                    val config = call.receive<String>()
                    val pc = PluginConfig(pluginId, config)
                    agentManager.agentSession(agentId)
                        ?.send(PluginConfig.serializer().agentWsMessage("/plugins/updatePluginConfig", pc))
                    if (agentManager.updateAgentPluginConfig(agentId, pc)) {
                        serverWs.sendToAllSubscribed("/$agentId/$pluginId/config")
                        call.respond(HttpStatusCode.OK, "")
                    } else call.respond(HttpStatusCode.NotFound, "")
                }
            }
            authenticate {
                post<Routes.Api.Agent.DispatchPluginAction> { (agentId, pluginId) ->
                    val action = call.receive<String>()
                    val dp: Plugin? = plugins[pluginId]
                    val agentInfo = agentManager[agentId]
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
                            if (adminActionResult is StatusMessage) {
                                HttpStatusCode.fromValue(adminActionResult.code) to adminActionResult.message
                            } else {
                                HttpStatusCode.OK to when (adminActionResult) {
                                    is String -> adminActionResult
                                    else -> EmptyContent
                                }
                            }
                        }
                    }
                    call.respond(statusCode, response)
                }
            }

            authenticate {
                post<Routes.Api.Agent.AddNewPlugin> { (agentId) ->
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
                post<Routes.Api.Agent.TogglePlugin> { (agentId, pluginId) ->
                    val dp: Plugin? = plugins[pluginId]
                    val session = agentManager.agentSession(agentId)
                    val (statusCode, response) = when {
                        (dp == null) -> HttpStatusCode.NotFound to "plugin with id $pluginId not found"
                        (session == null) -> HttpStatusCode.NotFound to "agent with id $agentId not found"
                        else -> {
                            session.send(
                                Frame.Text(
                                    Message.serializer() stringify
                                            Message(
                                                MessageType.MESSAGE,
                                                "/plugins/togglePlugin", pluginId
                                            )
                                )
                            )
                            HttpStatusCode.OK to "OK"
                        }
                    }
                    call.respond(statusCode, response)
                }
            }

        }
    }
}

@Serializable
data class PluginId(val pluginId: String?)
