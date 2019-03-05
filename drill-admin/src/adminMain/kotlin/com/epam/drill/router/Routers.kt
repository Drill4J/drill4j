@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object Routes {

    @Location("/api")
    class Api {
        @Location("/agent")
        class Agent {
            @Location("/{agentName}/toggle-standby")
            class AgentToggleStandby

            @Location("/load-plugin/{agentName}/{pluginName}")
            data class LoadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

            @Location("/unload-plugin/{agentName}/{pluginName}")
            data class UnloadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

            @Location("/{agentName}")
            data class Agent(val agentName: String)

            @Location("/{agentName}/update-plugin")
            data class UpdatePlugin(val agentName: String)

            @Location("/{agentName}/{pluginId}/toggle-plugin")
            data class TogglePlugin(val agentName: String, val pluginId: String)

            abstract class PluginManageLink {
                abstract val agentName: String
                abstract val pluginName: String
            }
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