package com.epam.drill.common

import kotlinx.serialization.Serializable



@Serializable
data class AgentEvent(val event: DrillEvent, val data: String = "", val id: String? = "")


enum class DrillEvent {
    LOAD_PLUGIN, UNLOAD_PLUGIN, AGENT_LOAD_SUCCESSFULLY
}