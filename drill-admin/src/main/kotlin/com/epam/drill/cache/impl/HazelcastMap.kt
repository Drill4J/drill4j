package com.epam.drill.cache.impl

import com.epam.drill.cache.type.*
import com.hazelcast.core.*

class HazelcastMap<K, V>(private val cache: IMap<K, V>) : Cache<K, V> {

    override fun get(key: K) = cache[key]

    override fun set(key: K, value: V) {
        cache[key] = value
    }

    override fun remove(key: K) {
        cache.remove(key)
    }


}