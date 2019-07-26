package com.epam.drill.plugin.api.processing

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import kotlinx.serialization.*

actual abstract class AgentPart<T, A> : DrillPlugin<A>, Switchable, Lifecycle {

    abstract suspend fun isEnabled(): Boolean

    abstract suspend fun setEnabled(value: Boolean)

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

    actual abstract val confSerializer: KSerializer<T>

    abstract fun updateRawConfig(config: PluginConfig)

    actual fun rawConfig(): String {
        return if (np != null)
            np!!.confSerializer stringify np!!.config!!
        else ""
    }

    actual var enabled: Boolean = false

}