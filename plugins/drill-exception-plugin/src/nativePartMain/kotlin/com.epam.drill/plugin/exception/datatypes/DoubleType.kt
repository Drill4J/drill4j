package com.epam.drill.plugin.exception.datatypes


import jvmapi.*
import kotlinx.cinterop.*

class DoubleType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jdoubleVar>()
        GetLocalDouble(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("double", valuePtr.value)
    }



}