package com.epam.drill.plugins.coverage

import io.vavr.collection.*
import io.vavr.kotlin.*
import kotlinx.atomicfu.*

class AtomicCache<K, V> : (K, (V?) -> V?) -> V? {

    private val _map = atomic(LinkedHashMap.empty<K, V>())

    val map get() = _map.value!!

    override fun invoke(key: K, mutator: (V?) -> V?) = _map.updateAndGet {
        val oldVal = it.getOrNull(key)
        when (val newVal = mutator(oldVal)) {
            oldVal -> it
            null -> it.remove(key)
            else -> it.put(key, newVal)
        }
    }.getOrNull(key)


    operator fun get(key: K): V? = map.getOrNull(key)

    operator fun set(key: K, value: V): V? = this(key) { value }

    fun remove(key: K) = _map.getAndUpdate { it.remove(key) }.getOrNull(key)

    override fun toString(): String = map.toString()
}

val <K, V> AtomicCache<K, V>.keys get() = map.keySet().asSequence().toSet()

val <K, V> AtomicCache<K, V>.values get() = map.values().asSequence()

fun <K, V> AtomicCache<K, V>.getOrPut(key: K, producer: () -> V): V = this(key) { it ?: producer() }!!

fun <K, V> AtomicCache<K, V>.count() = map.size()

fun <K, V> AtomicCache<K, V>.isEmpty() = map.isEmpty

fun <K, V> AtomicCache<K, V>.isNotEmpty() = !isEmpty()
