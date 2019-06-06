package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class AgentInfo(
    val id: String,
    var name: String,
    var status: AgentStatus,
    var groupName: String? = "",
    var description: String,
    var isEnable: Boolean,
    var buildVersion: String,

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
data class AgentBuildVersionJson(val id: String, val name: String)

enum class AgentStatus(val code: Int) {
    NOT_REGISTERED(0),
    READY(1);

    companion object {
        fun from(findValue: Int): AgentStatus = AgentStatus.values().first { it.code == findValue }
    }
}