package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.*
import com.epam.drill.plugin.exception.datatypes.*
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.serialization.*


@Suppress("unused")
class ExceptionNativePlugin constructor(override var id: String) : NativePart<ExceptionConfig>() {

    override fun on() {
        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, null)
        println("$id is enabled")
    }

    override fun off() {
        SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, null)
        println("$id is disabled")
    }

    override fun destroyPlugin(unloadReason: UnloadReason) {
        val clb = pluginApi { clb }?.pointed
        clb?.Exception = null
        SetEventCallbacks(clb?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        println("$id plugin destroyed")
    }

    override fun initPlugin() {
        val clb = pluginApi { clb }?.pointed
        clb?.Exception = staticCFunction(::exceptionCallback)
        SetEventCallbacks(clb?.ptr, sizeOf<jvmtiEventCallbacks>().toInt())
        println("$id plugin initialized")
    }


    fun exception(thread: jthread, method: jmethodID, exception: jobject) {
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
        ) return
        memScoped {
            val exceptionClass = GetObjectClass(exception)
            val name = alloc<CPointerVar<ByteVar>>()
            GetClassSignature(exceptionClass, name.ptr, null)
            val exType = name.value?.toKString() ?: "UnknownExType"
            if (exType == "Ljava/lang/ClassNotFoundException;") return
        }
        try {
            val stacktrace = frameWalker(thread, 4) {
                Frame(method.getName() ?: "crtn", iterateLocalVariables { createVariableLine(thread, currentDepth) })
            }
            if (stacktrace.isNotEmpty()) {
                val exceptionData = js.stringify(
                    ExceptionDataClass.serializer(), ExceptionDataClass(
                        type = exception.getType(),
                        message = exception.getMessage() ?: "null",
                        stackTrace = stacktrace,
                        occurredTime = "10.10.12"
                    )
                )
                pluginApi { sender }(id.cstr.getPointer(Arena()), exceptionData.cstr.getPointer(Arena()))

            }
        } catch (ex: Throwable) {
            println("Error during process an exceptions: ${ex.message}")
        }
    }


    override var confSerializer: kotlinx.serialization.KSerializer<ExceptionConfig> = ExceptionConfig.serializer()
}

@Serializable
data class ExceptionConfig(val blackList: List<String> = emptyList())