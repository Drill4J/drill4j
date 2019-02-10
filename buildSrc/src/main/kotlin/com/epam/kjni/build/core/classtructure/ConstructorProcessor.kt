package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.primitives
import com.epam.kjni.build.core.utils.typeMappings
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.Type
import kotlin.reflect.KClass

fun generateDefaultWrapConstructors(constructors: List<Method>): List<FunSpec> {
    return constructors.filter {
 true
    }.mapIndexed { index, method ->
        val constructorBuilder = FunSpec.constructorBuilder()
        val argumentTypes: Array<out Type> = method.argumentTypes


        argumentTypes.forEachIndexed { i, x ->
            val kClass1: KClass<out Any>? = typeMappings[getClassName(x)]
            //            if (kClass1 != null) {
            val kClass = kClass1 as KClass<*>
            constructorBuilder.addParameter("cp$i", kClass)
            //            }
        }
        val joinToString = method.argumentTypes.mapIndexed { i, x -> i to x }.joinToString(",") {
            "com.epam.kjni.core.util.X(cp${it.first}, ${primitives.contains(
                getClassName(it.second)
            )})"
        }

        constructorBuilder.addStatement("memScoped {").addStatement("javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!")
            .addStatement("val toJObjectArray = toJObjectArray(arrayOf($joinToString))")
            .addStatement("val methodName = \"${method.name}\".cstr.getPointer(this)")
            .addStatement("val methodSignature = \"${method.signature}\".cstr.getPointer(this)")
            .addStatement("val jconstructor = %T(javaClass, methodName, methodSignature).getMethod()", javaConstructor)
            .addStatement("javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!")
            //            .addStatement("jni.DeleteLocalRef!!(env, javaObject)")
            //            .addStatement("jni.DeleteLocalRef!!(env, javaClass)")
            .addStatement("nativeHeap.free(toJObjectArray)").addStatement("}")
        constructorBuilder.build()
    }
}

fun generateDefaultWrapConstructors(): FunSpec {
    val constructorBuilder = FunSpec.constructorBuilder().addParameter("jobj", jobject).addStatement("memScoped {")
        .addStatement("javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!").addStatement("javaObject = jobj")
        .addStatement("}")
    return constructorBuilder.build()
}