package com.epam.kjni.build.core.utils

import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.classfile.Method
import org.apache.bcel.generic.BasicType
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.Type

fun getClassName(s: Type): String {
    return when (s) {
        is BasicType -> s.toString()
        is ObjectType -> s.className
        else -> "ss"
    }
}

val primitives = setOf(
    "int",
    "double",
    "byte",
    "short",
    "long",
    "float",
    "boolean",
    "char"
)

val typeMappings = mapOf(
    "int" to Int::class,
    "double" to Double::class,
    "byte" to Byte::class,
    "short" to Short::class,
    "long" to Long::class,
    "float" to Float::class,
    "boolean" to Boolean::class,
    "char" to Char::class,

    "java.lang.Integer"     to Int::class,
    "java.lang.Double"      to Double::class,
    "java.lang.Byte"        to Byte::class,
    "java.lang.Short"       to Short::class,
    "java.lang.Long"        to Long::class,
    "java.lang.Float"       to Float::class,
    "java.lang.Boolean"     to Boolean::class,
    "java.lang.Character"   to Char::class,
    "java.lang.Object"      to Any::class,

    "java.lang.String" to String::class,

//        collections
    "java.util.List" to List::class,
    "java.util.Map" to Map::class,
    "java.util.Set" to Set::class
)

fun getClassName(metaOfClass: JavaClass) =
    metaOfClass.className.replace(metaOfClass.packageName, "").replace(".", "")

fun Array<Method>.getConstructors(): List<Method> {
    return this.partition { it.name != "<init>" }.second
}

fun Array<Method>.getMethods(): List<Method> {
    return this.partition { it.name != "<init>" }.first.filter { it.isPublic }
}