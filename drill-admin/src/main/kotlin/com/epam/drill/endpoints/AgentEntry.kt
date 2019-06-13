package com.epam.drill.endpoints

import com.epam.drill.common.AgentInfo
import com.epam.drill.plugin.api.end.AdminPluginPart
import io.ktor.http.cio.websocket.DefaultWebSocketSession

class AgentEntry(var agent: AgentInfo, val agentSession: DefaultWebSocketSession, var instance: AdminPluginPart? = null)