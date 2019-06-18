package com.epam.drill.plugin.exception

import kotlinx.serialization.Serializable


@Serializable
data class Frame(val name: String, val localVariables: List<VariableLine>)

@Serializable
data class VariableLine(val typeName: String?, val variableName: String?, val variableValue: String)

@Serializable
data class ExceptionDataClass(
    val type: String,
    val message: String,
    val id: String = "0",
    val stackTrace: List<Frame>,
    val occurredTime: String
)
