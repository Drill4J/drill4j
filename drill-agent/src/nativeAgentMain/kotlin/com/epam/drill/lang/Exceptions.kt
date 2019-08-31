package com.epam.drill.lang

open class IOException constructor(msg: String) : Exception(msg)
open class EOFException constructor(msg: String) : IOException(msg)
class InvalidOperationException(str: String = "Invalid Operation") : Exception(str)

fun invalidOp(msg: String): Nothing = throw InvalidOperationException(msg)
fun unsupported(): Nothing = throw UnsupportedOperationException("unsupported")

