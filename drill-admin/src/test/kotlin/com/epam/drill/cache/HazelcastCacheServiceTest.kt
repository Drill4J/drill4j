package com.epam.drill.cache

import com.epam.drill.cache.impl.*
import com.epam.drill.cache.type.*
import com.hazelcast.core.*
import org.junit.Test
import kotlin.test.*


class HazelcastCacheServiceTest {

    @Test
    fun shouldDelegateHazelcastCacheToAPI() {
        val hInstance = Hazelcast.newHazelcastInstance()
        val cache: CacheService = HazelcastCacheService(hInstance)
        val hiCache: Cache<String, String> by cache
        hiCache["a"] = "xxx"
        assertNotNull(hiCache)
        assertEquals(hInstance.getMap<String, String>("hiCache")["a"], hiCache["a"])

    }
}