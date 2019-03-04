package com.epam.drill.plugin

import com.epam.drill.plugin.api.processing.AgentPluginPart
import java.util.concurrent.ConcurrentHashMap

actual object PluginStorage {
    actual val storage: MutableMap<String, AgentPluginPart<*>>
        get() = ConcurrentHashMap()
}