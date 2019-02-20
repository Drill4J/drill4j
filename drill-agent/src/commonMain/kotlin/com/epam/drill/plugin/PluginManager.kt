package com.epam.drill.plugin

import com.epam.drill.plugin.PluginStorage.storage
import com.epam.drill.plugin.api.processing.AgentPluginPart

object PluginManager {

    fun addPlugin(plugin: AgentPluginPart) {
        storage[plugin.id] = plugin
    }

    operator fun get(id: String): AgentPluginPart? {
        return storage[id]
    }


}