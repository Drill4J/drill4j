package com.epam.drill.plugins.coverage

import io.vavr.kotlin.*
import kotlinx.atomicfu.*

class ActiveScope(
    name: String
) : Sequence<FinishedSession> {

    val id = genUuid()

    private val _sessions = atomic(list<FinishedSession>())

    val started: Long = currentTimeMillis()

    private val _summary = atomic(ScopeSummary(
        id = id,
        name = name,
        started = started
    ))
    
    val summary get() = _summary.value
    
    val name = summary.name

    val activeSessions = AtomicCache<String, ActiveSession>()

    fun update(session: FinishedSession, classesData: ClassesData): ScopeSummary {
        _sessions.update { it.append(session) }
        return _summary.updateAndGet { summary ->
            summary.copy(
                coverage = classesData.coverage(this),
                coveragesByType = classesData.coveragesByTestType(this)
            )
        }
    }

    fun rename(name: String): ScopeSummary = _summary.getAndUpdate { it.copy(name = name) }

    fun finish(enabled: Boolean) = FinishedScope(
        id = id,
        name = summary.name,
        enabled = enabled,
        summary = summary.copy(finished = currentTimeMillis(), active = false, enabled = enabled),
        probes = _sessions.value.asIterable().groupBy { it.testType }
    )

    override fun iterator(): Iterator<FinishedSession> = _sessions.value.iterator()

    fun startSession(msg: SessionStarted) {
        activeSessions(msg.sessionId) { ActiveSession(msg.sessionId, msg.testType) }
    }

    fun addProbes(msg: CoverDataPart) {
        activeSessions[msg.sessionId]?.let { activeSession ->
            for (probe in msg.data) {
                activeSession.append(probe)
            }
        }
    }

    fun cancelSession(msg: SessionCancelled) = activeSessions.remove(msg.sessionId)

    fun finishSession(msg: SessionFinished): FinishedSession? {
        return when (val activeSession = activeSessions.remove(msg.sessionId)) {
            null -> null
            else -> activeSession.finish()
        }
    }

    override fun toString() = "act-scope($id, $name)"
}

data class FinishedScope(
    val id: String,
    val name: String,
    val summary: ScopeSummary,
    val probes: Map<String, List<FinishedSession>>,
    var enabled: Boolean = true
)  : Sequence<FinishedSession> {
    
    fun toggle() {
        enabled = !enabled
        summary.enabled = enabled
    }
    
    override fun iterator() = probes.values.flatten().iterator()

    override fun toString() = "fin-scope($id, $name)"
}