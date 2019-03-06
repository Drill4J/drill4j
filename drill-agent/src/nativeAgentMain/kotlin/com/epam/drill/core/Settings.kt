package com.epam.drill.core


import com.epam.drill.common.AgentAdditionalInfo
import com.epam.drill.common.AgentInfo
import com.epam.drill.logger.readProperties
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.util.OS
import drillInternal.config
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.Json


suspend fun parseConfigs() {
    fillMainProperties()
}

private suspend fun fillMainProperties() {
    val path = "$drillInstallationDir/configs"

    val any = Json().parse(
        AgentInfo.serializer(),
        resourcesVfs["${"$path/"}drillConfig.json"].readString()
    )
    config.agentInfo = StableRef.create(
        any
    ).asCPointer()

    //fixme retrieve a real IP
    any.agentAddress = "127.0.0.1"
    any.agentAdditionalInfo = AgentAdditionalInfo(
        listOf(),
        4,
        "x64",
        OS.platformNameLC + ":" + OS.platformName,
        "10",
        mapOf()
    )

    config.loggerConfig =
        StableRef.create(
            resourcesVfs["${"$path/"}logger.properties"].readProperties()
        ).asCPointer()
}

val agentInfo: AgentInfo
    get() = config.agentInfo?.asStableRef<AgentInfo>()?.get()!!

val drillInstallationDir: String
    get() = config.drillInstallationDir?.toKString()!!

