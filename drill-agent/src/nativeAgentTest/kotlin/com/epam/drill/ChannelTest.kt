package com.epam.drill

import com.soywiz.korio.async.await
import com.soywiz.korio.lang.Thread_sleep
import drillInternal.config
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test


val chan
    get() = config.xx?.asStableRef<Channel<String>>()?.get()!!

class ChannelTest {


    @Test
    fun channel() = runBlocking {
        config.xx = StableRef.create(Channel<String>()).asCPointer()
        val sender = Worker.start(true).execute(TransferMode.UNSAFE, {}) {
            runBlocking {
                while (true) {
                    delay(5000)
                    chan.send("xx")
                }
            }
        }


        val receiveChannel = Worker.start(true).execute(TransferMode.UNSAFE, {}) {
            runBlocking {
                while (true)
                    println(chan.receive())
            }

        }

        Thread_sleep(5000)
        receiveChannel.await()

    }
}