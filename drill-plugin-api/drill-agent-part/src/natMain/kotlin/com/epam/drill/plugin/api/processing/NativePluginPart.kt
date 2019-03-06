package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual abstract class NativePluginPart<T> {
    var rawConfig: CPointer<ByteVar>? = null

    val config: T?
        get() {
            return if (rawConfig != null)
                Json().parse(confSerializer, rawConfig!!.toKString())
            else null
        }

    open fun load(id: Long) {
        this.id = "test".cstr.getPointer(Arena())
    }

    abstract fun unload(id: Long)

    abstract var id: CPointer<ByteVar>
    actual abstract val confSerializer: KSerializer<T>


    actual fun updateRawConfig(someText: String) {

        try {
            try {
                rawConfig = someText.cstr.getPointer(Arena())
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }

//            updateConfig(restring)

        } catch (ex: Throwable) {
        }
    }

}

actual abstract class AgentPluginPart<T> : DrillPlugin(), SwitchablePlugin {

    actual var enabled: Boolean = false

    actual open fun init(nativePluginPartPath: String) {
    }


    actual override fun load() {
        on()
    }

    actual override fun unload() {
        off()
    }

    actual var np: NativePluginPart<T>? = null

    actual abstract var confSerializer: KSerializer<T>?

    abstract fun updateRawConfig(config: String)
    actual abstract fun updateConfig(config: T)
}