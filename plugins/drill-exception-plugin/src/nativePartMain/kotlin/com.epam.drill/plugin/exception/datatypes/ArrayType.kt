package com.epam.drill.plugin.exception.datatypes

import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry

class ArrayType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any>? {

        return null
    }


}