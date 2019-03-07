package com.epam.drill.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.PluginStorage.storage
import com.epam.drill.plugin.api.processing.AgentPart

object PluginManager {

    fun addPlugin(plugin: AgentPart<*>) {
        storage[plugin.id] = plugin
    }

    operator fun get(id: String): AgentPart<*>? {
        return storage[id]
    }

    fun pluginsState(): HashSet<PluginBean> {
        return storage.map {
            val id = it.key
            val enabled = it.value.enabled
            val pluginBean = PluginBean()
            pluginBean.id = id
            pluginBean.enabled = enabled
            pluginBean
        }.toHashSet()
    }

    fun activate(id: String) {
        storage[id]?.enabled = true
    }

    fun deactivate(id: String) {
        storage[id]?.enabled = false
    }

}