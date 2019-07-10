package com.epam.drill.plugin

import com.epam.drill.common.*
import com.epam.drill.plugin.api.processing.*

expect val storage: MutableMap<String, AgentPart<*, *>>


expect fun AgentPart<*, *>.actualPluginConfig(): PluginBean