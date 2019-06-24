package com.epam.drill.kafka.consumer

import kafka.rd_kafka_consumer_poll
import kafka.rd_kafka_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret

class Consumer(val rk: CPointer<rd_kafka_t>?) {


    suspend inline fun consumeEach(function: (ByteArray) -> Unit) {
        while (true) {
            kotlinx.coroutines.delay(10)
            val polled = rd_kafka_consumer_poll(rk, 100)
            if (polled != null) {
                val len = polled.pointed.len
                val payload = polled.pointed.payload
                val bytes = payload?.reinterpret<ByteVar>()?.readBytes(len.toInt())
                if (bytes != null) {
                    function(bytes)
                }
            }
        }
    }

}