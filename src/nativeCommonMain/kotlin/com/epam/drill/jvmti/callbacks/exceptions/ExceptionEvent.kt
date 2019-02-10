package com.epam.drill.jvmti.callbacks.exceptions

import com.epam.drill.jvmti.callbacks.exceptions.data.ExceptionDataClass
import com.epam.drill.jvmti.callbacks.exceptions.data.Frame
import com.epam.drill.jvmti.logger.DLogger
import com.epam.drill.jvmti.types.createVariableLine
import com.epam.drill.jvmti.util.frameWalker
import com.epam.drill.jvmti.util.getDeclaringClassName
import com.epam.drill.jvmti.util.getName
import com.epam.drill.jvmti.ws.MessageQueue
import com.epam.kjni.core.GlobState
import com.soywiz.klock.DateTime
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json


val exceptionLogger
    get() = DLogger("jvmtiEventExceptionEvent")

@ImplicitReflectionSerializer
@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventExceptionEvent")
fun jvmtiEventExceptionEvent(
    jvmtiEnv: jvmtiEnv,
    jniEnv: JNIEnv,
    thread: jthread,
    method: jmethodID,
    location: jlocation,
    exception: jthrowable,
    catchMethod: jmethodID,
    catchLocation: jlocation
) {
    val declaringClassName = method.getDeclaringClassName()
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
        val getObjectClass = GlobState.jni.GetObjectClass?.invoke(GlobState.env, exception)
        val name = this.alloc<CPointerVar<ByteVar>>()
        GetClassSignature(getObjectClass, name.ptr, null)
        val exType = name.value?.toKString() ?: "UnknownExType"
        if (
            exType == "Ljava/lang/ClassNotFoundException;"
        )
            return@jvmtiEventExceptionEvent
    }

    exceptionLogger.debug {
        "className: $declaringClassName\n" +
                "method: ${method.getName()}"
    }
    try {
        val stacktrace = frameWalker(thread, 4) {
            val className = getClassName()
            if (className == null ||
                className.startsWith("Ljava/") ||
                className.startsWith("Lsun/") ||
                className.startsWith("Lsun/")
            )
                return@jvmtiEventExceptionEvent
            else {

                val iterateLocalVariables =
                    iterateLocalVariables {
                        createVariableLine(thread, currentDepth)
                    }
                Frame(method.getName() ?: "cantRetrieveTheName", iterateLocalVariables)

            }
        }
        if (stacktrace.isNotEmpty()) {
            memScoped {

                val exceptionData = Json.stringify(
                    ExceptionDataClass.serializer(), ExceptionDataClass(
                        type = exception.getType(),
                        message = exception.getMessage() ?: "null",
                        stackTrace = stacktrace,
                        occurredTime = DateTime.now().toString()
                    )
                )
                MessageQueue.sendMessage(exceptionData)
            }

        }
    } catch (ex: Throwable) {
        exceptionLogger.error { "Error during process an exceptions: ${ex.message}" }
    }
}