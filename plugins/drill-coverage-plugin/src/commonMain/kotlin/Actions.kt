package com.epam.drill.plugins.coverage

import kotlinx.serialization.*

@Polymorphic
@Serializable
abstract class Action

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

@SerialName("SWITCH_SCOPE")
@Serializable
data class SwitchScope(val payload: ScopePayload) : Action()

@SerialName("TOGGLE_SCOPE")
@Serializable
data class IgnoreScope(val payload: ToggleScopePayload) : Action()

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
data class ScopePayload(val scopeName: String = "")

@Serializable
data class ToggleScopePayload(val scopeName: String = "", val enabled: Boolean)
