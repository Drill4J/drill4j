package com.epam.drill.kafka

import com.epam.drill.kafka.consumer.Consumer
import com.epam.drill.kafka.producer.Producer
import kafka.*
import kotlinx.cinterop.*

class KafkaBroker(private val url: String) {
    fun createProducerFor(topic: String, config: Map<String, Any> = mapOf("bootstrap.servers" to url)): Producer {
        return Producer(topic, configure(config))
    }


    fun createConsumerFor(
        topic: String, config: MutableMap<String, Any>.() -> Unit = {}
    ): Consumer {
        val cm: MutableMap<String, Any> = mutableMapOf(
            "bootstrap.servers" to url,
            "broker.address.family" to "v4",
            "group.id" to url,
            "session.timeout.ms" to 6000
        )
        val mut = mutableMapOf<String, Any>()
        config(mut)
        cm.putAll(mut)
        val topics = arrayOf(topic)
        cm.forEach { (k, v) ->
            println("$k: $v")
        }
        val conf = configure(cm)
        val const = Arena()
        val errstr: CPointer<ByteVarOf<Byte>> = const.allocArray(522)


        val rk = rd_kafka_new(
            rd_kafka_type_t.RD_KAFKA_CONSUMER,
            conf,
            errstr, sizeOf<ByteVar>().toULong()
        )

        rd_kafka_poll_set_consumer(rk)

        var rkq = rd_kafka_queue_get_consumer(rk)
        if (rkq == null) {
            println("lol")
            rkq = rd_kafka_queue_get_main(rk)
        }
        println("$rkq")
        val ctopics = rd_kafka_topic_partition_list_new(topics.size)
        for (top in topics) {
            rd_kafka_topic_partition_list_add(ctopics, top, RD_KAFKA_PARTITION_UA)
        }
        println("$rk")
        rd_kafka_subscribe(rk, ctopics)

        return Consumer(rk)
    }

    private fun configure(config: Map<String, Any>): CPointer<rd_kafka_conf_t>? = memScoped {
        val conf = rd_kafka_conf_new()
        val errstr: CPointer<ByteVarOf<Byte>> = this.allocArray(522)

        config.forEach { (k, v) ->
            rd_kafka_conf_set(conf, k, v.toString(), errstr, sizeOf<ByteVar>().toULong())
        }
        conf
    }
}