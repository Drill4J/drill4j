package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.json.JSON


actual abstract class AgentPart<T> : DrillPlugin(), Switchable, Lifecycle {
    private var rawConfig: String? = null
    val config: T get() = JSON().parse(confSerializer, rawConfig!!)

    actual open fun init(nativePluginPartPath: String) {
        try {
            System.load(nativePluginPartPath)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    actual fun load(onImmediately: Boolean) {
        initPlugin()
        if (onImmediately)
            on()
    }

    actual fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    actual abstract override fun on()

    actual abstract override fun off()

    external fun loadNative(ss: Long)
    actual var np: NativePart<T>? = null

    open fun updateRawConfig(configs: String) {
        rawConfig = configs
    }

    actual abstract var confSerializer: kotlinx.serialization.KSerializer<T>
    actual abstract override fun initPlugin()

    actual abstract override fun destroyPlugin(unloadReason: UnloadReason)
    actual fun rawConfig(): String {
        return JSON.stringify(confSerializer, config!!)
    }
}