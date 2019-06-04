package com.epam.drill.plugins.coverage

data class ClassData(
    val id: Long? = null,
    val className: String? = null,
    val probes: List<Boolean>? = null
)