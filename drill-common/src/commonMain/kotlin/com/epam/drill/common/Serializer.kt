package com.epam.drill.common

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@SharedImmutable
val json = Json(JsonConfiguration.Stable)

infix fun <T> KSerializer<T>.parse(rawData: String) = json.parse(this, rawData)
//    Cbor.loads(this, rawData)
infix fun <T> KSerializer<T>.stringify(rawData: T) = json.stringify(this, rawData)
//    Cbor.dumps(this, rawData)
