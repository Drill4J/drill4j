package com.epam.drill.core


import com.epam.drill.common.AgentAdditionalInfo
import com.epam.drill.common.AgentInfo
import com.epam.drill.core.util.dumpConfigToFileSystem
import com.epam.drill.logger.readProperties
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.util.OS
import drillInternal.config
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun parseConfigs() = runBlocking {
    val path = "$drillInstallationDir/configs"

    val agInfo = Json().parse(
        AgentInfo.serializer(),
        localVfs("${"$path/"}drillConfig.json").readString()
    )
    di {
        agentInfo = agInfo
        //fixme retrieve a real IP
        agInfo.ipAddress = "127.0.0.1"
        agInfo.additionalInfo = AgentAdditionalInfo(
            listOf(),
            4,
            "x64",
            OS.platformNameLC + ":" + OS.platformName,
            "10",
            mapOf()
        )

        loggerConfig = localVfs("${"$path/"}logger.properties").readProperties()
    }

}

var agentInfo: AgentInfo
    get() = di { agentInfo }
    set(value) {
        di { agentInfo = value }
        value.dumpConfigToFileSystem()
    }

val drillInstallationDir: String
    get() = config.drillInstallationDir?.toKString()!!

