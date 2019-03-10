package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class PluginBean(
    var id: String, var enabled: Boolean = true, var config: String = ""
)