package com.epam.drill.kafka.producer

import kafka.*
import kotlinx.cinterop.*
import platform.posix.NULL

class Producer(val topic: String, val config: CPointer<rd_kafka_conf_t>?) {
    val rkt: CPointer<rd_kafka_topic_t>?
    val kafka: CPointer<rd_kafka_t>?

    init {
        val const = Arena()

        val errstr: CPointer<ByteVarOf<Byte>> = const.allocArray(522)
        kafka = rd_kafka_new(rd_kafka_type_t.RD_KAFKA_PRODUCER, config, errstr, sizeOf<ByteVar>().toULong())
        rkt = rd_kafka_topic_new(kafka, topic, null)
        const.clear()
    }

    fun send(message: String, partition: Int = RD_KAFKA_PARTITION_UA, copy: Int = RD_KAFKA_MSG_F_COPY) = memScoped {
        val rawMessage = message.cstr.getPointer(this)
        //fixme str to bytes in 1.3.40
        val len = message.toCharArray().size.toULong()
        val success = rd_kafka_produce(
            /* Topic object */
            rkt,
            /* Use builtin partitioner to select partition*/
            partition,
            /* Make a copy of the payload. */
            copy,
            /* Message payload (value) and length */
            rawMessage,
            len,
            /* Optional key and its length */
            NULL,
            0.toULong(),
            /* Message opaque, provided in
             * delivery report callback as
             * msg_opaque. */
            NULL
        )
        rd_kafka_flush(kafka, 10 * 1000 /* wait for max 10 seconds */);
        success
    }

}