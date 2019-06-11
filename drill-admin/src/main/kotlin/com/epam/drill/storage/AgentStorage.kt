package com.epam.drill.storage

import com.epam.drill.common.AgentInfo
import com.epam.drill.endpoints.DrillWsSession
import io.ktor.http.cio.websocket.DefaultWebSocketSession

typealias AgentStorage = ObservableMapStorage<String, Pair<AgentInfo, DefaultWebSocketSession>, MutableSet<DrillWsSession>>

operator fun AgentStorage.invoke(block: AgentStorage.() -> Unit) {
    block(this)
}
