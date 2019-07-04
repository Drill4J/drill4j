package com.epam.drill.plugins.coverage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SharedImmutable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@SharedImmutable
val serialModule = SerializersModule {
    polymorphic(Action::class) {
        StartSession::class with StartSession.serializer()
        StopSession::class with StopSession.serializer()
        CancelSession::class with CancelSession.serializer()
        SwitchScope::class with SwitchScope.serializer()
        IgnoreScope::class with IgnoreScope.serializer()
        DropScope::class with DropScope.serializer()
    }
}

@SharedImmutable
val json = Json(context = serialModule)


infix fun <T> KSerializer<T>.parse(rawData: String) = json.parse(this, rawData)

infix fun <T> KSerializer<T>.stringify(rawData: T) = json.stringify(this, rawData)
