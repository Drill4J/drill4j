package com.epam.drill.plugin.exception.datatypes


import jvmapi.GetLocalDouble
import jvmapi.jdoubleVar
import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

class DoubleType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jdoubleVar>()
        GetLocalDouble(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("double", valuePtr.value)
    }



}