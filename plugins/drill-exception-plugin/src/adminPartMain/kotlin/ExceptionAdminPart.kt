package com.epam.drill.plugins.exception

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.end.*
import com.epam.drill.plugin.api.message.*
import kotlinx.serialization.*


@Suppress("unused")
class ExceptionAdminPart(private val ws: Sender, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<String>(ws, agentInfo, id) {

    override val serDe = SerDe(String.serializer())

    override suspend fun doAction(action: String) {
    }

    override suspend fun processData(dm: DrillMessage): Any {
        println("$id got a message ${dm.content}")
        return ""
    }
}