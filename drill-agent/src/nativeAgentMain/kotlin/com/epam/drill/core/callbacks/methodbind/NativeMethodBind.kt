//package com.epam.drill.core.callbacks.methodbind
//
//import com.epam.drill.core.util.getDeclaringClassName
//import com.epam.drill.core.util.getName
//import jvmapi.*
//import kotlinx.cinterop.*
//
//@Suppress("unused", "UNUSED_PARAMETER")
//@CName("jvmtiEventNativeMethodBindEvent")
//fun nativeMethodBind(
//    jvmtiEnv: jvmtiEnv,
//    jniEnv: JNIEnv,
//    thread: jthread,
//    method: jmethodID,
//    address: COpaquePointer,
//    newAddressPtr: CPointer<COpaquePointerVar>
//) {
//    if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "read0" == method.getName()) {
//        newAddressPtr.pointed.value = SCReadMethodInterceptorAddress
//        SCReadMethodAddress = address.reinterpret()
//    } else if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "write0" == method.getName()) {
//        newAddressPtr.pointed.value = SCWriteMethodInterceptorAddress
//        SCWriteMethodAddress = address.reinterpret()
//    } else if ("Lsun/nio/ch/SocketDispatcher;" == method.getDeclaringClassName() && "writev0" == method.getName()) {
//        newAddressPtr.pointed.value = SCWritevMethodInterceptorAddress
//        SCWritevMethodAddress = address.reinterpret()
//    }
//}
