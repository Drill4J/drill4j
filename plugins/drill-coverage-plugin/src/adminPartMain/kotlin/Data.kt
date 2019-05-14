package com.epam.drill.plugins.coverage

import kotlinx.serialization.Serializable

@Serializable
data class JavaClass(
    val name: String,
    val path: String,
    val methods: Set<JavaMethod>
)

@Serializable
data class JavaMethod(
    val ownerClass: String,
    val name: String,
    val desc: String
)

@Serializable
data class CoverageBlock(
    val coverage: Double?,
    val classesCount: Int = 0,
    val methodsCount: Int = 0,
    val uncoveredMethodsCount: Int = 0,
    val arrow: ArrowType? = null
)

enum class ArrowType {
    INCREASE,
    DECREASE
}

@Serializable
data class NewCoverageBlock(
    val methodsCount: Int = 0,
    val methodsCovered: Int = 0,
    val coverage: Double? = null,
    val methods: List<JavaMethod> = emptyList()
)

@Serializable
data class JavaPackageCoverage(
    val id: String,
    val name: String,
    val coverage: Double?,
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
    val coverage: Double?,
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
    val coverage: Double?,
    val assocTestsCount: Int?
)

@Serializable
data class NewMethodsCoverages(
    val values :List<SimplifiedJavaMethodCoverage> = emptyList()
)

@Serializable
data class SimplifiedJavaMethodCoverage(
    val name: String,
    val desc: String,
    val coverage: Double?
)

@Serializable
data class AssociatedTests(
    val id: String,
    val packageName: String?,
    val className: String?,
    val methodName: String?,
    val tests: List<String>
)

