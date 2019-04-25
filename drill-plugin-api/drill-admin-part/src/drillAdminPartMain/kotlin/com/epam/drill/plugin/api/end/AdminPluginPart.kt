package com.epam.drill.plugin.api.end

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.DrillPlugin
import com.epam.drill.plugin.api.message.DrillMessage

abstract class AdminPluginPart(val sender: WsService, override val id: String) : DrillPlugin() {
    abstract suspend fun processData(agentInfo: AgentInfo, dm: DrillMessage): Any
}
