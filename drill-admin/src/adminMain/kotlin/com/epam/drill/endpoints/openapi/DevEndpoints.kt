package com.epam.drill.endpoints.openapi

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.endpoints.SeqMessage
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.storage.MongoClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.routing
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.litote.kmongo.getCollection

class DevEndpoints(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val mc: MongoClient by instance()
    private val ws: WsService by instance()

    init {
        app.routing {
            registerDrillAdminDev()
        }
    }

    /**
     * drill-admin only for dev
     */
    private fun Routing.registerDrillAdminDev() {
        get<Exceptionss> { tpd ->
            val objects = mc.client!!.getDatabase("test").getCollection<SeqMessage>(tpd.topicName)
            call.respond(objects.find().map { getMessageForSocket(it) }.map {
                Message(
                    MessageType.MESSAGE,
                    tpd.topicName,
                    it
                )
            }.toList())
        }
        get<getAllSubscibers> {
            call.respond(ws.getPlWsSession())
        }
    }

    private fun getMessageForSocket(ogs: SeqMessage): String {
        val content = ogs.drillMessage.content
        val map: Map<*, *>? = ObjectMapper().readValue(content, Map::class.java)
        //fixme log
//        logDebug("return data for socket")
        val hashMap = HashMap<Any, Any>(map)
        hashMap["id"] = ogs.id ?: ""
        return Gson().toJson(hashMap)
    }


    @Location("/ws/ex/exceptions/{topicName}")
    data class Exceptionss(val topicName: String)

    @Location("/ws/ex/exceptions/getAllSubscibers")
    class getAllSubscibers
}