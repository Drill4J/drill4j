package com.epam.drill.logger

class Logger {
    fun warn(function: () -> String) {
        println(function())
    }

    fun error(function: () -> String?) {
        println(function())
    }

    fun debug(function: () -> String) {
        println(function())
    }

    fun info(function: () -> String) {
        println(function())
    }

}