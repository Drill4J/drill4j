package com.epam.drill

class Pool<T>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = Stack<T>()
    private var lastId = 0

    init {
        for (n in 0 until preallocate) items.push(gen(lastId++))
    }

    fun alloc(): T = if (items.isNotEmpty()) items.pop() else gen(lastId++)

    fun free(element: T) {
        reset(element)
        items.push(element)
    }

}

class Stack<TGen> : Collection<TGen> {
    private val items = arrayListOf<TGen>()

    override val size: Int get() = items.size
    override fun isEmpty() = size == 0

    fun push(v: TGen) = run { items.add(v) }
    fun pop(): TGen = items.removeAt(items.size - 1)

    override fun contains(element: TGen): Boolean = items.contains(element)
    override fun containsAll(elements: Collection<TGen>): Boolean = items.containsAll(elements)
    override fun iterator(): Iterator<TGen> = items.iterator()
}
