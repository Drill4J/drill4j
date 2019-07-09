package com.epam.drill.plugins.coverage

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

@kotlinx.serialization.SerialName("TOGGLE_SCOPE")
@kotlinx.serialization.Serializable
data class IgnoreScope(val payload: ToggleScopePayload) : Action()

@kotlinx.serialization.SerialName("DROP_SCOPE")
@kotlinx.serialization.Serializable
data class DropScope(val payload: ScopePayload) : Action()

@kotlinx.serialization.Serializable
data class SessionPayload(val sessionId: String)

@kotlinx.serialization.Serializable
data class ScopePayload(val scopeName: String = "")

@kotlinx.serialization.Serializable
data class ToggleScopePayload(val scopeName: String = "", val enabled: Boolean)
