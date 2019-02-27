package com.epam.drill.plugin.api.processing

object Sender {
    external fun sendMessage(pluginId: String, message: String)
}