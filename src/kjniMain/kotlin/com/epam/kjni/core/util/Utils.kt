package com.epam.kjni.core.util

import com.epam.kjni.core.GlobState.env
import com.epam.kjni.core.Synthetic
import jvmapi.jclass
import jvmapi.jmethodID
import jvmapi.jobject
import jvmapi.jstring
import jvmapi.jvalue
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value


data class X(val value: Any, val primitive: Boolean)


abstract class JavaMethod(
    val jO: jobject?,
    val javaClass: jclass?,
    val methodName: CPointer<ByteVar>,
    val methodSignature: CPointer<ByteVar>
) {

    fun getMethod(): jmethodID? {
        val jniEnv = env!!.pointed.value?.pointed
        return jniEnv?.GetMethodID!!(env, javaClass, methodName, methodSignature)
    }

    abstract operator fun invoke(vararg raw: X): Any

}

@ExperimentalUnsignedTypes
fun toJObjectArray(raw: Array<out X>): CArrayPointer<jvalue> {
    return nativeHeap.allocArray(raw.size) { index ->
        val it = raw[index]
        val value = it.value
        when (value) {
            is Synthetic -> {
                l = value.javaObject
            }
            is String -> {
                l = getJString(value)!!
            }
            is Int -> {
                if (it.primitive) {
                    i = value
                } else {
                    l = java.lang.Integer(value).javaObject
                }
            }
            is Double -> {
                if (it.primitive) {
                    d = value
                } else {
                    l = java.lang.Double(value).javaObject
                }
            }
            is Byte -> {
                if (it.primitive) {
                    b = value
                } else {
                    l = java.lang.Byte(value).javaObject
                }
            }
            is Short -> {
                if (it.primitive) {
                    s = value
                } else {
                    l = java.lang.Short(value).javaObject
                }
            }
            is Long -> {
                if (it.primitive) {
                    j = value
                } else {
                    l = java.lang.Long(value).javaObject
                }
            }
            is Float -> {
                if (it.primitive) {
                    f = value
                } else {
                    l = java.lang.Float(value).javaObject
                }
            }
            is Boolean -> {
                if (it.primitive) {
                    z = if (value) {
                        1.toUByte()
                    } else {
                        0.toUByte()
                    }
                } else {
                    l = java.lang.Boolean(value).javaObject
                }
            }
            is Char -> {
                if (it.primitive) {
                    c = value.toInt().toUShort()
                } else {
                    l = java.lang.Character(value).javaObject
                }
            }
            else -> {
                println("Please Implement me ${it.value::class}")
                throw RuntimeException("Please Implement me ${it::class}")
            }
        }
    }
}

class JavaConstructor(
    val javaClass: jclass?,
    val methodName: CPointer<ByteVar>,
    val methodSignature: CPointer<ByteVar>
) {

    fun getMethod(): jmethodID? {
        val jniEnv = env!!.pointed.value?.pointed
        return jniEnv?.GetMethodID!!(env, javaClass, methodName, methodSignature)
    }
}

fun getJString(value: String): jstring? {
    //fixme deallocate
    val newStringUTF = env?.pointed?.value?.pointed?.NewStringUTF!!(env, value.cstr.getPointer(Arena()))
    return newStringUTF
}

fun getKString(value: jobject?): String {
    //fixme deallocate
    val getStringUTFChars = env?.pointed?.value?.pointed?.GetStringUTFChars!!(env, value, null)
    return getStringUTFChars?.toKString()!!
}
