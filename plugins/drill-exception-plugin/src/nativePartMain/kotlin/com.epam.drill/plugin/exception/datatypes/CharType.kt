package com.epam.drill.plugin.exception.datatypes


import jvmapi.*
import kotlinx.cinterop.*

class CharType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any> {
        val valuePtr = nativeHeap.alloc<jintVar>()
        GetLocalInt(thread, depth, currentEntry.slot, valuePtr.ptr)
        return Pair("char", valuePtr.value)
    }



}