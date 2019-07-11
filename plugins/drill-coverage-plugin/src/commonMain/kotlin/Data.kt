package com.epam.drill.plugins.coverage

import kotlinx.serialization.*

@Serializable
data class CoverConfig(
        val pathPrefixes: List<String>,
        val message: String
)

@kotlinx.serialization.Polymorphic
@Serializable
abstract class CoverMessage

@SerialName("INIT")
@Serializable
data class InitInfo(
        val classesCount: Int,
        val message: String
) : CoverMessage()


@SerialName("CLASS_BYTES")
@Serializable
data class ClassBytes(
        val className: String,
        val bytes: List<Byte>
) : CoverMessage()

@SerialName("INITIALIZED")
@Serializable
data class Initialized(val msg: String) : CoverMessage()

@SerialName("SESSION_STARTED")
@Serializable
data class SessionStarted(val ts: Long) : CoverMessage()

@SerialName("SESSION_CANCELLED")
@Serializable
data class SessionCancelled(val ts: Long) : CoverMessage()

@SerialName("COVERAGE_DATA_PART")
@Serializable
data class CoverDataPart(val data: List<ExDataTemp>) : CoverMessage()

@SerialName("SESSION_FINISHED")
@Serializable
data class SessionFinished(val ts: Long) : CoverMessage()

@Serializable
data class ExDataTemp(
        val id: Long,
        val className: String,
        val probes: List<Boolean>,
        val testType: String,
        val testName: String? = null
)
