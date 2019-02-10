package com.epam.drill.ws

import com.epam.drill.jvmti.callbacks.exceptions.data.ExceptionDataClass
import com.epam.drill.jvmti.callbacks.exceptions.data.Frame
import com.epam.drill.jvmti.ws.MessageQueue
import com.epam.drill.jvmti.ws.startWs
import com.soywiz.korio.async.await
import jvmapi.config
import jvmapi.createQueue
import jvmapi.getMessage
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Ignore
import kotlin.test.Test

class Rss {
    @Test
    @Ignore
    fun asd() = runBlocking {

        memScoped {
            try {
                config.drillAdminUrl = "localhost:8090".cstr.getPointer(Arena())

                createQueue()




                delay(1500)
                repeat(50) { tak() }
                val start = Worker.start()
                start.execute(TransferMode.UNSAFE, {}) {
                    runBlocking {
                        while (true) {
                            try {
                                val message = getMessage()
                                if (message != null) {
//                                    sq?.send(message.toKString())
                                }
                            } catch (sxs: Throwable) {
                                sxs.printStackTrace()
                            }
                        }
                    }
                }


                startWs().await()





                Unit
//                startWs.await()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
//        }
        }
    }


}


private fun tak() {
    val start2 = Worker.start()


//    threadSafeHeap.peek()


    start2.execute(TransferMode.UNSAFE, {}) {
        runBlocking {
            while (true) {
                try {

                    sendMessage(mutableListOf())

                } catch (ex: Throwable) {
                    println("something happening ${ex.message}")

                    ex.printStackTrace()
                }
            }
        }
    }
}
fun sendMessage(stacktrace: MutableList<Frame>) {
    val exception = Json.stringify(
        ExceptionDataClass.serializer(), ExceptionDataClass(
            type = "type",
            message = "message",
            stackTrace = stacktrace,
            occurredTime = "10.10.2019"
        )
    )
    MessageQueue.sendMessage(exception)
}
