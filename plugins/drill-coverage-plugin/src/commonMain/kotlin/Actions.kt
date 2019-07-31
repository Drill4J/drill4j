package com.epam.drill.plugins.coverage

import kotlinx.serialization.*

@Polymorphic
@Serializable
abstract class Action

@SerialName("VALIDATION_RESULT")
@Serializable
data class ValidationResult(val payload: String) : Action()

@SerialName("START")
@Serializable
data class StartNewSession(val payload: StartPayload) : Action()

@SerialName("START_AGENT_SESSION")
@Serializable
data class StartSession(val payload: StartSessionPayload) : Action()

@SerialName("STOP")
@Serializable
data class StopSession(val payload: SessionPayload) : Action()

@SerialName("CANCEL")
@Serializable
data class CancelSession(val payload: SessionPayload) : Action()

@SerialName("SWITCH_ACTIVE_SCOPE")
@Serializable
data class SwitchActiveScope(val payload: ActiveScopeChangePayload) : Action()

@SerialName("RENAME_ACTIVE_SCOPE")
@Serializable
data class RenameActiveScope(val payload: ActiveScopePayload) : Action()

@SerialName("TOGGLE_SCOPE")
@Serializable
data class ToggleScope(val payload: ScopePayload) : Action()

@SerialName("DROP_SCOPE")
@Serializable
data class DropScope(val payload: ScopePayload) : Action()

@Serializable
data class StartPayload(val testType: String)

@Serializable
data class StartSessionPayload(val sessionId: String, val startPayload: StartPayload)

@Serializable
data class SessionPayload(val sessionId: String)

@Serializable
data class ActiveScopeChangePayload(
    val scopeName: String,
    val savePrevScope: Boolean = false,
    val prevScopeEnabled: Boolean = true
)

@Serializable
data class ActiveScopePayload(val scopeName: String)

@Serializable
data class ScopePayload(val scopeId: String = "")
