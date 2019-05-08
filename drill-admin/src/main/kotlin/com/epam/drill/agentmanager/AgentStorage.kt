package com.epam.drill.agentmanager

import com.epam.drill.common.AgentInfo
import com.epam.drill.endpoints.DrillWsSession
import com.epam.drill.endpoints.ObservableMapStorage
import io.ktor.http.cio.websocket.DefaultWebSocketSession

typealias AgentStorage = ObservableMapStorage<AgentInfo, DefaultWebSocketSession, MutableSet<DrillWsSession>>

operator fun AgentStorage.invoke(block: AgentStorage.() -> Unit) {
    block(this)
}

operator fun AgentStorage.get(k: String): DefaultWebSocketSession? {
    return this.entries.associate { it.key.id to it.value }[k]
}

fun AgentStorage.self(k: String) = this.keys.find { it.id == k }

fun AgentStorage.byId(agentId: String): AgentInfo? {
    return this.keys.firstOrNull { it.id == agentId }
}
