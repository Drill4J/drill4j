package com.epam.drill.plugin.exception.datatypes

import com.epam.drill.jvmapi.gen.*

class ArrayType : JType() {


    override fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any>? {

        return null
    }


}