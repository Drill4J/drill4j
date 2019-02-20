package com.epam.drill.plugin.exception

import com.epam.drill.plugin.exception.datatypes.createVariableLine
import com.epam.drillnative.api.DrillGetClassSignature
import com.epam.drillnative.api.sendToSocket
import com.soywiz.klock.DateTime
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.serialization.json.Json


@Suppress("UNUSED_ANONYMOUS_PARAMETER")
fun exceptionCallback(): CPointer<CFunction<(CPointer<jvmtiEnvVar>?, CPointer<JNIEnvVar>?, jthread?, jmethodID?, jlocation, jobject?, jmethodID?, jlocation) -> Unit>> {
    return staticCFunction { jvmtiEnv, jniEnv, thread, method, location, exception, catchMethod, catchLocation ->
        initRuntimeIfNeeded()
        val declaringClassName = method?.getDeclaringClassName() ?: return@staticCFunction
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
            return@staticCFunction
        memScoped {
            val getObjectClass = GetObjectClass(exception)
            val name = alloc<CPointerVar<ByteVar>>()
            DrillGetClassSignature(getObjectClass, name.ptr, null)
            val exType = name.value?.toKString() ?: "UnknownExType"
            if (exType == "Ljava/lang/ClassNotFoundException;")
                return@staticCFunction
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
                sendToSocket(exceptionData)
            }
        } catch (ex: Throwable) {
            println("Error during process an exceptions: ${ex.message}")
        }
    }
}
