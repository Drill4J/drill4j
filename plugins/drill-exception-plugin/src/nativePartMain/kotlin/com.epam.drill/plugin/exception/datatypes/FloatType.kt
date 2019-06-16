package com.epam.drill.plugin.exception.datatypes


import jvmapi.GetLocalFloat
import jvmapi.jfloatVar
import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

class FloatType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jfloatVar>()
        GetLocalFloat(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("float", valuePtr.value)
    }



}