package com.epam.drill.plugin.api.processing

import com.epam.drill.common.stringify
import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.json.Json


actual abstract class AgentPart<T, A> : DrillPlugin(), Switchable, Lifecycle {
    private var rawConfig: String? = null
    //    val config: T get() = confSerializer parse rawConfig!!
    val config: T
        get() {
          return  Json.parse(confSerializer, rawConfig!!)
        }

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

    abstract fun doAction(action: A)

    actual abstract fun doRawAction(action: String)

    abstract var actionSerializer: kotlinx.serialization.KSerializer<A>


    actual abstract override fun destroyPlugin(unloadReason: UnloadReason)
    actual fun rawConfig(): String {
        return confSerializer stringify config!!
    }
}