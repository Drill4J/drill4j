package com.epam.drill.storage

import com.epam.drill.common.AgentInfo
import com.epam.drill.endpoints.DrillWsSession
import io.ktor.http.cio.websocket.DefaultWebSocketSession

typealias AgentStorage = ObservableMapStorage<String, Pair<AgentInfo, DefaultWebSocketSession>, MutableSet<DrillWsSession>>

operator fun AgentStorage.invoke(block: AgentStorage.() -> Unit) {
    block(this)
}

operator fun AgentStorage.get(k: String): DefaultWebSocketSession? {
    val defaultWebSocketSession = this.targetMap[k]
    return defaultWebSocketSession?.second
}

fun AgentStorage.self(k: String): String? {
    val find = this.keys.find { it == k }
    return find
}

fun AgentStorage.byId(agentId: String) = this.targetMap[agentId]?.first

