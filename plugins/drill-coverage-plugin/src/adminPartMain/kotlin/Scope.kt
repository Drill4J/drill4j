package com.epam.drill.plugins.coverage

import io.vavr.kotlin.*
import kotlinx.atomicfu.*
import kotlin.math.*

class ActiveScope(
        val name: String = ""
) : Sequence<FinishedSession> {

    private val _sessions = atomic(list<FinishedSession>())
    
    val started: Long = currentTimeMillis()

    private val _summary = atomic(ScopeSummary(
        name = name,
        started = started
    ))
    
    val summary get() = _summary.value

    val sessionCount get() = _sessions.value.count()
    
    val probes get() = _sessions.value.asSequence().flatten()


    fun update(session: FinishedSession, classesData: ClassesData): ScopeSummary {
        _sessions.update { it.append(session) }
        return _summary.updateAndGet {
            ScopeSummary(
                name = name,
                started = started,
                coverage = classesData.coverage(this.flatten()),
                coveragesByType = this.groupBy { it.testType }.mapValues { (_, l) ->
                    TestTypeSummary(
                        coverage = classesData.coverage(l.asSequence().flatten()),
                        testCount = l.flatMap { it.testNames }.toSet().count()
                    )
                }
            )
        }
    }

    fun finish() = FinishedScope(
        id = genUuid(),
        name = name,
        started = started,
        finished = currentTimeMillis(),
        summary = summary,
        probes = _sessions.value.asIterable().groupBy { it.testType }
    )

    override fun iterator(): Iterator<FinishedSession> = _sessions.value.iterator()
}

fun ActiveScope.arrowType(totalCoveragePercent: Double): ArrowType? {
    val diff = totalCoveragePercent - summary.coverage
    return when {
        abs(diff) < 1E-7 -> null
        diff > 0.0 -> ArrowType.INCREASE
        else -> ArrowType.DECREASE
    }
}

class FinishedScope(
    val id: String,
    val name: String,
    val started: Long,
    val finished: Long,
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