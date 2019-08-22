package com.epam.drill.lang


interface Closeable {
    fun close()

    companion object {
        operator fun invoke(callback: () -> Unit) = object : Closeable {
            override fun close() = callback()
        }
    }
}


interface OptionalCloseable : Closeable {
    override fun close(): Unit = Unit
}

