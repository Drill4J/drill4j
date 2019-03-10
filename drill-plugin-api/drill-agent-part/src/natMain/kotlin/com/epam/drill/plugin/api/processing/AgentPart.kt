package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual abstract class AgentPart<T> : DrillPlugin(), Switchable, Lifecycle {

    abstract var enabled: Boolean

    actual open fun init(nativePluginPartPath: String) {
    }

    actual open fun load(onImmediately: Boolean) {
        initPlugin()
        if (onImmediately)
            on()
    }

    actual open fun unload(unloadReason: UnloadReason) {
        off()
        destroyPlugin(unloadReason)
    }

    actual var np: NativePart<T>? = null

    actual abstract var confSerializer: KSerializer<T>

    abstract fun updateRawConfig(config: PluginBean)

    actual fun rawConfig(): String {
        return if (np != null)
            Json().stringify(np!!.confSerializer, np!!.config!!)
        else ""
    }

}