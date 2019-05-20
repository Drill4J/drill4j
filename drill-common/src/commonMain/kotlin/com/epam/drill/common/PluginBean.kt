package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
data class PluginBean(
    var id: String, var name: String = "", var description: String = "", var type: String = "",
    var enabled: Boolean = true, var config: String = ""
)