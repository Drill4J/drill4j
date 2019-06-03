package com.epam.drill.dataclasses

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.Column

object AgentBuildVersions : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("version", length = 100).primaryKey().entityId()
    val name = varchar("name", length = 100)
}


class AgentBuildVersion(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AgentBuildVersion>(AgentBuildVersions)

    var name by AgentBuildVersions.name
}

fun AgentBuildVersion.toAgentBuildVersionJson() =
    AgentBuildVersionJson(this.id.value, this.name)

data class AgentBuildVersionJson(val id: String, val name: String)
