package com.epam.drill.storage


class ObservableSetStorage<T>(private val targetSet: MutableSet<T> = mutableSetOf()) : MutableSet<T> by targetSet {
    val onUpdate: MutableSet<(MutableSet<T>) -> Unit> = mutableSetOf()
    val onAdd: MutableSet<(T) -> Unit> = mutableSetOf()
    val onRemove: MutableSet<(T) -> Unit> = mutableSetOf()
    val onClear: MutableSet<(MutableSet<T>) -> Unit> = mutableSetOf()


    override fun add(element: T): Boolean {
        val add = targetSet.add(element)
        onAdd.forEach { it(element) }
        onUpdate.forEach { it(targetSet) }
        return add
    }

    override fun remove(element: T): Boolean {
        val remove = targetSet.remove(element)
        onRemove.forEach { it(element) }
        onUpdate.forEach { it(targetSet) }
        return remove
    }

  override fun clear() {
        targetSet.clear()
        onClear.forEach { it(targetSet) }
        onUpdate.forEach { it(targetSet) }
    }
}

fun test() {
    val kta = ObservableSetStorage(mutableSetOf<String>())
    kta.onAdd += { item ->
        println("$item added")
    }
    kta.onUpdate += { set ->
        println("$set updated")
    }
    kta.onRemove += { item ->
        println("$item removed")
    }
    kta.onClear += {
        println("cleaned")
    }
    kta.add("2313")
    kta.add("1231")
    kta.remove("1231")
    kta.add("1")
    kta.clear()
}