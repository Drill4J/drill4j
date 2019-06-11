package com.epam.drill.cache.impl

import com.epam.drill.cache.type.Cache
import com.hazelcast.core.IMap

class HazelcastMap<K, V>(private val cache: IMap<K, V>) : Cache<K, V> {
    override fun get(key: K) = cache[key]

    override fun set(key: K, value: V) {
        cache[key] = value
    }


}