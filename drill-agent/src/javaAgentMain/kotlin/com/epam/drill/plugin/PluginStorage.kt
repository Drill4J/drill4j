package com.epam.drill.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.AgentPart
import java.util.concurrent.ConcurrentHashMap

actual object PluginStorage {
    actual val storage: MutableMap<String, AgentPart<*>>
        get() = ConcurrentHashMap()
}

actual fun AgentPart<*>.actualPluginConfig(): PluginBean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}