//package com.epam.drill.ws
//
//import com.epam.drill.common.Message
//import com.epam.drill.common.MessageType
//import com.epam.drill.common.util.DJSON
//import com.soywiz.klogger.Logger
//import java.nio.ByteBuffer
//import kotlin.reflect.KClass
//
//actual object Ws {
//    val logger = Logger("JavaWS").apply { level = Logger.Level.TRACE }
//
//    val destinationSubMapping: MutableMap<String, Pair<KClass<*>, ((Any) -> Unit)>> = HashMap()
//
//
//    inline fun <reified T> subscribe(topic: String, noinline block: (T) -> Unit) {
//        @Suppress("UNCHECKED_CAST")
//        destinationSubMapping[topic] = T::class to block as (Any) -> Unit
//    }
//
//    fun <T : Any> subscribe(xx: KClass<T>, topic: String, block: (T) -> Unit) {
//        @Suppress("UNCHECKED_CAST")
//        destinationSubMapping[topic] = xx to block as (Any) -> Unit
//    }
//
//    fun onMessage(rawMessage: String) {
//        try {
//            val message = DJSON.parse<Message>(rawMessage)
//            if (message.type == MessageType.MESSAGE) {
//
//                if (message.message.isEmpty() || message.message == "[]") {
//
////                    logWarn("Somewho sends the empty message on '${message.destination}' topic")
//                    return
//                }
//                val function = destinationSubMapping[message.destination]
//                val second = function?.second
//                if (second != null) {
//                    val type = function.first
//                    @Suppress("UNCHECKED_CAST")
//                    when (type) {
//                        String::class -> second(message.message)
////                        List::class -> second(DJSON.parse(message.message, StringSerializer.list))
//                        Array<Any>::class -> println(type)
//                        Set::class -> println(type)
//                        else -> second(DJSON.parse(message.message, type as KClass<Any>))
//
//                    }
//                } else {
////                    logWarn("we don't have subs for '${message.destination}' destination")
//                }
//            }
//        } catch (ex: Exception) {
////            logError("Are you fucking seriously? ${ex.message}")
//            throw ex
//        }
//    }
//
//    external fun send(message: String)
//
//    //fixme ?????????? move to native to?? partially
//    fun binaryRetriever(block: (ByteBuffer) -> Unit) {
//
//    }
//
//}
//
package com.epam.drill.ws
object Ws {
    external fun asdasd()

}
