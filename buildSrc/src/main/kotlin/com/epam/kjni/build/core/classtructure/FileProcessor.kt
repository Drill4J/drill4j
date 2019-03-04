package com.epam.kjni.build.core.classtructure

import com.squareup.kotlinpoet.FileSpec

fun createFile(packageName: String, fileName: String) = FileSpec.builder(packageName, fileName)

    .addImport(
        "kotlinx.cinterop", "alloc",
        "cstr",
        "free",
        "invoke",
        "nativeHeap",
        "memScoped",
        "pointed",
        "ptr",
        "value",
        "toKString"
    ).addImport(
        "com.epam.kjni.core.util", "getJString",
        "getKString",
        "toJObjectArray",
        "CallJavaVoidMethod"
    )
    .addImport(
        "jvmapi", "FindClass", "NewObjectA"
    )


