package com.epam.drill.plugins.coverage

import java.util.concurrent.ConcurrentHashMap

//TODO This is a temporary storage API. It will be removed when the core API has been developed
interface StoreKey<T : Any>

interface Storage {
    suspend fun <T : Any> store(key: StoreKey<T>, value: T)
    suspend fun <T : Any> retrieve(key: StoreKey<T>): T?
    suspend fun <T : Any> delete(key: StoreKey<T>): T?
}

class MapStorage : Storage {

    private val m = ConcurrentHashMap<Any, Any>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> retrieve(key: StoreKey<T>): T? = m[key] as? T

    override suspend fun <T : Any> store(key: StoreKey<T>, value: T) {
        m[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> delete(key: StoreKey<T>) = m.remove(key) as? T
}