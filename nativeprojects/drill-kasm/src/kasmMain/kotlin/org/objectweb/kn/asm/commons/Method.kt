/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.kn.asm.commons

import org.objectweb.kn.asm.Type

/**
 * A named method descriptor.
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 */
class Method
/**
 * Creates a new [Method].
 *
 * @param name
 * the method's name.
 * @param desc
 * the method's descriptor.
 */
    (
    /**
     * The method name.
     */
    /**
     * Returns the name of the method described by this object.
     *
     * @return the name of the method described by this object.
     */
    val name: String,
    /**
     * The method descriptor.
     */
    /**
     * Returns the descriptor of the method described by this object.
     *
     * @return the descriptor of the method described by this object.
     */
    val descriptor: String
) {

    /**
     * Returns the return type of the method described by this object.
     *
     * @return the return type of the method described by this object.
     */
    val returnType: Type
        get() = Type.getReturnType(descriptor)

    /**
     * Returns the argument types of the method described by this object.
     *
     * @return the argument types of the method described by this object.
     */
    val argumentTypes: Array<Type>
        get() = Type.getArgumentTypes(descriptor)

    /**
     * Creates a new [Method].
     *
     * @param name
     * the method's name.
     * @param returnType
     * the method's return type.
     * @param argumentTypes
     * the method's argument types.
     */
    constructor(
        name: String, returnType: Type,
        argumentTypes: Array<Type>
    ) : this(name, Type.getMethodDescriptor(returnType, *argumentTypes)) {
    }

    override fun toString(): String {
        return name + descriptor
    }

    override fun equals(o: Any?): Boolean {
        if (o !is Method) {
            return false
        }
        val other = o as Method?
        return name == other!!.name && descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        return name.hashCode() xor descriptor.hashCode()
    }

    companion object {

        /**
         * Maps primitive Java type names to their descriptors.
         */
        private val DESCRIPTORS: MutableMap<String, String>

        init {
            DESCRIPTORS = HashMap()
            DESCRIPTORS["void"] = "V"
            DESCRIPTORS["byte"] = "B"
            DESCRIPTORS["char"] = "C"
            DESCRIPTORS["double"] = "D"
            DESCRIPTORS["float"] = "F"
            DESCRIPTORS["int"] = "I"
            DESCRIPTORS["long"] = "J"
            DESCRIPTORS["short"] = "S"
            DESCRIPTORS["boolean"] = "Z"
        }

        /**
         * Creates a new [Method].
         *
         * @param m
         * a java.lang.reflect method descriptor
         * @return a [Method] corresponding to the given Java method
         * declaration.
         */
//        fun getMethod(m: java.lang.reflect.Method): Method {
//            return Method(m.name, Type.getMethodDescriptor(m))
//        }

        /**
         * Creates a new [Method].
         *
         * @param c
         * a java.lang.reflect constructor descriptor
         * @return a [Method] corresponding to the given Java constructor
         * declaration.
         */
//        fun getMethod(c: java.lang.reflect.Constructor<*>): Method {
//            return Method("<init>", Type.getConstructorDescriptor(c))
//        }

        /**
         * Returns a [Method] corresponding to the given Java method
         * declaration.
         *
         * @param method
         * a Java method declaration, without argument names, of the form
         * "returnType name (argumentType1, ... argumentTypeN)", where
         * the types are in plain Java (e.g. "int", "float",
         * "java.util.List", ...). Classes of the java.lang package may
         * be specified by their unqualified name, depending on the
         * defaultPackage argument; all other classes names must be fully
         * qualified.
         * @param defaultPackage
         * true if unqualified class names belong to the default package,
         * or false if they correspond to java.lang classes. For instance
         * "Object" means "Object" if this option is true, or
         * "java.lang.Object" otherwise.
         * @return a [Method] corresponding to the given Java method
         * declaration.
         * @throws IllegalArgumentException
         * if `method` could not get parsed.
         */
        @Throws(IllegalArgumentException::class)
        fun getMethod(
            method: String,
            defaultPackage: Boolean = false
        ): Method {
            val space = method.indexOf(' ')
            var start = method.indexOf('(', space) + 1
            val end = method.indexOf(')', start)
            if (space == -1 || start == -1 || end == -1) {
                throw IllegalArgumentException()
            }
            val returnType = method.substring(0, space)
            val methodName = method.substring(space + 1, start - 1).trim { it <= ' ' }
            val sb = StringBuilder()
            sb.append('(')
            var p: Int
            do {
                val s: String
                p = method.indexOf(',', start)
                if (p == -1) {
                    s = map(method.substring(start, end).trim { it <= ' ' }, defaultPackage)
                } else {
                    s = map(method.substring(start, p).trim { it <= ' ' }, defaultPackage)
                    start = p + 1
                }
                sb.append(s)
            } while (p != -1)
            sb.append(')')
            sb.append(map(returnType, defaultPackage))
            return Method(methodName, sb.toString())
        }

        private fun map(type: String, defaultPackage: Boolean): String {
            if ("" == type) {
                return type
            }

            val sb = StringBuilder()
            var index = 0
            while ({ index = type.indexOf("[]", index) + 1;index }() > 0) {
                sb.append('[')
            }

            val t = type.substring(0, type.length - sb.length * 2)
            val desc = DESCRIPTORS[t]
            if (desc != null) {
                sb.append(desc)
            } else {
                sb.append('L')
                if (t.indexOf('.') < 0) {
                    if (!defaultPackage) {
                        sb.append("java/lang/")
                    }
                    sb.append(t)
                } else {
                    sb.append(t.replace('.', '/'))
                }
                sb.append(';')
            }
            return sb.toString()
        }
    }
}
