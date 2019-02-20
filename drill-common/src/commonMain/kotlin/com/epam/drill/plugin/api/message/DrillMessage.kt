package com.epam.drill.plugin.api.message

import kotlinx.serialization.Serializable

@Serializable
data class DrillMessage(var sessionId: String? = null, var content: String?)
