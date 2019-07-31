package com.epam.drill.plugins.coverage

import kotlinx.serialization.*

@Serializable
data class CoverConfig(
        val pathPrefixes: List<String>,
        val message: String = ""
)

@Polymorphic
@Serializable
abstract class CoverMessage

@SerialName("INIT")
@Serializable
data class InitInfo(
        val classesCount: Int,
        val message: String
) : CoverMessage()

typealias EncodedString = String

@SerialName("CLASS_BYTES")
@Serializable
data class ClassBytes(
        val className: String,
        val bytes: EncodedString
) : CoverMessage()

@SerialName("INITIALIZED")
@Serializable
data class Initialized(val msg: String) : CoverMessage()

@SerialName("SESSION_STARTED")
@Serializable
data class SessionStarted(val sessionId: String, val testType: String, val ts: Long) : CoverMessage()

@SerialName("SESSION_CANCELLED")
@Serializable
data class SessionCancelled(val sessionId: String, val ts: Long) : CoverMessage()

@SerialName("COVERAGE_DATA_PART")
@Serializable
data class CoverDataPart(val sessionId: String, val data: List<ExecClassData>) : CoverMessage()

@SerialName("SESSION_FINISHED")
@Serializable
data class SessionFinished(val sessionId: String, val ts: Long) : CoverMessage()

@Serializable
data class ExecClassData(
        val id: Long,
        val className: String,
        val probes: List<Boolean>,
        val testName: String = ""
)
