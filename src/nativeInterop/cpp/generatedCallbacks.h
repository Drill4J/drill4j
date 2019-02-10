extern void jvmtiEventBreakpointEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location);

extern void jvmtiEventClassFileLoadHookEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jclass class_being_redefined,
     jobject loader,
     const char* name,
     jobject protection_domain,
     jint class_data_len,
     const unsigned char* class_data,
     jint* new_class_data_len,
     unsigned char** new_class_data);

extern void jvmtiEventClassLoadEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jclass klass);

extern void jvmtiEventClassPrepareEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jclass klass);

extern void jvmtiEventCompiledMethodLoadEvent(jvmtiEnv *jvmti_env,
     jmethodID method,
     jint code_size,
     const void* code_addr,
     jint map_length,
     const jvmtiAddrLocationMap* map,
     const void* compile_info);

extern void jvmtiEventCompiledMethodUnloadEvent(jvmtiEnv *jvmti_env,
     jmethodID method,
     const void* code_addr);

extern void jvmtiEventDataDumpRequestEvent(jvmtiEnv *jvmti_env);

extern void jvmtiEventDynamicCodeGeneratedEvent(jvmtiEnv *jvmti_env,
     const char* name,
     const void* address,
     jint length);

extern void jvmtiEventExceptionEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jobject exception,
     jmethodID catch_method,
     jlocation catch_location);

extern void jvmtiEventExceptionCatchEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jobject exception);

extern void jvmtiEventFieldAccessEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jclass field_klass,
     jobject object,
     jfieldID field);

extern void jvmtiEventFieldModificationEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location,
     jclass field_klass,
     jobject object,
     jfieldID field,
     char signature_type,
     jvalue new_value);

extern void jvmtiEventFramePopEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jboolean was_popped_by_exception);

extern void jvmtiEventGarbageCollectionFinishEvent(jvmtiEnv *jvmti_env);

extern void jvmtiEventGarbageCollectionStartEvent(jvmtiEnv *jvmti_env);

extern void jvmtiEventMethodEntryEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method);

extern void jvmtiEventMethodExitEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jboolean was_popped_by_exception,
     jvalue return_value);

extern void jvmtiEventMonitorContendedEnterEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object);

extern void jvmtiEventMonitorContendedEnteredEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object);

extern void jvmtiEventMonitorWaitEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jlong timeout);

extern void jvmtiEventMonitorWaitedEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jboolean timed_out);

extern void jvmtiEventNativeMethodBindEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     void* address,
     void** new_address_ptr);

extern void jvmtiEventObjectFreeEvent(jvmtiEnv *jvmti_env,
     jlong tag);

extern void jvmtiEventResourceExhaustedEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jint flags,
     const void* reserved,
     const char* description);

extern void jvmtiEventSingleStepEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jmethodID method,
     jlocation location);

extern void jvmtiEventThreadEndEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

extern void jvmtiEventThreadStartEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

extern void jvmtiEventVMDeathEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env);

extern void jvmtiEventVMInitEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread);

extern void jvmtiEventVMObjectAllocEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env,
     jthread thread,
     jobject object,
     jclass object_klass,
     jlong size);

extern void jvmtiEventVMStartEvent(jvmtiEnv *jvmti_env,
     JNIEnv* jni_env);