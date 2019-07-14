package com.epam.drill.plugins.coverage

import java.util.*

fun ByteArray.encode(): EncodedString = Base64.getEncoder().encodeToString(this)

fun EncodedString.decode(): ByteArray = Base64.getDecoder().decode(this)

fun genUuid(): String = UUID.randomUUID().toString()

fun currentTimeMillis(): Long = System.currentTimeMillis()