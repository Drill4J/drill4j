package com.epam.drill.jvmti.logger

import com.soywiz.klogger.Logger
import jvmapi.config
import kotlinx.cinterop.asStableRef

object DLogger {

    operator fun invoke(name: String): Logger {
        val loggers1 = config.loggers ?: return Logger("random").apply { level = Logger.Level.TRACE }
        val logges = loggers1.asStableRef<MutableMap<String, Logger>>().get()
        val logger = logges[name]
        val get = config.loggerConfig?.asStableRef<Properties>()?.get()

        val s = get?.get(name)

        return if (logger != null) {
            logger
        } else {
            logges[name] = Logger(name).apply {
                if (s != null) {
                    level = Logger.Level.valueOf(s)
                }
            }
            logges[name] ?: Logger("random").apply { level = Logger.Level.TRACE }
        }

    }

}