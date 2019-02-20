package com.epam.drill

import jvmapi.jstring
import kotlinx.cinterop.toKString

fun jstring?.toKString() = jvmapi.GetStringUTFChars(this, null)?.toKString()

