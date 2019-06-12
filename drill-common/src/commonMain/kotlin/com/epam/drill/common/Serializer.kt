package com.epam.drill.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SharedImmutable
import kotlinx.serialization.json.Json

@SharedImmutable
val json = Json()

infix fun <T> KSerializer<T>.parse(rawData: String) = json.parse(this, rawData)
//    Cbor.loads(this, rawData)
infix fun <T> KSerializer<T>.stringify(rawData: T) = json.stringify(this, rawData)
//    Cbor.dumps(this, rawData)
