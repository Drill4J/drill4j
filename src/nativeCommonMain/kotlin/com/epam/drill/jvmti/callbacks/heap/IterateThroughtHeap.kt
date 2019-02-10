package com.epam.drill.jvmti.callbacks.heap

import jvmapi.JVMTI_VISIT_OBJECTS
import kotlinx.cinterop.asStableRef


@ExperimentalUnsignedTypes
@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiHeapIterationCallbackEv")
fun jvmtiHeapIterationCallbackEv(
    class_tag: jvmapi.jlong,
    size: jvmapi.jlong,
    tag_ptr: kotlinx.cinterop.CPointer<jvmapi.jlongVar>?,
    length: jvmapi.jint,
    user_data: kotlinx.cinterop.COpaquePointer?
): UInt {
    val stableRef = user_data?.asStableRef<UserData>()
    val kotlinReference = stableRef?.get()
    kotlinReference?.hi = "Ohhhhhh it's after something iteration..."
    kotlinReference?.i = kotlinReference?.i?.plus(1)!!
    return JVMTI_VISIT_OBJECTS
}


data class UserData(var hi: String, var i: Int)