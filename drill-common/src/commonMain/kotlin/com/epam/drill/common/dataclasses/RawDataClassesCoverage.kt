package com.epam.drill.plugins.coverage.dataclasses

class RawScope(
    val id: Long? = null,
    val name: String? = null,
    val buildVersion: String? = null,
    val tests: List<RawTest>
)

data class RawTest(
    val id: Long,
    val testName: String? = null,
    val testType: Int? = null,
    val data: List<RawClassData>
)

data class RawClassData(
    val id: Long,
    val className: String,
    val probes: List<Boolean>
)

enum class TestType(val type: Int) {
    AUTO(0),
    MANUAL(1),
    PERFORMANCE(2)
}
