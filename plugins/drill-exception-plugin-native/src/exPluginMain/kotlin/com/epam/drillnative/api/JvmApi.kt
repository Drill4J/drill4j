@file:Suppress("FunctionName", "unused")

package com.epam.drillnative.api

import jvmapi.jvmtiCapabilities
import jvmapi.jvmtiEventCallbacks
import kotlinx.cinterop.*
import jvmapi.*

fun DrillAllocate(size: jlong, mem_ptr: CValuesRef<CPointerVar<UByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.Allocate?.invoke(jvmtix(), size, mem_ptr?.getPointer(this))
    }
}


fun DrillGetClassSignature(
    klass: jclass?,
    signature_ptr: CValuesRef<CPointerVar<ByteVar>>?,
    generic_ptr: CValuesRef<CPointerVar<ByteVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassSignature?.invoke(
            jvmtix(),
            klass,
            signature_ptr?.getPointer(this),
            generic_ptr?.getPointer(this)
        )
    }
}

fun DrillDeallocate(mem: CValuesRef<UByteVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.Deallocate?.invoke(jvmtix(), mem?.getPointer(this))
    }
}

fun DrillGetThreadState(thread: jthread?, thread_state_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadState?.invoke(jvmtix(), thread, thread_state_ptr?.getPointer(this))
    }
}

fun DrillGetCurrentThread(thread_ptr: CValuesRef<jthreadVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetCurrentThread?.invoke(jvmtix(), thread_ptr?.getPointer(this))
    }
}

fun DrillGetAllThreads(threads_count_ptr: CValuesRef<jintVar>?, threads_ptr: CValuesRef<CPointerVar<jthreadVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetAllThreads?.invoke(
            jvmtix(),
            threads_count_ptr?.getPointer(this),
            threads_ptr?.getPointer(this)
        )
    }
}

fun DrillSuspendThread(thread: jthread?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SuspendThread?.invoke(jvmtix(), thread)
    }
}

fun DrillSuspendThreadList(
    request_count: jint,
    request_list: CValuesRef<jthreadVar>?,
    results: CValuesRef<jvmtiErrorVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SuspendThreadList?.invoke(
            jvmtix(),
            request_count,
            request_list?.getPointer(this),
            results?.getPointer(this)
        )
    }
}

fun DrillResumeThread(thread: jthread?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ResumeThread?.invoke(jvmtix(), thread)
    }
}

fun DrillResumeThreadList(
    request_count: jint,
    request_list: CValuesRef<jthreadVar>?,
    results: CValuesRef<jvmtiErrorVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ResumeThreadList?.invoke(
            jvmtix(),
            request_count,
            request_list?.getPointer(this),
            results?.getPointer(this)
        )
    }
}

fun DrillStopThread(thread: jthread?, exception: jobject?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.StopThread?.invoke(jvmtix(), thread, exception)
    }
}

fun DrillInterruptThread(thread: jthread?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.InterruptThread?.invoke(jvmtix(), thread)
    }
}

fun DrillGetThreadInfo(thread: jthread?, info_ptr: CValuesRef<jvmtiThreadInfo>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadInfo?.invoke(jvmtix(), thread, info_ptr?.getPointer(this))
    }
}

fun DrillGetOwnedMonitorInfo(
    thread: jthread?,
    owned_monitor_count_ptr: CValuesRef<jintVar>?,
    owned_monitors_ptr: CValuesRef<CPointerVar<jobjectVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetOwnedMonitorInfo?.invoke(
            jvmtix(),
            thread,
            owned_monitor_count_ptr?.getPointer(this),
            owned_monitors_ptr?.getPointer(this)
        )
    }
}

fun DrillGetOwnedMonitorStackDepthInfo(
    thread: jthread?,
    monitor_info_count_ptr: CValuesRef<jintVar>?,
    monitor_info_ptr: CValuesRef<CPointerVar<jvmtiMonitorStackDepthInfo>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetOwnedMonitorStackDepthInfo?.invoke(
            jvmtix(),
            thread,
            monitor_info_count_ptr?.getPointer(this),
            monitor_info_ptr?.getPointer(this)
        )
    }
}

fun DrillGetCurrentContendedMonitor(thread: jthread?, monitor_ptr: CValuesRef<jobjectVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetCurrentContendedMonitor?.invoke(jvmtix(), thread, monitor_ptr?.getPointer(this))
    }
}

fun DrillRunAgentThread(thread: jthread?, proc: jvmtiStartFunction?, arg: CValuesRef<*>?, priority: jint) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RunAgentThread?.invoke(jvmtix(), thread, proc, arg?.getPointer(this), priority)
    }
}

fun DrillSetThreadLocalStorage(thread: jthread?, data: CValuesRef<*>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetThreadLocalStorage?.invoke(jvmtix(), thread, data?.getPointer(this))
    }
}

fun DrillGetThreadLocalStorage(thread: jthread?, data_ptr: CValuesRef<COpaquePointerVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadLocalStorage?.invoke(jvmtix(), thread, data_ptr?.getPointer(this))
    }
}

fun DrillGetTopThreadGroups(
    group_count_ptr: CValuesRef<jintVar>?,
    groups_ptr: CValuesRef<CPointerVar<jthreadGroupVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetTopThreadGroups?.invoke(
            jvmtix(),
            group_count_ptr?.getPointer(this),
            groups_ptr?.getPointer(this)
        )
    }
}

fun DrillGetThreadGroupInfo(group: jthreadGroup?, info_ptr: CValuesRef<jvmtiThreadGroupInfo>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadGroupInfo?.invoke(jvmtix(), group, info_ptr?.getPointer(this))
    }
}

fun DrillGetThreadGroupChildren(
    group: jthreadGroup?,
    thread_count_ptr: CValuesRef<jintVar>?,
    threads_ptr: CValuesRef<CPointerVar<jthreadVar>>?,
    group_count_ptr: CValuesRef<jintVar>?,
    groups_ptr: CValuesRef<CPointerVar<jthreadGroupVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadGroupChildren?.invoke(
            jvmtix(),
            group,
            thread_count_ptr?.getPointer(this),
            threads_ptr?.getPointer(this),
            group_count_ptr?.getPointer(this),
            groups_ptr?.getPointer(this)
        )
    }
}

fun DrillGetStackTrace(
    thread: jthread?,
    start_depth: jint,
    max_frame_count: jint,
    frame_buffer: CValuesRef<jvmtiFrameInfo>?,
    count_ptr: CValuesRef<jintVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetStackTrace?.invoke(
            jvmtix(),
            thread,
            start_depth,
            max_frame_count,
            frame_buffer?.getPointer(this),
            count_ptr?.getPointer(this)
        )
    }
}

fun DrillGetAllStackTraces(
    max_frame_count: jint,
    stack_info_ptr: CValuesRef<CPointerVar<jvmtiStackInfo>>?,
    thread_count_ptr: CValuesRef<jintVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetAllStackTraces?.invoke(
            jvmtix(),
            max_frame_count,
            stack_info_ptr?.getPointer(this),
            thread_count_ptr?.getPointer(this)
        )
    }
}

fun DrillGetThreadListStackTraces(
    thread_count: jint,
    thread_list: CValuesRef<jthreadVar>?,
    max_frame_count: jint,
    stack_info_ptr: CValuesRef<CPointerVar<jvmtiStackInfo>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadListStackTraces?.invoke(
            jvmtix(),
            thread_count,
            thread_list?.getPointer(this),
            max_frame_count,
            stack_info_ptr?.getPointer(this)
        )
    }
}

fun DrillGetFrameCount(thread: jthread?, count_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetFrameCount?.invoke(jvmtix(), thread, count_ptr?.getPointer(this))
    }
}

fun DrillPopFrame(thread: jthread?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.PopFrame?.invoke(jvmtix(), thread)
    }
}

fun DrillGetFrameLocation(
    thread: jthread?,
    depth: jint,
    method_ptr: CValuesRef<jmethodIDVar>?,
    location_ptr: CValuesRef<jlocationVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetFrameLocation?.invoke(
            jvmtix(),
            thread,
            depth,
            method_ptr?.getPointer(this),
            location_ptr?.getPointer(this)
        )
    }
}

fun DrillNotifyFramePop(thread: jthread?, depth: jint) {
    memScoped {
        jvmtix()?.pointed?.pointed?.NotifyFramePop?.invoke(jvmtix(), thread, depth)
    }
}

fun DrillForceEarlyReturnObject(thread: jthread?, value: jobject?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnObject?.invoke(jvmtix(), thread, value)
    }
}

fun DrillForceEarlyReturnInt(thread: jthread?, value: jint) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnInt?.invoke(jvmtix(), thread, value)
    }
}

fun DrillForceEarlyReturnLong(thread: jthread?, value: jlong) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnLong?.invoke(jvmtix(), thread, value)
    }
}

fun DrillForceEarlyReturnFloat(thread: jthread?, value: jfloat) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnFloat?.invoke(jvmtix(), thread, value)
    }
}

fun DrillForceEarlyReturnDouble(thread: jthread?, value: jdouble) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnDouble?.invoke(jvmtix(), thread, value)
    }
}

fun DrillForceEarlyReturnVoid(thread: jthread?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceEarlyReturnVoid?.invoke(jvmtix(), thread)
    }
}

fun DrillFollowReferences(
    heap_filter: jint,
    klass: jclass?,
    initial_object: jobject?,
    callbacks: CValuesRef<jvmtiHeapCallbacks>?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.FollowReferences?.invoke(
            jvmtix(),
            heap_filter,
            klass,
            initial_object,
            callbacks?.getPointer(this),
            user_data?.getPointer(this)
        )
    }
}

fun DrillIterateThroughHeap(
    heap_filter: jint,
    klass: jclass?,
    callbacks: CValuesRef<jvmtiHeapCallbacks>?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IterateThroughHeap?.invoke(
            jvmtix(),
            heap_filter,
            klass,
            callbacks?.getPointer(this),
            user_data?.getPointer(this)
        )
    }
}

fun DrillGetTag(`object`: jobject?, tag_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetTag?.invoke(jvmtix(), `object`, tag_ptr?.getPointer(this))
    }
}

fun DrillSetTag(`object`: jobject?, tag: jlong) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetTag?.invoke(jvmtix(), `object`, tag)
    }
}

fun DrillGetObjectsWithTags(
    tag_count: jint,
    tags: CValuesRef<jlongVar>?,
    count_ptr: CValuesRef<jintVar>?,
    object_result_ptr: CValuesRef<CPointerVar<jobjectVar>>?,
    tag_result_ptr: CValuesRef<CPointerVar<jlongVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetObjectsWithTags?.invoke(
            jvmtix(),
            tag_count,
            tags?.getPointer(this),
            count_ptr?.getPointer(this),
            object_result_ptr?.getPointer(this),
            tag_result_ptr?.getPointer(this)
        )
    }
}

fun DrillForceGarbageCollection() {
    memScoped {
        jvmtix()?.pointed?.pointed?.ForceGarbageCollection?.invoke(jvmtix())
    }
}

fun DrillIterateOverObjectsReachableFromObject(
    `object`: jobject?,
    object_reference_callback: jvmtiObjectReferenceCallback?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IterateOverObjectsReachableFromObject?.invoke(
            jvmtix(),
            `object`,
            object_reference_callback,
            user_data?.getPointer(this)
        )
    }
}

fun DrillIterateOverReachableObjects(
    heap_root_callback: jvmtiHeapRootCallback?,
    stack_ref_callback: jvmtiStackReferenceCallback?,
    object_ref_callback: jvmtiObjectReferenceCallback?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IterateOverReachableObjects?.invoke(
            jvmtix(),
            heap_root_callback,
            stack_ref_callback,
            object_ref_callback,
            user_data?.getPointer(this)
        )
    }
}

fun DrillIterateOverHeap(
    object_filter: jvmtiHeapObjectFilter,
    heap_object_callback: jvmtiHeapObjectCallback?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IterateOverHeap?.invoke(
            jvmtix(),
            object_filter,
            heap_object_callback,
            user_data?.getPointer(this)
        )
    }
}

fun DrillIterateOverInstancesOfClass(
    klass: jclass?,
    object_filter: jvmtiHeapObjectFilter,
    heap_object_callback: jvmtiHeapObjectCallback?,
    user_data: CValuesRef<*>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IterateOverInstancesOfClass?.invoke(
            jvmtix(),
            klass,
            object_filter,
            heap_object_callback,
            user_data?.getPointer(this)
        )
    }
}

fun DrillGetLocalObject(thread: jthread?, depth: jint, slot: jint, value_ptr: CValuesRef<jobjectVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalObject?.invoke(jvmtix(), thread, depth, slot, value_ptr?.getPointer(this))
    }
}

fun DrillGetLocalInstance(thread: jthread?, depth: jint, value_ptr: CValuesRef<jobjectVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalInstance?.invoke(jvmtix(), thread, depth, value_ptr?.getPointer(this))
    }
}

fun DrillGetLocalInt(thread: jthread?, depth: jint, slot: jint, value_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalInt?.invoke(jvmtix(), thread, depth, slot, value_ptr?.getPointer(this))
    }
}

fun DrillGetLocalLong(thread: jthread?, depth: jint, slot: jint, value_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalLong?.invoke(jvmtix(), thread, depth, slot, value_ptr?.getPointer(this))
    }
}

fun DrillGetLocalFloat(thread: jthread?, depth: jint, slot: jint, value_ptr: CValuesRef<jfloatVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalFloat?.invoke(jvmtix(), thread, depth, slot, value_ptr?.getPointer(this))
    }
}

fun DrillGetLocalDouble(thread: jthread?, depth: jint, slot: jint, value_ptr: CValuesRef<jdoubleVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalDouble?.invoke(jvmtix(), thread, depth, slot, value_ptr?.getPointer(this))
    }
}

fun DrillSetLocalObject(thread: jthread?, depth: jint, slot: jint, value: jobject?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetLocalObject?.invoke(jvmtix(), thread, depth, slot, value)
    }
}

fun DrillSetLocalInt(thread: jthread?, depth: jint, slot: jint, value: jint) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetLocalInt?.invoke(jvmtix(), thread, depth, slot, value)
    }
}

fun DrillSetLocalLong(thread: jthread?, depth: jint, slot: jint, value: jlong) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetLocalLong?.invoke(jvmtix(), thread, depth, slot, value)
    }
}

fun DrillSetLocalFloat(thread: jthread?, depth: jint, slot: jint, value: jfloat) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetLocalFloat?.invoke(jvmtix(), thread, depth, slot, value)
    }
}

fun DrillSetLocalDouble(thread: jthread?, depth: jint, slot: jint, value: jdouble) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetLocalDouble?.invoke(jvmtix(), thread, depth, slot, value)
    }
}

fun DrillSetBreakpoint(method: jmethodID?, location: jlocation) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetBreakpoint?.invoke(jvmtix(), method, location)
    }
}

fun DrillClearBreakpoint(method: jmethodID?, location: jlocation) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ClearBreakpoint?.invoke(jvmtix(), method, location)
    }
}

fun DrillSetFieldAccessWatch(klass: jclass?, field: jfieldID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetFieldAccessWatch?.invoke(jvmtix(), klass, field)
    }
}

fun DrillClearFieldAccessWatch(klass: jclass?, field: jfieldID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ClearFieldAccessWatch?.invoke(jvmtix(), klass, field)
    }
}

fun DrillSetFieldModificationWatch(klass: jclass?, field: jfieldID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetFieldModificationWatch?.invoke(jvmtix(), klass, field)
    }
}

fun DrillClearFieldModificationWatch(klass: jclass?, field: jfieldID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.ClearFieldModificationWatch?.invoke(jvmtix(), klass, field)
    }
}

fun DrillGetLoadedClasses(class_count_ptr: CValuesRef<jintVar>?, classes_ptr: CValuesRef<CPointerVar<jclassVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLoadedClasses?.invoke(
            jvmtix(),
            class_count_ptr?.getPointer(this),
            classes_ptr?.getPointer(this)
        )
    }
}

fun DrillGetClassLoaderClasses(
    initiating_loader: jobject?,
    class_count_ptr: CValuesRef<jintVar>?,
    classes_ptr: CValuesRef<CPointerVar<jclassVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassLoaderClasses?.invoke(
            jvmtix(),
            initiating_loader,
            class_count_ptr?.getPointer(this),
            classes_ptr?.getPointer(this)
        )
    }
}

fun DrillGetClassStatus(klass: jclass?, status_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassStatus?.invoke(jvmtix(), klass, status_ptr?.getPointer(this))
    }
}

fun DrillGetSourceFileName(klass: jclass?, source_name_ptr: CValuesRef<CPointerVar<ByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetSourceFileName?.invoke(jvmtix(), klass, source_name_ptr?.getPointer(this))
    }
}

fun DrillGetClassModifiers(klass: jclass?, modifiers_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassModifiers?.invoke(jvmtix(), klass, modifiers_ptr?.getPointer(this))
    }
}

fun DrillGetClassMethods(
    klass: jclass?,
    method_count_ptr: CValuesRef<jintVar>?,
    methods_ptr: CValuesRef<CPointerVar<jmethodIDVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassMethods?.invoke(
            jvmtix(),
            klass,
            method_count_ptr?.getPointer(this),
            methods_ptr?.getPointer(this)
        )
    }
}

fun DrillGetClassFields(
    klass: jclass?,
    field_count_ptr: CValuesRef<jintVar>?,
    fields_ptr: CValuesRef<CPointerVar<jfieldIDVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassFields?.invoke(
            jvmtix(),
            klass,
            field_count_ptr?.getPointer(this),
            fields_ptr?.getPointer(this)
        )
    }
}

fun DrillGetImplementedInterfaces(
    klass: jclass?,
    interface_count_ptr: CValuesRef<jintVar>?,
    interfaces_ptr: CValuesRef<CPointerVar<jclassVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetImplementedInterfaces?.invoke(
            jvmtix(),
            klass,
            interface_count_ptr?.getPointer(this),
            interfaces_ptr?.getPointer(this)
        )
    }
}

fun DrillGetClassVersionNumbers(
    klass: jclass?,
    minor_version_ptr: CValuesRef<jintVar>?,
    major_version_ptr: CValuesRef<jintVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassVersionNumbers?.invoke(
            jvmtix(),
            klass,
            minor_version_ptr?.getPointer(this),
            major_version_ptr?.getPointer(this)
        )
    }
}

fun DrillGetConstantPool(
    klass: jclass?,
    constant_pool_count_ptr: CValuesRef<jintVar>?,
    constant_pool_byte_count_ptr: CValuesRef<jintVar>?,
    constant_pool_bytes_ptr: CValuesRef<CPointerVar<UByteVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetConstantPool?.invoke(
            jvmtix(),
            klass,
            constant_pool_count_ptr?.getPointer(this),
            constant_pool_byte_count_ptr?.getPointer(this),
            constant_pool_bytes_ptr?.getPointer(this)
        )
    }
}

fun DrillIsInterface(klass: jclass?, is_interface_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsInterface?.invoke(jvmtix(), klass, is_interface_ptr?.getPointer(this))
    }
}

fun DrillIsArrayClass(klass: jclass?, is_array_class_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsArrayClass?.invoke(jvmtix(), klass, is_array_class_ptr?.getPointer(this))
    }
}

fun DrillIsModifiableClass(klass: jclass?, is_modifiable_class_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsModifiableClass?.invoke(
            jvmtix(),
            klass,
            is_modifiable_class_ptr?.getPointer(this)
        )
    }
}

fun DrillGetClassLoader(klass: jclass?, classloader_ptr: CValuesRef<jobjectVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetClassLoader?.invoke(jvmtix(), klass, classloader_ptr?.getPointer(this))
    }
}

fun DrillGetSourceDebugExtension(klass: jclass?, source_debug_extension_ptr: CValuesRef<CPointerVar<ByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetSourceDebugExtension?.invoke(
            jvmtix(),
            klass,
            source_debug_extension_ptr?.getPointer(this)
        )
    }
}

fun DrillRetransformClasses(class_count: jint, classes: CValuesRef<jclassVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RetransformClasses?.invoke(jvmtix(), class_count, classes?.getPointer(this))
    }
}

fun DrillRedefineClasses(class_count: jint, class_definitions: CValuesRef<jvmtiClassDefinition>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RedefineClasses?.invoke(jvmtix(), class_count, class_definitions?.getPointer(this))
    }
}

fun DrillGetObjectSize(`object`: jobject?, size_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetObjectSize?.invoke(jvmtix(), `object`, size_ptr?.getPointer(this))
    }
}

fun DrillGetObjectHashCode(`object`: jobject?, hash_code_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetObjectHashCode?.invoke(jvmtix(), `object`, hash_code_ptr?.getPointer(this))
    }
}

fun DrillGetObjectMonitorUsage(`object`: jobject?, info_ptr: CValuesRef<jvmtiMonitorUsage>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetObjectMonitorUsage?.invoke(jvmtix(), `object`, info_ptr?.getPointer(this))
    }
}

fun DrillGetFieldName(
    klass: jclass?,
    field: jfieldID?,
    name_ptr: CValuesRef<CPointerVar<ByteVar>>?,
    signature_ptr: CValuesRef<CPointerVar<ByteVar>>?,
    generic_ptr: CValuesRef<CPointerVar<ByteVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetFieldName?.invoke(
            jvmtix(),
            klass,
            field,
            name_ptr?.getPointer(this),
            signature_ptr?.getPointer(this),
            generic_ptr?.getPointer(this)
        )
    }
}

fun DrillGetFieldDeclaringClass(klass: jclass?, field: jfieldID?, declaring_class_ptr: CValuesRef<jclassVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetFieldDeclaringClass?.invoke(
            jvmtix(),
            klass,
            field,
            declaring_class_ptr?.getPointer(this)
        )
    }
}

fun DrillGetFieldModifiers(klass: jclass?, field: jfieldID?, modifiers_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetFieldModifiers?.invoke(jvmtix(), klass, field, modifiers_ptr?.getPointer(this))
    }
}

fun DrillIsFieldSynthetic(klass: jclass?, field: jfieldID?, is_synthetic_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsFieldSynthetic?.invoke(jvmtix(), klass, field, is_synthetic_ptr?.getPointer(this))
    }
}

fun DrillGetMethodName(
    method: jmethodID?,
    name_ptr: CValuesRef<CPointerVar<ByteVar>>?,
    signature_ptr: CValuesRef<CPointerVar<ByteVar>>?,
    generic_ptr: CValuesRef<CPointerVar<ByteVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetMethodName?.invoke(
            jvmtix(),
            method,
            name_ptr?.getPointer(this),
            signature_ptr?.getPointer(this),
            generic_ptr?.getPointer(this)
        )
    }
}

fun DrillGetMethodDeclaringClass(method: jmethodID?, declaring_class_ptr: CValuesRef<jclassVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetMethodDeclaringClass?.invoke(
            jvmtix(),
            method,
            declaring_class_ptr?.getPointer(this)
        )
    }
}

fun DrillGetMethodModifiers(method: jmethodID?, modifiers_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetMethodModifiers?.invoke(jvmtix(), method, modifiers_ptr?.getPointer(this))
    }
}

fun DrillGetMaxLocals(method: jmethodID?, max_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetMaxLocals?.invoke(jvmtix(), method, max_ptr?.getPointer(this))
    }
}

fun DrillGetArgumentsSize(method: jmethodID?, size_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetArgumentsSize?.invoke(jvmtix(), method, size_ptr?.getPointer(this))
    }
}

fun DrillGetLineNumberTable(
    method: jmethodID?,
    entry_count_ptr: CValuesRef<jintVar>?,
    table_ptr: CValuesRef<CPointerVar<jvmtiLineNumberEntry>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLineNumberTable?.invoke(
            jvmtix(),
            method,
            entry_count_ptr?.getPointer(this),
            table_ptr?.getPointer(this)
        )
    }
}

fun DrillGetMethodLocation(
    method: jmethodID?,
    start_location_ptr: CValuesRef<jlocationVar>?,
    end_location_ptr: CValuesRef<jlocationVar>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetMethodLocation?.invoke(
            jvmtix(),
            method,
            start_location_ptr?.getPointer(this),
            end_location_ptr?.getPointer(this)
        )
    }
}

fun DrillGetLocalVariableTable(
    method: jmethodID?,
    entry_count_ptr: CValuesRef<jintVar>?,
    table_ptr: CValuesRef<CPointerVar<jvmtiLocalVariableEntry>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetLocalVariableTable?.invoke(
            jvmtix(),
            method,
            entry_count_ptr?.getPointer(this),
            table_ptr?.getPointer(this)
        )
    }
}

fun DrillGetBytecodes(
    method: jmethodID?,
    bytecode_count_ptr: CValuesRef<jintVar>?,
    bytecodes_ptr: CValuesRef<CPointerVar<UByteVar>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetBytecodes?.invoke(
            jvmtix(),
            method,
            bytecode_count_ptr?.getPointer(this),
            bytecodes_ptr?.getPointer(this)
        )
    }
}

fun DrillIsMethodNative(method: jmethodID?, is_native_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsMethodNative?.invoke(jvmtix(), method, is_native_ptr?.getPointer(this))
    }
}

fun DrillIsMethodSynthetic(method: jmethodID?, is_synthetic_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsMethodSynthetic?.invoke(jvmtix(), method, is_synthetic_ptr?.getPointer(this))
    }
}

fun DrillIsMethodObsolete(method: jmethodID?, is_obsolete_ptr: CValuesRef<jbooleanVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.IsMethodObsolete?.invoke(jvmtix(), method, is_obsolete_ptr?.getPointer(this))
    }
}

fun DrillSetNativeMethodPrefix(prefix: String?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetNativeMethodPrefix?.invoke(jvmtix(), prefix?.cstr?.getPointer(this))
    }
}

fun DrillSetNativeMethodPrefixes(prefix_count: jint, prefixes: CValuesRef<CPointerVar<ByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetNativeMethodPrefixes?.invoke(jvmtix(), prefix_count, prefixes?.getPointer(this))
    }
}

fun DrillCreateRawMonitor(name: String?, monitor_ptr: CValuesRef<jrawMonitorIDVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.CreateRawMonitor?.invoke(
            jvmtix(),
            name?.cstr?.getPointer(this),
            monitor_ptr?.getPointer(this)
        )
    }
}

fun DrillDestroyRawMonitor(monitor: jrawMonitorID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.DestroyRawMonitor?.invoke(jvmtix(), monitor)
    }
}

fun DrillRawMonitorEnter(monitor: jrawMonitorID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RawMonitorEnter?.invoke(jvmtix(), monitor)
    }
}

fun DrillRawMonitorExit(monitor: jrawMonitorID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RawMonitorExit?.invoke(jvmtix(), monitor)
    }
}

fun DrillRawMonitorWait(monitor: jrawMonitorID?, millis: jlong) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RawMonitorWait?.invoke(jvmtix(), monitor, millis)
    }
}

fun DrillRawMonitorNotify(monitor: jrawMonitorID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RawMonitorNotify?.invoke(jvmtix(), monitor)
    }
}

fun DrillRawMonitorNotifyAll(monitor: jrawMonitorID?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RawMonitorNotifyAll?.invoke(jvmtix(), monitor)
    }
}

fun DrillSetJNIFunctionTable(function_table: CValuesRef<jniNativeInterface>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetJNIFunctionTable?.invoke(jvmtix(), function_table?.getPointer(this))
    }
}

fun DrillGetJNIFunctionTable(function_table: CValuesRef<CPointerVar<jniNativeInterface>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetJNIFunctionTable?.invoke(jvmtix(), function_table?.getPointer(this))
    }
}

fun DrillSetEventCallbacks(callbacks: CValuesRef<jvmtiEventCallbacks>?, size_of_callbacks: jint) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetEventCallbacks?.invoke(jvmtix(), callbacks?.getPointer(this), size_of_callbacks)
    }
}


fun DrillGenerateEvents(event_type: jvmtiEvent) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GenerateEvents?.invoke(jvmtix(), event_type)
    }
}

fun DrillGetExtensionFunctions(
    extension_count_ptr: CValuesRef<jintVar>?,
    extensions: CValuesRef<CPointerVar<jvmtiExtensionFunctionInfo>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetExtensionFunctions?.invoke(
            jvmtix(),
            extension_count_ptr?.getPointer(this),
            extensions?.getPointer(this)
        )
    }
}

fun DrillGetExtensionEvents(
    extension_count_ptr: CValuesRef<jintVar>?,
    extensions: CValuesRef<CPointerVar<jvmtiExtensionEventInfo>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetExtensionEvents?.invoke(
            jvmtix(),
            extension_count_ptr?.getPointer(this),
            extensions?.getPointer(this)
        )
    }
}

fun DrillSetExtensionEventCallback(extension_event_index: jint, callback: jvmtiExtensionEvent?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetExtensionEventCallback?.invoke(jvmtix(), extension_event_index, callback)
    }
}

fun DrillAddCapabilities(capabilities_ptr: CValuesRef<jvmtiCapabilities>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.AddCapabilities?.invoke(jvmtix(), capabilities_ptr?.getPointer(this))
    }
}

fun DrillRelinquishCapabilities(capabilities_ptr: CValuesRef<jvmtiCapabilities>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.RelinquishCapabilities?.invoke(jvmtix(), capabilities_ptr?.getPointer(this))
    }
}

fun DrillGetCapabilities(capabilities_ptr: CValuesRef<jvmtiCapabilities>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetCapabilities?.invoke(jvmtix(), capabilities_ptr?.getPointer(this))
    }
}

fun DrillGetCurrentThreadCpuTimerInfo(info_ptr: CValuesRef<jvmtiTimerInfo>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetCurrentThreadCpuTimerInfo?.invoke(jvmtix(), info_ptr?.getPointer(this))
    }
}

fun DrillGetCurrentThreadCpuTime(nanos_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetCurrentThreadCpuTime?.invoke(jvmtix(), nanos_ptr?.getPointer(this))
    }
}

fun DrillGetThreadCpuTimerInfo(info_ptr: CValuesRef<jvmtiTimerInfo>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadCpuTimerInfo?.invoke(jvmtix(), info_ptr?.getPointer(this))
    }
}

fun DrillGetThreadCpuTime(thread: jthread?, nanos_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetThreadCpuTime?.invoke(jvmtix(), thread, nanos_ptr?.getPointer(this))
    }
}

fun DrillGetTimerInfo(info_ptr: CValuesRef<jvmtiTimerInfo>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetTimerInfo?.invoke(jvmtix(), info_ptr?.getPointer(this))
    }
}

fun DrillGetTime(nanos_ptr: CValuesRef<jlongVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetTime?.invoke(jvmtix(), nanos_ptr?.getPointer(this))
    }
}

fun DrillGetAvailableProcessors(processor_count_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetAvailableProcessors?.invoke(jvmtix(), processor_count_ptr?.getPointer(this))
    }
}

fun DrillAddToBootstrapClassLoaderSearch(segment: String?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.AddToBootstrapClassLoaderSearch?.invoke(jvmtix(), segment?.cstr?.getPointer(this))
    }
}

fun DrillAddToSystemClassLoaderSearch(segment: String?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.AddToSystemClassLoaderSearch?.invoke(jvmtix(), segment?.cstr?.getPointer(this))
    }
}

fun DrillGetSystemProperties(
    count_ptr: CValuesRef<jintVar>?,
    property_ptr: CValuesRef<CPointerVar<CPointerVar<ByteVar>>>?
) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetSystemProperties?.invoke(
            jvmtix(),
            count_ptr?.getPointer(this),
            property_ptr?.getPointer(this)
        )
    }
}

fun DrillGetSystemProperty(property: String?, value_ptr: CValuesRef<CPointerVar<ByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetSystemProperty?.invoke(
            jvmtix(),
            property?.cstr?.getPointer(this),
            value_ptr?.getPointer(this)
        )
    }
}

fun DrillSetSystemProperty(property: String?, value: String?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetSystemProperty?.invoke(
            jvmtix(),
            property?.cstr?.getPointer(this),
            value?.cstr?.getPointer(this)
        )
    }
}

fun DrillGetPhase(phase_ptr: CValuesRef<jvmtiPhaseVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetPhase?.invoke(jvmtix(), phase_ptr?.getPointer(this))
    }
}

fun DrillDisposeEnvironment() {
    memScoped {
        jvmtix()?.pointed?.pointed?.DisposeEnvironment?.invoke(jvmtix())
    }
}

fun DrillSetEnvironmentLocalStorage(data: CValuesRef<*>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetEnvironmentLocalStorage?.invoke(jvmtix(), data?.getPointer(this))
    }
}

fun DrillGetEnvironmentLocalStorage(data_ptr: CValuesRef<COpaquePointerVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetEnvironmentLocalStorage?.invoke(jvmtix(), data_ptr?.getPointer(this))
    }
}

fun DrillGetVersionNumber(version_ptr: CValuesRef<jintVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetVersionNumber?.invoke(jvmtix(), version_ptr?.getPointer(this))
    }
}

fun DrillGetErrorName(error: jvmtiError, name_ptr: CValuesRef<CPointerVar<ByteVar>>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetErrorName?.invoke(jvmtix(), error, name_ptr?.getPointer(this))
    }
}

fun DrillSetVerboseFlag(flag: jvmtiVerboseFlag, value: jboolean) {
    memScoped {
        jvmtix()?.pointed?.pointed?.SetVerboseFlag?.invoke(jvmtix(), flag, value)
    }
}

fun DrillGetJLocationFormat(format_ptr: CValuesRef<jvmtiJlocationFormatVar>?) {
    memScoped {
        jvmtix()?.pointed?.pointed?.GetJLocationFormat?.invoke(jvmtix(), format_ptr?.getPointer(this))
    }
}

fun jstring.toKString(): String? {
    //fixme deallocate
    val getStringUTFChars = GetStringUTFChars(this, null)
    return getStringUTFChars!!.toKString()
}
