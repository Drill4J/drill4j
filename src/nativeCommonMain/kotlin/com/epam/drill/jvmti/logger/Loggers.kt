package com.epam.drill.jvmti.logger

import com.soywiz.klogger.Logger

object DrillLogger {

    operator fun invoke(name: String): Logger {

        val levelString = "TRACE"
        val logLevel = Logger.Level.valueOf(levelString)
        val logger = Logger(name)
        logger.level = logLevel
        return logger
    }
}