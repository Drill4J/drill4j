package com.epam.drill.endpoints.plugin


import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.PluginBean
import com.epam.drill.common.parse
import com.epam.drill.common.stringify
import com.epam.drill.endpoints.AgentEntry
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.endpoints.agentWsMessage
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.MessageWrapper
import com.epam.drill.plugins.DP
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.pluginClass
import com.epam.drill.router.Routes
import com.epam.drill.util.parse
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.locations.KtorExperimentalLocationsAPI
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

@KtorExperimentalLocationsAPI
class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by instance()
    private val agentManager: AgentManager by instance()
    private val wsService: WsService by kodein.instance()

    suspend fun processPluginData(pluginData: String, agentInfo: AgentInfo) {
        val message = MessageWrapper.serializer().parse(pluginData)
        val pluginId = message.pluginId
        try {
            val dp: DP = plugins.plugins[pluginId] ?: return
            val pluginClass = dp.pluginClass
            val agentEntry = agentManager.full(agentInfo.id)
            val plugin: AdminPluginPart = fillPluginInstance(agentEntry, pluginClass, pluginId)
            plugin.processData(agentInfo, message.drillMessage)
        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }

    private fun fillPluginInstance(
        agentEntry: AgentEntry?,
        pluginClass: Class<AdminPluginPart>,
        pluginId: String
    ): AdminPluginPart {
        return agentEntry?.instance ?: run {
            val constructor = pluginClass.getConstructor(WsService::class.java, String::class.java)
            val plugin = constructor.newInstance(wsService, pluginId)
            agentEntry?.instance = plugin
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
                    call.respond { if (pluginBean != null) HttpStatusCode.OK else HttpStatusCode.NotFound }
                }
            }
            authenticate {
                post<Routes.Api.Agent.PluginAction> { ll ->
                    val message = call.receive<String>()

                    agentManager.agentSession(ll.agentId)
                        ?.send(
                            Frame.Text(
                                Message.serializer() stringify Message(
                                    MessageType.MESSAGE,
                                    "/plugins/action",
                                    message
                                )
                            )
                        )
                    call.respond(HttpStatusCode.OK ,"")
                }
            }


            authenticate {
                post<Routes.Api.Agent.AddNewPlugin> { ll ->
                    val agentId = ll.agentId
                    val pluginId = call.parse(PluginId.serializer()).pluginId
                    val (status, msg) = when (pluginId) {
                        null -> HttpStatusCode.BadRequest to "Plugin id is null for agent '$agentId'"
                        in plugins -> {
                            if (agentId in agentManager) {
                                val agentInfo = agentManager[agentId]!!
                                if (agentInfo.plugins.any { it.id == pluginId }) {
                                    HttpStatusCode.BadRequest to "Plugin '$pluginId' is already in agent '$agentId'"
                                } else {
                                    agentManager.addPluginFromLib(agentId, pluginId)
                                    val dp: DP = plugins.plugins[pluginId]!!
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
                    call.respond { HttpStatusCode.OK }
                }
            }
            authenticate {
                get<Routes.Api.PluginConfiguration> {

                    call.respond(plugins.plugins.keys.toTypedArray())

                }
            }
        }
    }
}

@Serializable
data class PluginId(val pluginId: String?)
