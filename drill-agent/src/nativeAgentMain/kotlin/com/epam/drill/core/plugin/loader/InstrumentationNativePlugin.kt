package com.epam.drill.core.plugin.loader

import com.epam.drill.DrillPluginFile
import com.epam.drill.plugin.api.processing.InstrumentationPlugin
import jvmapi.CallObjectMethod
import jvmapi.CallVoidMethodA
import jvmapi.GetMethodID
import jvmapi.NewStringUTF
import jvmapi.jbyteArray
import jvmapi.jmethodID


@ExperimentalUnsignedTypes
open class InstrumentationNativePlugin(pf: DrillPluginFile) : GenericNativePlugin(pf),
    InstrumentationPlugin {
    override fun instrument(className: String, initialBytes: ByteArray): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var qs: jmethodID? = null

    override suspend fun connect() {
        super.connect()
        qs = GetMethodID(pluginApiClass, "instrument", "(Ljava/lang/String;[B)[B")
    }


    override fun retransform() {
        CallVoidMethodA(userPlugin, GetMethodID(pluginApiClass, "retransform", "()V"), null)
    }

    fun instrument(className: String, x1: jbyteArray): jbyteArray? {
        return CallObjectMethod(userPlugin, qs, NewStringUTF(className), x1)
    }
}
