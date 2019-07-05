package com.epam.drill.plugins.coverage

typealias Scopes = Set<String>

data class Scope(
    val name: String,
    val probes: MutableList<ExDataTemp> = mutableListOf(),
    var enabled: Boolean = true
)

data class ScopesKey(
    val buildVersion: String
) : StoreKey<Scopes>

data class ScopeKey(
    val buildVersion: String,
    val name: String
) : StoreKey<Scope>