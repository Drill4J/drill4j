package com.epam.drill.common.dynamicloader

import java.io.File
import java.net.URL
import java.util.jar.JarEntry
import java.util.jar.JarFile

private fun getClassName(je: JarEntry): String {
    var className = je.name.substring(0, je.name.length - 6)
    className = className.replace('/', '.')
    return className
}

fun retrieveApiClass(targetClass: Class<*>, entrySet: Set<JarEntry>, cl: ClassLoader): Class<*> {

    //fixme
    return entrySet.filter { it.name.endsWith(".class") && !it.name.contains("$") }.map { je ->
        val className = getClassName(je)
        val basClass = cl.loadClass(className)
        var parentClass = basClass
        while (parentClass != null && parentClass != targetClass) {
            parentClass = parentClass.superclass
        }
        basClass
    }.first()
}


//fixme duplicate...
fun retrieveApiClass(targetClass: Class<*>, entrySet: Set<JarEntry>): Class<*> {

    //fixme
    return entrySet.filter { it.name.endsWith(".class") && !it.name.contains("$") }.map { je ->
        val className = getClassName(je)

        //fixme it should be work
        val basClass = Class.forName(className)
        var parentClass = basClass
        while (parentClass != null && parentClass != targetClass) {
            parentClass = parentClass.superclass
        }
        basClass
    }.first()
}

fun extractConfigFile(jarFile: JarFile, parCat: File) {
    val jarEntry: JarEntry = jarFile.getJarEntry("static/plugin_config.json")
    val cs = File(parCat, jarEntry.name)
    if (!cs.exists()) {
        cs.parentFile.mkdirs()
        cs.createNewFile()
        jarFile.getInputStream(jarEntry).use { input ->
            cs.outputStream().use { fileOut ->
                input.copyTo(fileOut)
            }
        }
    }
}


fun loadInRuntime(f: File, classLoader: ClassLoader) {
    val parameters = arrayOf<Class<*>>(URL::class.java)
    try {
        val method = classLoader.javaClass.superclass.getDeclaredMethod("addURL", *parameters)
        method.isAccessible = true
        method.invoke(classLoader, f.toURI().toURL())
        //fixme log
//        logDebug("Result: plugin jar was loaded in Runtime")
    } catch (t: Exception) {
        //fixme log
//        logError("Result: failed to load jar in runtime $t")
    }

}