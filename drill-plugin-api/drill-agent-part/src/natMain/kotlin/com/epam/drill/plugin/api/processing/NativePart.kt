package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import com.epam.drill.common.parse
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.serialization.KSerializer
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