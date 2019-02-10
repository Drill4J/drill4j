package com.epam.drill.agentmanager

import com.epam.drill.common.AgentInfo
import io.ktor.websocket.DefaultWebSocketServerSession

class DrillAgent(val agentInfo: AgentInfo, val storage: AgentStorage, val agentWsSession: DefaultWebSocketServerSession) {


}
