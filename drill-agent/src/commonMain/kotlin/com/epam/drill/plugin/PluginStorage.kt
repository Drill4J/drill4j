package com.epam.drill.plugin

import com.epam.drill.plugin.api.processing.AgentPluginPart

expect object PluginStorage {
    val storage: MutableMap<String, AgentPluginPart<*>>

}