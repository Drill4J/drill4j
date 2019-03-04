package com.epam.drill

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.test.Test


class MySerialDescriptor(val sd: SerialDescriptor) : SerialDescriptor by sd {

    override fun getElementIndex(name: String): Int {
        println(
            String(name.toCharArray())
        )
//        println(elementIndex)
        println(String(getElementName(0).toCharArray()))
        val elementIndex = sd.getElementIndex(String(name.toCharArray()))

        return elementIndex
    }

    override fun getElementName(index: Int): String {
//        throw RuntimeException("s")
        println("hi")
        return "xx"
    }

}

class DecoderDelegator(val w: CompositeDecoder) : CompositeDecoder by w {

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        val decodeElementIndex = w.decodeElementIndex(MySerialDescriptor(desc))
        println(decodeElementIndex)
        return decodeElementIndex
    }


}

class MyDecoder(val decoder: Decoder) : Decoder by decoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return DecoderDelegator(decoder.beginStructure(MySerialDescriptor(desc), *typeParams))
    }
}


class MyDeserializer<T>(val deserializer: KSerializer<T>) : KSerializer<T> by deserializer {
    override fun deserialize(decoder: Decoder): T {
        return deserializer.deserialize(MyDecoder(decoder))
    }

}

class Tesa : Ax<TestExConf>() {
    override val deserializer: KSerializer<TestExConf> = TestExConf.serializer()


    @ImplicitReflectionSerializer
    @Test
    fun xx() {

        println(
            Json().parse(
                MyDeserializer(deserializer), """{st:"xxxx"}""".trim()
            )
        )

    }

}


@Serializable
data class TestExConf(val st: String)