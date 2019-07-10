package com.epam.drill.common

import com.epam.drill.dataclasses.*
import com.epam.drill.endpoints.*
import com.epam.drill.service.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import java.util.*


object AgentInfos : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", length = 50).primaryKey().entityId().uniqueIndex()
    val name = varchar("name", length = 50)
    val groupName = varchar("group_name", length = 50).nullable()
    val description = varchar("description", length = 50)
    var status = enumeration("status", AgentStatus::class)
    val adminUrl = varchar("admin_url", length = 50)
    val buildVersion = varchar("build_version", length = 50)
    val buildAlias = varchar("build_alias", length = 50)
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
    var groupName by AgentInfos.groupName
    var description by AgentInfos.description
    var status by AgentInfos.status
    var adminUrl by AgentInfos.adminUrl
    var buildVersion by AgentInfos.buildVersion
    var buildAlias by AgentInfos.buildAlias
    var plugins by PluginBeanDb via APConnectedTable
    var buildVersions by AgentBuildVersion via ABVsConnectedTable
}

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
        status = this.status,
        groupName = this.groupName,
        description = this.description,
        buildVersion = this.buildVersion,
        buildAlias = this.buildAlias,
        adminUrl = this.adminUrl,
        plugins = this@toAgentInfo.plugins.map { it.toPluginBean() }.toMutableSet(),
        buildVersions = this.buildVersions.map { it.toAgentBuildVersionJson() }.toMutableSet()
    )

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

suspend fun AgentInfo.update(agentManager: AgentManager) {
    val ai = this
    asyncTransaction {
        AgentInfoDb.findById(this@update.id)?.apply {
            name = ai.name
            groupName = ai.groupName
            description = ai.description
            status = ai.status
            adminUrl = ai.adminUrl
            buildVersion = ai.buildVersion
            buildAlias = ai.buildAlias
            plugins = SizedCollection(ai.plugins.map { plugin ->
                PluginBeanDb.find { PluginBeans.pluginId eq plugin.id }.firstOrNull()?.fill(plugin)
                    ?: PluginBeanDb.new {
                        fill(plugin)
                    }
            })

            buildVersions = SizedCollection(ai.buildVersions.map { bv ->
                AgentBuildVersion.find { AgentBuildVersions.buildVersion eq bv.id }.firstOrNull()
                    ?.fill(bv) ?: AgentBuildVersion.new { fill(bv) }
            })
        }
    }
    agentManager.update()
    agentManager.singleUpdate(this.id)
}

fun PluginBeanDb.fill(plugin: PluginBean) = this.apply {
    this.pluginId = plugin.id
    this.name = plugin.name
    this.description = plugin.description
    this.type = plugin.type
    this.family = plugin.family
    this.enabled = plugin.enabled
    this.config = plugin.config
}

fun AgentBuildVersion.fill(plugin: AgentBuildVersionJson) = this.apply {
    this.buildVersion = plugin.id
    this.name = plugin.name
}