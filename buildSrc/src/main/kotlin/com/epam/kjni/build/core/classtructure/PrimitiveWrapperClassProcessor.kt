package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.getConstructors
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.apache.bcel.classfile.JavaClass

fun createPrimitiveWrapperClass(metaOfClass: JavaClass): TypeSpec {
    val className = getClassName(metaOfClass)
    val classBuilder = TypeSpec.classBuilder(className)
    generateDefaultWrapConstructors(metaOfClass.methods.getConstructors()).forEach { classBuilder.addFunction(it) }
    classBuilder.addFunction(generateDefaultWrapConstructors())

    classBuilder.addFunction(generatePrimitiveMethods(metaOfClass.className))

    classBuilder.superclass(synthetic)
    classBuilder.addProperty(
        PropertySpec.builder("javaObject", jobject).mutable().addModifiers(
            KModifier.OVERRIDE, KModifier.LATEINIT
        ).build()
    )
    classBuilder.addProperty(
        PropertySpec.builder(
            "javaClass",
            jclass
        ).mutable().addModifiers(KModifier.LATEINIT).build()
    )
    classBuilder.addProperty(
        PropertySpec.builder("className", String::class).initializer(
            "%S",
            metaOfClass.className.replace(".", "/")
        ).build()
    )
    return classBuilder.build()
}