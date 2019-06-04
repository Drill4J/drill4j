package com.epam.drill.endpoints.openapi

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.endpoints.SeqMessage
import com.epam.drill.plugin.api.end.WsService
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

class DevEndpoints(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val ws: WsService by instance()

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
}