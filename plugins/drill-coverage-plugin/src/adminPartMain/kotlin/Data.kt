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
    val coverage: Double,
    val uncoveredMethodsCount: Int,
    val newMethodsCount: Int = 0,
    val newMethodsCovered: Int = 0,
    val newMethodsCoverage: Double = 100.0
)

@Serializable
data class NewCoverageBlock(
    val methodsCount: Int = 0,
    val methodsCovered: Int = 0,
    val coverage: Double = 100.0
)


