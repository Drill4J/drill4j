@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.*
import kotlinx.coroutines.*
import java.util.logging.*
import kotlin.system.*

object Initializer {
    private val log = Logger.getLogger(Initializer::class.java.name)

    fun calculateBuild() = runBlocking(Dispatchers.IO) {
        val scanItPlease = ClassPath().scanItPlease(ClassLoader.getSystemClassLoader())
        val chunked = scanItPlease.toList().chunked(scanItPlease.size / 3)
        var buildVersion = 0
        log.info("Build version calculating took ${measureTimeMillis {
            val map = chunked.map {
                async {
                    it.sumBy { (k, v) ->
                        try {
                            v.getResource(k)?.readBytes()?.sum() ?: 0
                        } catch (ex: Exception) {
                            0
                        }
                    }

                }
            }
            log.info("Calculating the build version...")
            buildVersion = map.sumBy { it.await() }
        }
        } milliseconds ")
        buildVersion

    }

}
