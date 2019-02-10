package com.epam.drill.jvmti.callbacks.methodbind

import com.epam.drill.jvmti.util.getDeclaringClassName
import com.epam.drill.jvmti.util.getName
import jvmapi.*
import kotlinx.cinterop.*

@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventNativeMethodBindEvent")
fun nativeMethodBind(
    jvmtiEnv: jvmtiEnv,
    jniEnv: JNIEnv,
    thread: jthread,
    method: jmethodID,
    address: COpaquePointer,
    newAddressPtr: CPointer<COpaquePointerVar>
) {
    if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "read0" == method.getName()) {
        newAddressPtr.pointed.value = SCReadMethodInterceptorAddress
        SCReadMethodAddress = address.reinterpret()
    }
}
