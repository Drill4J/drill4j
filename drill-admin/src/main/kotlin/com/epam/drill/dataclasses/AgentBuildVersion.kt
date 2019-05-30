package com.epam.drill.dataclasses

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "AGENT_BUILD_VERSION")
data class AgentBuildVersion(
    @Id @Column(name = "ID")
    val version: String? = null,
    @Column
    val name: String? = null
)