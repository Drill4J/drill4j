package com.epam.drill.core.callbacks.methodbind

import com.epam.drill.core.methodbind.nativeMethodBindMapper
import com.epam.drill.jvmapi.getDeclaringClassName
import com.epam.drill.jvmapi.getName
import jvmapi.JNIEnv
import jvmapi.jmethodID
import jvmapi.jthread
import jvmapi.jvmtiEnv
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

    nativeMethodBindMapper[method.getDeclaringClassName() + method.getName()]?.let {
        println(method.getDeclaringClassName() + method.getName())
        newAddressPtr.pointed.value = it(address)
    }

//    if ("Lsun/nio/ch/SocketDispatcher;" == declaringClassName && "read0" == name) {
//        newAddressPtr.pointed.value = SCReadMethodInterceptorAddress
//        SCReadMethodAddress = address.reinterpret()
//    } else if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "write0" == method.getName()) {
//        newAddressPtr.pointed.value = SCWriteMethodInterceptorAddress
//        SCWriteMethodAddress = address.reinterpret()
//    } else if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "writev0" == method.getName()) {
//        newAddressPtr.pointed.value = SCWritevMethodInterceptorAddress
//        SCWritevMethodAddress = address.reinterpret()
//    }
}
