package com.epam.drill.ws

import com.epam.drill.async.Signal

abstract class WebSocketClient protected constructor(
    val url: String,
    val protocols: List<String>?
) {
    val onOpen = Signal<Unit>()
    val onError = Signal<Throwable>()
    val onClose = Signal<Unit>()

    val onBinaryMessage = mutableSetOf<suspend (ByteArray) -> Unit>()
    val onStringMessage = mutableSetOf<suspend (String) -> Unit>()
    val onAnyMessage = mutableSetOf<suspend (Any) -> Unit>()

    open fun close(code: Int = 0, reason: String = ""): Unit = Unit
    open suspend fun send(message: String): Unit = Unit
    open suspend fun send(message: ByteArray): Unit = Unit
}