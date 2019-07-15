package com.epam.drill.plugins.coverage

import io.vavr.kotlin.*
import kotlinx.atomicfu.*

typealias Scopes = Map<String, ScopeSummary>

class ActiveScope(
        val name: String = ""
) : Sequence<FinishedSession> {

    private val _sessions = atomic(list<FinishedSession>())
    
    private val _lastCoverage = atomic(0.0)

    var lastCoverage: Double
        get() = _lastCoverage.value
        set(value) = _lastCoverage.update { value }
    
    val started: Long = currentTimeMillis()

    val sessionCount get() = _sessions.value.count()
    
    val probes get() = _sessions.value.asSequence().flatten()

    
    fun append(session: FinishedSession) {
        _sessions.update { it.append(session) }
    }

    fun finish() = FinishedScope(
        id = genUuid(),
        name = name,
        started = started,
        finished = currentTimeMillis(),
        probes = _sessions.value.asIterable().groupBy { it.testType }
    )

    override fun iterator(): Iterator<FinishedSession> = _sessions.value.iterator()
}

class FinishedScope(
    val id: String,
    val name: String,
    val started: Long,
    val finished: Long,
    val probes: Map<String, List<FinishedSession>>,
    var enabled: Boolean = true
)  : Sequence<FinishedSession> {
    val duration = finished - started

    override fun iterator() = probes.values.flatten().iterator()
}

data class ScopesKey(
        val buildVersion: String
) : StoreKey<Scopes>
