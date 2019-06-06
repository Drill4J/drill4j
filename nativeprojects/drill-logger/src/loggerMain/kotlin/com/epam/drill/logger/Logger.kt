package com.epam.drill.logger

class Logger(val name: String) {
    fun warn(function: () -> String) {
        println("$name [DRILL WARN] " + function())
    }

    fun error(function: () -> String?) {
        println("$name [DRILL ERROR] " + function())
    }

    fun debug(function: () -> String) {
        println("$name [DRILL DEBUG] " + function())
    }

    fun info(function: () -> String) {
        println("$name [DRILL INFO] " + function())
    }

}