package com.epam.drill.plugin.exception.datatypes

import jvmapi.jthread
import jvmapi.jvmtiLocalVariableEntry

abstract class JType {

    abstract fun retrieveValue(thread: jthread?, depth: Int, currentEntry: jvmtiLocalVariableEntry): Pair<String, Any>?
}