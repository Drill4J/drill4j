package java.security

import com.epam.kjni.core.GlobState.env
import com.epam.kjni.core.GlobState.jni
import com.epam.kjni.core.util.CallJavaVoidMethod
import com.epam.kjni.core.util.getJString
import com.epam.kjni.core.util.getKString
import com.epam.kjni.core.util.toJObjectArray
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

class AccessControlContext {
    val className: String = "java/security/AccessControlContext"
}
