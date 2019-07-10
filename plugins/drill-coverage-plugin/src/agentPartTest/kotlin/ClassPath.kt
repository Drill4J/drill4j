package com.epam.drill.plugins.coverage

import com.epam.drill.*
import org.junit.Test
import kotlin.test.*


open class ClassPathTest {

    @Test
    fun `should return Analyzer bytecode`() {
        val classLoader = ClassLoader.getSystemClassLoader()
        val classPath = ClassPath()
        val classes = classPath.scanItPlease(classLoader)
        assertNotNull(classes.getByteCodeOf("org/objectweb/asm/tree/analysis/Analyzer.class"))
    }


    /**
     * Top level class is the main file which doesn't contains any $ symbols
     */
    @Test
    fun `should filter only top level class`() {
        assertTrue { isTopLevelClass("com/epam/drill/Omg") }
        //anonymous case
        assertFalse { isTopLevelClass("com/epam/drill/Omg$1") }
    }

}