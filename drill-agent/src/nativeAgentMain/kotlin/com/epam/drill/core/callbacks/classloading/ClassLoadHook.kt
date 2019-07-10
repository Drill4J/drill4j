package com.epam.drill.core.callbacks.classloading

import com.epam.drill.core.*
import com.epam.drill.core.plugin.loader.*
import jvmapi.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.set
import kotlinx.cinterop.value

@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventClassFileLoadHookEvent")
fun classLoadEvent(
    jvmtiEnv: CPointer<jvmapi.jvmtiEnvVar>?,
    jniEnv: CPointer<jvmapi.JNIEnvVar>?,
    classBeingRedefined: jvmapi.jclass?,
    loader: jobject?,
    kClassName: String?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jvmapi.jintVar>?,
    newData: CPointer<CPointerVar<UByteVar>>?
) {
    if (isNotSuitableClass(kClassName, classData, loader, protection_domain)) return

    exec { pstorage.values.filterIsInstance<InstrumentationNativePlugin>() }.forEach { instrumentedPlugin ->
        instrumentedPlugin.instrument(kClassName!!, classData!!.readBytes(classDataLen))?.let { instrumentedBytes ->
            val instrumentedSize = instrumentedBytes.size
            Allocate(instrumentedSize.toLong(), newData)
            instrumentedBytes.forEachIndexed { index, byte ->
                val innerValue = newData!!.pointed.value!!
                innerValue[index] = byte.toUByte()
            }
            newClassDataLen!!.pointed.value = instrumentedSize
        }
    }

}

private fun isNotSuitableClass(
    kClassName: String?,
    classData: CPointer<UByteVar>?,
    loader: jobject?,
    protection_domain: jobject?
): Boolean {
    return (isSyntheticClass(kClassName, classData) || isBootstrapClassLoading(loader, protection_domain)
            || exec { pstorage.isEmpty() })
}

private fun isBootstrapClassLoading(loader: jobject?, protection_domain: jobject?) =
    loader == null || protection_domain == null

private fun isSyntheticClass(
    kClassName: String?,
    classData: CPointer<UByteVar>?
) = kClassName == null || classData == null

