package com.epam.drill.plugins.coverage

import io.vavr.collection.*
import io.vavr.kotlin.*
import kotlinx.atomicfu.*

//TODO This is a temporary storage API. It will be removed when the core API has been developed
interface StoreKey<T : Any>

interface Storage {
    suspend fun <T : Any> retrieve(key: StoreKey<T>): T?

    suspend fun <T : Any> store(key: StoreKey<T>, value: T)

    suspend fun <T : Any> update(key: StoreKey<T>, function: (T?) -> T?): T?

    suspend fun <T : Any> delete(key: StoreKey<T>): T?
}

class MapStorage : Storage {

    private val _map = atomic(HashMap.empty<Any, Any>())

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> retrieve(key: StoreKey<T>): T? = _map.value.getOrNull(key) as T?

    override suspend fun <T : Any> store(key: StoreKey<T>, value: T) {
        _map.update { it.put(key, value) }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> update(key: StoreKey<T>, function: (T?) -> T?): T? {
        return _map.getAndUpdate {
            val oldValue = it.getOrNull(key) as T?
            it.put(key, function(oldValue))
        }.getOrNull(key) as T?
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> delete(key: StoreKey<T>) = _map.getAndUpdate {
        it.remove(key)
    }.getOrNull(key) as T?
}