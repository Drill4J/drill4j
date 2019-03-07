@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object Routes {

    @Location("/api")
    class Api {
        @Location("/agent")
        class Agent {
            @Location("/{agentId}/toggle-standby")
            data class AgentToggleStandby(val agentId: String)

            @Location("/load-plugin/{agentId}")
            data class LoadPlugin(val agentId: String)

            @Location("/unload-plugin/{agentId}")
            data class UnloadPlugin(val agentId: String)

            @Location("/{agentId}")
            data class Agent(val agentId: String)

            @Location("/{agentId}/update-plugin")
            data class UpdatePlugin(val agentId: String)

            @Location("/{agentId}/{pluginId}/toggle-plugin")
            data class TogglePlugin(val agentId: String, val pluginId: String)
        }

        @Location("/plugin-content/{pluginId}")
        data class PluginContent(val pluginId: String)

        @Location("/drill-admin/plugin/get-all-plugins")
        class AllPlugins

        @Location("/plugin-info/get-plugins-configuration")
        class PluginConfiguration

        @Location("/login")
        class Login
    }

}