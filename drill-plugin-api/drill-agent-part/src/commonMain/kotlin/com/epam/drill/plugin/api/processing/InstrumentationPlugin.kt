package com.epam.drill.plugin.api.processing

interface InstrumentationPlugin {

    fun retransform()

    fun instrument(className: String, initialBytes: ByteArray): ByteArray?

}