package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JSON

actual abstract class NativePluginPart<T> {

    actual abstract val confSerializer: KSerializer<T>
    actual fun updateRawConfig(someText: String) {
    }

}

actual abstract class AgentPluginPart<T> : DrillPlugin(), SwitchablePlugin {
    actual var enabled: Boolean = false
    actual open fun init(nativePluginPartPath: String) {
        loadNativePart(nativePluginPartPath)
    }


    actual override fun load() {
        on()
    }

    actual override fun unload() {
        off()
    }

    actual abstract override fun on()
    actual abstract override fun off()


    external fun loadNative(ss: Long)
    actual var np: NativePluginPart<T>? = null


    fun updateRawConfig(config: String) {
        if (confSerializer != null) {
            println(config)
            updateConfig(JSON().parse(confSerializer!!, config))
        }
    }

    actual abstract fun updateConfig(config: T)

    actual abstract var confSerializer: kotlinx.serialization.KSerializer<T>?
}