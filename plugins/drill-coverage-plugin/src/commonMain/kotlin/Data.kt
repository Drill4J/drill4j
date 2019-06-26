package com.epam.drill.plugins.coverage

import kotlinx.serialization.Serializable


@kotlinx.serialization.Serializable
data class CoverConfig(
    val pathPrefixes: List<String>,
    val message: String
)


@kotlinx.serialization.Serializable
data class CoverageAction(
    val sessionId: String,
    val scopeName: String = ""
)

@kotlinx.serialization.Serializable
data class Action(val type: ActionType, val payload: CoverageAction)

enum class ActionType {
    START, STOP, CANCEL, CREATE_SCOPE, CLOSE_SCOPE, DROP_SCOPE, MANAGE_SCOPE
}

@kotlinx.serialization.Serializable
data class CoverageMessage(val type: CoverageEventType, val data: String)

enum class CoverageEventType {
    INIT,
    CLASS_BYTES,
    INITIALIZED,
    SESSION_STARTED,
    COVERAGE_DATA_PART,
    SESSION_FINISHED,
    SESSION_CANCELLED
}

@kotlinx.serialization.Serializable
data class InitInfo(val classesCount: Int, val message: String)

@kotlinx.serialization.Serializable
data class ClassBytes(val className: String, val bytes: List<Byte>)

@kotlinx.serialization.Serializable
data class ExDataTemp(
    val id: Long,
    val className: String,
    val probes: List<Boolean>,
    val testName: String? = null,
    val testType: TestType
)

enum class TestType {
    AUTO,
    MANUAL,
    PERFORMANCE,
    UNDEFINED;

    companion object {
        operator fun get(input: String?) = when (input) {
            null -> UNDEFINED
            else -> values().firstOrNull { it.name == input } ?: UNDEFINED
        }
    }
}

data class Scope(
    val name: String,
    val probes: MutableSet<ExDataTemp> = mutableSetOf(),
    var accounted: Boolean = true
)

@Serializable
data class ActiveScope(
    val name: String,
    val accounted: Boolean
)