package com.epam.drill.cache.type

interface Cache<T, U> {
    operator fun get(key: T): U?

    operator fun set(key: T, value: U)

    fun remove(key: T)
}