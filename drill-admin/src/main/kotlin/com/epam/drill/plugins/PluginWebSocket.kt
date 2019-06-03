package com.epam.drill.plugins

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import kotlinx.serialization.Serializable

@Serializable
data class PluginWebSocket(
    var id: String, var name: String = "", var description: String = "", var type: String = "",
    var status: Boolean? = true, var config: String? = "", var installedAgentsCount: Int? = 0
)

fun PluginBean.toPluginWebSocket() = PluginWebSocket(
    id = id,
    name = name,
    description = description,
    type = type,
    status = enabled,
    config = config,
    installedAgentsCount = 0
)

fun MutableSet<PluginBean>.toPluginsWebSocket() = this.map { it.toPluginWebSocket() }.toMutableSet()

fun MutableSet<PluginBean>.toAllPluginsWebSocket(agents: Set<AgentInfo>?) = this.map {
    val pluginBeanWebSocket = it.toPluginWebSocket()
    pluginBeanWebSocket.config = null
    pluginBeanWebSocket.status = null
    pluginBeanWebSocket.installedAgentsCount = calculateInstalledAgentsCount(pluginBeanWebSocket.id, agents)
    return@map pluginBeanWebSocket
}.toMutableSet()

private fun calculateInstalledAgentsCount(id: String, agents: Set<AgentInfo>?): Int {
    if (agents == null || agents.isEmpty()) {
        return 0
    } else {
        return agents.count { plugins -> plugins.rawPluginNames!!.count { plugin -> plugin.id == id } > 0 }
    }
}