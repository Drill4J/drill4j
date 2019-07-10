package com.epam.drill.endpoints

import com.epam.drill.common.*
import com.epam.drill.plugin.api.end.*
import io.ktor.http.cio.websocket.*

class AgentEntry(
    val agent: AgentInfo,
    val agentSession: DefaultWebSocketSession,
    var instance: MutableMap<String, AdminPluginPart<*>> = mutableMapOf()
)