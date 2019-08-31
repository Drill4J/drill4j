@file:Suppress("RemoveRedundantCallsOfConversionMethods", "RedundantSuspendModifier")

package com.epam.drill.net

import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.coroutines.delay
import kotlinx.io.internal.utils.KX_SOCKET
import platform.posix.*

class NativeSocket private constructor(@Suppress("RedundantSuspendModifier") private val sockfd: KX_SOCKET) {
    companion object {
        init {
            init_sockets()
        }

        operator fun invoke(): NativeSocket {
            val socket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
            return NativeSocket(socket)
        }


    }


    val connected get() = _connected

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    fun connect(host: String, port: Int) {
        memScoped {
            val inetaddr = resolveAddress(host, port)
            checkErrors("getaddrinfo")

            @Suppress("RemoveRedundantCallsOfConversionMethods") val connected =
                connect(sockfd, inetaddr, sockaddr_in.size.convert())
            println(connected)
            checkErrors("connect")
            setNonBlocking()
            if (connected != 0) {
                _connected = false
            }
            _connected = true
        }
    }

    private val availableBytes
        get() = run {
            if (!_connected) {
                error("closed")
            }
            getAvailableBytes(sockfd.toULong())
        }
    private var _connected = false

    private fun recv(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
        val result = recv(sockfd, data.refTo(offset), count.convert(), 0)
//        checkErrors("recv")
        return result.toInt()
    }

    fun tryRecv(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
        if (availableBytes <= 0) return -1
        return recv(data, offset, count)
    }

    fun send(data: ByteArray, offset: Int = 0, count: Int = data.size - offset) {
        if (count <= 0) return

        memScoped {
            val result = send(sockfd, data.refTo(offset), count.convert(), 0)
            checkErrors("send")
            if (result < count) {
                _connected = false
                error("Socket write error")
            }
        }
    }

    @Suppress("RemoveRedundantQualifierName")
    fun close() {
        com.epam.drill.net.close(sockfd.toULong())
        _connected = false
    }

    private fun setNonBlocking() {
        setSocketNonBlocking(sockfd.toULong())

    }

    fun disconnect() {
        _connected = false
    }
}

suspend fun NativeSocket.suspendRecvUpTo(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
    if (count <= 0) return count

    while (true) {
        val read = tryRecv(data, offset, count)
        if (read <= 0) {
            delay(10L)
            continue
        }
        return read
    }
}


suspend fun NativeSocket.suspendSend(data: ByteArray, offset: Int = 0, count: Int = data.size - offset) {
    send(data, offset, count)
}
