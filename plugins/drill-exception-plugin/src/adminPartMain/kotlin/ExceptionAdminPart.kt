package com.epam.drill.plugins.exception

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer


@Suppress("unused")
class ExceptionAdminPart(private val ws: WsService, agentInfo: AgentInfo, id: String) :
    AdminPluginPart<String>(ws, agentInfo, id) {

    override var actionSerializer: KSerializer<String> = String.serializer()

    override suspend fun doAction(action: String) {
    }

    override suspend fun processData(dm: DrillMessage): Any {
        println("$id got a message ${dm.content}")
        return ""
    }
}