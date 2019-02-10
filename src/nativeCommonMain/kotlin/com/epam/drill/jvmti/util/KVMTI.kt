@file:Suppress("unused")

package com.epam.drill.jvmti.util

import com.epam.drill.jvmti.callbacks.exceptions.data.Frame
import com.epam.drill.jvmti.callbacks.exceptions.data.VariableLine
import jvmapi.GetLocalVariableTable
import jvmapi.GetStackTrace
import jvmapi.JVMTI_DISABLE
import jvmapi.JVMTI_ENABLE
import jvmapi.JVMTI_EVENT_BREAKPOINT
import jvmapi.JVMTI_EVENT_CLASS_FILE_LOAD_HOOK
import jvmapi.JVMTI_EVENT_CLASS_LOAD
import jvmapi.JVMTI_EVENT_CLASS_PREPARE
import jvmapi.JVMTI_EVENT_COMPILED_METHOD_LOAD
import jvmapi.JVMTI_EVENT_COMPILED_METHOD_UNLOAD
import jvmapi.JVMTI_EVENT_DATA_DUMP_REQUEST
import jvmapi.JVMTI_EVENT_DYNAMIC_CODE_GENERATED
import jvmapi.JVMTI_EVENT_EXCEPTION
import jvmapi.JVMTI_EVENT_EXCEPTION_CATCH
import jvmapi.JVMTI_EVENT_FIELD_ACCESS
import jvmapi.JVMTI_EVENT_FIELD_MODIFICATION
import jvmapi.JVMTI_EVENT_FRAME_POP
import jvmapi.JVMTI_EVENT_GARBAGE_COLLECTION_FINISH
import jvmapi.JVMTI_EVENT_GARBAGE_COLLECTION_START
import jvmapi.JVMTI_EVENT_METHOD_ENTRY
import jvmapi.JVMTI_EVENT_METHOD_EXIT
import jvmapi.JVMTI_EVENT_MONITOR_CONTENDED_ENTER
import jvmapi.JVMTI_EVENT_MONITOR_CONTENDED_ENTERED
import jvmapi.JVMTI_EVENT_MONITOR_WAIT
import jvmapi.JVMTI_EVENT_MONITOR_WAITED
import jvmapi.JVMTI_EVENT_NATIVE_METHOD_BIND
import jvmapi.JVMTI_EVENT_OBJECT_FREE
import jvmapi.JVMTI_EVENT_RESOURCE_EXHAUSTED
import jvmapi.JVMTI_EVENT_SINGLE_STEP
import jvmapi.JVMTI_EVENT_THREAD_END
import jvmapi.JVMTI_EVENT_THREAD_START
import jvmapi.JVMTI_EVENT_VM_DEATH
import jvmapi.JVMTI_EVENT_VM_INIT
import jvmapi.JVMTI_EVENT_VM_OBJECT_ALLOC
import jvmapi.JVMTI_EVENT_VM_START
import jvmapi.SetEventNotificationMode
import jvmapi.gdata
import jvmapi.jint
import jvmapi.jintVar
import jvmapi.jthread
import jvmapi.jvmtiFrameInfo
import jvmapi.jvmtiLocalVariableEntry
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value


@Suppress("ClassName")
object frameWalker {
    inline operator fun invoke(
        thread: jthread?,
        maxFrameCount: jint, block: FrameInfo.() -> Frame?
    ) = memScoped {
        val frames = allocArray<jvmtiFrameInfo>(maxFrameCount)
        val count = alloc<jintVar>()
        GetStackTrace(thread, 0, maxFrameCount, frames, count.ptr)
        val stackTrace = mutableListOf<Frame>()
        for (depth in 0 until count.value) {
            val info: jvmtiFrameInfo = frames[depth]
            val frame = block(FrameInfo(info, depth))
            if (frame != null) {
                stackTrace.add(frame)
            }
        }
        stackTrace
    }

}


class FrameInfo(val info: jvmtiFrameInfo, val currentDepth: Int) {

    fun getClassName(): String? {
        return info.method?.getDeclaringClassName()
    }


    inline fun iterateLocalVariables(block: jvmtiLocalVariableEntry.() -> VariableLine?) = memScoped {
        val localVariableEntry = alloc<CPointerVar<jvmtiLocalVariableEntry>>()
        val entryCountPtr = alloc<jintVar>()
        GetLocalVariableTable(info.method, entryCountPtr.ptr, localVariableEntry.ptr)
        val frames = mutableListOf<VariableLine>()
        for (j in (0 until entryCountPtr.value).reversed()) {
            val currentEntry: jvmtiLocalVariableEntry = localVariableEntry.value!![j]
            val element = block(currentEntry)
            if (element != null)
                frames.add(element)
        }
        frames
    }

}

fun enableJvmtiEventBreakpoint(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_BREAKPOINT = true
}

fun enableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = true
}

fun enableJvmtiEventClassLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_LOAD = true
}

fun enableJvmtiEventClassPrepare(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_PREPARE = true
}

fun enableJvmtiEventCompiledMethodLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_COMPILED_METHOD_LOAD = true
}

fun enableJvmtiEventCompiledMethodUnload(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_COMPILED_METHOD_UNLOAD = true
}

fun enableJvmtiEventDataDumpRequest(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_DATA_DUMP_REQUEST = true
}

fun enableJvmtiEventDynamicCodeGenerated(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_DYNAMIC_CODE_GENERATED = true
}

fun enableJvmtiEventException(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_EXCEPTION = true
}

fun enableJvmtiEventExceptionCatch(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION_CATCH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_EXCEPTION_CATCH = true
}

fun enableJvmtiEventFieldAccess(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FIELD_ACCESS = true
}

fun enableJvmtiEventFieldModification(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FIELD_MODIFICATION = true
}

fun enableJvmtiEventFramePop(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FRAME_POP, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FRAME_POP = true
}

fun enableJvmtiEventGarbageCollectionFinish(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = true
}

fun enableJvmtiEventGarbageCollectionStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_GARBAGE_COLLECTION_START = true
}

fun enableJvmtiEventMethodEntry(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_METHOD_ENTRY = true
}

fun enableJvmtiEventMethodExit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_METHOD_EXIT = true
}

fun enableJvmtiEventMonitorContendedEnter(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_CONTENDED_ENTER = true
}

fun enableJvmtiEventMonitorContendedEntered(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = true
}

fun enableJvmtiEventMonitorWait(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_WAIT = true
}

fun enableJvmtiEventMonitorWaited(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_WAITED = true
}

fun enableJvmtiEventNativeMethodBind(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_NATIVE_METHOD_BIND = true
}

fun enableJvmtiEventObjectFree(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_OBJECT_FREE = true
}

fun enableJvmtiEventResourceExhausted(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_RESOURCE_EXHAUSTED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_RESOURCE_EXHAUSTED = true
}

fun enableJvmtiEventSingleStep(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_SINGLE_STEP = true
}

fun enableJvmtiEventThreadEnd(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_THREAD_END = true
}

fun enableJvmtiEventThreadStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_THREAD_START = true
}

fun enableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_DEATH = true
}

fun enableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_INIT = true
}

fun enableJvmtiEventVmObjectAlloc(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_OBJECT_ALLOC = true
}

fun enableJvmtiEventVmStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_START = true
}

fun disableJvmtiEventBreakpoint(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_BREAKPOINT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_BREAKPOINT = false
}

fun disableJvmtiEventClassFileLoadHook(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_FILE_LOAD_HOOK = false
}

fun disableJvmtiEventClassLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_LOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_LOAD = false
}

fun disableJvmtiEventClassPrepare(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_PREPARE, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_CLASS_PREPARE = false
}

fun disableJvmtiEventCompiledMethodLoad(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_COMPILED_METHOD_LOAD = false
}

fun disableJvmtiEventCompiledMethodUnload(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_COMPILED_METHOD_UNLOAD = false
}

fun disableJvmtiEventDataDumpRequest(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_DATA_DUMP_REQUEST = false
}

fun disableJvmtiEventDynamicCodeGenerated(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_DYNAMIC_CODE_GENERATED = false
}

fun disableJvmtiEventException(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_EXCEPTION = false
}

fun disableJvmtiEventExceptionCatch(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION_CATCH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_EXCEPTION_CATCH = false
}

fun disableJvmtiEventFieldAccess(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FIELD_ACCESS, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FIELD_ACCESS = false
}

fun disableJvmtiEventFieldModification(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FIELD_MODIFICATION, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FIELD_MODIFICATION = false
}

fun disableJvmtiEventFramePop(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FRAME_POP, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_FRAME_POP = false
}

fun disableJvmtiEventGarbageCollectionFinish(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_GARBAGE_COLLECTION_FINISH = false
}

fun disableJvmtiEventGarbageCollectionStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_GARBAGE_COLLECTION_START = false
}

fun disableJvmtiEventMethodEntry(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_METHOD_ENTRY = false
}

fun disableJvmtiEventMethodExit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_EXIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_METHOD_EXIT = false
}

fun disableJvmtiEventMonitorContendedEnter(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_CONTENDED_ENTER = false
}

fun disableJvmtiEventMonitorContendedEntered(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_CONTENDED_ENTERED = false
}

fun disableJvmtiEventMonitorWait(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_WAIT = false
}

fun disableJvmtiEventMonitorWaited(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAITED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_MONITOR_WAITED = false
}

fun disableJvmtiEventNativeMethodBind(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_NATIVE_METHOD_BIND = false
}

fun disableJvmtiEventObjectFree(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_OBJECT_FREE, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_OBJECT_FREE = false
}

fun disableJvmtiEventResourceExhausted(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_RESOURCE_EXHAUSTED, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_RESOURCE_EXHAUSTED = false
}

fun disableJvmtiEventSingleStep(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_SINGLE_STEP = false
}

fun disableJvmtiEventThreadEnd(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_END, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_THREAD_END = false
}

fun disableJvmtiEventThreadStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_THREAD_START = false
}

fun disableJvmtiEventVmDeath(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_DEATH, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_DEATH = false
}

fun disableJvmtiEventVmInit(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_INIT, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_INIT = false
}

fun disableJvmtiEventVmObjectAlloc(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_OBJECT_ALLOC = false
}

fun disableJvmtiEventVmStart(thread: jthread? = null) {
    SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_VM_START, thread)
    gdata?.pointed?.IS_JVMTI_EVENT_VM_START = false
}