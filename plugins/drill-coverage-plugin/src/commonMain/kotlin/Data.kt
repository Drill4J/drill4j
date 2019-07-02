package com.epam.drill.plugins.coverage


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
    START, STOP, CANCEL, CREATE_SCOPE, DROP_SCOPE
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
}