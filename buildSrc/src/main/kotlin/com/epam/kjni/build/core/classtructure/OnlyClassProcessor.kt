package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.getConstructors
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.apache.bcel.classfile.JavaClass

fun createEmptyClass(packageName: String, className: String): TypeSpec {
    val classBuilder = TypeSpec.classBuilder(className)
//    classBuilder.superclass(synthetic)
//    classBuilder.addProperty(
//        PropertySpec.builder("javaObject", jobject).mutable().addModifiers(
//            KModifier.OVERRIDE, KModifier.LATEINIT
//        ).build()
//    )
//    classBuilder.addProperty(
//        PropertySpec.builder(
//            "javaClass", jclass
//        ).mutable().addModifiers(KModifier.LATEINIT).build()
//    )
    classBuilder.addProperty(
        PropertySpec.builder("className", String::class).initializer(
            "%S", ("$packageName.$className").replace(".", "/")
        ).build()
    )
    return classBuilder.build()
}