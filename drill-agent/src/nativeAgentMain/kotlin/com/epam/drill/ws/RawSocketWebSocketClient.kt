package com.epam.drill.ws

import com.epam.drill.lang.UTF8
import com.epam.drill.lang.toByteArray
import com.epam.drill.lang.toString
import com.epam.drill.net.AsyncClient
import com.epam.drill.net.URL
import com.epam.drill.stream.*
import com.epam.drill.util.encoding.toBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random


fun Int.mask(): Int = (1 shl this) - 1

inline fun <T> buildList(callback: ArrayList<T>.() -> Unit): List<T> = arrayListOf<T>().apply(callback)

suspend fun RWebsocketClient(
    url: String,
    protocols: List<String>?,
    origin: String?,
    wskey: String?,
    params: Map<String, String>
): WebSocketClient {
    val uri = URL(url)
    val secure = when (uri.scheme) {
        "ws" -> false
        "wss" -> true
        else -> error("Unknown ws protocol ${uri.scheme}")
    }
    val host = uri.host ?: "127.0.0.1"
    val port = uri.defaultPort.takeIf { it != URL.DEFAULT_PORT } ?: if (secure) 443 else 80

    val client = AsyncClient(host, port, secure = secure)
    return RawSocketWebSocketClient(
        coroutineContext,
        client,
        uri,
        protocols,
        origin,
        wskey ?: "mykey",
        params
    ).apply {
        connect()
    }
}

class WsFrame(val data: ByteArray, val type: WsOpcode, val isFinal: Boolean = true, val frameIsBinary: Boolean = true) {
    fun toByteArray(): ByteArray = MemorySyncStreamToByteArray {
        val isMasked = false
        val mask = Random.nextBytes(4)
        val sizeMask = (0x00)

        write8(type.id or (if (isFinal) 0x80 else 0x00))

        when {
            data.size < 126 -> write8(data.size or sizeMask)
            data.size < 65536 -> {
                write8(126 or sizeMask)
                write16BE(data.size)
            }
            else -> {
                write8(127 or sizeMask)
                write32BE(0)
                write32BE(data.size)
            }
        }

        if (isMasked) writeBytes(mask)

        writeBytes(if (isMasked) applyMask(data, mask) else data)
    }

    companion object {
        fun applyMask(payload: ByteArray, mask: ByteArray?): ByteArray {
            if (mask == null) return payload
            val maskedPayload = ByteArray(payload.size)
            for (n in 0 until payload.size) maskedPayload[n] =
                (payload[n].toInt() xor mask[n % mask.size].toInt()).toByte()
            return maskedPayload
        }
    }
}

class RawSocketWebSocketClient(
    override val coroutineContext: CoroutineContext,
    val client: AsyncClient,
    url: URL,
    protocols: List<String>?,
    val origin: String?,
    val key: String,
    val param: Map<String, String> = mutableMapOf()
) : WebSocketClient(url.fullUrl, protocols), CoroutineScope {
    private var frameIsBinary = false
    val host = url.host ?: "127.0.0.1"
    val port = url.port
    val path = url.path

    internal suspend fun connect() {
        val data = (buildList<String> {
            add(
                "GET ${if (path.isEmpty()) {
                    url
                } else {
                    path
                }
                } HTTP/1.1"
            )
            add("Host: $host:$port")
            add("Pragma: no-cache")
            add("Cache-Control: no-cache")
            add("Upgrade: websocket")
            if (protocols != null) {
                add("Sec-WebSocket-Protocol: ${protocols.joinToString(", ")}")
            }
            add("Sec-WebSocket-Version: 13")
            add("Connection: Upgrade")
            add("Sec-WebSocket-Key: ${key.toByteArray().toBase64()}")
            add("Origin: $origin")
            add("User-Agent: Mozilla/5.0")
            param.forEach { (k, v) ->
                add("$k: $v")
            }
        }.joinToString("\r\n") + "\r\n\n").toByteArray()
        client.writeBytes(data)
        // Read response
        val headers = arrayListOf<String>()
        while (true) {
            val line = client.readLine().trimEnd()
            if (line.isEmpty()) {
                headers += line
                break
            }
        }

        launch {
            onOpen(Unit)
            try {
                launch {
                    try {
                        while (!closed) {
                            sendWsFrame(
                                WsFrame(
                                    "".toByteArray(),
                                    WsOpcode.Ping
                                )
                            )
                            delay(3000)
                        }
                    } catch (ignored: Throwable) {
                        client.disconnect()
                    }

                }




                loop@ while (!closed) {
                    val frame = readWsFrame()
                    @Suppress("IMPLICIT_CAST_TO_ANY") val payload =
                        if (frame.frameIsBinary) frame.data else frame.data.toString(UTF8)


                    when (frame.type) {
                        WsOpcode.Close -> {
                            break@loop
                        }
                        WsOpcode.Ping -> {
                            sendWsFrame(WsFrame(frame.data, WsOpcode.Pong))
                        }
                        WsOpcode.Pong -> {
                            //todo
                            lastPong = 100
                        }
                        else -> {
                            when (payload) {
                                is String -> onStringMessage.forEach { it(payload) }
                                is ByteArray -> onBinaryMessage.forEach { it(payload) }
                            }
                            onAnyMessage.forEach { it(payload) }
                        }
                    }
                }
            } catch (e: Throwable) {
                onError(e)
            }
            onClose(Unit)

        }

    }

    private var lastPong: Long=0

    var closed = false

    override fun close(code: Int, reason: String) {
        closed = true
        launch {
            sendWsFrame(WsFrame(byteArrayOf(), WsOpcode.Close))
        }
    }

    override suspend fun send(message: String) {
        sendWsFrame(
            WsFrame(
                message.toByteArray(UTF8),
                WsOpcode.Text
            )
        )
    }

    override suspend fun send(message: ByteArray) {
        sendWsFrame(WsFrame(message, WsOpcode.Binary))
    }

    suspend fun readWsFrame(): WsFrame {
        val b0 = client.readU8()
        val b1 = client.readU8()

        val isFinal = b0.extract(7)
        val opcode = WsOpcode(b0.extract(0, 4))
        val frameIsBinary = when (opcode) {
            WsOpcode.Text -> false
            WsOpcode.Binary -> true
            else -> frameIsBinary
        }

        val partialLength = b1.extract(0, 7)
        val isMasked = b1.extract(7)

        val length = when (partialLength) {
            126 -> client.readU16BE()
            127 -> {
                val tmp = client.readS32BE()
                if (tmp != 0) error("message too long")
                client.readS32BE()
            }
            else -> partialLength
        }
        val mask = if (isMasked) client.readBytesExact(4) else null
        val unmaskedData = readExactBytes(length)
        val finalData = WsFrame.applyMask(unmaskedData!!, mask)
        return WsFrame(finalData, opcode, isFinal, frameIsBinary)
    }


    private suspend fun readExactBytes(length: Int): ByteArray? {
        var byteArray: ByteArray?
        client.apply {
            byteArray = ByteArray(length)

            var remaining = length
            var coffset = 0
            val reader = this
            while (remaining > 0) {
                val read = reader.read(byteArray!!, coffset, remaining)
                if (read < 0) break
                //		if (read == 0) throw EOFException("Not enough data. Expected=$len, Read=${len - remaining}, Remaining=$remaining")
                coffset += read
                remaining -= read
            }
        }
        return byteArray
    }

    suspend fun sendWsFrame(frame: WsFrame) {
        client.writeBytes(frame.toByteArray())
    }
}

inline class WsOpcode(val id: Int) {

    companion object {
        val Text = WsOpcode(0x01)
        val Binary = WsOpcode(0x02)
        val Close = WsOpcode(0x08)
        val Ping = WsOpcode(0x09)
        val Pong = WsOpcode(0x0A)
    }
}

fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0