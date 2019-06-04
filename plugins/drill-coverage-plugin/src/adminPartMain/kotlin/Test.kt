package com.epam.drill.plugins.coverage

data class Test(
    val id: String? = null,
    val testName: String? = null,
    val testType: Int? = null,
    val data: List<ClassData>? = null
)