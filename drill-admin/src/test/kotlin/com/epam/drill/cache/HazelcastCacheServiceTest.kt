package com.epam.drill.cache

import com.epam.drill.cache.impl.HazelcastCacheService
import com.epam.drill.cache.type.Cache
import com.hazelcast.core.Hazelcast
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


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