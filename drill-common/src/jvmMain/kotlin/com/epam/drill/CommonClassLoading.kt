package com.epam.drill

import java.util.jar.JarEntry


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
