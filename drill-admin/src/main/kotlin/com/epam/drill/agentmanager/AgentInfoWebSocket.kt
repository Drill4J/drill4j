package com.epam.drill.agentmanager

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.plugins.PluginWebSocket
import com.epam.drill.plugins.toPluginsWebSocket
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
open class AgentInfoWebSocket(
    val id: String,
    val name: String,
    val description: String,
    val group: String,
    val status: Boolean = true,
    var buildVersion: String,

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
    id: String,
    name: String,
    description: String,
    group: String,
    status: Boolean = true,
    buildVersion: String,

    adminUrl: String = "",
    ipAddress: String = "",
    activePluginsCount: Int = 0,
    pluginsCount: Int = 0,
    val rawPluginsName: MutableSet<PluginWebSocket> = mutableSetOf()
) : AgentInfoWebSocket(
    id = id,
    name = name,
    description = description,
    group = group,
    status = status,
    adminUrl = adminUrl,
    ipAddress = ipAddress,
    buildVersion = buildVersion,
    activePluginsCount = activePluginsCount,
    pluginsCount = pluginsCount
)

fun AgentInfo.toAgentInfoWebSocket() = AgentInfoWebSocketSingle(
    id = id,
    name = name,
    description = description,
    group = groupName,
    status = isEnable,
    buildVersion = buildVersion,
    adminUrl = adminUrl,
    ipAddress = ipAddress,
    activePluginsCount = rawPluginNames.activePluginsCount(),
    pluginsCount = rawPluginNames.size,
    rawPluginsName = rawPluginNames.toPluginsWebSocket()
)

fun MutableSet<PluginBean>.activePluginsCount() = this.count { it.enabled }

fun MutableSet<AgentInfo>.toAgentInfosWebSocket() = this.map {
    it.run {
        AgentInfoWebSocket(
            id = id.take(20),
            name = name,
            description = description.take(200),
            group = groupName,
            status = isEnable,
            buildVersion = buildVersion,
            adminUrl = adminUrl,
            ipAddress = ipAddress,
            activePluginsCount = rawPluginNames.activePluginsCount(),
            pluginsCount = rawPluginNames.size
        )
    }
}