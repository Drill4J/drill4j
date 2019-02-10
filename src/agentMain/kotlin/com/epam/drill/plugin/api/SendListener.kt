package com.epam.drill.plugin.api

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.common.util.DJSON
import com.epam.drill.plugin.api.message.DrillMessage
import com.epam.drill.plugin.api.message.MessageWrapper
import com.epam.drill.plugin.api.processing.ProcessListener
import com.epam.drill.storage.RequestContainer
import com.epam.drill.ws.Ws
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Igor Kuzminykh
 */
class SendListener : ProcessListener {

    override fun doAction(dest: String, message: String?) {
        var sessionId: String? = null
        if (RequestContainer.resource() != null) {
            sessionId = RequestContainer.resource().cookies["DrillSessionId"]
        }
//        logDebug("sessionId is $sessionId")
        val value = MessageWrapper(dest, DrillMessage(sessionId, message))
        GlobalScope.launch {
            Ws.send(DJSON.stringify(Message(MessageType.PLUGIN_DATA, "", DJSON.stringify(value))))
//            logDebug("Some data was sent to $dest plugin")
        }
    }

}
