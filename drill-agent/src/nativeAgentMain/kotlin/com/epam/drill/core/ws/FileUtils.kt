@file:Suppress(
    "UNCHECKED_CAST", "RemoveEmptyClassBody", "UnnecessaryVariable", "DeferredIsResult", "FunctionName",
    "ArrayInDataClass"
)

package com.epam.drill.core.ws

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.native.concurrent.*

fun ByteArray.openAsync(): AsyncStream =
    MemoryAsyncStreamBase(this).toAsyncStream(0L)

class MemoryAsyncStreamBase(var data: ByteArray) : AsyncStreamBase() {

    var ilength: Int = data.size
//        get() = data.size
//        set(value) = run { data.size = value }

    override suspend fun setLength(value: Long) = run { ilength = value.toInt() }
    override suspend fun getLength(): Long = ilength.toLong()

    fun checkPosition(position: Long) = run { if (position < 0) throw RuntimeException("Invalid position $position") }

    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        checkPosition(position)
        if (position !in 0 until ilength) return 0
        val end = min(this.ilength.toLong(), position + len)
        val actualLen = maxOf((end - position).toInt(), 0)
        arraycopy(this.data, position.toInt(), buffer, offset, actualLen)
        return actualLen
    }

    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
        checkPosition(position)
//        data.size = max(data.size, (position + len).toInt())
        arraycopy(buffer, offset, this.data, position.toInt(), len)

    }

    override suspend fun close() = Unit

    override fun toString(): String = "MemoryAsyncStreamBase(${data.size})"
}

fun AsyncStreamBase.toAsyncStream(position: Long = 0L): AsyncStream = AsyncStream(this, position)
@SharedImmutable
private val IOWorker = Worker.start()

internal suspend fun fileWrite(file: CPointer<FILE>, position: Long, buffer: ByteArray, offset: Int, len: Int) {
    if (len > 0) {
        fileWrite(file, position, buffer.copyOfRange(offset, offset + len))
    }
}

suspend fun writeFileAsync(path: String, content: ByteArray): Long {
    return open(path, "w+b").use {
        content.openAsync().copyTo(this)
    }
}


suspend inline fun <T : AsyncCloseable, TR> T.use(callback: T.() -> TR): TR {
    var error: Throwable? = null
    val result = try {
        callback()
    } catch (e: Throwable) {
        error = e
        null
    }
    close()
    if (error != null) throw error
    return result as TR
}


suspend fun AsyncInputStream.copyTo(target: AsyncOutputStream, chunkSize: Int = 0x10000): Long {
    val chunk = ByteArray(chunkSize)
    var totalCount = 0L
    while (true) {
        val count = this.read(chunk)
        if (count <= 0) break
        target.write(chunk, 0, count)
        totalCount += count
    }
    return totalCount
}

suspend fun AsyncInputStream.read(data: ByteArray): Int = read(data, 0, data.size)

interface AsyncOutputStream : AsyncBaseStream {
    suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
}

interface AsyncBaseStream : AsyncCloseable {
}

interface AsyncCloseable {
    suspend fun close()

    companion object
}

suspend fun open(rpath: String, mode: String): AsyncStream {
    var fd: CPointer<FILE>? = fileOpen(rpath, mode)
    val errno = posix_errno()
    //if (fd == null || errno != 0) {
    if (fd == null) {
        val errstr = strerror(errno)?.toKString()
        throw RuntimeException("Can't open '$rpath' with mode '$mode' errno=$errno, errstr=$errstr")
    }

    fun checkFd() {
        if (fd == null) error("Error with file '$rpath'")
    }

    return object : AsyncStreamBase() {
        override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
            checkFd()
            return 0
        }

        override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
            checkFd()
            return fileWrite(fd!!, position, buffer, offset, len)
        }

        override suspend fun setLength(value: Long) {
            checkFd()
            fileSetLength(rpath, value)
        }

        override suspend fun getLength(): Long {
            checkFd()
            return fileLength(fd!!)
        }

        override suspend fun close() {
            if (fd != null) {
                fileClose(fd!!)
            }
            fd = null
        }

        override fun toString(): String = "($rpath)"
    }.toAsyncStream()
}

interface AsyncInputStreamWithLength : AsyncInputStream, AsyncGetPositionStream, AsyncGetLengthStream {
}

interface AsyncGetPositionStream : AsyncBaseStream {
    suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncPositionLengthStream : AsyncPositionStream, AsyncLengthStream {
}

interface AsyncPositionStream : AsyncGetPositionStream {
    suspend fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
}

class AsyncStream(val base: AsyncStreamBase, var position: Long = 0L) : AsyncInputStream, AsyncInputStreamWithLength,
    AsyncOutputStream, AsyncPositionLengthStream,
    AsyncCloseable {
    private val readQueue = AsyncThread()
    private val writeQueue = AsyncThread()

    override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue {
        val read = base.read(position, buffer, offset, len)
        if (read >= 0) position += read
        read
    }

    override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
        base.write(position, buffer, offset, len)
        position += len
    }

    override suspend fun setPosition(value: Long): Unit = run { this.position = value }
    override suspend fun getPosition(): Long = this.position
    override suspend fun setLength(value: Long): Unit = base.setLength(value)
    override suspend fun getLength(): Long = base.getLength()
    suspend fun size(): Long = base.getLength()
    override suspend fun close(): Unit = base.close()

}

interface AsyncInvokable {
    suspend operator fun <T> invoke(func: suspend () -> T): T
}

class AsyncThread : AsyncInvokable {
    private var lastPromise: Deferred<*> = CompletableDeferred(Unit).apply {
        this.complete(Unit)
    }

    override suspend operator fun <T> invoke(func: suspend () -> T): T {
        val task = sync(coroutineContext, func)
        try {
            val res = task.await()
            return res
        } catch (e: Throwable) {
            throw e
        }
    }


    fun <T> sync(context: CoroutineContext, func: suspend () -> T): Deferred<T> {
        val oldPromise = lastPromise
        val promise = asyncImmediately(context) {
            oldPromise.await()
            func()
        }
        lastPromise = promise
        return promise

    }
}

fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) =
    CoroutineScope(context).asyncImmediately(callback)

fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T) = _async(CoroutineStart.UNDISPATCHED, callback)
private fun <T> CoroutineScope._async(start: CoroutineStart, callback: suspend () -> T): Deferred<T> =
    async(coroutineContext, start = start) {
        try {
            callback()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }


open class AsyncStreamBase : AsyncCloseable, AsyncRAInputStream, AsyncRAOutputStream, AsyncLengthStream {
    //var refCount = 0

    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
        throw UnsupportedOperationException()

    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit =
        throw UnsupportedOperationException()

    override suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
    override suspend fun getLength(): Long = throw UnsupportedOperationException()

    override suspend fun close(): Unit = Unit
}

interface AsyncRAInputStream {
    suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncRAOutputStream {
    suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int)
}

interface AsyncInputStream : AsyncBaseStream {
    suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncLengthStream : AsyncGetLengthStream {
    suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
}

interface AsyncGetLengthStream : AsyncBaseStream {
    suspend fun getLength(): Long = throw UnsupportedOperationException()
}


internal suspend fun fileSetLength(file: String, length: Long) {
    data class Info(val file: String, val length: Long)

    return executeInWorker(IOWorker, Info(file, length)) { (fd, len) ->
        platform.posix.truncate(fd, len.convert())
        Unit
    }
}

suspend fun <T, R> executeInWorker(worker: Worker, value: T, func: (T) -> R): R {
    class Info(val value: T, val func: (T) -> R)

    val info = Info(value.freeze(), func.freeze())
    val future =
        worker.execute(kotlin.native.concurrent.TransferMode.UNSAFE, { info }, { it: Info -> it.func(it.value) })
    return future.await()
}

suspend fun <T> kotlin.native.concurrent.Future<T>.await(): T {
    var n = 0
    while (this.state != kotlin.native.concurrent.FutureState.COMPUTED) {
        when (this.state) {
            kotlin.native.concurrent.FutureState.INVALID -> error("Error in worker")
            kotlin.native.concurrent.FutureState.CANCELLED -> throw CancellationException("cancelled")
            kotlin.native.concurrent.FutureState.THROWN -> error("Worker thrown exception")
            else -> kotlinx.coroutines.delay(((n++).toDouble() / 3.0).toLong())
        }
    }
    return this.result
}


internal suspend fun fileClose(file: CPointer<FILE>): Unit = executeInWorker(IOWorker, file) { fd ->
    platform.posix.fclose(fd)
    Unit
}

internal suspend fun fileLength(file: CPointer<FILE>): Long = executeInWorker(IOWorker, file) { fd ->
    val prev = platform.posix.ftell(fd)
    fseek(fd, 0L.convert(), platform.posix.SEEK_END)
    val end = platform.posix.ftell(fd)
    fseek(fd, prev.convert(), SEEK_SET)
    end.toLong()
}


internal suspend fun fileOpen(name: String, mode: String): CPointer<FILE>? {
    data class Info(val name: String, val mode: String)
    return executeInWorker(IOWorker, Info(name, mode)) { it ->
        platform.posix.fopen(it.name, it.mode)
    }
}

internal suspend fun fileWrite(file: CPointer<FILE>, position: Long, data: ByteArray): Long {
    data class Info(val file: CPointer<FILE>, val position: Long, val data: ByteArray)

    if (data.isEmpty()) return 0L

    return executeInWorker(
        IOWorker,
        Info(file, position, if (data.isFrozen) data else data.copyOf())
    ) { (fd, position, data) ->
        data.usePinned { pin ->
            fseek(fd, position.convert(), SEEK_SET)
            fwrite(pin.addressOf(0), 1.convert(), data.size.convert(), fd).toLong()
        }.toLong()
    }
}

fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }
