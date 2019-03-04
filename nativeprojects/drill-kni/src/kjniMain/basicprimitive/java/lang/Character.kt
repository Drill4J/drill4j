package java.lang

import com.epam.kjni.core.Synthetic
import com.epam.kjni.core.util.CallJavaCharMethod
import com.epam.kjni.core.util.JavaConstructor
import com.epam.kjni.core.util.toJObjectArray
import jvmapi.FindClass
import jvmapi.NewObjectA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap

class Character : Synthetic {
    override lateinit var javaObject: jobject

    lateinit var javaClass: jclass

    val className: String = "java/lang/Character"

    constructor(cp0: Char) {
        memScoped {
            javaClass = FindClass(className)!!
            val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0, true)))
            val methodName = "<init>"
            val methodSignature = "(C)V"
            val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
            javaObject = NewObjectA(javaClass, jconstructor, toJObjectArray)!!
            nativeHeap.free(toJObjectArray)
        }
    }

    constructor(jobj: jobject) {
        memScoped {
            javaClass = FindClass(className)!!
            javaObject = jobj
        }
    }

    fun toPrimitive(): Char {
        memScoped {
            val methodName = "charValue"
            val methodSignature = "()C"
            return CallJavaCharMethod(
                javaObject,
                javaClass,
                methodName,
                methodSignature
            )()
        }
    }
}
