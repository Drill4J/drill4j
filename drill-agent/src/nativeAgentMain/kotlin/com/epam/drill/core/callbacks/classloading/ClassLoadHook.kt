package com.epam.drill.core.callbacks.classloading

import com.epam.drill.core.exec
import jvmapi.Allocate
import jvmapi.DeleteLocalRef
import jvmapi.ExceptionDescribe
import jvmapi.GetArrayLength
import jvmapi.GetByteArrayElements
import jvmapi.JNI_ABORT
import jvmapi.NewByteArray
import jvmapi.ReleaseByteArrayElements
import jvmapi.SetByteArrayRegion
import jvmapi.jbyteArray
import jvmapi.jbyteVar
import jvmapi.jint
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

@ExperimentalUnsignedTypes
@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventClassFileLoadHookEvent")
fun classLoadEvent(
    jvmtiEnv: CPointer<jvmapi.jvmtiEnvVar>?,
    jniEnv: CPointer<jvmapi.JNIEnvVar>?,
    classBeingRedefined: jvmapi.jclass?,
    loader: jvmapi.jobject?,
    className: CPointer<ByteVar>?,
    protection_domain: jvmapi.jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jvmapi.jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?


) {
    initRuntimeIfNeeded()

    try {

        if (className != null && classData != null) {
            val kClassName = className.toKString()
            if (loader != null && protection_domain != null) {
                exec {
                    pInstrumentedStorage["coverage"]
                }?.let { instrumentedPlugin ->
                    val newByteArray: jbyteArray? = NewByteArray(classDataLen)
                    ExceptionDescribe()
                    SetByteArrayRegion(
                        newByteArray, 0, classDataLen,
                        getBytes(newByteArray, classDataLen, classData)
                    )

                    instrumentedPlugin.instrument(kClassName, newByteArray!!)?.let { instrument ->

                        val size = GetArrayLength(instrument)
                        Allocate(size.toLong(), newData)

                        GetByteArrayElements(instrument, null)?.let { nativeBytes ->

                            for (i in 0 until size) {
                                val pointed = newData!!.pointed
                                val innerValue = pointed.value!!
                                innerValue[i] = nativeBytes[i].toUByte()
                            }

                            ReleaseByteArrayElements(instrument, nativeBytes, JNI_ABORT)
                            newClassDataLen!!.pointed.value = size
                        }
                    }
                    DeleteLocalRef(newByteArray)
                    ExceptionDescribe()
                }
            }
        }

    } catch (ex: Throwable) {
        println(className?.toKString())
        ex.printStackTrace()
    }
}

private fun getBytes(
    newByteArray: jbyteArray?,
    classDataLen: jint,
    classData: CPointer<UByteVar>
): CPointer<jbyteVar>? {
    val bytess: CPointer<jbyteVar>? = GetByteArrayElements(newByteArray, null)

    ExceptionDescribe()
    for (i in 0 until classDataLen) {
        val uByte = classData[i]
        bytess!![i] = uByte.toByte()
    }
    return bytess
}

