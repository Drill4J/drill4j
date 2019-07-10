package com.epam.drill.plugin

import com.epam.drill.common.*
import com.epam.drill.plugin.api.processing.*

object PluginManager {

    fun addPlugin(plugin: AgentPart<*, *>) {
        storage[plugin.id] = plugin
    }

    operator fun get(id: String) = storage[id]
    operator fun get(id: Family) = storage.values.groupBy { it.actualPluginConfig().family }[id]
}


//val xx: List<AgentPart<*, *>>?
//    get() =