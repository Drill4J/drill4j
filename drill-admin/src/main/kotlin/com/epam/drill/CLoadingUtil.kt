package com.epam.drill

import mu.KotlinLogging
import java.net.URL


private val logger = KotlinLogging.logger {}

fun ClassLoader.loadClassesFrom(source: URL) {
    val parameters = arrayOf<Class<*>>(URL::class.java)
    try {
        val method = javaClass.superclass.getDeclaredMethod("addURL", *parameters)
        method.isAccessible = true
        method.invoke(this, source)
    } catch (e: Exception) {
        logger.error(e) { "Error loading classes from $source" }
    }

}