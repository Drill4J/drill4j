package com.epam.kjni.build.core

import com.epam.kjni.build.core.classtructure.*
import com.epam.kjni.build.core.utils.getClassName
import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.JavaClass
import java.io.File
import java.util.jar.JarFile


val wrappers = setOf(
    "java/lang/Integer.class",
    "java/lang/Double.class",
    "java/lang/Byte.class",
    "java/lang/Short.class",
    "java/lang/Long.class",
    "java/lang/Float.class",
    "java/lang/Boolean.class",
    "java/lang/Character.class"
)

class NativeGenerator {

    fun generateSingleClassByMeta(metaOfClass: JavaClass, outputDir: File) {
        val className = getClassName(metaOfClass)
        createFile(metaOfClass.packageName, className).addType(createClass(metaOfClass)).build().writeTo(outputDir)

    }

    fun generatePrimitiveWrapper(metaOfClass: JavaClass, outputDir: File) {
        val className = getClassName(metaOfClass)
        createFile(metaOfClass.packageName, className).addType(createPrimitiveWrapperClass(metaOfClass)).build()
            .writeTo(outputDir)
    }


    fun JustConstructorsWrapper(metaOfClass: JavaClass, outputDir: File) {
        val className = getClassName(metaOfClass)
        createFile(metaOfClass.packageName, className).addType(createJustConstructWrapperClass(metaOfClass, outputDir))
            .build()
            .writeTo(outputDir)
    }
}

fun generateJavaPrimitivesWrapper(outputDir: File) {
    val klass = String::class.java
    val location = klass.getResource('/'.toString() + klass.name.replace('.', '/') + ".class")
    val rtJar = location.path.replace("file:/", "").replace("!/" + klass.name.replace('.', '/') + ".class", "")
    val jarFile = JarFile(rtJar)
    jarFile.entries().toList().forEach {
        if (wrappers.contains(it.name)) {
            val zipIn = jarFile.getInputStream(it)
            val name = it.name
            val j = ClassParser(zipIn, name).parse()
            NativeGenerator().generatePrimitiveWrapper(j, outputDir)
        }
    }
}


fun systemClasses(outputDir: File) {
    oh(Thread::class.java, outputDir)
    oh(ThreadGroup::class.java, outputDir)
    oh(Throwable::class.java, outputDir)
}

private fun oh(klass: Class<*>, outputDir: File) {
    val location = klass.getResource('/'.toString() + klass.name.replace('.', '/') + ".class")
    val j = ClassParser(location.openStream(), klass.name.replace(".", "/") + ".class").parse()
    NativeGenerator().JustConstructorsWrapper(j, outputDir)
}


fun justTheClass(packageName: String, className: String, outputDir: File) {
    createFile(packageName, className).addType(createEmptyClass(packageName, className)).build()
        .writeTo(outputDir)
}

