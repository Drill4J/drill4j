package com.epam.drill.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.PluginStorage.storage
import com.epam.drill.plugin.api.processing.AgentPart

object PluginManager {

    fun addPlugin(plugin: AgentPart<*>) {
        storage[plugin.id] = plugin
    }

    operator fun get(id: String) = storage[id]

    suspend fun pluginsState(): HashSet<PluginBean> = storage.values.map { it.actualPluginConfig() }.toHashSet()
}
