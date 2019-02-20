package com.epam.drill.plugin.exception.datatypes

import com.epam.drill.plugin.exception.datatypes.JType
import com.epam.drillnative.api.DrillGetLocalLong
import jvmapi.jlongVar
import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

class LongType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jlongVar>()
        DrillGetLocalLong(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("long", valuePtr.value)
    }



}