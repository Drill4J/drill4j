package com.epam.drill.plugins.coverage

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage


@Suppress("unused")
class CoverageController(private val ws: WsService, id: String) : AdminPluginPart(ws, id) {


    override suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any {
        println("ololo,")
        return ""
    }
}