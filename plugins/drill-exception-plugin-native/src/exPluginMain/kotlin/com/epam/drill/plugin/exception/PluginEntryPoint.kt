package com.epam.drill.plugin.exception

import com.epam.drill.plugin.api.processing.NativePart
import com.epam.drillnative.api.addPluginToRegistry
import com.epam.drillnative.api.getPlugin
import kotlinx.cinterop.Arena
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlin.native.concurrent.ThreadLocal


@Suppress("FunctionName", "UNUSED_PARAMETER", "unused")
@CName("JNI_OnLoad")
fun pluginSetup(vm: Long, reservedPtr: Long): Int {

    //fixme fix this hardcode...!!!!
    val cls = ExNative("except-ions".cstr.getPointer(Arena()))
    addPluginToRegistry(cls)

    return 65542
}

@ThreadLocal
object PluginContext {

    @ThreadLocal
    var temp: NativePart<*>? = null

    val sd: ExceptionConfig?
        get() {
            return try {
                if (temp != null) {
                    Json().parse(ExceptionConfig.serializer(), temp?.rawConfig!!.toKString())
                } else {
                    temp = getPlugin("except-ions".cstr.getPointer(Arena()))
                    Json().parse(ExceptionConfig.serializer(), temp?.rawConfig!!.toKString())
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
                return null
            }
        }

    inline operator fun invoke(block: ExceptionConfig.() -> Unit) {
        if (sd != null)
            block(sd!!)
    }

}