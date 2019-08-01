package com.epam.drill.router

import io.ktor.locations.*

object WsRoutes {
    @Location("/get-all-agents")
    class GetAllAgents

    @Location("/get-agent/{agentId}")
    data class GetAgent(val agentId: String)

    @Location("/{agentId}/{pluginId}/config")
    data class GetPluginConfig(val agentId: String, val pluginId: String)

    @Location("/agent/{agentId}/get-builds")
    data class GetAgentBuilds(val agentId: String)

    @Location("/get-all-plugins")
    class GetAllPlugins

    @Location("/{agentId}/get-plugin-info")
    data class GetPluginInfo(val agentId: String)
}