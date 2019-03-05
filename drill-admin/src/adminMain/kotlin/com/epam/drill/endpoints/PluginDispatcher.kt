package com.epam.drill.endpoints

import com.epam.drill.agentmanager.AgentStorage
import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugins.Plugins
import com.epam.drill.plugins.serverInstance
import com.epam.drill.storage.MongoClient
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.cio.websocket.Frame
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respondText
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

    suspend fun processPluginData(pluginData: String) {
        val message = parseRequest(pluginData)
        val pluginId = message.pluginId
        val dp = plugins.plugins[pluginId]
        val sessionId = message.drillMessage.sessionId ?: ""
        val destination = pluginId + sessionId

        try {
            //fixme
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


    @Location("/api/agent/updatePluginConfig/{agentName}/{pluginName}")
    data class PluginConfig(val agentName: String, val pluginName: String)

    init {
        app.routing {
            authenticate {
                post<PluginConfig> { ll ->
                    val text = Message(MessageType.MESSAGE, "/plugins/updatePluginConfig", call.receive())
                    agentStorage.agents[ll.agentName]?.agentWsSession?.send(Frame.Text(Gson().toJson(text)))
                    call.respondText { "Update sent" }
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