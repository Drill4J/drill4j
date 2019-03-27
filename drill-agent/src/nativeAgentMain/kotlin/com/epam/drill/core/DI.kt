package com.epam.drill.core

import com.epam.drill.common.AgentInfo
import com.epam.drill.core.plugin.loader.Instrumented
import com.epam.drill.logger.Properties
import com.epam.drill.plugin.api.processing.AgentPart
import com.soywiz.korio.lang.Thread_sleep
import drillInternal.anyLock
import drillInternal.anyUnlock
import drillInternal.config
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.asStableRef
import kotlinx.coroutines.channels.Channel
import kotlin.collections.HashMap
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.*

data class JClassVersions(val initialBytes: ByteArray, val versions: MutableMap<Int, ByteArray> = mutableMapOf())

fun JClassVersions.addNewVersion(bytes: ByteArray) {
    versions[versions.size] = bytes
}

class DI {
    val x = Channel<ByteArray>()
    inline operator fun <reified T> invoke(bock: DI.() -> T): T {
        try {
//            anyLock()
            Thread_sleep(1)
            val bock1 = bock(this)
//            anyUnlock()
            return bock1
        } finally {


        }
    }


    val objects = mutableMapOf<KClass<*>, Any>()

    val loadedClasses = mutableMapOf<String, JClassVersions>()
    val sxs = HashMap<String, ByteArray>()

    fun singleton(obj: Any) {
        objects[obj::class] = obj
    }

    inline fun <reified T> get(): T? {
        return objects[T::class] as T
    }

    lateinit var agentInfo: AgentInfo
    lateinit var loggerConfig: Properties
    var pstorage: MutableMap<String, AgentPart<*>> = mutableMapOf()
    var pInstrumentedStorage: MutableMap<String, Instrumented> = mutableMapOf()

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

@ThreadLocal
var qs: DI? = null


inline val di: DI
    get() {
        return if (qs != null)
            try {
                qs!!
            } catch (exL: Throwable) {
                exL.printStackTrace()
                throw exL
            }
        else {
            try {
                qs = config.di!!.asStableRef<DI>().get()
                qs!!
            } catch (exL: Throwable) {
                exL.printStackTrace()
                throw exL
            }
        }

    }
