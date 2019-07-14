package com.epam.drill.plugins.coverage

typealias Scopes = Map<String, ScopeSummary>

data class Scope(
    val name: String = "",
    val probes: MutableList<ExDataTemp> = mutableListOf(),
    var enabled: Boolean = true
) {
    var started: Long = 0L
        private set

    var finished: Long? = null
        private set

    var duration: Long = 0L
        private set

    var sessionCount: Int = 0
        private set

    init {
        start()
    }

    fun start() {
        started = currentTimeMillis()
        finished = null
    }

    fun finish() {
        val t = currentTimeMillis()
        finished = t
        duration += t - started
    }

    fun incSessionCount() = sessionCount++
}

data class ScopesKey(
    val buildVersion: String
) : StoreKey<Scopes>

data class ScopeKey(
    val buildVersion: String,
    val name: String = ""
) : StoreKey<Scope>