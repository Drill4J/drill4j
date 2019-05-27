package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val id: String,
    val name: String,
    val groupName: String,
    val description: String,
    var isEnable: Boolean,
    val buildVersion: String,

    val adminUrl: String = "",
    var ipAddress: String = "",
    val rawPluginNames: MutableSet<PluginBean> = mutableSetOf(),
    var additionalInfo: AgentAdditionalInfo? = null

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AgentInfo

        if (id != other.id) return false
        if (buildVersion != other.buildVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + buildVersion.hashCode()
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
data class AgentBuildVersion(val version: String)

const val AgentIdParam = "agentId"
const val NeedSyncParam = "needSync"