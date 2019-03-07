package com.epam.drill.plugin.api.processing

import com.epam.drill.common.PluginBean
import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

actual abstract class NativePart<T> : Switchable, Lifecycle {
    var rawConfig: CPointer<ByteVar>? = null

    val config: T?
        get() {
            return if (rawConfig != null)
                Json().parse(confSerializer, rawConfig!!.toKString())
            else null
        }

    fun load() {
        initPlugin()
        on()
    }

    fun unload(reason: Reason) {
        off()
        destroyPlugin(reason)
    }

    abstract var id: CPointer<ByteVar>
    actual abstract val confSerializer: KSerializer<T>


    actual fun updateRawConfig(someText: PluginBean) {
        try {
            try {
                rawConfig = someText.config.cstr.getPointer(Arena())
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        } catch (ex: Throwable) {
        }
    }

}

abstract class PluginRepresenter : AgentPart<Any>() {
    override var confSerializer: KSerializer<Any>
        get() = TODO("stubValue") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun initPlugin() {
        TODO("stubValue") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroyPlugin(reason: Reason) {
        TODO("stubValue") //To change body of created functions use File | Settings | File Templates.
    }

}


actual abstract class AgentPart<T> : DrillPlugin(), Switchable, Lifecycle {

    actual var enabled: Boolean = false

    actual open fun init(nativePluginPartPath: String) {
    }


    actual open fun load(onImmediately: Boolean) {
        initPlugin()
        if (onImmediately)
            on()
    }

    actual open fun unload(reason: Reason) {
        off()
        destroyPlugin(reason)
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