package com.epam.drill.plugin

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.processing.AgentPart

expect val storage: MutableMap<String, AgentPart<*, *>>


expect fun AgentPart<*, *>.actualPluginConfig(): PluginBean