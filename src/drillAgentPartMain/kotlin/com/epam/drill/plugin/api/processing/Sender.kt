package com.epam.drill.plugin.api.processing

object Sender {

    lateinit var listener: ProcessListener

    fun send(id: String, message: String?) {
        listener.doAction(id,message)
    }


}
