package com.epam.drill.core.request

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
open class MessageWrapper(var pluginId: String, var drillMessage: DrillMessage)
