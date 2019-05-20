package com.epam.drill.core

import com.epam.drill.common.AgentInfo
import com.epam.drill.core.concurrency.LockFreeMPSCQueue
import com.epam.drill.core.plugin.loader.IInstrumented
import com.epam.drill.plugin.api.processing.AgentPart
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlin.collections.set
import kotlin.native.concurrent.SharedImmutable
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5

class DI {
    lateinit var drillInstallationDir: String
    lateinit var agentInfo: AgentInfo
    var pInstrumentedStorage: MutableMap<String, IInstrumented> = mutableMapOf()
    var pstorage: MutableMap<String, AgentPart<*>> = mutableMapOf()
    val originalMethod = NativeMethodBinder()
    val queue = LockFreeMPSCQueue<String>()
    val objects = mutableMapOf<KClass<*>, Any>()

    fun singleton(obj: Any) {
        objects[obj::class] = obj
    }

    inline fun <reified T> get(): T? {
        return objects[T::class] as T
    }


    class NativeMethodBinder {
        val misfeatureToFunctionDictionary = mutableMapOf<KFunction<*>, CPointer<*>>()

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, R> get(targetFunction: KFunction2<P1, P2, R>): CPointer<CFunction<(P1, P2) -> R>> {
            val cPointer = misfeatureToFunctionDictionary[targetFunction]
            return cPointer as CPointer<CFunction<(P1, P2) -> R>>
        }


        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, R> get(targetFunction: KFunction3<P1, P2, P3, R>): CPointer<CFunction<(P1, P2, P3) -> R>> {
            val cPointer = misfeatureToFunctionDictionary[targetFunction]
            return cPointer as CPointer<CFunction<(P1, P2, P3) -> R>>
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, P4, R> get(targetFunction: KFunction4<P1, P2, P3, P4, R>): CPointer<CFunction<(P1, P2, P3, P4) -> R>> {
            val cPointer = misfeatureToFunctionDictionary[targetFunction]
            return cPointer as CPointer<CFunction<(P1, P2, P3, P4) -> R>>
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, P4, P5, R> get(targetFunction: KFunction5<P1, P2, P3, P4, P5, R>): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>> {
            val cPointer = misfeatureToFunctionDictionary[targetFunction]
            return cPointer as CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>>
        }


    }

}

inline fun <reified T> exec(noinline what: DI.() -> T) = work.execute(TransferMode.UNSAFE, { what }) {
    it(dsa)
}.result


@SharedImmutable
val work = Worker.start(true)

@ThreadLocal
val dsa = DI()

