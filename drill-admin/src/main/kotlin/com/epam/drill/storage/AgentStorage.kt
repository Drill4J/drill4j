package com.epam.drill.storage

import com.epam.drill.endpoints.*

typealias AgentStorage = ObservableMapStorage<String, AgentEntry, MutableSet<DrillWsSession>>
