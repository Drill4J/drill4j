@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object Routes {

    @Location("/api")
    class Api {
        @Location("/agent")
        class Agent {
            @Location("/{agentId}/toggle-standby")
            class AgentToggleStandby

            @Location("/load-plugin/{agentId}/{pluginName}")
            data class LoadPlugin(override val agentId: String, override val pluginName: String) : PluginManageLink()

            @Location("/unload-plugin/{agentId}/{pluginName}")
            data class UnloadPlugin(override val agentId: String, override val pluginName: String) : PluginManageLink()

            @Location("{agentId}")
            data class Agent(val agentId: String)

            abstract class PluginManageLink {
                abstract val agentId: String
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