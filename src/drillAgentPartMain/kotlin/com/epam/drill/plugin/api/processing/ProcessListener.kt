package com.epam.drill.plugin.api.processing

interface ProcessListener {

    fun doAction(dest: String, message: String?)
}
