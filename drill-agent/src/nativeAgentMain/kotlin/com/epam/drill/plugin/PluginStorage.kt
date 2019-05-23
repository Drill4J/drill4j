package com.epam.drill.plugin

import com.epam.drill.core.exec
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.plugin.api.processing.AgentPart

actual object PluginStorage {
    actual val storage: MutableMap<String, AgentPart<*>>
        get() = exec { pstorage }
}

actual suspend fun AgentPart<*>.actualPluginConfig() = pluginConfigById(this.id)
