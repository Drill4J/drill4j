package com.epam.drill.plugin.api.processing

import com.epam.drill.common.*
import kotlinx.serialization.*

actual abstract class NativePart<T> {

    actual abstract val confSerializer: KSerializer<T>
    actual fun updateRawConfig(someText: PluginBean) {
    }

}