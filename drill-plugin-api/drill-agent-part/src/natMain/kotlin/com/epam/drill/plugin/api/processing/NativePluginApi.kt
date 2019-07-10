package com.epam.drill.plugin.api.processing

import jvmapi.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*

class NativePluginApi(
    val pluginId: String,
    val jvmti: CPointer<jvmtiEnvVar>?,
    val jvm: CPointer<JavaVMVar>?,
    val clb: CPointer<jvmtiEventCallbacks>?,
    val sender: CPointer<CFunction<(pluginId: CPointer<ByteVar>, message: CPointer<ByteVar>) -> Unit>>
)

@SharedImmutable
val natContex = Worker.start(true)

@ThreadLocal
var api: NativePluginApi? = null

@ThreadLocal
var plugin: NativePart<*>? = null


inline fun <reified T> pluginApi(noinline what: NativePluginApi.() -> T) =
    natContex.execute(TransferMode.UNSAFE, { what }) {
        it(api!!)
    }.result
