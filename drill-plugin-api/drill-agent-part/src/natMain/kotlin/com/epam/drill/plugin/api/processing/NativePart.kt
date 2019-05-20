package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual abstract class NativePart<T> : Switchable, Lifecycle {
    var rawConfig: CPointer<ByteVar>? = null

    val config: T get() = Json().parse(confSerializer, rawConfig!!.toKString())

    fun load(immediately: Boolean) {
        initPlugin()
        if (immediately)
            on()
    }

    fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    abstract var id: CPointer<ByteVar>

    actual abstract val confSerializer: KSerializer<T>

    actual fun updateRawConfig(someText: PluginBean) {
        rawConfig = someText.config.cstr.getPointer(Arena())
    }

}