package com.epam.drill.plugin

import com.epam.drill.core.di
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.plugin.api.processing.AgentPart

actual object PluginStorage {
    actual val storage: MutableMap<String, AgentPart<*>>
        get() = di { pstorage }
}

actual fun AgentPart<*>.actualPluginConfig() = pluginConfigById(this.id)
