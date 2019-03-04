package com.epam.kjni.build.core.classtructure

import com.squareup.kotlinpoet.ClassName

val synthetic = ClassName("com.epam.kjni.core", "Synthetic")
val callJavaVoidMethod = ClassName("com.epam.kjni.core.util", "CallJavaVoidMethod")
val javaConstructor = ClassName("com.epam.kjni.core.util", "JavaConstructor")
val jobject = ClassName("jvmapi", "jobject")
val jclass = ClassName("jvmapi", "jclass")
val jvalue = ClassName("jvmapi", "jvalue")
val arena = ClassName("kotlinx.cinterop", "Arena")