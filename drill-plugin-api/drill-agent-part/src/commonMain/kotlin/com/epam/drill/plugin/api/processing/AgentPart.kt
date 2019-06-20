package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.serialization.KSerializer

expect abstract class AgentPart<T, A>() : DrillPlugin, Switchable, Lifecycle {
    var np: NativePart<T>?
    var enabled: Boolean

    abstract var confSerializer: KSerializer<T>

    open fun init(nativePluginPartPath: String)

    fun load(onImmediately: Boolean)
    fun unload(unloadReason: UnloadReason)


    abstract override fun initPlugin()

    abstract override fun destroyPlugin(unloadReason: UnloadReason)

    abstract override fun on()

    abstract override fun off()

    fun rawConfig(): String

    abstract fun doRawAction(action: String)
}


interface Switchable {
    fun on()
    fun off()
}

interface Lifecycle {
    fun initPlugin()
    fun destroyPlugin(unloadReason: UnloadReason)
}

expect abstract class NativePart<T> {
    actual abstract val confSerializer: KSerializer<T>
    fun updateRawConfig(someText: PluginBean)
}

enum class UnloadReason {
    ACTION_FROM_ADMIN, SH
}