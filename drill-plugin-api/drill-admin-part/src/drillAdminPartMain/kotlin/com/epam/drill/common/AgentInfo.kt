package com.epam.drill.common

import kotlinx.serialization.*

@Serializable
data class AgentInfo(
    val id: String,
    var name: String,
    var status: AgentStatus,
    var groupName: String? = "",
    var description: String,
    var buildVersion: String,
    var buildAlias: String,

    val adminUrl: String = "",
    var ipAddress: String = "",
    val buildVersions: MutableSet<AgentBuildVersionJson> = mutableSetOf(),
    val plugins: MutableSet<PluginBean> = mutableSetOf()

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
data class AgentBuildVersionJson(val id: String, var name: String)

enum class AgentStatus{
    NOT_REGISTERED,
    READY,
    DISABLED;
}