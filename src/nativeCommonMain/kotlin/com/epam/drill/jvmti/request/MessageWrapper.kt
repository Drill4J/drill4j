package com.epam.drill.jvmti.request

import com.epam.drill.jvmti.request.DrillMessage
import kotlinx.serialization.Serializable

@Serializable
open class MessageWrapper(var pluginId: String, var drillMessage: DrillMessage)
