package com.epam.drill.plugins.coverage

import io.vavr.kotlin.*
import kotlinx.atomicfu.*

class ActiveScope(
    name: String? = null
) : Sequence<FinishedSession> {

    val id = genUuid()

    private val _sessions = atomic(list<FinishedSession>())

    private val _name = atomic(name ?: id)

    var name: String
        get() = _name.value
        set(value) {
            _name.value = value
        } 

    val started: Long = currentTimeMillis()

    private val _summary = atomic(ScopeSummary(
        id = id,
        name = this.name,
        started = started
    ))
    
    val summary get() = _summary.value

    fun update(session: FinishedSession, classesData: ClassesData): ScopeSummary {
        _sessions.update { it.append(session) }
        return _summary.updateAndGet {
            ScopeSummary(
                id = id,
                name = name,
                started = started,
                coverage = classesData.coverage(this.flatten()),
                coveragesByType = this.groupBy { it.testType }.mapValues { (testType, finishedSessions) ->
                    TestTypeSummary(
                        testType = testType,
                        coverage = classesData.coverage(finishedSessions.asSequence().flatten()),
                        testCount = finishedSessions.flatMap { it.testNames }.toSet().count()
                    )
                }
            )
        }
    }

    fun finish() = FinishedScope(
        id = id,
        name = name,
        summary = summary.copy(finished = currentTimeMillis(), active = false),
        probes = _sessions.value.asIterable().groupBy { it.testType }
    )

    override fun iterator(): Iterator<FinishedSession> = _sessions.value.iterator()
}

class FinishedScope(
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
}