package com.epam.drill.plugins.coverage

import com.epam.drill.session.*

private val instrContext = object : InstrContext {
    override fun invoke(): String? = DrillRequest.currentSession()
    override fun get(key: String): String? = DrillRequest[key.toLowerCase()]
}

/**
 * The probe provider MUST be a Kotlin singleton object
 */
internal object DrillProbeArrayProvider : SimpleSessionProbeArrayProvider(instrContext)

