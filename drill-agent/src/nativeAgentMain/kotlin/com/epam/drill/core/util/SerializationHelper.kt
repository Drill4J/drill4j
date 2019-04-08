package com.epam.drill.core.util

import com.epam.drill.common.AgentInfo

import com.epam.drill.core.drillInstallationDir
import com.soywiz.korio.file.std.localVfs

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun AgentInfo.stringify() = Json().stringify(AgentInfo.serializer(), this)

fun AgentInfo.dumpConfigToFileSystem() = runBlocking {
    localVfs("$drillInstallationDir/configs/drillConfig.json").writeString(
        this@dumpConfigToFileSystem.stringify()
    )
}
