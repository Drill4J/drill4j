package com.epam.drill.plugin.api.end

import com.epam.drill.common.*

interface Sender {
    suspend fun send(agentInfo: AgentInfo, destination: String, message: String)
}
