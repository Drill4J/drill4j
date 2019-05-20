package com.epam.drill.core.util

import com.epam.drill.common.AgentInfo

import com.epam.drill.core.drillInstallationDir
import com.soywiz.korio.file.std.localVfs

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.native.concurrent.SharedImmutable

fun AgentInfo.stringify() = json.stringify(AgentInfo.serializer(), this)

fun AgentInfo.dumpConfigToFileSystem() = runBlocking {
    localVfs("$drillInstallationDir/configs/drillConfig.json").writeString(
        this@dumpConfigToFileSystem.stringify()
    )
}


@SharedImmutable
val json: Json.Companion = Json.Companion