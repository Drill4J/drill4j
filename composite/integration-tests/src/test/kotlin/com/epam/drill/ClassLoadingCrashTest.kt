package com.epam.drill

import com.epam.drill.session.DrillRequest
import org.junit.Test
import kotlin.concurrent.thread

class ClassLoadingCrashTest {

    @Test
    fun crashIt() {
        println("XXX")
        val message = DrillRequest.GetAllLoadedClasses()
        println("XXX")
//        println(message[0])
//        println("_____")
//        val java = X::class.java
//        List(50) {
//            thread(start = true) {
//                repeat(5000) {
//                    DrillRequest.RetransformClasses(arrayOf(java))
//                }
//            }
//        }.forEach { it.join() }
    }

}

class X