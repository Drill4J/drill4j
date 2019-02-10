package com.epam.drill.logger

import com.epam.drill.jvmti.logger.DLogger
import com.epam.drill.jvmti.logger.readProperties
import com.soywiz.klogger.Logger
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import jvmapi.config

import kotlinx.cinterop.StableRef
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipleThreadsAccess {

    @Test
    fun theSameInstanceOfLogger() = suspendTest {
        val sr = StableRef.create(mutableMapOf<String, Logger>())
        config.loggers = sr.asCPointer()
        val create = StableRef.create(resourcesVfs["drill-logger.properties"].readProperties())
        config.loggerConfig = create.asCPointer()
        repeat(5) {
            assertEquals(DLogger("x"), DLogger("x"))
        }
        sr.dispose()
        create.dispose()

    }

}