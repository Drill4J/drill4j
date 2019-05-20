package com.epam.drill.core.callbacks.singlestep

import com.epam.drill.jvmapi.getDeclaringClassName
import com.epam.drill.jvmapi.getName
import com.epam.drill.jvmapi.toJLocation
import jvmapi.*
import kotlinx.cinterop.memScoped

@Suppress("unused", "UNUSED_PARAMETER")
@CName("jvmtiEventSingleStepEvent")
fun singleStep(jmvtiEnv: jvmtiEnv?, jniEnv: JNIEnv?, thread: jthread?, method: jmethodID?, location: jlocation) {
    val declaringClassName = method?.getDeclaringClassName()
    if (declaringClassName!!.contains("Applicationa") || declaringClassName == "Lorg/somevendor/petproject/test/A")
        processOneStep(thread, method, location)
}

fun processOneStep(
    @Suppress("UNUSED_PARAMETER") thread: jthread?,
    method: jmethodID?,
    loc: jlocation
) = memScoped {
    @Suppress("UNUSED_VARIABLE") val currentLocation = loc.toJLocation(method)
    @Suppress("UNUSED_VARIABLE") val currentMethod = method?.getName()

//    val maxCountOfFrame = 1
//    val sb = StringBuilder()
//    frameWalker(thread, maxCountOfFrame) {
//        val className = getClassName()
//
//
//        if (className == null || className.startsWith("Ljava/") || className.startsWith("Lsun/"))
//            return@frameWalker
//
//        iterateLocalVariables {
//            val manag = TypeManager(thread, currentDepth)
//            val vLi = manag.pr(this, sb)
//            sb.appendln(vLi)
//        }
//    }
}