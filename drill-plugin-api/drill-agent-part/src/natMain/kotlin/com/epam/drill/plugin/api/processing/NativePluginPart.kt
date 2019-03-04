package com.epam.drill.plugin.api.processing

import com.epam.drill.plugin.api.DrillPlugin
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.json.Json

class MySerialDescriptor(val sd: SerialDescriptor) : SerialDescriptor by sd {
    override fun getElementIndex(name: String): Int {


        for (i in 0 until elementsCount) {
            if (String(getElementName(i).toCharArray()) == String(name.toCharArray())) {
                println(i)
                return i
            }
        }

        println(String(getElementName(0).toCharArray()))
        return sd.getElementIndex(String(name.toCharArray()))
    }

    override fun getElementName(index: Int): String {
        println("lol")
        val string = String(sd.getElementName(index).toCharArray())
        println(string)
        return string
    }
}

class DecoderDelegator(val w: CompositeDecoder) : CompositeDecoder by w {

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        val decodeElementIndex = w.decodeElementIndex(MySerialDescriptor(desc))
        println("XLASDLASDL :$decodeElementIndex")
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


actual abstract class NativePluginPart<T> {

    open fun load(id: Long) {
        this.id = "test".cstr.getPointer(Arena())
    }

    abstract fun unload(id: Long)

    abstract var id: CPointer<ByteVar>
    actual abstract fun updateConfig(someText: T)
    abstract fun restring(someText: String): String
    actual abstract val confSerializer: KSerializer<T>


    actual fun updateRawConfig(someText: String) {
        val restring = restring(someText)

        try {
            //fixme workaround for unreallable encoding
            try {
                println("NASDASD")

                updateConfig(Json().parse(MyDeserializer(confSerializer), restring))
//                println(message)
                println("xxx")
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            try {
                var st = ""
                restring.forEach { st += it }
                println(st)
                println(Json().parse(confSerializer, st))
            } catch (ex: Throwable) {
                println("error")
            }
//            updateConfig(restring)

        } catch (ex: Throwable) {
        }
    }

}

actual abstract class AgentPluginPart<T> : DrillPlugin(), SwitchablePlugin {

    actual var enabled: Boolean = false

    actual open fun init(nativePluginPartPath: String) {
    }


    actual abstract override fun load()
    actual abstract override fun unload()
    actual var np: NativePluginPart<T>? = null

    actual abstract var confSerializer: KSerializer<T>?

    abstract fun updateRawConfig(config: String)
    actual abstract fun updateConfig(config: T)
}