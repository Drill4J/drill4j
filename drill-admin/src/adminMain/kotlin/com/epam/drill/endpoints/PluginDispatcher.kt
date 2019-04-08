package com.epam.drill.endpoints


import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.agentmanager.byId
import com.epam.drill.agentmanager.get
import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugins.Plugins
import com.epam.drill.router.Routes
import com.epam.drill.storage.MongoClient
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.patch
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.routing
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.litote.kmongo.getCollection
import java.util.*

@KtorExperimentalLocationsAPI
@ObsoleteCoroutinesApi
class PluginDispatcher(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val mc: MongoClient by instance()
    private val plugins: Plugins by instance()
    private val agentStorage: AgentStorage by instance()

    private val wsService: WsService by kodein.instance()

    suspend fun processPluginData(pluginData: String, agentInfo: AgentInfo) {
        val message: SeqMessage = parseRequest(pluginData)
        val pluginId = message.pluginId
        val sessionId = message.drillMessage.sessionId ?: ""
        val destination = pluginId + sessionId

        try {
            //fixme
            plugins.plugins[pluginId]?.first?.processData(agentInfo, message.drillMessage)
//                                    val processData = dp?.serverInstance?.processData(message.drillMessage)
//            dp?.serverInstance?.sender?.convertAndSend(destination, message)
            wsService.convertAndSend(destination, message)
        } catch (ee: Exception) {
            ee.printStackTrace()
            //fixme log
//                logError("we should rework the plugin API.")
        }

        try {
            val collection = mc.client!!.getDatabase("test").getCollection<SeqMessage>(destination)
            collection.insertOne(message)


        } catch (ex: Exception) {
            ex.printStackTrace()
            //fixme log
//            logError("cannot save the message, $ex")
        }
    }

    init {
        app.routing {
            authenticate {
                patch<Routes.Api.Agent.UpdatePlugin> { ll ->
                    val message = call.receive<String>()
                    val pluginId = Gson().fromJson<PluginBean>(message, PluginBean::class.java).id
                    agentStorage[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/updatePluginConfig", message)
                        )
                    val pluginBean = agentStorage.byId(ll.agentId)?.rawPluginNames?.first { it.id == pluginId }
                    call.respond { if (pluginBean != null) HttpStatusCode.OK else HttpStatusCode.NotFound }
                }
            }
            authenticate {
                patch<Routes.Api.Agent.PluginAction> { ll ->
                    val message = call.receive<String>()

                    agentStorage[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/action", message)
                        )
//                    val pluginBean = agentStorage.byId(ll.agentId)?.rawPluginNames?.first { it.id == pluginId }
                    call.respond { HttpStatusCode.OK }
                }
            }

            authenticate {
                post<Routes.Api.Agent.TogglePlugin> { ll ->
                    agentStorage[ll.agentId]
                        ?.send(
                            agentWsMessage("/plugins/togglePlugin", ll.pluginId)
                        )
                    call.respond { HttpStatusCode.OK }
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
            //fixme log
//            logError("cannot parse the incoming message $readText")
            throw ex
        }
    }
}