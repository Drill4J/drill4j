package com.epam.drill.plugin.api.processing

import com.epam.drill.common.*
import kotlinx.cinterop.*
import kotlinx.serialization.*

const val initPlugin = "initPlugin"

actual abstract class NativePart<T> : Switchable, Lifecycle {
    var rawConfig: CPointer<ByteVar>? = null

    val config: T get() = confSerializer parse rawConfig!!.toKString()

    fun load(immediately: Boolean) {
        initPlugin()
        if (immediately)
            on()
    }

    fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    abstract var id: String

    actual abstract val confSerializer: KSerializer<T>

    actual fun updateRawConfig(someText: PluginBean) {
        rawConfig = someText.config.cstr.getPointer(Arena())
    }

}