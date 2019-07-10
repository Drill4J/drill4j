@file:Suppress("unused")

package com.epam.drill.ws

import com.epam.drill.*
import kotlinx.coroutines.*
import kotlin.system.*

object Initializer {

    fun calculateBuild() = runBlocking(Dispatchers.IO) {
        val scanItPlease = ClassPath().scanItPlease(ClassLoader.getSystemClassLoader())
        val chunked = scanItPlease.toList().chunked(scanItPlease.size / 3)
        var buildVersion = 0
        println("calculate build version took ${measureTimeMillis {
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
            println("Calculate the build version...")
            buildVersion = map.sumBy { it.await() }
        }
        } milliseconds ")
        buildVersion

    }

}
