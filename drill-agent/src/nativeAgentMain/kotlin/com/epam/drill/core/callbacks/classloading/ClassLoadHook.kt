package com.epam.drill.core.callbacks.classloading

import com.epam.drill.core.JClassVersions
import com.epam.drill.core.addNewVersion
import com.epam.drill.core.di
import com.soywiz.kmem.buildByteArray
import drillInternal.addClass
import jvmapi.*
import kotlinx.cinterop.*

//@ThreadLocal
//val ss = mutableMapOf<String,ByteArray>()


@ExperimentalUnsignedTypes
@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventClassFileLoadHookEvent")
fun classLoadEvent(
    jvmtiEnv: kotlinx.cinterop.CPointer<jvmapi.jvmtiEnvVar /* = kotlinx.cinterop.CPointerVarOf<jvmapi.jvmtiEnv /* = kotlinx.cinterop.CPointer<jvmapi.jvmtiInterface_1_> */> */>?,
    jniEnv: kotlinx.cinterop.CPointer<jvmapi.JNIEnvVar /* = kotlinx.cinterop.CPointerVarOf<jvmapi.JNIEnv /* = kotlinx.cinterop.CPointer<jvmapi.JNINativeInterface_> */> */>?,
    classBeingRedefined: jvmapi.jclass? /* = kotlinx.cinterop.CPointer<cnames.structs._jobject>? */,
    loader: jvmapi.jobject? /* = kotlinx.cinterop.CPointer<cnames.structs._jobject>? */,
    className: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar /* = kotlinx.cinterop.ByteVarOf<kotlin.Byte> */>?,
    protection_domain: jvmapi.jobject? /* = kotlinx.cinterop.CPointer<cnames.structs._jobject>? */,
    classDataLen: jvmapi.jint /* = kotlin.Int */,
    classData: kotlinx.cinterop.CPointer<kotlinx.cinterop.UByteVar /* = kotlinx.cinterop.UByteVarOf<kotlin.UByte> */>?,
    newClassDataLen: kotlinx.cinterop.CPointer<jvmapi.jintVar /* = kotlinx.cinterop.IntVarOf<jvmapi.jint /* = kotlin.Int */> */>?,
    newData: kotlinx.cinterop.CPointer<kotlinx.cinterop.CPointerVar<kotlinx.cinterop.UByteVar> /* = kotlinx.cinterop.CPointerVarOf<kotlinx.cinterop.CPointer<kotlinx.cinterop.UByteVarOf<kotlin.UByte>>> */>?


) {
    initRuntimeIfNeeded()

    try {

        if (className != null && classData != null) {
            val kClassName = className.toKString()
//                launch {
//                    mutex.withLock {
//            println(kClassName)

            addClass(classData, classDataLen)
            if (loader != null && protection_domain != null) {


                if (kClassName.contains("/drilspringframework")) {

                    val instrumentedPlugin = di{pInstrumentedStorage}["coverage"]
                    if (instrumentedPlugin != null) {
                        val newByteArray: jbyteArray? = NewByteArray(classDataLen)
                        ExceptionDescribe()
                        SetByteArrayRegion(
                            newByteArray,
                            0,
                            classDataLen,
                            getBytes(newByteArray, classDataLen, classData)
                        )
                        ExceptionDescribe()
                        val instrument = instrumentedPlugin.instrument(kClassName, newByteArray!!)

                            val getByteArrayElements1 = GetByteArrayElements(instrument, null)
                            val size = GetArrayLength(instrument)
                            Allocate(size.toLong(), newData)
                            for (i in 0 until size) {
                                val pointed = newData!!.pointed
                                val value: CPointer<UByteVarOf<UByte>> = pointed.value!!
                                value[i] = getByteArrayElements1!![i].toUByte()
                            }


                            val byteArray = ByteArray(size)

                            for (i in 0 until size) {
                                byteArray[i] = getByteArrayElements1!![i].toByte()
                            }

                            newClassDataLen!!.pointed.value = size

////                            loadedClasses[kClassName]!!.addNewVersion(byteArray)
                    }
//                }
                }
//
            }
        }

    } catch (ex: Throwable) {
        println(className?.toKString())
        ex.printStackTrace()
    }
}

fun saveClassDataToStorage(
    kClassName: String,
    bytes: ByteArray
) {

    di {
        val jClassVersions = loadedClasses[kClassName]

        if (jClassVersions == null) {
            loadedClasses[kClassName] = JClassVersions(bytes)
        } else {
            jClassVersions.addNewVersion(bytes)
        }
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


@ExperimentalUnsignedTypes
fun CPointer<UByteVar>.toByteArray(size: Int) = buildByteArray {
    for (i in 0 until size) {
        append(this@toByteArray[i].toByte())
    }
}
