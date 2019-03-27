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
    jvmtiEnv: jvmtiEnv?,
    jniEnv: JNIEnv?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    className: String?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>,
    newClassDataLen: CPointer<jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?
) = memScoped {
    try {
        if (className != null) {
//            if (className == "sun/nio/ch/IOUtil") {
        //                modifyClass(classData, classDataLen, newData, newClassDataLen)
//            }
    }
    } catch (ex: Throwable) {
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
