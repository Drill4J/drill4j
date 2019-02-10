package com.epam.drill.jvmti.thread

import com.epam.drill.jvmti.request.DrillRequest
import jvmapi.GetCurrentThread
import jvmapi.GetThreadLocalStorage
import jvmapi.jthread
import jvmapi.jthreadVar
import kotlinx.cinterop.*

fun currentThread() = memScoped {
    val threadAllocation = alloc<jthreadVar>()
    GetCurrentThread(threadAllocation.ptr)
    threadAllocation.value
}

fun drillRequest(thread: jthread? = currentThread()) = memScoped {
    val drillReq = alloc<COpaquePointerVar>()
    GetThreadLocalStorage(thread, drillReq.ptr)
    drillReq.value?.asStableRef<DrillRequest>()?.get()
}