package com.epam.drill.storage

import com.epam.drill.endpoints.AgentEntry
import com.epam.drill.endpoints.DrillWsSession

typealias AgentStorage = ObservableMapStorage<String, AgentEntry, MutableSet<DrillWsSession>>

operator fun AgentStorage.invoke(block: AgentStorage.() -> Unit) {
    block(this)
}
