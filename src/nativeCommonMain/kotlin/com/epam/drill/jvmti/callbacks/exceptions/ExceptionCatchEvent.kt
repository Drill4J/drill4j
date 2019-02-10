package com.epam.drill.jvmti.callbacks.exceptions

import jvmapi.*


@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventExceptionCatchEvent")
fun jvmtiEventExceptionCatchEvent(
    jvmti_env: jvmtiEnv,
    jni_env: JNIEnv,
    thread: jthread,
    method: jmethodID,
    location: jlocation,
    exception: jobject
) {
    println("Hi I'm EXCEPTION_CATCH")
}