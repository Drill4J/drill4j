package com.epam.drill.jvmti

import com.epam.drill.jvmti.logger.Properties
import com.epam.drill.jvmti.logger.readProperties
import com.soywiz.korio.file.std.resourcesVfs
import jvmapi.config
import kotlinx.cinterop.Arena
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString

object AgentInfo {
    suspend fun parseConfigs(folder: String?) {
        if (folder == null) {
            println("________________________________________________________________")
            println(
                "NATIVE WARNING: Please fill the config folder in agent args.\n" +
                        "Loggers are OFF.\n" +
                        "Default url 'localhost:8090'"
            )
            println("________________________________________________________________")
        }
        fillMainProperties(folder)
    }

    private suspend fun fillMainProperties(path: String?) {

        val readProperties = if (path != null)
            resourcesVfs["${"$path/"}drillConfig.properties"].readProperties()
        else Properties(mutableMapOf())
        config.drillAdminUrl =
            readProperties["drillAdminUrl"]?.cstr?.getPointer(Arena()) ?: "localhost:8090".cstr.getPointer(Arena())
        //todo defaultAgentNameAsHostAddress
        config.agentName = run { readProperties["agentName"] ?: "undefinedName" }.cstr.getPointer(Arena())
        config.agentGroupName = run { readProperties["agentGroupName"] ?: "undefinedGroup" }.cstr.getPointer(Arena())

        config.loggerConfig =
            StableRef.create(
                if (path != null) resourcesVfs["${"$path/"}logger.properties"].readProperties() else Properties(
                    mutableMapOf()
                )
            ).asCPointer()
    }


}

val drillAdminUrl: String?
    get() = config.drillAdminUrl?.toKString()

val agentName: String
    get() = config.agentName?.toKString() ?: "undefinedGroup"

val agentGroupName: String
    get() = config.agentGroupName?.toKString() ?: "undefinedName"

