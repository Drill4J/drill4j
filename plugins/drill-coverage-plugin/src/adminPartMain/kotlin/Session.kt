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
            probes = _probes.value.asSequence().groupBy { TypedTest(it.testName, testType) }
    )
}

class FinishedSession(
        id: String,
        testType: String,
        val probes: Map<TypedTest, List<ExecClassData>>
) : Session(id, testType), Sequence<ExecClassData> {

    val testNames = probes.keys

    override fun iterator() = probes.asSequence()
        .flatMap { it.value.asSequence() }
        .iterator()
}

data class TypedTest(val name: String, val type: String) {
    override fun toString() = "$type::$name"
}
