package com.epam.drill.agentmanager

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.PluginBean
import com.epam.drill.plugins.PluginWebSocket
import com.epam.drill.plugins.toPluginsWebSocket
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

    val adminUrl: String = "",
    var ipAddress: String = "",
    val activePluginsCount: Int = 0,
    val pluginsCount: Int = 0
)

@Suppress("unused")
@Serializable
class AgentInfoWebSocketSingle(
    val id: String,
    val name: String,
    val description: String,
    val group: String,
    val status: Boolean = true,
    val buildVersion: String,
    val adminUrl: String = "",
    val ipAddress: String = "",
    val activePluginsCount: Int = 0,
    val pluginsCount: Int = 0,
    val rawPluginsName: MutableSet<PluginWebSocket> = mutableSetOf()
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