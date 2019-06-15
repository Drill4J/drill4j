package com.epam.drill

import mu.KotlinLogging
import java.net.URL
import java.util.jar.JarEntry


private val logger = KotlinLogging.logger {}

private fun getClassName(je: JarEntry): String {
    var className = je.name.substring(0, je.name.length - 6)
    className = className.replace('/', '.')
    return className
}

fun retrieveApiClass(targetClass: Class<*>, entrySet: Set<JarEntry>, cl: ClassLoader): Class<*>? {

    entrySet.filter { it.name.endsWith(".class") && !it.name.contains("$") }.map { je ->
        val className = getClassName(je)
        val basClass = cl.loadClass(className)
        var parentClass = basClass
        while (parentClass != null) {
            if (parentClass == targetClass) {
                return basClass
            }
            parentClass = parentClass.superclass
        }
        return@map
    }
    return null
}

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