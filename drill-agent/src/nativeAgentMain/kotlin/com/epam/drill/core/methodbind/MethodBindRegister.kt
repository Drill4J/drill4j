package com.epam.drill.core.methodbind

import com.epam.drill.core.di
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlin.collections.set
import kotlin.native.concurrent.SharedImmutable


@SharedImmutable
val nativeMethodBindMapper =
    mapOf(
        SocketDispatcher + ::read0.name to { initialMethod: COpaquePointer ->
            di {
                originalMethod.misfeatureToFunctionDictionary[::read0] = initialMethod.reinterpret<CFunction<*>>()
                staticCFunction(::read0)
            }
        },
        SocketDispatcher + ::write0.name to { initialMethod: COpaquePointer ->
            di {
                originalMethod.misfeatureToFunctionDictionary[::write0] = initialMethod.reinterpret<CFunction<*>>()
                staticCFunction(::write0)
            }
        }
    )


