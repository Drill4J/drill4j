


jvmtiError Allocate(jlong size, unsigned char** mem_ptr) {
    return (*(gdata->jvmti))->Allocate(gdata->jvmti, size, mem_ptr);
}


  jvmtiCapabilities GetPotentialCapabilities() {
    jvmtiCapabilities capabilities;
   (void)memset(&capabilities,0, sizeof(capabilities));
   (*(gdata->jvmti))->GetPotentialCapabilities(gdata->jvmti, &capabilities);
    return capabilities;
  }

  jvmtiError GetClassSignature(jclass klass,
            char** signature_ptr,
            char** generic_ptr) {
    return (*(gdata->jvmti))->GetClassSignature(gdata->jvmti, klass, signature_ptr, generic_ptr);
  }

  jvmtiError Deallocate(unsigned char* mem) {

    return (*(gdata->jvmti))->Deallocate(gdata->jvmti, mem);
  }

  jvmtiError GetThreadState(jthread thread,
            jint* thread_state_ptr) {
    return (*(gdata->jvmti))->GetThreadState(gdata->jvmti, thread, thread_state_ptr);
  }

  jvmtiError GetCurrentThread(jthread* thread_ptr) {
    return (*(gdata->jvmti))->GetCurrentThread(gdata->jvmti, thread_ptr);
  }

  jvmtiError GetAllThreads(jint* threads_count_ptr,
            jthread** threads_ptr) {
    return (*(gdata->jvmti))->GetAllThreads(gdata->jvmti, threads_count_ptr, threads_ptr);
  }

  jvmtiError SuspendThread(jthread thread) {
    return (*(gdata->jvmti))->SuspendThread(gdata->jvmti, thread);
  }

  jvmtiError SuspendThreadList(jint request_count,
            const jthread* request_list,
            jvmtiError* results) {
    return (*(gdata->jvmti))->SuspendThreadList(gdata->jvmti, request_count, request_list, results);
  }

  jvmtiError ResumeThread(jthread thread) {
    return (*(gdata->jvmti))->ResumeThread(gdata->jvmti, thread);
  }

  jvmtiError ResumeThreadList(jint request_count,
            const jthread* request_list,
            jvmtiError* results) {
    return (*(gdata->jvmti))->ResumeThreadList(gdata->jvmti, request_count, request_list, results);
  }

  jvmtiError StopThread(jthread thread,
            jobject exception) {
    return (*(gdata->jvmti))->StopThread(gdata->jvmti, thread, exception);
  }

  jvmtiError InterruptThread(jthread thread) {
    return (*(gdata->jvmti))->InterruptThread(gdata->jvmti, thread);
  }

  jvmtiError GetThreadInfo(jthread thread,
            jvmtiThreadInfo* info_ptr) {
    return (*(gdata->jvmti))->GetThreadInfo(gdata->jvmti, thread, info_ptr);
  }

  jvmtiError GetOwnedMonitorInfo(jthread thread,
            jint* owned_monitor_count_ptr,
            jobject** owned_monitors_ptr) {
    return (*(gdata->jvmti))->GetOwnedMonitorInfo(gdata->jvmti, thread, owned_monitor_count_ptr, owned_monitors_ptr);
  }

  jvmtiError GetOwnedMonitorStackDepthInfo(jthread thread,
            jint* monitor_info_count_ptr,
            jvmtiMonitorStackDepthInfo** monitor_info_ptr) {
    return (*(gdata->jvmti))->GetOwnedMonitorStackDepthInfo(gdata->jvmti, thread, monitor_info_count_ptr, monitor_info_ptr);
  }

  jvmtiError GetCurrentContendedMonitor(jthread thread,
            jobject* monitor_ptr) {
    return (*(gdata->jvmti))->GetCurrentContendedMonitor(gdata->jvmti, thread, monitor_ptr);
  }

  jvmtiError RunAgentThread(jthread thread,
            jvmtiStartFunction proc,
            const void* arg,
            jint priority) {
    return (*(gdata->jvmti))->RunAgentThread(gdata->jvmti, thread, proc, arg, priority);
  }

  jvmtiError SetThreadLocalStorage(jthread thread,
            const void* data) {
    return (*(gdata->jvmti))->SetThreadLocalStorage(gdata->jvmti, thread, data);
  }

  jvmtiError GetThreadLocalStorage(jthread thread,
            void** data_ptr) {
    return (*(gdata->jvmti))->GetThreadLocalStorage(gdata->jvmti, thread, data_ptr);
  }

  jvmtiError GetTopThreadGroups(jint* group_count_ptr,
            jthreadGroup** groups_ptr) {
    return (*(gdata->jvmti))->GetTopThreadGroups(gdata->jvmti, group_count_ptr, groups_ptr);
  }

  jvmtiError GetThreadGroupInfo(jthreadGroup group,
            jvmtiThreadGroupInfo* info_ptr) {
    return (*(gdata->jvmti))->GetThreadGroupInfo(gdata->jvmti, group, info_ptr);
  }

  jvmtiError GetThreadGroupChildren(jthreadGroup group,
            jint* thread_count_ptr,
            jthread** threads_ptr,
            jint* group_count_ptr,
            jthreadGroup** groups_ptr) {
    return (*(gdata->jvmti))->GetThreadGroupChildren(gdata->jvmti, group, thread_count_ptr, threads_ptr, group_count_ptr, groups_ptr);
  }

  jvmtiError GetStackTrace(jthread thread,
            jint start_depth,
            jint max_frame_count,
            jvmtiFrameInfo* frame_buffer,
            jint* count_ptr) {
    return (*(gdata->jvmti))->GetStackTrace(gdata->jvmti, thread, start_depth, max_frame_count, frame_buffer, count_ptr);
  }

  jvmtiError GetAllStackTraces(jint max_frame_count,
            jvmtiStackInfo** stack_info_ptr,
            jint* thread_count_ptr) {
    return (*(gdata->jvmti))->GetAllStackTraces(gdata->jvmti, max_frame_count, stack_info_ptr, thread_count_ptr);
  }

  jvmtiError GetThreadListStackTraces(jint thread_count,
            const jthread* thread_list,
            jint max_frame_count,
            jvmtiStackInfo** stack_info_ptr) {
    return (*(gdata->jvmti))->GetThreadListStackTraces(gdata->jvmti, thread_count, thread_list, max_frame_count, stack_info_ptr);
  }

  jvmtiError GetFrameCount(jthread thread,
            jint* count_ptr) {
    return (*(gdata->jvmti))->GetFrameCount(gdata->jvmti, thread, count_ptr);
  }

  jvmtiError PopFrame(jthread thread) {
    return (*(gdata->jvmti))->PopFrame(gdata->jvmti, thread);
  }

  jvmtiError GetFrameLocation(jthread thread,
            jint depth,
            jmethodID* method_ptr,
            jlocation* location_ptr) {
    return (*(gdata->jvmti))->GetFrameLocation(gdata->jvmti, thread, depth, method_ptr, location_ptr);
  }

  jvmtiError NotifyFramePop(jthread thread,
            jint depth) {
    return (*(gdata->jvmti))->NotifyFramePop(gdata->jvmti, thread, depth);
  }











  jvmtiError ForceEarlyReturnObject(jthread thread,
            jobject value) {
    return (*(gdata->jvmti))->ForceEarlyReturnObject(gdata->jvmti, thread, value);
  }

  jvmtiError ForceEarlyReturnInt(jthread thread,
            jint value) {
    return (*(gdata->jvmti))->ForceEarlyReturnInt(gdata->jvmti, thread, value);
  }

  jvmtiError ForceEarlyReturnLong(jthread thread,
            jlong value) {
    return (*(gdata->jvmti))->ForceEarlyReturnLong(gdata->jvmti, thread, value);
  }

  jvmtiError ForceEarlyReturnFloat(jthread thread,
            jfloat value) {
    return (*(gdata->jvmti))->ForceEarlyReturnFloat(gdata->jvmti, thread, value);
  }

 jvmtiError ForceEarlyReturnDouble(jthread thread,
            jdouble value) {
    return (*(gdata->jvmti))->ForceEarlyReturnDouble(gdata->jvmti, thread, value);
  }

  jvmtiError ForceEarlyReturnVoid(jthread thread) {
    return (*(gdata->jvmti))->ForceEarlyReturnVoid(gdata->jvmti, thread);
  }

  jvmtiError FollowReferences(jint heap_filter,
            jclass klass,
            jobject initial_object,
            const jvmtiHeapCallbacks* callbacks,
            const void* user_data) {
    return (*(gdata->jvmti))->FollowReferences(gdata->jvmti, heap_filter, klass, initial_object, callbacks, user_data);
  }

  jvmtiError IterateThroughHeap(jint heap_filter,
            jclass klass,
            const jvmtiHeapCallbacks* callbacks,
            const void* user_data) {
    return (*(gdata->jvmti))->IterateThroughHeap(gdata->jvmti, heap_filter, klass, callbacks, user_data);
  }

  jvmtiError GetTag(jobject object,
            jlong* tag_ptr) {
    return (*(gdata->jvmti))->GetTag(gdata->jvmti, object, tag_ptr);
  }

  jvmtiError SetTag(jobject object,
            jlong tag) {
    return (*(gdata->jvmti))->SetTag(gdata->jvmti, object, tag);
  }

  jvmtiError GetObjectsWithTags(jint tag_count,
            const jlong* tags,
            jint* count_ptr,
            jobject** object_result_ptr,
            jlong** tag_result_ptr) {
    return (*(gdata->jvmti))->GetObjectsWithTags(gdata->jvmti, tag_count, tags, count_ptr, object_result_ptr, tag_result_ptr);
  }

  jvmtiError ForceGarbageCollection() {
    return (*(gdata->jvmti))->ForceGarbageCollection(jvmti());
  }

  jvmtiError IterateOverObjectsReachableFromObject(jobject object,
            jvmtiObjectReferenceCallback object_reference_callback,
            const void* user_data) {
    return (*(gdata->jvmti))->IterateOverObjectsReachableFromObject(gdata->jvmti, object, object_reference_callback, user_data);
  }

  jvmtiError IterateOverReachableObjects(jvmtiHeapRootCallback heap_root_callback,
            jvmtiStackReferenceCallback stack_ref_callback,
            jvmtiObjectReferenceCallback object_ref_callback,
            const void* user_data) {
    return (*(gdata->jvmti))->IterateOverReachableObjects(gdata->jvmti, heap_root_callback, stack_ref_callback, object_ref_callback, user_data);
  }

  jvmtiError IterateOverHeap(jvmtiHeapObjectFilter object_filter,
            jvmtiHeapObjectCallback heap_object_callback,
            const void* user_data) {
    return (*(gdata->jvmti))->IterateOverHeap(gdata->jvmti, object_filter, heap_object_callback, user_data);
  }

  jvmtiError IterateOverInstancesOfClass(jclass klass,
            jvmtiHeapObjectFilter object_filter,
            jvmtiHeapObjectCallback heap_object_callback,
            const void* user_data) {
    return (*(gdata->jvmti))->IterateOverInstancesOfClass(gdata->jvmti, klass, object_filter, heap_object_callback, user_data);
  }

  jvmtiError GetLocalObject(jthread thread,
            jint depth,
            jint slot,
            jobject* value_ptr) {
    return (*(gdata->jvmti))->GetLocalObject(gdata->jvmti, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalInstance(jthread thread,
            jint depth,
            jobject* value_ptr) {
    return (*(gdata->jvmti))->GetLocalInstance(gdata->jvmti, thread, depth, value_ptr);
  }

  jvmtiError GetLocalInt(jthread thread,
            jint depth,
            jint slot,
            jint* value_ptr) {
    return (*(gdata->jvmti))->GetLocalInt(gdata->jvmti, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalLong(jthread thread,
            jint depth,
            jint slot,
            jlong* value_ptr) {
    return (*(gdata->jvmti))->GetLocalLong(gdata->jvmti, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalFloat(jthread thread,
            jint depth,
            jint slot,
            jfloat* value_ptr) {
    return (*(gdata->jvmti))->GetLocalFloat(gdata->jvmti, thread, depth, slot, value_ptr);
  }

  jvmtiError GetLocalDouble(jthread thread,
            jint depth,
            jint slot,
            jdouble* value_ptr) {
    return (*(gdata->jvmti))->GetLocalDouble(gdata->jvmti, thread, depth, slot, value_ptr);
  }

  jvmtiError SetLocalObject(jthread thread,
            jint depth,
            jint slot,
            jobject value) {
    return (*(gdata->jvmti))->SetLocalObject(gdata->jvmti, thread, depth, slot, value);
  }

  jvmtiError SetLocalInt(jthread thread,
            jint depth,
            jint slot,
            jint value) {
    return (*(gdata->jvmti))->SetLocalInt(gdata->jvmti, thread, depth, slot, value);
  }

  jvmtiError SetLocalLong(jthread thread,
            jint depth,
            jint slot,
            jlong value) {
    return (*(gdata->jvmti))->SetLocalLong(gdata->jvmti, thread, depth, slot, value);
  }

  jvmtiError SetLocalFloat(jthread thread,
            jint depth,
            jint slot,
            jfloat value) {
    return (*(gdata->jvmti))->SetLocalFloat(gdata->jvmti, thread, depth, slot, value);
  }

  jvmtiError SetLocalDouble(jthread thread,
            jint depth,
            jint slot,
            jdouble value) {
    return (*(gdata->jvmti))->SetLocalDouble(gdata->jvmti, thread, depth, slot, value);
  }

  jvmtiError SetBreakpoint(jmethodID method,
            jlocation location) {
    return (*(gdata->jvmti))->SetBreakpoint(gdata->jvmti, method, location);
  }

  jvmtiError ClearBreakpoint(jmethodID method,
            jlocation location) {
    return (*(gdata->jvmti))->ClearBreakpoint(gdata->jvmti, method, location);
  }

  jvmtiError SetFieldAccessWatch(jclass klass,
            jfieldID field) {
    return (*(gdata->jvmti))->SetFieldAccessWatch(gdata->jvmti, klass, field);
  }

  jvmtiError ClearFieldAccessWatch(jclass klass,
            jfieldID field) {
    return (*(gdata->jvmti))->ClearFieldAccessWatch(gdata->jvmti, klass, field);
  }

  jvmtiError SetFieldModificationWatch(jclass klass,
            jfieldID field) {
    return (*(gdata->jvmti))->SetFieldModificationWatch(gdata->jvmti, klass, field);
  }

  jvmtiError ClearFieldModificationWatch(jclass klass,
            jfieldID field) {
    return (*(gdata->jvmti))->ClearFieldModificationWatch(gdata->jvmti, klass, field);
  }

  jvmtiError GetLoadedClasses(jint* class_count_ptr,
            jclass** classes_ptr) {
    return (*(gdata->jvmti))->GetLoadedClasses(gdata->jvmti, class_count_ptr, classes_ptr);
  }

  jvmtiError GetClassLoaderClasses(jobject initiating_loader,
            jint* class_count_ptr,
            jclass** classes_ptr) {
    return (*(gdata->jvmti))->GetClassLoaderClasses(gdata->jvmti, initiating_loader, class_count_ptr, classes_ptr);
  }



  jvmtiError GetClassStatus(jclass klass,
            jint* status_ptr) {
    return (*(gdata->jvmti))->GetClassStatus(gdata->jvmti, klass, status_ptr);
  }



  jvmtiError GetSourceFileName(jclass klass,
             char** source_name_ptr) {
     return (*(gdata->jvmti))->GetSourceFileName(gdata->jvmti, klass, source_name_ptr);
   }

   jvmtiError GetClassModifiers(jclass klass,
             jint* modifiers_ptr) {
     return (*(gdata->jvmti))->GetClassModifiers(gdata->jvmti, klass, modifiers_ptr);
   }

   jvmtiError GetClassMethods(jclass klass,
             jint* method_count_ptr,
             jmethodID** methods_ptr) {
     return (*(gdata->jvmti))->GetClassMethods(gdata->jvmti, klass, method_count_ptr, methods_ptr);
   }

   jvmtiError GetClassFields(jclass klass,
             jint* field_count_ptr,
             jfieldID** fields_ptr) {
     return (*(gdata->jvmti))->GetClassFields(gdata->jvmti, klass, field_count_ptr, fields_ptr);
   }

   jvmtiError GetImplementedInterfaces(jclass klass,
             jint* interface_count_ptr,
             jclass** interfaces_ptr) {
     return (*(gdata->jvmti))->GetImplementedInterfaces(gdata->jvmti, klass, interface_count_ptr, interfaces_ptr);
   }

   jvmtiError GetClassVersionNumbers(jclass klass,
             jint* minor_version_ptr,
             jint* major_version_ptr) {
     return (*(gdata->jvmti))->GetClassVersionNumbers(gdata->jvmti, klass, minor_version_ptr, major_version_ptr);
   }

   jvmtiError GetConstantPool(jclass klass,
             jint* constant_pool_count_ptr,
             jint* constant_pool_byte_count_ptr,
             unsigned char** constant_pool_bytes_ptr) {
     return (*(gdata->jvmti))->GetConstantPool(gdata->jvmti, klass, constant_pool_count_ptr, constant_pool_byte_count_ptr, constant_pool_bytes_ptr);
   }

   jvmtiError IsInterface(jclass klass,
             jboolean* is_interface_ptr) {
     return (*(gdata->jvmti))->IsInterface(gdata->jvmti, klass, is_interface_ptr);
   }

   jvmtiError IsArrayClass(jclass klass,
             jboolean* is_array_class_ptr) {
     return (*(gdata->jvmti))->IsArrayClass(gdata->jvmti, klass, is_array_class_ptr);
   }

   jvmtiError IsModifiableClass(jclass klass,
             jboolean* is_modifiable_class_ptr) {
     return (*(gdata->jvmti))->IsModifiableClass(gdata->jvmti, klass, is_modifiable_class_ptr);
   }

   jvmtiError GetClassLoader(jclass klass,
             jobject* classloader_ptr) {
     return (*(gdata->jvmti))->GetClassLoader(gdata->jvmti, klass, classloader_ptr);
   }

   jvmtiError GetSourceDebugExtension(jclass klass,
             char** source_debug_extension_ptr) {
     return (*(gdata->jvmti))->GetSourceDebugExtension(gdata->jvmti, klass, source_debug_extension_ptr);
   }

   jvmtiError RetransformClasses(jint class_count,
             const jclass* classes) {
     return (*(gdata->jvmti))->RetransformClasses(gdata->jvmti, class_count, classes);
   }

   jvmtiError RedefineClasses(jint class_count,
             const jvmtiClassDefinition* class_definitions) {
     return (*(gdata->jvmti))->RedefineClasses(gdata->jvmti, class_count, class_definitions);
   }

   jvmtiError GetObjectSize(jobject object,
             jlong* size_ptr) {
     return (*(gdata->jvmti))->GetObjectSize(gdata->jvmti, object, size_ptr);
   }

   jvmtiError GetObjectHashCode(jobject object,
             jint* hash_code_ptr) {
     return (*(gdata->jvmti))->GetObjectHashCode(gdata->jvmti, object, hash_code_ptr);
   }

   jvmtiError GetObjectMonitorUsage(jobject object,
             jvmtiMonitorUsage* info_ptr) {
     return (*(gdata->jvmti))->GetObjectMonitorUsage(gdata->jvmti, object, info_ptr);
   }

   jvmtiError GetFieldName(jclass klass,
             jfieldID field,
             char** name_ptr,
             char** signature_ptr,
             char** generic_ptr) {
     return (*(gdata->jvmti))->GetFieldName(gdata->jvmti, klass, field, name_ptr, signature_ptr, generic_ptr);
   }

   jvmtiError GetFieldDeclaringClass(jclass klass,
             jfieldID field,
             jclass* declaring_class_ptr) {
     return (*(gdata->jvmti))->GetFieldDeclaringClass(gdata->jvmti, klass, field, declaring_class_ptr);
   }

   jvmtiError GetFieldModifiers(jclass klass,
             jfieldID field,
             jint* modifiers_ptr) {
     return (*(gdata->jvmti))->GetFieldModifiers(gdata->jvmti, klass, field, modifiers_ptr);
   }

   jvmtiError IsFieldSynthetic(jclass klass,
             jfieldID field,
             jboolean* is_synthetic_ptr) {
     return (*(gdata->jvmti))->IsFieldSynthetic(gdata->jvmti, klass, field, is_synthetic_ptr);
   }

   jvmtiError GetMethodName(jmethodID method,
             char** name_ptr,
             char** signature_ptr,
             char** generic_ptr) {
     return (*(gdata->jvmti))->GetMethodName(gdata->jvmti, method, name_ptr, signature_ptr, generic_ptr);
   }

   jvmtiError GetMethodDeclaringClass(jmethodID method,
             jclass* declaring_class_ptr) {
     return (*(gdata->jvmti))->GetMethodDeclaringClass(gdata->jvmti, method, declaring_class_ptr);
   }

   jvmtiError GetMethodModifiers(jmethodID method,
             jint* modifiers_ptr) {
     return (*(gdata->jvmti))->GetMethodModifiers(gdata->jvmti, method, modifiers_ptr);
   }

  jvmtiError GetMaxLocals(jmethodID method,
            jint* max_ptr) {
    return (*(gdata->jvmti))->GetMaxLocals(gdata->jvmti, method, max_ptr);
  }

  jvmtiError GetArgumentsSize(jmethodID method,
            jint* size_ptr) {
    return (*(gdata->jvmti))->GetArgumentsSize(gdata->jvmti, method, size_ptr);
  }

  jvmtiError GetLineNumberTable(jmethodID method,
            jint* entry_count_ptr,
            jvmtiLineNumberEntry** table_ptr) {
    return (*(gdata->jvmti))->GetLineNumberTable(gdata->jvmti, method, entry_count_ptr, table_ptr);
  }

  jvmtiError GetMethodLocation(jmethodID method,
            jlocation* start_location_ptr,
            jlocation* end_location_ptr) {
    return (*(gdata->jvmti))->GetMethodLocation(gdata->jvmti, method, start_location_ptr, end_location_ptr);
  }

  jvmtiError GetLocalVariableTable(jmethodID method,
            jint* entry_count_ptr,
            jvmtiLocalVariableEntry** table_ptr) {
    return (*(gdata->jvmti))->GetLocalVariableTable(gdata->jvmti, method, entry_count_ptr, table_ptr);
  }

  jvmtiError GetBytecodes(jmethodID method,
            jint* bytecode_count_ptr,
            unsigned char** bytecodes_ptr) {
    return (*(gdata->jvmti))->GetBytecodes(gdata->jvmti, method, bytecode_count_ptr, bytecodes_ptr);
  }

  jvmtiError IsMethodNative(jmethodID method,
            jboolean* is_native_ptr) {
    return (*(gdata->jvmti))->IsMethodNative(gdata->jvmti, method, is_native_ptr);
  }

  jvmtiError IsMethodSynthetic(jmethodID method,
            jboolean* is_synthetic_ptr) {
    return (*(gdata->jvmti))->IsMethodSynthetic(gdata->jvmti, method, is_synthetic_ptr);
  }

  jvmtiError IsMethodObsolete(jmethodID method,
            jboolean* is_obsolete_ptr) {
    return (*(gdata->jvmti))->IsMethodObsolete(gdata->jvmti, method, is_obsolete_ptr);
  }

  jvmtiError SetNativeMethodPrefix(const char* prefix) {
    return (*(gdata->jvmti))->SetNativeMethodPrefix(gdata->jvmti, prefix);
  }

  jvmtiError SetNativeMethodPrefixes(jint prefix_count,
            char** prefixes) {
    return (*(gdata->jvmti))->SetNativeMethodPrefixes(gdata->jvmti, prefix_count, prefixes);
  }

  jvmtiError CreateRawMonitor(const char* name,
            jrawMonitorID* monitor_ptr) {
    return (*(gdata->jvmti))->CreateRawMonitor(gdata->jvmti, name, monitor_ptr);
  }

  jvmtiError DestroyRawMonitor(jrawMonitorID monitor) {
    return (*(gdata->jvmti))->DestroyRawMonitor(gdata->jvmti, monitor);
  }

  jvmtiError RawMonitorEnter(jrawMonitorID monitor) {
    return (*(gdata->jvmti))->RawMonitorEnter(gdata->jvmti, monitor);
  }

  jvmtiError RawMonitorExit(jrawMonitorID monitor) {
    return (*(gdata->jvmti))->RawMonitorExit(gdata->jvmti, monitor);
  }

  jvmtiError RawMonitorWait(jrawMonitorID monitor,
            jlong millis) {
    return (*(gdata->jvmti))->RawMonitorWait(gdata->jvmti, monitor, millis);
  }

  jvmtiError RawMonitorNotify(jrawMonitorID monitor) {
    return (*(gdata->jvmti))->RawMonitorNotify(gdata->jvmti, monitor);
  }

  jvmtiError RawMonitorNotifyAll(jrawMonitorID monitor) {
    return (*(gdata->jvmti))->RawMonitorNotifyAll(gdata->jvmti, monitor);
  }

  jvmtiError SetJNIFunctionTable(const jniNativeInterface* function_table) {
    return (*(gdata->jvmti))->SetJNIFunctionTable(gdata->jvmti, function_table);
  }

  jvmtiError GetJNIFunctionTable(jniNativeInterface** function_table) {
    return (*(gdata->jvmti))->GetJNIFunctionTable(gdata->jvmti, function_table);
  }

  jvmtiError SetEventCallbacks(const jvmtiEventCallbacks* callbacks,
            jint size_of_callbacks) {
    return (*(gdata->jvmti))->SetEventCallbacks(gdata->jvmti, callbacks, size_of_callbacks);
  }

  jvmtiError SetEventNotificationMode(jvmtiEventMode mode,
            jvmtiEvent event_type,
            jthread event_thread,
             ...) {
    return (*(gdata->jvmti))->SetEventNotificationMode(gdata->jvmti, mode, event_type, event_thread);
  }

  jvmtiError GenerateEvents(jvmtiEvent event_type) {
    return (*(gdata->jvmti))->GenerateEvents(gdata->jvmti, event_type);
  }

  jvmtiError GetExtensionFunctions(jint* extension_count_ptr,
            jvmtiExtensionFunctionInfo** extensions) {
    return (*(gdata->jvmti))->GetExtensionFunctions(gdata->jvmti, extension_count_ptr, extensions);
  }

  jvmtiError GetExtensionEvents(jint* extension_count_ptr,
            jvmtiExtensionEventInfo** extensions) {
    return (*(gdata->jvmti))->GetExtensionEvents(gdata->jvmti, extension_count_ptr, extensions);
  }

  jvmtiError SetExtensionEventCallback(jint extension_event_index,
            jvmtiExtensionEvent callback) {
    return (*(gdata->jvmti))->SetExtensionEventCallback(gdata->jvmti, extension_event_index, callback);
  }

  jvmtiError AddCapabilities(const jvmtiCapabilities* capabilities_ptr) {
    return (*(gdata->jvmti))->AddCapabilities(gdata->jvmti, capabilities_ptr);
  }

  jvmtiError RelinquishCapabilities(const jvmtiCapabilities* capabilities_ptr) {
    return (*(gdata->jvmti))->RelinquishCapabilities(gdata->jvmti, capabilities_ptr);
  }

  jvmtiError GetCapabilities(jvmtiCapabilities* capabilities_ptr) {
    return (*(gdata->jvmti))->GetCapabilities(gdata->jvmti, capabilities_ptr);
  }

  jvmtiError GetCurrentThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
    return (*(gdata->jvmti))->GetCurrentThreadCpuTimerInfo(gdata->jvmti, info_ptr);
  }

  jvmtiError GetCurrentThreadCpuTime(jlong* nanos_ptr) {
    return (*(gdata->jvmti))->GetCurrentThreadCpuTime(gdata->jvmti, nanos_ptr);
  }

  jvmtiError GetThreadCpuTimerInfo(jvmtiTimerInfo* info_ptr) {
    return (*(gdata->jvmti))->GetThreadCpuTimerInfo(gdata->jvmti, info_ptr);
  }

  jvmtiError GetThreadCpuTime(jthread thread,
            jlong* nanos_ptr) {
    return (*(gdata->jvmti))->GetThreadCpuTime(gdata->jvmti, thread, nanos_ptr);
  }

  jvmtiError GetTimerInfo(jvmtiTimerInfo* info_ptr) {
    return (*(gdata->jvmti))->GetTimerInfo(gdata->jvmti, info_ptr);
  }

  jvmtiError GetTime(jlong* nanos_ptr) {
    return (*(gdata->jvmti))->GetTime(gdata->jvmti, nanos_ptr);
  }

  jvmtiError GetAvailableProcessors(jint* processor_count_ptr) {
    return (*(gdata->jvmti))->GetAvailableProcessors(gdata->jvmti, processor_count_ptr);
  }

  jvmtiError AddToBootstrapClassLoaderSearch(const char* segment) {
    return (*(gdata->jvmti))->AddToBootstrapClassLoaderSearch(gdata->jvmti, segment);
  }

  jvmtiError AddToSystemClassLoaderSearch(const char* segment) {
    return (*(gdata->jvmti))->AddToSystemClassLoaderSearch(gdata->jvmti, segment);
  }

  jvmtiError GetSystemProperties(jint* count_ptr,
            char*** property_ptr) {
    return (*(gdata->jvmti))->GetSystemProperties(gdata->jvmti, count_ptr, property_ptr);
  }

  jvmtiError GetSystemProperty(const char* property,
            char** value_ptr) {
    return (*(gdata->jvmti))->GetSystemProperty(gdata->jvmti, property, value_ptr);
  }

  jvmtiError SetSystemProperty(const char* property,
            const char* value) {
    return (*(gdata->jvmti))->SetSystemProperty(gdata->jvmti, property, value);
  }

  jvmtiError GetPhase(jvmtiPhase* phase_ptr) {
    return (*(gdata->jvmti))->GetPhase(gdata->jvmti, phase_ptr);
  }

  jvmtiError DisposeEnvironment() {
    return (*(gdata->jvmti))->DisposeEnvironment(jvmti());
  }

  jvmtiError SetEnvironmentLocalStorage(const void* data) {
    return (*(gdata->jvmti))->SetEnvironmentLocalStorage(gdata->jvmti, data);
  }

  jvmtiError GetEnvironmentLocalStorage(void** data_ptr) {
    return (*(gdata->jvmti))->GetEnvironmentLocalStorage(gdata->jvmti, data_ptr);
  }

  jvmtiError GetVersionNumber(jint* version_ptr) {
    return (*(gdata->jvmti))->GetVersionNumber(gdata->jvmti, version_ptr);
  }

  jvmtiError GetErrorName(jvmtiError error,
            char** name_ptr) {
    return (*(gdata->jvmti))->GetErrorName(gdata->jvmti, error, name_ptr);
  }

  jvmtiError SetVerboseFlag(jvmtiVerboseFlag flag,
            jboolean value) {
    return (*(gdata->jvmti))->SetVerboseFlag(gdata->jvmti, flag, value);
  }

  jvmtiError GetJLocationFormat(jvmtiJlocationFormat* format_ptr) {
    return (*(gdata->jvmti))->GetJLocationFormat(gdata->jvmti, format_ptr);
  }



