package com.epam.drill.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    var id: String,
    val name: String,
    val groupName: String,
    val description: String,
    var isEnable: Boolean,
    var buildVersion: String,

    @Optional
    val adminUrl: String = "",
    @Optional
    var ipAddress: String = "",
    @Optional
    val rawPluginNames: MutableSet<PluginBean> = mutableSetOf(),
    @Optional
    var additionalInfo: AgentAdditionalInfo? = null

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AgentInfo

        if (name != other.name) return false
        if (ipAddress != other.ipAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ipAddress.hashCode()
        return result
    }
}

@Serializable
data class AgentAdditionalInfo(
    val jvmInput: List<String>,
    val availableProcessors: Int,
    val arch: String,
    val name: String,
    val version: String,
    val systemProperties: Map<String, String>
)

@Serializable
data class AgentBuildVersion (val version: String)