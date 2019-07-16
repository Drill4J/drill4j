package com.epam.drill.core.agent

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.logger.*
import jvmapi.*

fun performAgentInitialization(initialParams: Map<String, String>) {
    val adminAddress = initialParams.getValue("adminAddress")
    val agentId = initialParams.getValue("agentId")
    val buildVersion = initialParams["buildVersion"] ?: ""
    val drillInstallationDir = initialParams.getValue("drillInstallationDir")
    exec {
        this.drillInstallationDir = drillInstallationDir
        this.agentConfig = AgentConfig(agentId, adminAddress, buildVersion)
    }
}

fun calculateBuildVersion() {
    if (exec { agentConfig.buildVersion }.isEmpty()) {
        val initializerClass = FindClass("com/epam/drill/ws/Initializer")
        val selfMethodId: jfieldID? =
                GetStaticFieldID(initializerClass, "INSTANCE", "Lcom/epam/drill/ws/Initializer;")
        val initializer: jobject? = GetStaticObjectField(initializerClass, selfMethodId)
        val calculateBuild: jmethodID? = GetMethodID(initializerClass, "calculateBuild", "()I")
        val buildVersion = CallIntMethod(initializer, calculateBuild)
        println("Calculated build version: $buildVersion")
        exec { agentConfig.buildVersion = buildVersion.toString() }
    }
}