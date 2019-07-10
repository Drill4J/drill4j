package com.epam.drill.core.messanger

import com.epam.drill.api.*
import com.epam.drill.common.*
import com.epam.drill.core.plugin.dto.*
import kotlinx.cinterop.*


fun sendNativeMessage(pluginId: CPointer<ByteVar>, content: CPointer<ByteVar>) {
    sendMessage(pluginId.toKString(), content.toKString())
}

fun sendMessage(pluginId: String, content: String) {
    val drillRequest = drillRequest()
    val message =

        Message.serializer() stringify Message(
            MessageType.PLUGIN_DATA, "",


            MessageWrapper.serializer() stringify
                    MessageWrapper(
                        pluginId, DrillMessage(
                            drillRequest?.drillSessionId ?: "", content
                        )
                    )
        )


    com.epam.drill.core.ws.sendMessage(message)
}