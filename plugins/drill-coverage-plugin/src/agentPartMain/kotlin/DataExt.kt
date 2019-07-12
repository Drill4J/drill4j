package com.epam.drill.plugins.coverage

import java.util.*

fun ByteArray.encode(): EncodedString = Base64.getEncoder().encodeToString(this)