@file:Suppress("FunctionName", "unused")

package com.epam.drillnative.api

import com.epam.drill.plugin.api.processing.NativePart
import jvmapi.*
import kotlinx.cinterop.*

//todo MOVE IT TO API KLIB:)

@SymbolName("currentThread")
internal external fun currentThread(): jthread?

@SymbolName("jvmtiCallbacks")
internal external fun jvmtiCallback(): jvmtiEventCallbacks?

@SymbolName("SetEventCallbacksP")
internal external fun SetEventCallbacksP(
    callbacks: kotlinx.cinterop.CValuesRef<jvmapi.jvmtiEventCallbacks>?,
    size_of_callbacks: jvmapi.jint /* = kotlin.Int */
)

@SymbolName("enableJvmtiEventExceptionCatch")
internal external fun enableJvmtiEventExceptionCatch(th: jthread?)

@SymbolName("jvmtix")
internal external fun jvmtix(): CPointer<jvmtiEnvVar>?


@SymbolName("sendToSocket")
internal external fun sendToSocket(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>)


@SymbolName("enableJvmtiEventException")
internal external fun enableJvmtiEventException(thread: jthread? = null)
@SymbolName("disableJvmtiEventException")
internal external fun disableJvmtiEventException(thread: jthread? = null)

@SymbolName("addPluginToRegistry")
internal external fun addPluginToRegistry(plugin: NativePart<*>)

@SymbolName("getPlugin")
internal external  fun getPlugin(id: CPointer<ByteVar>): NativePart<*>?

