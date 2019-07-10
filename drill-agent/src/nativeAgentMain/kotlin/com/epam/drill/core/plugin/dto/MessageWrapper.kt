package com.epam.drill.core.plugin.dto

import kotlinx.serialization.*

@Suppress("unused")
@Serializable
open class MessageWrapper(var pluginId: String, var drillMessage: DrillMessage)
