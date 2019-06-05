package com.epam.drill.plugins.coverage

import com.epam.drill.ClassPath
import com.epam.drill.getByteCodeOf
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