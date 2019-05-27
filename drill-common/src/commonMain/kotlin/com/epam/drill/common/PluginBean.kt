package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class PluginBean(
    val id: String,
    var name: String = "",
    var description: String = "",
    var type: String = "",
    var family: Family = Family.INSTRUMENTATION,
    var enabled: Boolean = true,
    var config: String = ""
)

enum class Family {
    GENERIC, INSTRUMENTATION
}