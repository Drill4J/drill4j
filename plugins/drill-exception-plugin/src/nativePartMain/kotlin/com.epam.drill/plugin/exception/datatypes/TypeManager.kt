package com.epam.drill.plugin.exception.datatypes


import com.epam.drill.jvmapi.gen.*
import com.epam.drill.plugin.exception.*
import kotlinx.cinterop.*


fun jvmtiLocalVariableEntry.createVariableLine(thread:jthread, depth:Int): VariableLine? {
    val signature: CPointer<ByteVar>? = this.signature
    val signa = signature?.toKString()
    val strategies: Map<String, JType> = mapOf(
        "I" to IntType(),
        "B" to ByteType(),
        "C" to CharType(),
        "D" to DoubleType(),
        "F" to FloatType(),
        "J" to LongType(),
        "S" to ShortType(),
        "Z" to BoolType(),
        "[" to ArrayType()
    )
    val type = strategies[signa?.take(1)] ?: return null

    val name = this.name?.toKString()
    if (name == "this") return null
    val retrieveValue = type.retrieveValue(thread, depth, this)
    retrieveValue ?: return null
    return VariableLine(retrieveValue.first, name, retrieveValue.second.toString())

}