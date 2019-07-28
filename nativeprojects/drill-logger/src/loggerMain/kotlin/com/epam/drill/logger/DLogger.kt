package com.epam.drill.logger

import io.ktor.util.date.*

class DLogger(val name: String) {

    fun warn(function: () -> String) {
        println("${GMTDate().toLogDate()} [DRILL WARN] " + function())
    }

    fun error(function: () -> String) {
        println("${GMTDate().toLogDate()} [DRILL ERROR] " + function())
    }

    fun debug(function: () -> String) {
        println("${GMTDate().toLogDate()} [DRILL DEBUG] " + function())
    }

    fun info(function: () -> String) {
        println("${GMTDate().toLogDate()} [DRILL INFO] " + function())
    }

}

fun GMTDate.toLogDate(): String = "${year.padZero(4)}-${month.ordinal.padZero(2)}-${dayOfMonth.padZero(2)} ${hours.padZero(2)}:${minutes.padZero(2)}:${seconds.padZero(2)} GTM"


private fun Int.padZero(length: Int): String = toString().padStart(length, '0')


