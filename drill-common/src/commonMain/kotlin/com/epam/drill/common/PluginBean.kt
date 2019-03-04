package com.epam.drill.common

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
class PluginBean {
    lateinit var id: String
    @Optional
    open var enabled: Boolean? = true
}