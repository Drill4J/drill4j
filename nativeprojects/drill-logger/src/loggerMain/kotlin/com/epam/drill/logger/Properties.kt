package com.epam.drill.logger

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Charset


class Properties(private val properties: Map<String, String>) {
    operator fun get(propertyName: String): String? {
        return properties[propertyName]
    }

    fun keySet(): Set<String> {
        return properties.keys
    }

}

suspend fun VfsFile.readProperties(): Properties {
    val filter = this.readLines(Charset.forName("UTF-8")).filter {!it.startsWith("#") && it.isNotEmpty() }
    val properties = filter.map { it.split("=") }
        .associate { it[0] to it[1] }
    return Properties(properties)
}