package com.epam.drill.plugins.coverage

import org.junit.Test


open class ClassPathX {

    @Test
    fun lol() {
        val classLoader = ClassLoader.getSystemClassLoader()

        val classPath = ClassPath()
        val mutableMapOf = classPath.scanItPlease(classLoader)
        mutableMapOf.getByteCodeOf("java/lang/Object.class")

    }

   }