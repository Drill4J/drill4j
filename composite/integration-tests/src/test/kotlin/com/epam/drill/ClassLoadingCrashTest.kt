package com.epam.drill

import com.epam.drill.session.DrillRequest
import org.junit.Test
import kotlin.concurrent.thread

class ClassLoadingCrashTest {

    @Test
    fun crashIt() {
        val java = X::class.java
        List(10) {
            thread(start = true) {
                repeat(1000) {
                    DrillRequest.RetransformClasses(arrayOf(java))
                }
            }
        }.forEach { it.join() }
    }

}

class X