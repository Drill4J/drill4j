package com.epam.drill.endpoints


class ObservableMapStorage<K, V, R>(val targetMap: MutableMap<K, V> = HashMap()) {
    val onUpdate: MutableSet<Pair<ObservableContext<R>, suspend R.((MutableMap<K, V>)) -> Unit>> = mutableSetOf()
    val onAdd: MutableSet<Pair<ObservableContext<R>, suspend R.(K, V) -> Unit>> = mutableSetOf()
    val onRemove: MutableSet<Pair<ObservableContext<R>, suspend R.(K) -> Unit>> = mutableSetOf()
    val onClear: MutableSet<Pair<ObservableContext<R>, suspend R.() -> Unit>> = mutableSetOf()


    val keys: MutableSet<K>
        get() = targetMap.keys

    val values: MutableCollection<V>
        get() = targetMap.values

    val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = targetMap.entries

    suspend fun put(key: K, value: V): V? {
        val putValue: V? = if (targetMap.containsKey(key)) {
            targetMap.remove(key)
            targetMap.put(key, value)
        } else {
            targetMap.put(key, value)
        }
        onAdd.forEach {
            val first = it.first
            val second = it.second
            first {
                second(key, value)
            }
        }
        onUpdate.forEach {
            val first = it.first
            val second = it.second
            first {
                second(targetMap)
            }
        }
        return putValue
    }

    suspend fun remove(key: K): V? {
        val remove = targetMap.remove(key)
        onRemove.forEach {
            val first = it.first
            val second = it.second
            first {
                second(key)
            }
        }
        onUpdate.forEach {
            val first = it.first
            val second = it.second
            first {
                second(targetMap)
            }
        }
        return remove
    }

    suspend fun clear() {
        targetMap.clear()
        onClear.forEach {
            val first = it.first
            val second = it.second
            first {
                second()
            }
        }
        onUpdate.forEach {
            val first = it.first
            val second = it.second
            first {
                second(targetMap)
            }
        }
    }

    suspend fun update() {
        onUpdate.forEach {
            val first = it.first
            val second = it.second
            first {
                second(targetMap)
            }
        }

    }

    suspend fun singleUpdate(key: K) {
        onAdd.forEach {
            val first = it.first
            val second = it.second
            first {
                second(key, targetMap[key]!!)
            }
        }

    }

}

class ObservableContext<R> {
    var context: R? = null

    constructor(context: R) {
        this.context = context
    }


    suspend operator fun invoke(block: suspend R.() -> Unit) {
        block(context!!)
    }
}


fun <K, R> remove(
    context: R,
    block: suspend R.(K) -> Unit
): Pair<ObservableContext<R>, suspend R.(K) -> Unit> {
    return ObservableContext(context) to block
}

fun <K, V, R> add(
    context: R,
    block: suspend R.(K, V) -> Unit
): Pair<ObservableContext<R>, suspend R.(K, V) -> Unit> {
    return ObservableContext(context) to block
}

//fun <K, V, R> add(block: suspend R.(K, V) -> Unit): Pair<ObservableContext<R>, suspend R.(K, V) -> Unit> {
//    return ObservableContext<R>(R) to block
//}

fun <K, V, R> update(
    context: R,
    block: suspend R.((MutableMap<K, V>)) -> Unit
): Pair<ObservableContext<R>, suspend R.((MutableMap<K, V>)) -> Unit> {
    return ObservableContext(context) to block
}

fun <R> clear(context: R, block: suspend R.() -> Unit): Pair<ObservableContext<R>, suspend R.() -> Unit> {
    return ObservableContext(context) to block
}


