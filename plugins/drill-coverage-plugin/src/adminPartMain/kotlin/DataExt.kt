package com.epam.drill.plugins.coverage

import java.util.*

fun EncodedString.decode() = Base64.getDecoder().decode(this)