package com.epam.kjni.build.core.classtructure

import com.epam.kjni.build.core.utils.getClassName
import com.epam.kjni.build.core.utils.primitives
import com.epam.kjni.build.core.utils.typeMappings
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import org.apache.bcel.Const
import org.apache.bcel.classfile.Method
import kotlin.reflect.KClass


fun generateMethods(methods: List<Method>): List<FunSpec> {
    val mapIndexed = methods.mapIndexed { index, it ->
        val retType = it.returnType
        val isVoid = retType.type == Const.T_VOID
        val sn = getClassName(retType)
        val builder = FunSpec.builder(it.name)
        it.argumentTypes.forEachIndexed { i, x ->
            val className = getClassName(x)
            val kClass = typeMappings[className] as KClass<*>
            builder.addParameter("p$i", kClass)
        }
        if (!isVoid)
            builder.returns(typeMappings[sn] as KClass<*>)


        val className = str[sn]

        val joinToString = it.argumentTypes.mapIndexed { i, x -> i to x }
            .joinToString(",") { "com.epam.kjni.core.util.X(p${it.first}, ${primitives.contains(getClassName(it.second))})" }

        builder
            .addStatement("val methodName = \"${it.name}\"")
            .addStatement("val methodSignature = \"${it.signature}\"")

        if (className != null)
            builder.addStatement(
                "return %T(\n" +
                        "            javaObject,\n" +
                        "            javaClass,\n" +
                        "            methodName,\n" +
                        "            methodSignature\n" +
                        "        )($joinToString)", className
            )
        else if ("java.lang.String" == sn) {
            builder.addStatement(
                "var qs = %T(\n" +
                        "            javaObject,\n" +
                        "            javaClass,\n" +
                        "            methodName,\n" +
                        "            methodSignature\n" +
                        "        )($joinToString)", ClassName("com.epam.kjni.core.util", "CallJavaObjectMethod")
            )

            builder.addStatement(
                "return getKString(qs)"
            )
        } else {
            builder.addStatement(
                "return ${sn}(%T(\n" +
                        "            javaObject,\n" +
                        "            javaClass,\n" +
                        "            methodName,\n" +
                        "            methodSignature\n" +
                        "        )($joinToString)).toPrimitive()",
                ClassName("com.epam.kjni.core.util", "CallJavaObjectMethod")
            )
        }
        builder.build()
    }

    return mapIndexed
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
    "void" to ClassName("com.epam.kjni.core.util", "CallJavaVoidMethod")

)