package com.epam.drill.plugin.exception

import com.epam.drill.plugin.exception.datatypes.createVariableLine
import com.epam.drillnative.api.DrillGetClassSignature
import com.epam.drillnative.api.sendToSocket
import com.soywiz.klock.DateTime
import jvmapi.*
import kotlinx.cinterop.*


fun exceptionCallback(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    thread: jthread?,
    method: jmethodID?,
    location: jlocation,
    exception: jobject?,
    catchMethod: jmethodID?,
    catchLocation: jlocation
) {
    initRuntimeIfNeeded()
    val declaringClassName = method?.getDeclaringClassName() ?: return

    PluginContext {

        if(blackList.isEmpty()) return

        if (
            declaringClassName.startsWith("Ljava/") ||
            declaringClassName.startsWith("Ljavax/") ||
            declaringClassName.startsWith("Lorg/apache/") ||
            declaringClassName.startsWith("Lsun/") ||
            declaringClassName.startsWith("Lorg/springframework/core/") ||
            declaringClassName.startsWith("Lorg/springframework/cglib/") ||
            declaringClassName.startsWith("Lorg/springframework/util") ||
            declaringClassName.startsWith("Lorg/springframework/beans/") ||
            declaringClassName.startsWith("Lorg/springframework/data/") ||
            declaringClassName.startsWith("Lorg/hibernate/") ||
            declaringClassName.startsWith("Lorg/hibernate/internal")
        )
            return
        memScoped {
            val getObjectClass = GetObjectClass(exception)
            val name = alloc<CPointerVar<ByteVar>>()
            DrillGetClassSignature(getObjectClass, name.ptr, null)
            val exType = name.value?.toKString() ?: "UnknownExType"
            if (exType == "Ljava/lang/ClassNotFoundException;")
                return
        }
        println(
            "className: $declaringClassName\n" +
                    "method: ${method.getName()}"
        )
        try {
            val stacktrace = frameWalker(thread, 4) {
                Frame(method.getName() ?: "crtn", iterateLocalVariables { createVariableLine(thread!!, currentDepth) })
            }
            if (stacktrace.isNotEmpty()) {
                val exceptionData = Json.stringify(
                    ExceptionDataClass.serializer(), ExceptionDataClass(
                        type = exception!!.getType(),
                        message = exception.getMessage() ?: "null",
                        stackTrace = stacktrace,
                        occurredTime = DateTime.now().toString()
                    )
                )
                //fixme
                val scope = Arena()
                val pluginId: CPointer<ByteVar> = "except-ions".cstr.getPointer(scope)
                sendToSocket(pluginId, exceptionData.cstr.getPointer(scope))
            }
        } catch (ex: Throwable) {
            println("Error during process an exceptions: ${ex.message}")
        }
    }
}

