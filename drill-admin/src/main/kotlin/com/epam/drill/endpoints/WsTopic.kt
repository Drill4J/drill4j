package com.epam.drill.endpoints

import com.epam.drill.common.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import org.kodein.di.*
import java.util.concurrent.*
import kotlin.collections.set
import kotlin.reflect.*
import kotlin.reflect.full.*

class WsTopic(override val kodein: Kodein) : KodeinAware {
    val xas: MutableMap<String, Pair<KClass<*>, Temp<Any, Any>>> = ConcurrentHashMap()
    private val p = "\\{(.*)}".toRegex()

    suspend operator fun invoke(block: suspend WsTopic.() -> Unit) {
        block(this)
    }


    fun Application.resolve(destination: String, x: MutableSet<DrillWsSession>): String {
        if (xas.isEmpty()) return ""
        val split = destination.split("/")

        val filter = xas.filter { it.key.count { c -> c == '/' } + 1 == split.size }.filter {
            var matche = true
            it.key.split("/").forEachIndexed { x, y ->
                if (y == split[x] || y.startsWith("{")) {
                } else {
                    matche = false
                }
            }
            matche
        }
        val next = filter.iterator().next()

        val parameters = next.run {
            val mutableMapOf = mutableMapOf<String, String>()
            key.split("/").forEachIndexed { x, y ->
                if (y == split[x]) {
                } else if (p.matches(y)) {
                    mutableMapOf[p.find(y)!!.groupValues[1]] = split[x]
                }
            }
            val map = mutableMapOf.map { Pair(it.key, listOf(it.value)) }
            parametersOf(* map.toTypedArray())
        }
        val param = feature(Locations).resolve<Any>(next.value.first, parameters)

        val result = next.value.second.resolve(param, x)
        return serialize(result)
    }
}

inline fun <reified R : Any> WsTopic.topic(noinline block: (R, MutableSet<DrillWsSession>) -> Any?) {
    val findAnnotation = R::class.findAnnotation<Location>()
    val path = findAnnotation?.path!!
    @Suppress("UNCHECKED_CAST")
    xas[path] = R::class to Temp(block) as Temp<Any, Any>
}

class Temp<T, R>(val block: (R, MutableSet<DrillWsSession>) -> T) {
    fun resolve(param: R, x: MutableSet<DrillWsSession>): T {
        return block(param, x)
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
fun serialize(value: Any?): String {
    if (value == null) return ""
    val serializer = when (value) {
        is String -> null
        is List<*> -> ArrayListSerializer(elementSerializer(value))
        is Set<*> -> HashSetSerializer(elementSerializer(value))
        is Map<*, *> -> HashMapSerializer(
            elementSerializer(value.keys),
            elementSerializer(value.values)
        )
        is Array<*> -> {
            @Suppress("UNCHECKED_CAST")
            (ReferenceArraySerializer(
                value::class as KClass<Any>,
                elementSerializer(value.asList()) as KSerializer<Any>
            ))
        }
        else -> value::class.serializer()
    }
    @Suppress("UNCHECKED_CAST")
    return if (serializer != null) {
        serializer as KSerializer<Any> stringify value
    } else value as String
}

@UseExperimental(ImplicitReflectionSerializer::class)
fun elementSerializer(collection: Collection<*>) = (collection.firstOrNull() ?: String)::class.serializer()