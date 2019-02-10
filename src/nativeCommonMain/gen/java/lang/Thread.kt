package java.lang

import com.epam.kjni.core.GlobState.env
import com.epam.kjni.core.GlobState.jni
import com.epam.kjni.core.Synthetic
import com.epam.kjni.core.util.CallJavaVoidMethod
import com.epam.kjni.core.util.JavaConstructor
import com.epam.kjni.core.util.getJString
import com.epam.kjni.core.util.getKString
import com.epam.kjni.core.util.toJObjectArray
import java.security.AccessControlContext
import jvmapi.jclass
import jvmapi.jobject
import kotlin.Long
import kotlin.String
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.free
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

class Thread : Synthetic {
    override lateinit var javaObject: jobject

    lateinit var javaClass: jclass

    val className: String = "java/lang/Thread"

    constructor() {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf())
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "()V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: Runnable) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "(Ljava/lang/Runnable;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: Runnable, cp1: AccessControlContext) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature =
                "(Ljava/lang/Runnable;Ljava/security/AccessControlContext;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: ThreadGroup, cp1: Runnable) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: String) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "(Ljava/lang/String;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: ThreadGroup, cp1: String) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "(Ljava/lang/ThreadGroup;Ljava/lang/String;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: Runnable, cp1: String) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature = "(Ljava/lang/Runnable;Ljava/lang/String;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(
        cp0: ThreadGroup,
        cp1: Runnable,
        cp2: String
    ) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false),com.epam.kjni.core.util.X(cp2, false)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature =
                "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }

    constructor(
        cp0: ThreadGroup,
        cp1: Runnable,
        cp2: String,
        cp3: Long
    ) {
        memScoped {
        javaClass = jni.FindClass!!(env, className.cstr.getPointer(this))!!
        val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0,
                false),com.epam.kjni.core.util.X(cp1, false),com.epam.kjni.core.util.X(cp2,
                false),com.epam.kjni.core.util.X(cp3, true)))
        val methodName = "<init>".cstr.getPointer(this)
        val methodSignature =
                "(Ljava/lang/ThreadGroup;Ljava/lang/Runnable;Ljava/lang/String;J)V".cstr.getPointer(this)
        val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
        javaObject = jni.NewObjectA!!(env, javaClass, jconstructor, toJObjectArray)!!
        nativeHeap.free(toJObjectArray)
        }
    }
}
