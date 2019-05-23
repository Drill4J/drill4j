package com.epam.drill.core


import com.epam.drill.common.AgentAdditionalInfo
import com.epam.drill.common.AgentInfo
import com.epam.drill.core.util.json
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.util.OS
import kotlinx.coroutines.runBlocking

fun parseConfigs() = runBlocking {
    val path = "$drillInstallationDir/configs"

    val agInfo = json.parse(
        AgentInfo.serializer(),
        localVfs("${"$path/"}drillConfig.json").readString()
    )

    exec {
        diAgentInfo = agInfo
        //fixme retrieve a real IP
        agInfo.ipAddress = "127.0.0.3"
        agInfo.additionalInfo = AgentAdditionalInfo(
            listOf(),
            4,
            "x64",
            OS.platformNameLC + ":" + OS.platformName,
            "10",
            mapOf()
        )
    }

}

var agentInfo: AgentInfo
    get() = exec { diAgentInfo }
    set(value) {
        exec { diAgentInfo = value }
//        value.dumpConfigToFileSystem()
    }

val drillInstallationDir: String
    get() = exec { drillInstallationDir }

