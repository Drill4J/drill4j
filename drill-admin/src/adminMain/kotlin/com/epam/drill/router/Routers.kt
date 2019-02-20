@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.epam.drill.router

import io.ktor.locations.Location

object Routes {

    @Location("/agent/loadPlugin/{agentName}/{pluginName}")
    data class LoadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

    @Location("/agent/unloadPlugin/{agentName}/{pluginName}")
    data class UnloadPlugin(override val agentName: String, override val pluginName: String) : PluginManageLink()

    abstract class PluginManageLink {
        abstract val agentName: String
        abstract val pluginName: String
    }


    @Location("/pluginContent/{pluginId}")
    data class PluginContent(val pluginId: String)

    @Location("/agent/agent-info/getAllAgents")
    class Agents

    @Location("/agent/agent-info/{agentName}")
    data class Agent(val agentName: String)

    @Location("/drill-admin/plugin/getAllPlugins")
    class AllPlugins
}