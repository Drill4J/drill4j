package com.epam.drill.plugin.exception.datatypes

import com.epam.drill.jvmapi.gen.*

abstract class JType {

    abstract fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any>?
}