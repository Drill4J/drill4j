package com.epam.drill

import com.epam.drill.plugin.api.processing.Sender
import com.epam.drill.session.DrillRequest
import com.epam.drill.test.IntegrationTestApi
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.awaitClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class CrashTest {

    @KtorExperimentalAPI
    @Ignore
    @Test(timeout = 31000)
    fun testConversation() = runBlocking(Dispatchers.IO) {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(port = 0)
        val localAddress: InetSocketAddress = serverSocket.localAddress as InetSocketAddress
        IntegrationTestApi.setAdminUrl("localhost:${localAddress.port}")
        println("Echo Server listening at $localAddress")

        launch {
            repeat(10000) {
                Sender.sendMessage("xx", "x")
            }
        }
        launch {
            IntegrationTestApi.LoadPlugin(File("../../distr/tests/coverage/agent-part.jar").absolutePath)
            val classes = DrillRequest.GetAllLoadedClasses()
            attackClassloading(classes)
        }
        launch {
            kotlinx.coroutines.delay(10000)

            stop@ while (true) {
                val socket = serverSocket.accept()
                println("Accepted $socket")
                launch {
                    val read = socket.openReadChannel()
                    val write = socket.openWriteChannel(autoFlush = true)
                    var key = true
                    val w = atomic(0)

                    try {
                        while (true) {
                            read.readPacket(read.availableForRead, 50)
                            if (w.value > 100000) {
                                println("socket is OK")
                                socket.awaitClosed()
                                @Suppress("NOT_A_FUNCTION_LABEL_WARNING")
                                return@stop
                            }

                            if (key) {

                                write.writeStringUtf8("response line\n\n\n")
                                key = false
                            } else {
                                w.incrementAndGet()
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        socket.awaitClosed()
                    }
                }
            }

        }
        kotlinx.coroutines.delay(20000)
        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
        System.exit(0)
    }


    private fun attackClassloading(classes: Array<Class<*>>) {
        val prefix = "com"

        System.gc()
        println("start")
        DrillRequest.RetransformClasses(classesByPackage(classes, prefix))
        thread(start = true) {
            DrillRequest.RetransformClasses(classesByPackage(classes, prefix))
            DrillRequest.RetransformClasses(classesByPackage(classes, "com"))
        }
        System.gc()

    }

    private fun classesByPackage(classes: Array<Class<*>>, @Suppress("SameParameterValue") prefix: String) =
        classes.filter { it.`package`?.name?.startsWith(prefix) ?: false }.toTypedArray()


}