package com.epam.drill.core

import com.epam.drill.common.*
import com.epam.drill.plugin.api.processing.*
import kotlinx.cinterop.*
import kotlin.collections.set
import kotlin.native.concurrent.*
import kotlin.reflect.*

class DI {
    lateinit var agentConfig: AgentConfig
    lateinit var drillInstallationDir: String
    var pstorage: MutableMap<String, AgentPart<*, *>> = mutableMapOf()
    val originalMethod = NativeMethodBinder()
    val objects = mutableMapOf<KClass<*>, Any>()

    val pl = mutableMapOf<String, PluginBean>()


    init {

    }

    @Suppress("unused")
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

