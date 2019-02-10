/*
 *  Copyright 2017 EPAM Systems <Igor_Kuzminykh@epam.com, Sergey_Larin@epam.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.epam.drill.plugin.api.processing

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.MessageFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This class represents the [DrillServerConfig.PROPERTY_FILE] with transformed data
 *
 * @author Igor Kuzminykh
 */
object DrillServerConfig : Properties() {

    private const val PROPERTY_FILE = "embedded-server-configuration.properties"
    const val SERVER_PORT_KEY = "drill.server.port"
    const val SERVER_HOST_KEY = "drill.server.host"
    val log: Logger = Logger.getLogger(DrillServerConfig::class.java.name)
    private var timeStamp: Long = 0
    val fileConf: File

    val serverPort: String
        get() {
            val lastModified = fileConf.lastModified()
            if (timeStamp <= lastModified) {
                timeStamp = lastModified
                loadProperties(fileConf)
            }
            return this.getProperty(SERVER_PORT_KEY)
        }

    val serverHost: String
        get() {
            val lastModified = fileConf.lastModified()
            if (timeStamp <= lastModified) {
                timeStamp = lastModified
                loadProperties(fileConf)
            }
            return this.getProperty(SERVER_HOST_KEY)
        }


    init {
        fileConf = reachFile()
        timeStamp = fileConf.lastModified()
        loadProperties(fileConf)

    }

     fun reachFile(): File {
        val file = File(DrillConstants.DRILL_HOME, PROPERTY_FILE)
        if (!file.exists()) {
            try {
                if (file.parentFile.mkdirs()) {
                    log.log(Level.INFO, file.absolutePath)
                }
                if (file.createNewFile()) {
                    extractConfFile(file)

                }
            } catch (e: IOException) {
                log.log(Level.SEVERE, "Can't load the properties... ", e)
            }

        }
        return file
    }

    private fun loadProperties(file: File) {
        try {
            this.load(FileInputStream(file))
        } catch (e: Exception) {
            log.log(Level.SEVERE, MessageFormat.format("Can not parse {0}. Use default settings.", PROPERTY_FILE), e)
        }

    }

    private fun extractConfFile(file: File) {
        val resourceAsStream = DrillServerConfig::class.java.getResourceAsStream(PROPERTY_FILE)
        resourceAsStream.use { input ->
            FileOutputStream(file)
                    .use { fileOut -> input.copyTo(fileOut) }
        }
    }


}
