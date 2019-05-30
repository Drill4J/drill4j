package com.epam.drill

import com.epam.drill.plugin.api.processing.Sender
import org.junit.Test

class SocketCrashTest {

    @Test
    fun crashIt() {
        println("xxxasd")
        repeat(10000) {
            Sender.sendMessage("xx", "x")
        }
        Thread.sleep(5000)
    }
}