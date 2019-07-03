package com.epam.drill.plugin.api.end

import com.epam.drill.common.AgentInfo

interface Sender {
    suspend fun send(agentInfo: AgentInfo, destination: String, message: String)
}
