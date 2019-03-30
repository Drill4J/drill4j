package com.epam.drill

import com.soywiz.korio.async.await
import com.soywiz.korio.file.modifiedDate
import com.soywiz.korio.file.std.localVfs
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.Charset
import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Ignore
import kotlin.test.Test

class KorioVfs {

    @Test
    @Ignore
    fun readRawString() = runBlocking {

        Worker.start(true).execute(TransferMode.UNSAFE, {}) {
            runBlocking {
                println(resourcesVfs["plugin_configss.json"].exists())
                println(resourcesVfs["plugin_config.json"].readLines())
                println(resourcesVfs["plugin_config.json"].readString(Charset.forName("UTF-8")))
                println(resourcesVfs["plugin_config.json"].readAll().stringFromUtf8OrThrow())
            }
        }.await()
    }
}