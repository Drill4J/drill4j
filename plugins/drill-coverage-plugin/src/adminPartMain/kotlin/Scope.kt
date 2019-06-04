package com.epam.drill.plugins.coverage

class Scope(
    val id: String? = null,
    val name: String? = null,
    val buildVersion: String? = null,
    val tests: List<Test>? = null
)