package com.epam.drill.endpoints

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import io.ktor.http.cio.websocket.DefaultWebSocketSession

class AgentEntry(val agent: AgentInfo, val agentSession: DefaultWebSocketSession, var instance: AdminPluginPart? = null)

fun AgentEntry(ae: AgentEntry, agInfo: AgentInfo): AgentEntry = AgentEntry(
    agInfo,
    ae.agentSession,
    ae.instance
)