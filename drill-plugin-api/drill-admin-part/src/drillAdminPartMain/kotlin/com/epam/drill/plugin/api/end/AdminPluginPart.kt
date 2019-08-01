package com.epam.drill.plugin.api.end

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import com.epam.drill.plugin.api.message.*

abstract class AdminPluginPart<A>(val sender: Sender, val agentInfo: AgentInfo, override val id: String) :
    DrillPlugin<A> {
    abstract suspend fun processData(dm: DrillMessage): Any

}
