package com.epam.drill.common

import com.epam.drill.endpoints.AgentUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "AGENT_INFO")
data class AgentInfoDb(
    @Id
    var id: String = "",
    var name: String = "",
    var groupName: String = "",
    var description: String = "",
    var isEnable: Boolean = false,
    var buildVersion: String = "",
    var adminUrl: String = "",
    var ipAddress: String = "",
    @OneToMany(cascade = [CascadeType.ALL])
    var rawPluginNames: MutableSet<PluginBeanDb> = mutableSetOf()
)

fun AgentInfoDb.serialize() = Json.stringify(AgentInfo.serializer(), this.toAgentInfo())


fun AgentInfoDb.toAgentInfo() =
    AgentInfo(
        id = this.id,
        name = this.name,
        groupName = this.groupName,
        description = this.description,
        ipAddress = this.ipAddress,
        buildVersion = this.buildVersion,
        isEnable = this.isEnable,
        adminUrl = this.adminUrl,
        rawPluginNames = this.rawPluginNames.map { it.toPluginBean() }.toMutableSet()
    )

fun AgentInfoDb.merge(au: AgentUpdate) {
    this.name = au.name
    this.groupName = au.groupName
    this.description = au.description
    this.buildVersion = au.buildVersion
}
