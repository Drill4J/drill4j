package com.epam.drill.plugins.custom


import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import com.epam.drill.plugin.api.end.WsService
import com.epam.drill.plugin.api.message.DrillMessage

@Suppress("unused")

class PluginController(private val ws: WsService, val name: String) : AdminPluginPart(ws, name) {

    override suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any {
        val sessionId = dm.sessionId
        ws.convertAndSend(agentInfo, id + (sessionId ?: ""), dm.content!!, sessionId ?: "")
        return ""
    }

}
