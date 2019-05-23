package com.epam.drill.logger

class Logger {
    fun warn(function: () -> String) {
        println("[DRILL WARN] " + function())
    }

    fun error(function: () -> String?) {
        println("[DRILL ERROR] " + function())
    }

    fun debug(function: () -> String) {
        println("[DRILL DEBUG] " + function())
    }

    fun info(function: () -> String) {
        println("[DRILL INFO] " + function())
    }

}