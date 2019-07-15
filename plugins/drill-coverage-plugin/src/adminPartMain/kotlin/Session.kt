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
    private val _probes = atomic(list<ExDataTemp>())

    fun append(probe: ExDataTemp) {
        _probes.update { it.append(probe) }
    }

    fun finish() = FinishedSession(
            id = id,
            testType = testType,
            probes = _probes.value.asIterable().groupBy { it.testName }
    )
}

class FinishedSession(
        id: String,
        testType: String,
        val probes: Map<String, List<ExDataTemp>>
) : Session(id, testType), Sequence<ExDataTemp> {

    override fun iterator() = probes.values.flatten().iterator()
}