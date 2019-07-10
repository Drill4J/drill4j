package com.epam.drill.plugin.exception.datatypes


import jvmapi.*
import kotlinx.cinterop.*

class FloatType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jfloatVar>()
        GetLocalFloat(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("float", valuePtr.value)
    }



}