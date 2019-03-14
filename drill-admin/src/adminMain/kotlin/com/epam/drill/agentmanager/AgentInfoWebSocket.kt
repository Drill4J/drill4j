package com.epam.drill.agentmanager

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
open class AgentInfoWebSocket(
    val name: String,
    val description: String,
    val group: String,
    val status: Boolean = true,

    @Optional
    val adminUrl: String = "",
    @Optional
    var ipAddress: String = "",
    @Optional
    val activePluginsCount: Int = 0,
    @Optional
    val pluginsCount: Int = 0
)

@Suppress("unused")
class AgentInfoWebSocketSingle(
    name: String,
    description: String,
    group: String,
    status: Boolean = true,

    adminUrl: String = "",
    ipAddress: String = "",
    activePluginsCount: Int = 0,
    pluginsCount: Int = 0,
    val rawPluginsName: MutableSet<PluginBean> = mutableSetOf()
) : AgentInfoWebSocket(name, description, group, status, adminUrl, ipAddress, activePluginsCount, pluginsCount)

fun AgentInfo.toAgentInfoWebSocket() = AgentInfoWebSocketSingle(
    name = name,
    description = description,
    group = groupName,
    status = isEnable,
    adminUrl = adminUrl,
    ipAddress = ipAddress,
    activePluginsCount = rawPluginNames.activePluginsCount(),
    pluginsCount = rawPluginNames.size,
    rawPluginsName = rawPluginNames
)

fun MutableSet<PluginBean>.activePluginsCount() = this.count { it.enabled }

fun MutableSet<AgentInfo>.toAgentInfosWebSocket() = this.map {
        AgentInfoWebSocket(
            name = it.name,
            description = it.description,
            group = it.groupName,
            status = it.isEnable,
            adminUrl = it.adminUrl,
            ipAddress = it.ipAddress,
            activePluginsCount = it.rawPluginNames.activePluginsCount(),
            pluginsCount = it.rawPluginNames.size
        )
}