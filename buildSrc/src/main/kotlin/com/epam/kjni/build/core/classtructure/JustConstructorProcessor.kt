package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.justTheClass
import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.primitives
import com.epam.kjni.build.core.utils.typeMappings
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.Type
import java.io.File
import kotlin.reflect.KClass

fun justGenerateDefaultWrapConstructors(
    constructors: List<Method>,
    outputDir: File
): List<FunSpec> {
    return constructors.mapIndexed { index, method ->
        val constructorBuilder = FunSpec.constructorBuilder()
        val argumentTypes: Array<out Type> = method.argumentTypes


        argumentTypes.forEachIndexed { i, x ->
            val className = getClassName(x)
            val kClass1: KClass<out Any>? = typeMappings[className]
            if (kClass1 != null) {
                val kClass = kClass1 as KClass<*>
                constructorBuilder.addParameter("cp$i", kClass)
            } else {
                val split = className.split(".")
                val clName = split.last()
                val packageName = split.dropLast(1).joinToString(separator = ".")
                justTheClass(packageName, clName, outputDir)
                constructorBuilder.addParameter("cp$i", ClassName(packageName, clName))
            }

        }
        val joinToString = method.argumentTypes.mapIndexed { i, x -> i to x }.joinToString(",") {
            "com.epam.kjni.core.util.X(cp${it.first}, ${primitives.contains(
                getClassName(it.second)
            )})"
        }

        constructorBuilder.addStatement("memScoped {")
            .addStatement("javaClass = FindClass(className)!!")
            .addStatement("val toJObjectArray = toJObjectArray(arrayOf($joinToString))")
            .addStatement("val methodName = \"${method.name}\"")
            .addStatement("val methodSignature = \"${method.signature}\"")
            .addStatement("val jconstructor = %T(javaClass, methodName, methodSignature).getMethod()", javaConstructor)
            .addStatement("javaObject = NewObjectA(javaClass, jconstructor, toJObjectArray)!!")
            .addStatement("nativeHeap.free(toJObjectArray)").addStatement("}")
        constructorBuilder.build()
    }
}
