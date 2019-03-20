package com.epam.drill.plugins

import com.epam.drill.common.PluginBean
import kotlinx.serialization.Serializable

@Serializable
data class PluginWebSocket (
    var id: String, var name: String = "", var description: String = "", var type: String = "",
    var status: Boolean = true, var config: String = ""
)

fun PluginBean.toPluginWebSocket() = PluginWebSocket (
    id = id,
    name = name,
    description = description,
    type = type,
    status = enabled,
    config = config
)

fun MutableSet<PluginBean>.toPluginsWebSocket() = this.map {it.toPluginWebSocket()}.toMutableSet()