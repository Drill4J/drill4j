package com.epam.drill.net

import com.epam.drill.stream.AsyncCloseable
import com.epam.drill.stream.AsyncInputStream
import com.epam.drill.stream.AsyncOutputStream

abstract class AsyncSocketFactory {
    abstract suspend fun createClient(secure: Boolean = false): AsyncClient
}

@SharedImmutable
internal val asyncSocketFactory: AsyncSocketFactory = NativeAsyncSocketFactory

interface AsyncClient : AsyncInputStream, AsyncOutputStream,
    AsyncCloseable {
    suspend fun connect(host: String, port: Int)
    val connected: Boolean
    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
    override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
    override suspend fun close()
    fun disconnect()


    companion object {
        suspend operator fun invoke(host: String, port: Int, secure: Boolean = false) =
            createAndConnect(host, port, secure)

        private suspend fun createAndConnect(host: String, port: Int, secure: Boolean = false): AsyncClient {
            val socket = asyncSocketFactory.createClient(secure)

            socket.connect(host, port)
            return socket
        }
    }
}