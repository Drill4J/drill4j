package com.epam.drill.plugins.coverage.dataclasses

import com.epam.drill.plugins.coverage.ExDataTemp

data class RawScope(
    val id: String,
    val name: String? = null,
    val buildVersion: String? = null,
    val tests: MutableMap<String, RawTest> = mutableMapOf()
)

data class RawTest(
    val id: String,
    val typedName: String? = null,
    val name: String? = null,
    val testType: TestType? = null,
    val data: MutableList<RawClassData> = mutableListOf()
)

data class RawClassData(
    val id: Long,
    val className: String,
    val probes: List<Boolean>
)

enum class TestType(val str: Int) {
    AUTO(0),
    MANUAL(1),
    PERFORMANCE(2)
}

fun RawScope.merge(other: RawScope?): Collection<ExDataTemp> {
    other?.tests?.forEach { (testName, test) ->
        tests[testName] = test
    }
    return retrieveRawData()
}

fun RawScope.retrieveRawData(): Collection<ExDataTemp> {
    val result: MutableSet<ExDataTemp> = mutableSetOf()
    tests.values.forEach { test ->
        test.data.forEach {
            result.add(exData(it, test.name))
        }
    }
    return result
}

fun exData(rawClass: RawClassData, testName: String?) = ExDataTemp(
    rawClass.id,
    rawClass.className,
    rawClass.probes,
    testName
)