package com.epam.drill.plugin.api.processing

import com.epam.drill.common.*
import com.epam.drill.plugin.api.*
import kotlinx.serialization.*

expect abstract class AgentPart<T, A>() : DrillPlugin<A>, Switchable, Lifecycle {
    var np: NativePart<T>?
    var enabled: Boolean

    abstract val confSerializer: KSerializer<T>

    open fun init(nativePluginPartPath: String)

    fun load(onImmediately: Boolean)
    fun unload(unloadReason: UnloadReason)


    abstract override fun initPlugin()

    abstract override fun destroyPlugin(unloadReason: UnloadReason)

    abstract override fun on()

    abstract override fun off()

    fun rawConfig(): String
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