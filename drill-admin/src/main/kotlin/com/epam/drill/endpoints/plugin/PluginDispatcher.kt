package com.epam.drill.endpoints.plugin


import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.endpoints.AgentManager
import com.epam.drill.endpoints.SeqMessage
import com.epam.drill.endpoints.agentWsMessage
import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.patch
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.util.*

@KtorExperimentalLocationsAPI
@ObsoleteCoroutinesApi
class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val plugins: Plugins by instance()
    private val agentManager: AgentManager by instance()

    suspend fun processPluginData(pluginData: String, agentInfo: AgentInfo) {
        val message: SeqMessage = parseRequest(pluginData)
        val pluginId = message.pluginId
        try {
            val plugin = plugins.plugins[pluginId]?.first
            plugin?.processData(agentInfo, message.drillMessage)
        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }

    init {
        app.routing {
            authenticate {
                patch<Routes.Api.Agent.UpdatePlugin> { ll ->
                    val message = call.receive<String>()
                    val pluginId = Gson().fromJson<PluginBean>(message, PluginBean::class.java).id
                    agentManager[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/updatePluginConfig", message)
                        )
                    val pluginBean = agentManager.byId(ll.agentId)?.rawPluginNames?.first { it.id == pluginId }
                    call.respond { if (pluginBean != null) HttpStatusCode.OK else HttpStatusCode.NotFound }
                }
            }
            authenticate {
                post<Routes.Api.Agent.PluginAction> { ll ->
                    val message = call.receive<String>()

                    agentManager[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/action", message)
                        )
                    call.respond { HttpStatusCode.OK }
                }
            }


            authenticate {
                post<Routes.Api.Agent.AddNewPlugin> { ll ->
                    val agentId = ll.agentId
                    val pluginId = Json.parse(PluginId.serializer(), call.receive()).pluginId
                    val (status, msg) = when (pluginId) {
                        null -> HttpStatusCode.BadRequest to "Plugin id is null for agent '$agentId'"
                        in plugins -> {
                            if (agentId in agentManager) {
                                agentManager.addPluginFromLib(agentId, pluginId)
                                HttpStatusCode.OK to "Plugin '$pluginId' was added to agent '$agentId'"
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
                    agentManager[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/togglePlugin", ll.pluginId)
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

    private fun parseRequest(readText: String): SeqMessage {
        try {
            val fromJson = Gson().fromJson(readText, SeqMessage::class.javaObjectType)
            fromJson.id = UUID.randomUUID().toString()
            return fromJson
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw ex
        }
    }
}

@Serializable
data class PluginId(val pluginId: String?)
