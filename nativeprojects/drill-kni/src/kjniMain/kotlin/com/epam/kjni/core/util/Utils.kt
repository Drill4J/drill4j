package com.epam.kjni.core.util

import com.epam.kjni.core.Synthetic
import jvmapi.*
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.toKString


data class X(val value: Any, val primitive: Boolean)


abstract class JavaMethod(
    val jO: jobject?,
    val javaClass: jclass?,
    val methodName: String,
    val methodSignature: String
) {

    fun getMethod(): jmethodID? {
        return GetMethodID(javaClass, methodName, methodSignature)
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
    val methodName: String,
    val methodSignature: String
) {

    fun getMethod(): jmethodID? {
        return GetMethodID( javaClass, methodName, methodSignature)
    }
}

fun getJString(value: String): jstring? {
    //fixme deallocate
    val newStringUTF = NewStringUTF(value)
    return newStringUTF
}

fun getKString(value: jobject?): String {
    //fixme deallocate
    val getStringUTFChars = GetStringUTFChars(value, null)
    return getStringUTFChars?.toKString()!!
}
