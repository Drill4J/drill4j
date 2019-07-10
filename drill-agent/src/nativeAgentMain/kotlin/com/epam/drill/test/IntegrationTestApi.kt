@file:Suppress("unused")

package com.epam.drill.test

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.plugin.loader.*
import com.epam.drill.jvmapi.*
import jvmapi.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.collections.set


@Suppress("UNUSED_PARAMETER")//this only for integrationTests
@CName("Java_com_epam_drill_test_IntegrationTestApi_LoadPlugin")
fun LoadPug(env: JNIEnv, thiz: jobject, path: jstring) = memScoped {
    val pluginBean = PluginBean(
        id = "coverage",
        config = "{\"pathPrefixes\": [\"org\"], \"message\": \"hello from default plugin config... This is 'plugin_config.json file\"}"
    )
    runBlocking {
        exec {
            pl["coverage"] = pluginBean
        }

        loadPlugin(path.toKString()!!, pluginBean)
    }
}

@Suppress("UNUSED_PARAMETER")//this only for integrationTests
@CName("Java_com_epam_drill_test_IntegrationTestApi_setAdminUrl")
fun setAdminUrl(env: JNIEnv, thiz: jobject, path: jstring) {
    exec {
        agentConfig = AgentConfig("test", path.toKString()!!, "")
    }
}