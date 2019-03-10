package com.epam.drill.core

import com.epam.drill.common.AgentInfo
import com.epam.drill.logger.Properties
import com.epam.drill.plugin.api.processing.AgentPart
import drillInternal.config
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.asStableRef
import kotlin.reflect.*

class DI {
    val objects = mutableMapOf<KClass<*>, Any>()

    fun singleton(obj: Any) {
        objects[obj::class] = obj
    }

    inline fun <reified T> get(): T? {
        return objects[T::class] as T
    }

    lateinit var agentInfo: AgentInfo
    lateinit var loggerConfig: Properties
    var pstorage: MutableMap<String, AgentPart<*>> = mutableMapOf()

    val originalMethod = NativeMethodBinder()

    class NativeMethodBinder {
        val w = mutableMapOf<KFunction<*>, CPointer<*>>()

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, R> get(xx: KFunction2<P1, P2, R>): CPointer<CFunction<(P1, P2) -> R>> {
            val cPointer = w[xx]
            return cPointer as kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(P1, P2) -> R>>
        }


        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, R> get(xx: KFunction3<P1, P2, P3, R>): CPointer<CFunction<(P1, P2, P3) -> R>> {
            val cPointer = w[xx]
            return cPointer as kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(P1, P2, P3) -> R>>
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, P4, R> get(xx: KFunction4<P1, P2, P3, P4, R>): CPointer<CFunction<(P1, P2, P3, P4) -> R>> {
            val cPointer = w[xx]
            return cPointer as kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(P1, P2, P3, P4) -> R>>
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <P1, P2, P3, P4, P5, R> get(xx: KFunction5<P1, P2, P3, P4, P5, R>): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>> {
            val cPointer = w[xx]
            return cPointer as kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(P1, P2, P3, P4, P5) -> R>>
        }


    }

}

val di: DI
    get() = config.di!!.asStableRef<DI>().get()