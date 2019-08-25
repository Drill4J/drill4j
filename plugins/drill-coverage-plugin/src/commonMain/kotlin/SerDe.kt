package com.epam.drill.plugins.coverage

import com.epam.drill.plugin.api.*
import kotlinx.serialization.modules.*
import kotlin.native.concurrent.SharedImmutable

private val serialModule = SerializersModule {
    polymorphic<Action> {
        addSubclass(SwitchActiveScope.serializer())
        addSubclass(RenameScope.serializer())
        addSubclass(ToggleScope.serializer())
        addSubclass(DropScope.serializer())

        addSubclass(StartNewSession.serializer())
        addSubclass(StartSession.serializer())
        addSubclass(StopSession.serializer())
        addSubclass(CancelSession.serializer())
    }
    polymorphic<CoverMessage> {
        addSubclass(InitInfo.serializer())
        addSubclass(ClassBytes.serializer())
        addSubclass(Initialized.serializer())

        addSubclass(SessionStarted.serializer())
        addSubclass(SessionCancelled.serializer())
        addSubclass(CoverDataPart.serializer())
        addSubclass(SessionFinished.serializer())
    }
}

@SharedImmutable
val commonSerDe = SerDe(
        actionSerializer = Action.serializer(),
        ctx = serialModule
)

