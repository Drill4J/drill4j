package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val agentAddress: String,
    val agentGroup: String,
    val rawPluginNames: MutableSet<PluginBean>,
    val agentAdditionalInfo: AgentAdditionalInfo


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
