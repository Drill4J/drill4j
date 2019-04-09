package com.epam.drill.plugins.coverage


@kotlinx.serialization.Serializable
data class CoverConfig(
    val packageNames: List<String>,
    val message: String
)


@kotlinx.serialization.Serializable
data class CoverageAction(
    val sessionId: String,
    val isRecord: Boolean
)


@kotlinx.serialization.Serializable
data class CoverageMessage(val type: CoverageEventType, val data: String)



enum class CoverageEventType {
    INIT,
    CLASS_BYTES,
    INITIALIZED,
    COVERAGE_DATA
}

@kotlinx.serialization.Serializable
data class ExDataTemp(val id: Long, val className: String, val probes: List<Boolean>)


@kotlinx.serialization.Serializable
data class ClassBytes(val className: String, val bytes: List<Byte>)


