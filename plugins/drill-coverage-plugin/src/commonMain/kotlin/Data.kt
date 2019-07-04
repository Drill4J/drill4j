package com.epam.drill.plugins.coverage

@kotlinx.serialization.Serializable
data class CoverConfig(
    val pathPrefixes: List<String>,
    val message: String
)

@kotlinx.serialization.Polymorphic
@kotlinx.serialization.Serializable
abstract class Action

@kotlinx.serialization.SerialName("START")
@kotlinx.serialization.Serializable
data class StartSession(val payload: SessionPayload) : Action()

@kotlinx.serialization.SerialName("STOP")
@kotlinx.serialization.Serializable
data class StopSession(val payload: SessionPayload) : Action()

@kotlinx.serialization.SerialName("CANCEL")
@kotlinx.serialization.Serializable
data class CancelSession(val payload: SessionPayload) : Action()


@kotlinx.serialization.SerialName("SWITCH_SCOPE")
@kotlinx.serialization.Serializable
data class SwitchScope(val payload: ScopePayload) : Action()

@kotlinx.serialization.SerialName("IGNORE_SCOPE")
@kotlinx.serialization.Serializable
data class IgnoreScope(val payload: ScopePayload) : Action()

@kotlinx.serialization.SerialName("DROP_SCOPE")
@kotlinx.serialization.Serializable
data class DropScope(val payload: ScopePayload) : Action()

@kotlinx.serialization.Serializable
data class SessionPayload(val sessionId: String)

@kotlinx.serialization.Serializable
data class ScopePayload(val scopeName: String = "")


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