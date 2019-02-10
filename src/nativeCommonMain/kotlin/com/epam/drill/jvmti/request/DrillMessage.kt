package com.epam.drill.jvmti.request

import kotlinx.serialization.Serializable

@Serializable
data class DrillMessage(var sessionId: String? = null, var content: String?)
