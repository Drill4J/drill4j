package com.epam.drill.jvmti.util

import jvmapi.GetClassSignature
import jvmapi.GetMethodDeclaringClass
import jvmapi.GetMethodName
import jvmapi.jclassVar
import jvmapi.jint
import jvmapi.jintVar
import jvmapi.jlocation
import jvmapi.jmethodID
import jvmapi.jvmtiLineNumberEntry
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value


fun jmethodID.getName(): String? = memScoped {
    val methodName = alloc<CPointerVar<ByteVar>>()
    val sig = alloc<CPointerVar<ByteVar>>()
    val gsig = alloc<CPointerVar<ByteVar>>()
    GetMethodName(this@getName, methodName.ptr, sig.ptr, gsig.ptr)
    return methodName.value?.toKString()
}

fun jmethodID.getDeclaringClassName(): String = memScoped {
    val jclass = alloc<jclassVar>()
    GetMethodDeclaringClass(this@getDeclaringClassName, jclass.ptr)

    if (jclass.value == null) {
        return ""
    }
    val name = alloc<CPointerVar<ByteVar>>()
    GetClassSignature(jclass.value, name.ptr, null)
    return name.value?.toKString() ?: ""

}

fun jlocation.toJLocation(methodId: jmethodID?): Int = kotlinx.cinterop.memScoped {
    val count = alloc<jintVar>()
    val localTable = alloc<CPointerVar<jvmtiLineNumberEntry>>()
    jvmapi.GetLineNumberTable(methodId, count.ptr, localTable.ptr)
    val locaTab = localTable.value
    var lineNumber: jint? = 0
    if (locaTab == null) return 0
    for (i in 0..(count.value - 1)) {
        val entry1 = locaTab[i]
        val entry2 = locaTab[i + 1]
        if (this@toJLocation >= entry1.start_location && this@toJLocation < entry2.start_location) {
            lineNumber = entry1.line_number
            break
        }

    }
    if (this@toJLocation >= locaTab[count.value - 1].start_location) {
        lineNumber = locaTab[count.value - 1].line_number
    }
    return lineNumber ?: 0
}
