@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.*

object Routes {

    @Location("/api")
    class Api {
        @Location("/agents")
        class Agent {
            @Location("/{agentId}/toggle-standby")
            data class AgentToggleStandby(val agentId: String)

            @Location("/{agentId}/unload-plugin")
            data class UnloadPlugin(val agentId: String)

            @Location("/{agentId}")
            data class Agent(val agentId: String)

            @Location("/{agentId}/update-plugin")
            data class UpdatePlugin(val agentId: String)

            @Location("/{agentId}/{pluginId}/dispatch-action")
            data class DispatchPluginAction(val agentId: String, val pluginId: String)

            @Location("/{agentId}/{pluginId}/toggle-plugin")
            data class TogglePlugin(val agentId: String, val pluginId: String)

            @Location("/{agentId}/load-plugin")
            data class AddNewPlugin(val agentId: String)

            @Location("/{agentId}/register")
            data class RegisterAgent(val agentId: String)
        }

        @Location("/agent/{agentId}")
        data class UpdateAgentConfig(val agentId: String)

        @Location("/drill-admin/plugin/get-all-plugins")
        class AllPlugins

        @Location("/plugin-info/get-plugins-configuration")
        class PluginConfiguration

        @Location("/login")
        class Login
    }

}