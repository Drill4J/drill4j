package com.epam.drill.core.callbacks.classloading

import com.epam.drill.core.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.gen.*
import kotlinx.cinterop.*

@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventClassFileLoadHookEvent")
fun classLoadEvent(
    jvmtiEnv: CPointer<jvmtiEnvVar>?,
    jniEnv: CPointer<JNIEnvVar>?,
    classBeingRedefined: jclass?,
    loader: jobject?,
    kClassName: String?,
    protection_domain: jobject?,
    classDataLen: jint,
    classData: CPointer<UByteVar>?,
    newClassDataLen: CPointer<jintVar>?,
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

