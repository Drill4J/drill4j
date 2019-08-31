package com.epam.drill.lang

import com.epam.drill.Pool
import kotlin.native.concurrent.ThreadLocal


@ThreadLocal
internal val smallBytesPool = Pool(preallocate = 16) { ByteArray(16) }


inline fun <T, R> Pool<T>.alloc2(callback: (T) -> R): R {
    val temp = alloc()
    try {
        return callback(temp)
    } finally {
        free(temp)
    }
}


inline fun <T, R> Pool<T>.allocThis(callback: T.() -> R): R {
    val temp = alloc()
    try {
        return callback(temp)
    } finally {
        free(temp)
    }
}
