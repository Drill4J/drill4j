package java.lang

import com.epam.kjni.core.Synthetic
import com.epam.kjni.core.util.CallJavaShortMethod
import com.epam.kjni.core.util.JavaConstructor
import com.epam.kjni.core.util.toJObjectArray
import jvmapi.FindClass
import jvmapi.NewObjectA
import jvmapi.jclass
import jvmapi.jobject
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap

class Short : Synthetic {
    override lateinit var javaObject: jobject

    lateinit var javaClass: jclass

    val className: String = "java/lang/Short"

    constructor(cp0: kotlin.Short) {
        memScoped {
            javaClass = FindClass(className)!!
            val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0, true)))
            val methodName = "<init>"
            val methodSignature = "(S)V"
            val jconstructor = JavaConstructor(javaClass, methodName, methodSignature).getMethod()
            javaObject = NewObjectA(javaClass, jconstructor, toJObjectArray)!!
            nativeHeap.free(toJObjectArray)
        }
    }

    constructor(cp0: String) {
        memScoped {
            javaClass = FindClass(className)!!
            val toJObjectArray = toJObjectArray(arrayOf(com.epam.kjni.core.util.X(cp0, false)))
            val methodName = "<init>"
            val methodSignature = "(Ljava/lang/String;)V"
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

    fun toPrimitive(): kotlin.Short {
        memScoped {
            val methodName = "shortValue"
            val methodSignature = "()S"
            return CallJavaShortMethod(
                javaObject,
                javaClass,
                methodName,
                methodSignature
            )()
        }
    }
}
