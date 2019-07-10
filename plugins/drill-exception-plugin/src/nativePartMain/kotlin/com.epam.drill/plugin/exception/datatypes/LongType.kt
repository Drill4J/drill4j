package com.epam.drill.plugin.exception.datatypes

import jvmapi.*
import kotlinx.cinterop.*

class LongType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jlongVar>()
        GetLocalLong(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("long", valuePtr.value)
    }



}