package com.epam.drill.endpoints

import com.epam.drill.common.WsUrl
import io.ktor.application.Application
import io.ktor.application.feature
import io.ktor.http.parametersOf
import io.ktor.locations.Location
import io.ktor.locations.Locations
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class WsTopic(override val kodein: Kodein) : KodeinAware {
    val xas: MutableMap<String, Pair<KClass<*>, Temp<Any, Any>>> = ConcurrentHashMap()
    val p = "\\{(.*)}".toRegex()

    suspend operator fun invoke(block: suspend WsTopic.() -> Unit) {
        block(this)
    }


    fun Application.resolve(destination: WsUrl, x: MutableSet<DrillWsSession>): Any? {
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

        val block = next.value.second.resolve(param, x)

        return block

    }
}

inline fun <reified R : Any> WsTopic.topic(noinline block: (R, MutableSet<DrillWsSession>) -> Any?) {
    val findAnnotation = R::class.findAnnotation<Location>()
    val path = findAnnotation?.path!!
    xas[path] = R::class to Temp(path, block) as Temp<Any, Any>
}

class Temp<T, R>(val url: String, val block: (R, MutableSet<DrillWsSession>) -> T) {
    fun resolve(param: R, x: MutableSet<DrillWsSession>): T {
        return block(param, x)
    }
}

