package com.epam.drill.plugin

import com.epam.drill.core.exec
import com.epam.drill.core.plugin.pluginConfigById
import com.epam.drill.plugin.api.processing.AgentPart

actual val storage: MutableMap<String, AgentPart<*, *>>
    get() = exec { pstorage }



actual fun AgentPart<*, *>.actualPluginConfig() = pluginConfigById(this.id)
