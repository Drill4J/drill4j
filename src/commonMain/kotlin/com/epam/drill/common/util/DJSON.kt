package com.epam.drill.common.util

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.serializer
import kotlin.reflect.KClass


@UseExperimental(ImplicitReflectionSerializer::class)
object DJSON {

    fun stringify(value: Any): String {
        @Suppress("UNCHECKED_CAST") val kClass = value::class as KClass<Any>
        return JSON.stringify(kClass.serializer(), value)
    }

    inline fun <reified T> parse(jsonInString: String): T {
        @Suppress("UNCHECKED_CAST") val str = T::class as KClass<Any>
        return JSON.parse(str.serializer(), jsonInString) as T
    }

    fun parse(jsonInString: String, type: KClass<Any>): Any {
        return JSON.parse(type.serializer(), jsonInString)
    }

   private fun parse(jsonInString: String, ser: KSerializer<List<String>>): Any {
        return JSON.parse(ser, jsonInString)
    }
}