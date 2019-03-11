package com.epam.drill.router

import io.ktor.locations.Location

object WsRoutes {
    @Location("/api/get-all-agents")
    class GetAllAgents()

    @Location("/api/get-agent/{agentId}")
    data class GetAgent(val agentId: String)

}