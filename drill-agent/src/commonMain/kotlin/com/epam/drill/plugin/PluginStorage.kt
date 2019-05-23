package com.epam.drill.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.AgentPart

expect object PluginStorage {
    val storage: MutableMap<String, AgentPart<*>>

}

expect suspend fun AgentPart<*>.actualPluginConfig(): PluginBean