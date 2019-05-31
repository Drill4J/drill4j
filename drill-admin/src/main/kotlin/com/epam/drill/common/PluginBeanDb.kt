package com.epam.drill.common

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "PLUGIN_BEAN")
data class PluginBeanDb(
    @Id
    @Column(name = "PLUGIN_ID")
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var type: String = "",
    var family: Family = Family.INSTRUMENTATION,
    var enabled: Boolean = true,
    var config: String = ""
)

fun PluginBeanDb.toPluginBean() =
    PluginBean(
        id = this.id,
        name = this.name,
        description = this.description,
        type = this.type,
        family = this.family,
        enabled = this.enabled,
        config = this.config
    )