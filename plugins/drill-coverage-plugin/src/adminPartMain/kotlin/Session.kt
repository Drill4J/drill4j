package com.epam.drill.plugins.coverage

import io.vavr.kotlin.*
import kotlinx.atomicfu.*

sealed class Session(
        val id: String,
        val testType: String
)

class ActiveSession(
        id: String,
        testType: String
) : Session(id, testType) {
    private val _probes = atomic(list<ExecClassData>())

    fun append(probe: ExecClassData) {
        _probes.update { it.append(probe) }
    }

    fun finish() = FinishedSession(
            id = id,
            testType = testType,
            probes = _probes.value.asIterable().groupBy { "$testType::${it.testName}" }
    )
}

class FinishedSession(
        id: String,
        testType: String,
        val probes: Map<String, List<ExecClassData>>
) : Session(id, testType), Sequence<ExecClassData> {
    
    val testNames = probes.keys
    
    override fun iterator() = probes.values.flatten().iterator()
}