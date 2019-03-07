package com.epam.drill.common

import kotlinx.serialization.Serializable

@Serializable
class PluginBean {
    lateinit var id: String
    var enabled: Boolean = true
    var config: String = ""
}