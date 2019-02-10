package com.epam.drill.jvmti.types

import jvmapi.GetLocalInt
import jvmapi.jintVar
import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

class ShortType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jintVar>()
        GetLocalInt( thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("short", valuePtr.value)
    }



}