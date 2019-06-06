package com.epam.drill.common

import com.epam.drill.agentmanager.AgentInfoWebSocketSingle
import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.dataclasses.AgentBuildVersions
import com.epam.drill.dataclasses.toAgentBuildVersionJson
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.util.*

object AgentInfos : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", length = 50).primaryKey().entityId().uniqueIndex()
    val name = varchar("name", length = 50)
    val status = integer("status")
    val groupName = varchar("group_name", length = 50).nullable()
    val description = varchar("description", length = 50)
    val isEnable = bool("is_enabled")
    val adminUrl = varchar("admin_url", length = 50)
    val buildVersion = varchar("build_version", length = 50)
}


object ABVsConnectedTable : Table() {
    val agentId = reference("agentId", AgentInfos, ReferenceOption.CASCADE).primaryKey(0)
    val buildVersionId = reference("buildVersionId", AgentBuildVersions, ReferenceOption.CASCADE).primaryKey(1)

    init {
        index(true, agentId, buildVersionId)
    }
}

object APConnectedTable : Table() {
    val agentId = reference("agentId", AgentInfos, ReferenceOption.CASCADE).primaryKey(0)
    val pluginId = reference("pluginId", PluginBeans, ReferenceOption.CASCADE).primaryKey(1)

    init {
        index(true, agentId, pluginId)
    }
}

class AgentInfoDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AgentInfoDb>(AgentInfos)

    var name by AgentInfos.name
    var status by AgentInfos.status
    var groupName by AgentInfos.groupName
    var description by AgentInfos.description
    var isEnable by AgentInfos.isEnable
    var adminUrl by AgentInfos.adminUrl
    var buildVersion by AgentInfos.buildVersion
    var plugins by PluginBeanDb via APConnectedTable
    var buildVersions by AgentBuildVersion via ABVsConnectedTable

}

fun AgentInfoDb.status() = AgentStatus.from(this.status)

object PluginBeans : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", length = 50).primaryKey().clientDefault {
        UUID.randomUUID().toString()
    }.entityId().uniqueIndex()
    var pluginId = varchar("plugin_id", length = 50)
    var name = varchar("name", length = 50)
    var description = varchar("description", length = 50)
    var type = varchar("type", length = 15)
    var family = enumeration("family", Family::class)
    var enabled = bool("enabled")
    var config = varchar("config", length = 2000)
}

class PluginBeanDb(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PluginBeanDb>(PluginBeans)

    var pluginId by PluginBeans.pluginId
    var name by PluginBeans.name
    var description by PluginBeans.description
    var type by PluginBeans.type
    var family by PluginBeans.family
    var enabled by PluginBeans.enabled
    var config by PluginBeans.config
}


//fun AgentInfoDb.serialize() = Json.stringify(AgentInfo.serializer(), this.toAgentInfo())


fun AgentInfoDb.toAgentInfo() =
    AgentInfo(
        id = this.id.value,
        name = this.name,
        status = AgentStatus.from(this.status),
        groupName = this.groupName,
        description = this.description,
        buildVersion = this.buildVersion,
        isEnable = this.isEnable,
        adminUrl = this.adminUrl,
        plugins = this@toAgentInfo.plugins.map { it.toPluginBean() }.toMutableSet(),
        buildVersions = this.buildVersions.map { it.toAgentBuildVersionJson() }.toMutableSet()
    )

fun AgentInfoDb.merge(au: AgentInfoWebSocketSingle) {
    this.name = au.name
    this.groupName = au.group
    this.description = au.description
    this.buildVersion = au.buildVersion
    this.status = au.status.code
    au.buildVersions.forEach { (k, v) ->
        this.buildVersions.forEach {
            if (it.buildVersion == k) {
                it.name = v
            }

        }

    }
}

fun PluginBeanDb.toPluginBean() =
    PluginBean(
        id = this.pluginId,
        name = this.name,
        description = this.description,
        type = this.type,
        family = this.family,
        enabled = this.enabled,
        config = this.config
    )