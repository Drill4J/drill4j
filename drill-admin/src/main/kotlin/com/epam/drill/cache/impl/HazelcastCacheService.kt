package com.epam.drill.cache.impl

import com.epam.drill.cache.CacheService
import com.epam.drill.cache.type.Cache
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance

class HazelcastCacheService(private val ins: HazelcastInstance = Hazelcast.newHazelcastInstance()) :
    CacheService {
    private val cacheMapper = mutableMapOf<String, Cache<*, *>>()

    override fun <K, V> getOrCreateMapCache(cacheName: String): Cache<K, V> {
        val cache = ins.getMap<K, V>(cacheName)
        @Suppress("UNCHECKED_CAST")
        return cacheMapper[cacheName] as Cache<K, V>? ?: run {
            val hazelcastMap = HazelcastMap(cache)
            cacheMapper[cacheName] = hazelcastMap
            hazelcastMap
        }
    }

}