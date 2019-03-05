package com.epam.drill.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val agentName: String,
    val agentGroupName: String,
    val agentDescription: String,
    val isEnable: Boolean,

    @Optional
    val drillAdminUrl: String = "",
    @Optional
    val agentAddress: String = "",
    @Optional
    val rawPluginNames: MutableSet<PluginBean> = mutableSetOf(),
    @Optional
    var agentAdditionalInfo: AgentAdditionalInfo? = null
)

@Serializable
data class AgentAdditionalInfo(
    val jvmInput: List<String>,
    val availableProcessors: Int,
    val arch: String,
    val name: String,
    val version: String,
    val systemProperties: Map<String, String>
)
