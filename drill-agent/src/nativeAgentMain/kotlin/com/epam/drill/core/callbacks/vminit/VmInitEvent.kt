@file:Suppress("unused")

package com.epam.drill.core.callbacks.vminit

import com.epam.drill.core.ws.startWs
import com.epam.drill.logger.DLogger
import com.soywiz.klogger.Logger
import com.soywiz.kmem.buildByteArray
import drillInternal.getLoadedClass
import jvmapi.JNIEnvVar
import jvmapi.jthread
import jvmapi.jvmtiEnvVar
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.objectweb.kn.asm.ClassReader
import platform.posix.pthread_self
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


val initLogger: Logger
    get() = DLogger("initLogger")

@ExperimentalUnsignedTypes
@Suppress("UNUSED_PARAMETER")
@CName("jvmtiEventVMInitEvent")
fun jvmtiEventVMInitEvent(env: CPointer<jvmtiEnvVar>?, jniEnv: CPointer<JNIEnvVar>?, thread: jthread?) = runBlocking {
    startWs()
    println("WM IS INIT")
    Worker.start(true).execute(TransferMode.UNSAFE, {}) {
        runBlocking {

            launch {
                while (true) {
                    delay(5)
//                    getLoadedClass()?.apply {
//                        try {
//                            val pointed = this.pointed
//                            val classDataLen = pointed.size
//                            val classData = pointed.array!!
//
//                            val bytes = buildByteArray {
//                                for (i in 0 until classDataLen) {
//                                    append(classData[i].toByte())
//                                }
//                            }
//                            println(bytes)
//                        }catch (ex:Throwable){
//                            ex.printStackTrace()
//                        }
//
////                        println(this)
//                    }
                }
            }
        }
    }
//    val findClass: jclass? = FindClass("com/epam/drill/instr/ASM")
//    println(findClass)
//    val getStaticFieldID: jfieldID? = GetStaticFieldID(findClass, "INSTANCE", "Lcom/epam/drill/instr/ASM;")
//    val getStaticObjectField: jobject? = GetStaticObjectField(findClass, getStaticFieldID)
//    val getMethodID: jmethodID? = GetMethodID(findClass, "emulate", "()[B")
////
//    di.singleton(findClass!! as Any)
//    di.singleton(getStaticObjectField!! as Any)
//    di.singleton(getMethodID!! as Any)
    println(pthread_self())
}

