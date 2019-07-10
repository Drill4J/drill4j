package com.epam.drill.dataclasses

import com.epam.drill.common.*
import org.jetbrains.exposed.dao.*

object AgentBuildVersions : IntIdTable() {
    val buildVersion = varchar("build_version", length = 100)
    val name = varchar("name", length = 100)
}

class AgentBuildVersion(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AgentBuildVersion>(AgentBuildVersions)

    var buildVersion by AgentBuildVersions.buildVersion
    var name by AgentBuildVersions.name
}

fun AgentBuildVersion.toAgentBuildVersionJson() = AgentBuildVersionJson(this.buildVersion, this.name)
