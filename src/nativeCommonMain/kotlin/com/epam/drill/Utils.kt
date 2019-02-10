package com.epam.drill

import com.epam.kjni.core.GlobState
import jvmapi.jstring
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

fun jstring.toKString(): String {
    //fixme deallocate
    val getStringUTFChars = GlobState.env.pointed.value?.pointed?.GetStringUTFChars!!(GlobState.env, this, null)
    return getStringUTFChars?.toKString()!!
}
