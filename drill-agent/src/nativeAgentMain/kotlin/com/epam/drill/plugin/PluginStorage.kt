package com.epam.drill.plugin

import com.epam.drill.core.*
import com.epam.drill.core.plugin.*
import com.epam.drill.plugin.api.processing.*

actual val storage: MutableMap<String, AgentPart<*, *>>
    get() = exec { pstorage }



actual fun AgentPart<*, *>.actualPluginConfig() = pluginConfigById(this.id)
