package com.epam.drill.cache

import com.epam.drill.cache.type.*
import kotlin.reflect.*

interface CacheService {
    fun <K, V> getOrCreateMapCache(cacheName: String): Cache<K, V>

    operator fun <K, V> getValue(thisRef: Any?, property: KProperty<*>): Cache<K, V> {
        return getOrCreateMapCache(property.name)
    }
}