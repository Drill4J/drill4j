package com.epam.drill.plugins.coverage

import kotlinx.serialization.*

@Serializable
data class JavaMethod(
    val ownerClass: String,
    val name: String,
    val desc: String,
    val hash: String?,
    val coverageRate: CoverageRate = CoverageRate.MISSED
) {

    val sign = "$name$desc"

    fun nameModified(otherMethod: JavaMethod) = hash == otherMethod.hash && desc == otherMethod.desc

    fun descriptorModified(otherMethod: JavaMethod) = name == otherMethod.name && hash == otherMethod.hash

    fun bodyModified(otherMethod: JavaMethod) = name == otherMethod.name && desc == otherMethod.desc

}

enum class CoverageRate {
    MISSED,
    PARTLY,
    FULL
}

@Serializable
data class Coverage(
    val coverage: Double,
    val coverageByType: Map<String, TestTypeSummary>,
    val arrow: ArrowType? = null
)

enum class ArrowType {
    INCREASE,
    DECREASE
}

@Serializable
data class BuildMethods(
    val totalMethods: MethodsInfo = MethodsInfo(),
    val newMethods: MethodsInfo = MethodsInfo(),
    val modifiedNameMethods: MethodsInfo = MethodsInfo(),
    val modifiedDescMethods: MethodsInfo = MethodsInfo(),
    val modifiedBodyMethods: MethodsInfo = MethodsInfo(),
    val deletedMethods: MethodsInfo = MethodsInfo()
)

@Serializable
data class MethodsInfo(
    val totalCount: Int = 0,
    val coveredCount: Int = 0,
    val methods: Methods = emptyList()
)

@Serializable
data class JavaPackageCoverage(
    val id: String,
    val name: String,
    val coverage: Double,
    val totalClassesCount: Int,
    val coveredClassesCount: Int,
    val totalMethodsCount: Int,
    val coveredMethodsCount: Int,
    val classes: List<JavaClassCoverage>,
    val assocTestsCount: Int?
)

@Serializable
data class JavaClassCoverage(
    val id: String,
    val name: String,
    val path: String,
    val coverage: Double,
    val totalMethodsCount: Int,
    val coveredMethodsCount: Int,
    val methods: List<JavaMethodCoverage>,
    val assocTestsCount: Int?
)

@Serializable
data class JavaMethodCoverage(
    val id: String,
    val name: String,
    val desc: String,
    val decl: String,
    val coverage: Double,
    val assocTestsCount: Int?
)

@Serializable
data class SimpleJavaMethodCoverage(
    val name: String,
    val desc: String,
    val ownerClass: String,
    val coverage: Double
)

@Serializable
data class AssociatedTests(
    val id: String,
    val packageName: String?,
    val className: String?,
    val methodName: String?,
    val tests: List<String>
)

@Serializable
data class TestUsagesInfo(
    val testName: String,
    val methodCalls: Int,
    val testType: String
)

@Serializable
data class ActiveSessions(
    val count: Int,
    val testTypes: Set<String>)

@Serializable
data class ScopeSummary(
    val name: String,
    val id: String,
    val started: Long,
    val finished: Long? = null,
    val coverage: Double = 0.0,
    var enabled: Boolean = true,
    val active: Boolean = true,
    val coveragesByType: Map<String, TestTypeSummary> = emptyMap()
)

@Serializable
data class TestTypeSummary(
    val testType: String,
    val coverage: Double = 0.0,
    val testCount: Int = 0
)
