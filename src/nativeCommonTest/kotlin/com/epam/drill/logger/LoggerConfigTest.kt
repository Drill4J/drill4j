package com.epam.drill.logger

import com.epam.drill.jvmti.logger.readProperties
import com.soywiz.korio.file.std.resourcesVfs
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LoggerConfigTest {
    @Test
    fun parseConfigFile() {
        runBlocking {
            val vfsFile = resourcesVfs["drill-logger.properties"]
            val readProperties = vfsFile.readProperties()
            assertEquals("INFO", readProperties["websocket"])
        }
    }
}