@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object Routes {

    @Location("/api")
    class Api {
        @Location("/agent")
        class Agent {
                @Location("/loadPlugin/{agentName}/{pluginName}")
                data class LoadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

                @Location("/unloadPlugin/{agentName}/{pluginName}")
                data class UnloadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

                @Location("/agent-info/getAllAgents")
                class Agents

                @Location("/agent-info/{agentName}")
                data class Agent(val agentName: String)

                abstract class PluginManageLink {
                        abstract val agentName: String
                        abstract val pluginName: String
                }
        }

        @Location("/pluginContent/{pluginId}")
        data class PluginContent(val pluginId: String)

        @Location("/drill-admin/plugin/getAllPlugins")
        class AllPlugins

        @Location("/plugin-info/getPluginsConfiguration")
        class PluginConfiguration

        @Location("/login")
        class Login
    }

}