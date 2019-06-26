package com.epam.drill.plugin.api.end

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.parse
import com.epam.drill.plugin.api.DrillPlugin
import com.epam.drill.plugin.api.message.DrillMessage

abstract class AdminPluginPart<A>(val sender: WsService, override val id: String) : DrillPlugin() {
    abstract var actionSerializer: kotlinx.serialization.KSerializer<A>

    abstract suspend fun processData(pAgentInfo: AgentInfo, dm: DrillMessage): Any

    abstract suspend fun doAction(pAgentInfo: AgentInfo, action: A)

    suspend fun doRawAction(agentInfo: AgentInfo, action: String) = doAction(
        agentInfo, actionSerializer parse action
    )
}
