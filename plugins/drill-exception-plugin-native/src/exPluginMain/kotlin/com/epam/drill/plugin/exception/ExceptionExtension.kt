package com.epam.drill.plugin.exception

import com.epam.drillnative.api.*
import jvmapi.*
import kotlinx.cinterop.*

fun jthrowable.getMessage() = memScoped {
    val throwableClass = FindClass("java/lang/Throwable")
    val methodID = GetMethodID(throwableClass, "getMessage", "()Ljava/lang/String;")
    val message: jstring? = CallObjectMethodV(this@getMessage, methodID, null)
    message?.toKString()
}


fun jthrowable.getType() = memScoped {
    val getObjectClass = GetObjectClass(this@getType)
    val name = this.alloc<CPointerVar<ByteVar>>()
    DrillGetClassSignature(getObjectClass, name.ptr, null)
    name.value?.toKString()?.replace("/", ".")?.dropLast(1)?.drop(1) ?: "UnknownExType"

}

fun jmethodID.getName(): String? = kotlinx.cinterop.memScoped {
    val methodName = alloc<CPointerVar<ByteVar>>()
    val sig = alloc<CPointerVar<ByteVar>>()
    val gsig = alloc<CPointerVar<ByteVar>>()
    com.epam.drillnative.api.DrillGetMethodName(this@getName, methodName.ptr, sig.ptr, gsig.ptr)
    return methodName.value?.toKString()
}

fun jmethodID.getDeclaringClassName(): String = kotlinx.cinterop.memScoped {
    val jclass = alloc<jclassVar>()
    com.epam.drillnative.api.DrillGetMethodDeclaringClass(this@getDeclaringClassName, jclass.ptr)
    if (jclass.value == null) {
        return ""
    }
    val name = alloc<CPointerVar<ByteVar>>()
    com.epam.drillnative.api.DrillGetClassSignature(jclass.value, name.ptr, null)
    val toKString = name.value?.toKString()
    return toKString ?: ""

}

@Suppress("unused")
fun jlocation.toJLocation(methodId: jmethodID?): Int = kotlinx.cinterop.memScoped {
    val count = alloc<jintVar>()
    val localTable = alloc<CPointerVar<jvmtiLineNumberEntry>>()
    com.epam.drillnative.api.DrillGetLineNumberTable(methodId, count.ptr, localTable.ptr)
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


@Suppress("ClassName")
object frameWalker {
    internal inline operator fun invoke(
        thread: jthread?,
        maxFrameCount: jint, block: FrameInfo.() -> Frame?
    ) = memScoped {
        val frames = allocArray<jvmtiFrameInfo>(maxFrameCount)
        val count = alloc<jintVar>()
        DrillGetStackTrace(thread, 0, maxFrameCount, frames, count.ptr)
        val stackTrace = mutableListOf<Frame>()
        for (depth in 0 until count.value) {
            val info: jvmtiFrameInfo = frames[depth]
            val frame = block(FrameInfo(info, depth))
            if (frame != null) {
                stackTrace.add(frame)
            }
        }
        stackTrace
    }

}


class FrameInfo(val info: jvmtiFrameInfo, val currentDepth: Int) {

    @Suppress("unused")
    fun getClassName(): String? {
        return info.method?.getDeclaringClassName()
    }


    internal inline fun iterateLocalVariables(block: jvmtiLocalVariableEntry.() -> VariableLine?) = memScoped {
        val localVariableEntry = alloc<CPointerVar<jvmtiLocalVariableEntry>>()
        val entryCountPtr = alloc<jintVar>()
        DrillGetLocalVariableTable(
            info.method,
            entryCountPtr.ptr,
            localVariableEntry.ptr
        )

        val frames = mutableListOf<VariableLine>()
        for (j in (0 until entryCountPtr.value).reversed()) {
            val currentEntry: jvmtiLocalVariableEntry = localVariableEntry.value!![j]
            val element = block(currentEntry)
            if (element != null)
                frames.add(element)
        }
        frames
    }

}