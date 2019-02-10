package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.primitives
import com.epam.kjni.build.core.utils.typeMappings
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import org.apache.bcel.Const
import kotlin.reflect.KClass


fun generatePrimitiveMethods(className: String): FunSpec {
    val builder = FunSpec.builder("toPrimitive")

    builder.returns(typeMappings[className] as KClass<*>)

//    val className = str[sn] ?: ClassName("com.epam.kjni.core.util", "CallJavaObjectMethod")
    val cn = str[className]!!
    val s = oh[className]!!
    builder
        .addStatement("memScoped{")
        .addStatement("val methodName = \"${s.second}\".cstr.getPointer(this)")
        .addStatement("val methodSignature = \"${s.first}\".cstr.getPointer(this)")
        .addStatement(
            "return %T(\n" +
                    "            javaObject,\n" +
                    "            javaClass,\n" +
                    "            methodName,\n" +
                    "            methodSignature\n" +
                    "        )()", cn
        )
        .addStatement("}")
//        .addStatement("val methodSignature = \"${it.signature}\".cstr.getPointer(%T())", arena)
//
//        .addStatement(

//        )
    return builder.build()

}

private val str = mapOf(
    "boolean" to ClassName("com.epam.kjni.core.util", "CallJavaBooleanMethod"),
    "byte" to ClassName("com.epam.kjni.core.util", "CallJavaByteMethod"),
    "char" to ClassName("com.epam.kjni.core.util", "CallJavaCharMethod"),
    "double" to ClassName("com.epam.kjni.core.util", "CallJavaDoubleMethod"),
    "float" to ClassName("com.epam.kjni.core.util", "CallJavaFloatMethod"),
    "int" to ClassName("com.epam.kjni.core.util", "CallJavaIntMethod"),
    "long" to ClassName("com.epam.kjni.core.util", "CallJavaLongMethod"),
    "short" to ClassName("com.epam.kjni.core.util", "CallJavaShortMethod"),
    "void" to ClassName("com.epam.kjni.core.util", "CallJavaVoidMethod"),
    "java.lang.Boolean" to ClassName("com.epam.kjni.core.util", "CallJavaBooleanMethod"),
    "java.lang.Byte" to ClassName("com.epam.kjni.core.util", "CallJavaByteMethod"),
    "java.lang.Character" to ClassName("com.epam.kjni.core.util", "CallJavaCharMethod"),
    "java.lang.Double" to ClassName("com.epam.kjni.core.util", "CallJavaDoubleMethod"),
    "java.lang.Float" to ClassName("com.epam.kjni.core.util", "CallJavaFloatMethod"),
    "java.lang.Integer" to ClassName("com.epam.kjni.core.util", "CallJavaIntMethod"),
    "java.lang.Long" to ClassName("com.epam.kjni.core.util", "CallJavaLongMethod"),
    "java.lang.Short" to ClassName("com.epam.kjni.core.util", "CallJavaShortMethod")
)

private val oh = mapOf(
    "java.lang.Boolean" to ("()Z" to "booleanValue"),
    "java.lang.Byte" to ("()B" to "byteValue"),
    "java.lang.Character" to ("()C" to "charValue"),
    "java.lang.Double" to ("()D" to "doubleValue"),
    "java.lang.Float" to ("()F" to "floatValue"),
    "java.lang.Integer" to ("()I" to "intValue"),
    "java.lang.Long" to ("()J" to "longValue"),
    "java.lang.Short" to ("()S" to "shortValue")

)
