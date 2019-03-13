package com.epam.drill.router

import io.ktor.locations.Location

object WsRoutes {
    @Location("/get-all-agents")
    class GetAllAgents

    @Location("/get-agent/{agentId}")
    data class GetAgent(val agentId: String)

}