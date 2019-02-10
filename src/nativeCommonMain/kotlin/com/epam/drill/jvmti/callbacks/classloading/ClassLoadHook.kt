package com.epam.drill.jvmti.callbacks.classloading

import com.epam.drill.jvmti.callbacks.classloading.instrumentation.modifyClass
import com.soywiz.kmem.buildByteArray
import jvmapi.*
import kotlinx.cinterop.*

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


@ExperimentalUnsignedTypes
fun CPointer<UByteVar>.toByteArray(size: Int) = buildByteArray {
    for (i in 0 until size) {
        append(this@toByteArray[i].toByte())
    }
}
