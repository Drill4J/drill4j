package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import com.epam.drill.common.stringify
import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer

actual abstract class AgentPart<T, A> : DrillPlugin(), Switchable, Lifecycle {

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

    actual abstract var confSerializer: KSerializer<T>

    abstract fun updateRawConfig(config: PluginBean)

    actual fun rawConfig(): String {
        return if (np != null)
            np!!.confSerializer stringify np!!.config!!
        else ""
    }

    actual abstract fun doRawAction(action: String)

}