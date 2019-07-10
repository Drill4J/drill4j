package com.epam.drill.core.plugin.dto

import kotlinx.serialization.*

@Serializable
data class DrillMessage(var sessionId: String? = null, var content: String?)
