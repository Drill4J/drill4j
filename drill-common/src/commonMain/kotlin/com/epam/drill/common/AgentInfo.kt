package com.epam.drill.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val name: String,
    val groupName: String,
    val description: String,
    var isEnable: Boolean,

    @Optional
    val adminUrl: String = "",
    @Optional
    var ipAddress: String = "",
    @Optional
    val rawPluginNames: MutableSet<PluginBean> = mutableSetOf(),
    @Optional
    var additionalInfo: AgentAdditionalInfo? = null
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
