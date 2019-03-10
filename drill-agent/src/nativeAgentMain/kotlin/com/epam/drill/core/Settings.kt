package com.epam.drill.core


import com.epam.drill.common.AgentAdditionalInfo
import com.epam.drill.common.AgentInfo
import com.epam.drill.core.util.dumpConfigToFileSystem
import com.epam.drill.logger.readProperties
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.util.OS
import drillInternal.config
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
    di.agentInfo = any


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

    any.agentAddress = "127.0.0.1"

    di.loggerConfig = resourcesVfs["${"$path/"}logger.properties"].readProperties()

}

var agentInfo: AgentInfo
    get() = di.agentInfo
    set(value) {
        di.agentInfo = value
        value.dumpConfigToFileSystem()
    }

val drillInstallationDir: String
    get() = config.drillInstallationDir?.toKString()!!

