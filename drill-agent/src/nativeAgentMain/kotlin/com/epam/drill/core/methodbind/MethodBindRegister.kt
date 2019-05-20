package com.epam.drill.core.methodbind

import com.epam.drill.core.exec
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
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        SocketDispatcher + ::readv0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        FileDispatcherImpl + ::read0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        },
        FileDispatcherImpl + ::readv0.name to { initialMethod: COpaquePointer ->
            exec {
                val reinterpret = initialMethod.reinterpret<CFunction<*>>()
                originalMethod.misfeatureToFunctionDictionary[::read0] = reinterpret
                staticCFunction(::read0)
            }
        }
    )


