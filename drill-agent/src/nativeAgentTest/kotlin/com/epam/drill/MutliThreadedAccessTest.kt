package com.epam.drill

import com.epam.drill.core.DI
import com.epam.drill.core.JClassVersions
import com.epam.drill.core.di
import com.soywiz.korio.lang.Thread_sleep
import drillInternal.config
import drillInternal.createQueue
import kotlinx.cinterop.StableRef
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test

class MutliThreadedAccessTest {


    @Test
    fun accessWithoutCrash() = runBlocking {
        try {
            val any = DI()
            config.di = StableRef.create(any).asCPointer()

            createQueue()
            Worker.start(true).execute(TransferMode.UNSAFE, {}) {
                repeat(10000) {
                    access()
                }
            }
            Worker.start(true).execute(TransferMode.UNSAFE, {}) {
                repeat(100000) {
                    access()
                }
            }
            Worker.start(true).execute(TransferMode.UNSAFE, {}) {
                repeat(100000) {
                    access()
                }
            }

            Worker.start(true).execute(TransferMode.UNSAFE, {}) {
                repeat(10000) {
                    access()
                }
            }


println("hi")
            any.x.consumeEach {
                println(it)
            }
            println("hrenay")
            Thread_sleep(10000)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        Unit

    }
}


fun access() {
    runBlocking {
        initRuntimeIfNeeded()
        val get = di.x
        get?.send(byteArrayOf(1,2,3))
    }
}