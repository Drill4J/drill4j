package com.epam.drill.common

import kotlinx.serialization.*

@Serializable
data class PluginAction(
    val id: String,
    val message: String
)