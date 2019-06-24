package com.epam.drill.core.wsk

import com.epam.drill.core.ws.executeCoroutines
import com.epam.drill.kafka.KafkaBroker
import com.epam.drill.socket.resolveAddress
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.core.ByteOrder
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.toByteArray
import kotlinx.io.core.writeFully
import kotlinx.io.errors.PosixException
import kotlinx.io.internal.utils.KX_SOCKET
import kotlinx.io.internal.utils.test.close_socket
import kotlinx.io.internal.utils.test.make_socket_non_blocking
import kotlinx.io.internal.utils.test.set_no_delay
import kotlinx.io.internal.utils.test.socket_get_error
import kotlinx.io.streams.send
import kotlinx.serialization.toUtf8Bytes
import platform.posix.AF_INET
import platform.posix.EAGAIN
import platform.posix.EINPROGRESS
import platform.posix.EISCONN
import platform.posix.SOCKET
import platform.posix.SOCK_STREAM
import platform.posix.init_sockets
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.uint16_t
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

@SharedImmutable
val kafkaWorker = Worker.start(true)
@SharedImmutable
val kafkaAccess = Worker.start(true)

@ThreadLocal
val h: KafkaContext = KafkaContext()


inline fun <reified T> process(noinline xx: KafkaContext.() -> T) =
    kafkaAccess.execute(TransferMode.UNSAFE, { xx }) {
        it(h)
    }.result


class KafkaContext {
    lateinit var kafka: KafkaBroker

}

private fun connect() {
    process {
        kafka = KafkaBroker("localhost:6001")
    }
}


fun startKfk() {
    connect()

    kafkaWorker.executeCoroutines {
        val kafka = process { kafka }

        val topName = "qq"
        xa(kafka, topName)
        xa(kafka, "ww")
        xa(kafka, "ee")
        xa(kafka, "rr")
        xa(kafka, "tt")
        xa(kafka, "yy")
        xa(kafka, "uu")
        xa(kafka, "ii")
        launch {
            val ww = kafka.createProducerFor("ww")
            val ee = kafka.createProducerFor("ee")
            val rr = kafka.createProducerFor("rr")
            val tt = kafka.createProducerFor("tt")
            val yy = kafka.createProducerFor("yy")
            val uu = kafka.createProducerFor("uu")
            val ii = kafka.createProducerFor("ii")
            while (true) {
                kotlinx.coroutines.delay(700)
                ww.send("this is for ww")
                ee.send("this is for ee")
                rr.send("this is for rr")
                tt.send("this is for tt")
                uu.send("this is for uu")
                ii.send("this is for ii")
                yy.send("this is for yy")
                yy.send("this is for yy")
            }
        }
        launch {
            testSendRecvFunctions()
        }
//        launch { topicRegister() }
//        while (true) {
//            delay(3000)
//            try {
//                runBlocking {
//                    websocket(exec { agentConfig.adminUrl })
//                }
//            } catch (ex: Exception) {
////                when (ex) {
////                    is WsClosedException -> {
////                    }
//                println(ex.message + "\ntry reconnect\n")
////                }
//            }
//        }
    }
}

private fun CoroutineScope.xa(kafka: KafkaBroker, topName: String) {
    launch {
        kafka.createConsumerFor(topName).consumeEach {
            println(it.stringFromUtf8())
        }
    }
}


fun testSendRecvFunctions(): Unit = memScoped {
    init_sockets()
//        .let { rc ->
//        if (rc == 0) {
//            println("WSAStartup failed with $rc")
//        }
//    }

//
//    with(clientAddr) {
//        memset(this.ptr, 0, sockaddr_in.size.convert())
//        sin_family = AF_INET.convert()
//        sin_port = my_htons(port)
////        sin_addr =
//        val hname = gethostbyname("")
//
////        set_loopback(ptr)
//    }

println(1)
    val clientAddr = resolveAddress("localhost", 9002)
    println(2)
    val socket: SOCKET = socket(AF_INET, SOCK_STREAM, 0)
    println(3)
    val connected: KX_SOCKET = socket

    val result = platform.posix.connect(connected, clientAddr.reinterpret(), sockaddr_in.size.convert())
    println(4)
    println(result)
    if (result != 0) {
        val error = socket_get_error()
        if (error == EINPROGRESS || error == EISCONN) {
            throw PosixException.forErrno(error, "connect()")
        }

    }
    println(5)

    connected.makeNonBlocking()
    set_no_delay(connected)
    println(6)
    val data = "xasda".toUtf8Bytes()
    println(7)
    val offset = 0
    val count = data.size
    println(8)
    // send
    while (true) {
        println(10)
        val result = platform.posix.send(connected,  data.refTo(offset), count.convert(),0)
        println(result)
        if (result < 0) {
            val error = socket_get_error()
            if (error == EAGAIN) continue
            throw PosixException.forErrno(error, "send()")
        }
//        assertEquals(4, result.convert())
        break
    }


    close_socket(connected)
}

private fun my_htons(value: UShort): uint16_t = when {
    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN -> value
    else -> swap(value.toShort()).toUShort()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Short): Short =
    (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()

private fun KX_SOCKET.makeNonBlocking() {
    make_socket_non_blocking(this)
}

@Suppress("unused")
internal fun Int.checkError(action: String = ""): Int = when {
    this < 0 -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
    else -> this
}