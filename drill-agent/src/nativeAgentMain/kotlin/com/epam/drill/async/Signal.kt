@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.epam.drill.async

import com.epam.drill.stream.Closeable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


inline fun <T> ArrayList<T>.fastIterateRemove(callback: (T) -> Boolean): ArrayList<T> {
    var n = 0
    var m = 0
    while (n < size) {
        if (m >= 0 && m != n) this[m] = this[n]
        if (callback(this[n])) m--
        n++
        m++
    }
    while (this.size > m) this.removeAt(this.size - 1)
    return this
}


abstract class BaseSignal<T, THandler>(val onRegister: () -> Unit = {}) {
    inner class Node(val once: Boolean, val item: THandler) : Closeable {
        override fun close() {
            if (iterating > 0) {
                handlersToRemove.add(this)
            } else {
                handlers.remove(this)
            }
        }
    }

    protected var handlers = ArrayList<Node>()
    protected var handlersToRemove = ArrayList<Node>()
    val listenerCount: Int get() = handlers.size
    fun clear() = handlers.clear()

    protected fun _add(once: Boolean, handler: THandler): Closeable {
        onRegister()
        val node = Node(once, handler)
        handlers.add(node)
        return node
    }

    protected var iterating: Int = 0
    protected inline fun iterateCallbacks(callback: (THandler) -> Unit) {
        try {
            iterating++
            handlers.fastIterateRemove { node ->
                val remove = node.once
                callback(node.item)
                remove
            }
        } finally {
            iterating--
            if (handlersToRemove.isNotEmpty()) {
                handlersToRemove.fastIterateRemove {
                    handlers.remove(it)
                    true
                }
            }
        }
    }


    abstract suspend fun waitOneBase(): T
}

class AsyncSignal<T>(onRegister: () -> Unit = {}) : BaseSignal<T, suspend (T) -> Unit>(onRegister) {
    fun once(handler: suspend (T) -> Unit): Closeable = _add(true, handler)
    fun add(handler: suspend (T) -> Unit): Closeable = _add(false, handler)
    operator fun invoke(handler: suspend (T) -> Unit): Closeable = add(handler)

    suspend operator fun invoke(value: T) = iterateCallbacks { it(value) }
    override suspend fun waitOneBase(): T = suspendCancellableCoroutine { c ->
        var close: Closeable? = null
        close = once {
            close?.close()
            c.resume(it)
        }
        c.invokeOnCancellation {
            close.close()
        }
    }
}

class Signal<T>(onRegister: () -> Unit = {}) : BaseSignal<T, (T) -> Unit>(onRegister) {
    fun once(handler: (T) -> Unit): Closeable = _add(true, handler)
    fun add(handler: (T) -> Unit): Closeable = _add(false, handler)
    operator fun invoke(handler: (T) -> Unit): Closeable = add(handler)
    operator fun invoke(value: T) = iterateCallbacks { it(value) }
    override suspend fun waitOneBase(): T = suspendCancellableCoroutine { c ->
        var close: Closeable? = null
        close = once {
            close?.close()
            c.resume(it)
        }
        c.invokeOnCancellation {
            close.close()
        }
    }
}


suspend operator fun AsyncSignal<Unit>.invoke() = invoke(Unit)


operator fun Signal<Unit>.invoke() = invoke(Unit)


