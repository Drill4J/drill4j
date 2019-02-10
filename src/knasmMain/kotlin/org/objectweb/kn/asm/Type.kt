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
package org.objectweb.kn.asm


/**
 * A Java field or method type. This class can be used to make it easier to
 * manipulate type and method descriptors.
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
class Type
// ------------------------------------------------------------------------
// Constructors
// ------------------------------------------------------------------------

/**
 * Constructs a reference type.
 *
 * @param sort
 * the sort of the reference type to be constructed.
 * @param buf
 * a buffer containing the descriptor of the previous type.
 * @param off
 * the offset of this descriptor in the previous buffer.
 * @param len
 * the length of this descriptor.
 */
private constructor(
        // ------------------------------------------------------------------------
        // Fields
        // ------------------------------------------------------------------------

        /**
         * The sort of this Java type.
         */
        // ------------------------------------------------------------------------
        // Accessors
        // ------------------------------------------------------------------------

        /**
         * Returns the sort of this Java type.
         *
         * @return [VOID][.VOID], [BOOLEAN][.BOOLEAN], [CHAR][.CHAR],
         * [BYTE][.BYTE], [SHORT][.SHORT], [INT][.INT],
         * [FLOAT][.FLOAT], [LONG][.LONG], [DOUBLE][.DOUBLE],
         * [ARRAY][.ARRAY], [OBJECT][.OBJECT] or [         METHOD][.METHOD].
         */
        val sort: Int,
        /**
         * A buffer containing the internal name of this Java type. This field is
         * only used for reference types.
         */
        private val buf: CharArray?,
        /**
         * The offset of the internal name of this Java type in [buf][.buf] or,
         * for primitive types, the size, descriptor and getOpcode offsets for this
         * type (byte 0 contains the size, byte 1 the descriptor, byte 2 the offset
         * for IALOAD or IASTORE, byte 3 the offset for all other instructions).
         */
        private val off: Int,
        /**
         * The length of the internal name of this Java type.
         */
        private val len: Int) {

    /**
     * Returns the number of dimensions of this array type. This method should
     * only be used for an array type.
     *
     * @return the number of dimensions of this array type.
     */
    val dimensions: Int
        get() {
            var i = 1
            while (buf!![off + i] == '[') {
                ++i
            }
            return i
        }

    /**
     * Returns the type of the elements of this array type. This method should
     * only be used for an array type.
     *
     * @return Returns the type of the elements of this array type.
     */
    val elementType: Type
        get() = getType(buf!!, off + dimensions)

    /**
     * Returns the binary name of the class corresponding to this type. This
     * method must not be used on method types.
     *
     * @return the binary name of the class corresponding to this type.
     */
    val className: String?
        get() {
            when (sort) {
                VOID -> return "void"
                BOOLEAN -> return "boolean"
                CHAR -> return "char"
                BYTE -> return "byte"
                SHORT -> return "short"
                INT -> return "int"
                FLOAT -> return "float"
                LONG -> return "long"
                DOUBLE -> return "double"
                ARRAY -> {
                    val b = StringBuilder(elementType.className!!)
                    for (i in dimensions downTo 1) {
                        b.append("[]")
                    }
                    return b.toString()
                }
                OBJECT -> return String(buf!!, off, len).replace('/', '.')
                else -> return null
            }
        }

    /**
     * Returns the internal name of the class corresponding to this object or
     * array type. The internal name of a class is its fully qualified name (as
     * returned by Class.getName(), where '.' are replaced by '/'. This method
     * should only be used for an object or array type.
     *
     * @return the internal name of the class corresponding to this object type.
     */
    val internalName: String
        get() = String(buf!!, off, len)

    /**
     * Returns the argument types of methods of this type. This method should
     * only be used for method types.
     *
     * @return the argument types of methods of this type.
     */
    val argumentTypes: Array<Type>
        get() = getArgumentTypes(descriptor)




    /**
     * Returns the size of the arguments and of the return value of methods of
     * this type. This method should only be used for method types.
     *
     * @return the size of the arguments (plus one for the implicit this
     * argument), argSize, and the size of the return value, retSize,
     * packed into a single int i = <tt>(argSize << 2) | retSize</tt>
     * (argSize is therefore equal to <tt>i >> 2</tt>, and retSize to
     * <tt>i & 0x03</tt>).
     */
    val argumentsAndReturnSizes: Int
        get() = getArgumentsAndReturnSizes(descriptor)

    // ------------------------------------------------------------------------
    // Conversion to type descriptors
    // ------------------------------------------------------------------------

    /**
     * Returns the descriptor corresponding to this Java type.
     *
     * @return the descriptor corresponding to this Java type.
     */
    val descriptor: String
        get() {
            val buf = StringBuilder()
            getDescriptor(buf)
            return buf.toString()
        }

    // ------------------------------------------------------------------------
    // Corresponding size and opcodes
    // ------------------------------------------------------------------------

    /**
     * Returns the size of values of this type. This method must not be used for
     * method types.
     *
     * @return the size of values of this type, i.e., 2 for <tt>long</tt> and
     * <tt>double</tt>, 0 for <tt>void</tt> and 1 otherwise.
     */
    // the size is in byte 0 of 'off' for primitive types (buf == null)
    val size: Int
        get() = if (buf == null) off and 0xFF else 1

    /**
     * Appends the descriptor corresponding to this Java type to the given
     * string buffer.
     *
     * @param buf
     * the string buffer to which the descriptor must be appended.
     */
    private fun getDescriptor(buf: StringBuilder) {
        if (this.buf == null) {
            // descriptor is in byte 3 of 'off' for primitive types (buf ==
            // null)
            buf.append((off and -0x1000000).ushr(24).toChar())
        } else if (sort == OBJECT) {
            buf.append('L')
            buf.append(this.buf, off, len)
            buf.append(';')
        } else { // sort == ARRAY || sort == METHOD
            buf.append(this.buf, off, len)
        }
    }

    /**
     * Returns a JVM instruction opcode adapted to this Java type. This method
     * must not be used for method types.
     *
     * @param opcode
     * a JVM instruction opcode. This opcode must be one of ILOAD,
     * ISTORE, IALOAD, IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG,
     * ISHL, ISHR, IUSHR, IAND, IOR, IXOR and IRETURN.
     * @return an opcode that is similar to the given opcode, but adapted to
     * this Java type. For example, if this type is <tt>float</tt> and
     * <tt>opcode</tt> is IRETURN, this method returns FRETURN.
     */
    fun getOpcode(opcode: Int): Int {
        return if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            // the offset for IALOAD or IASTORE is in byte 1 of 'off' for
            // primitive types (buf == null)
            opcode + if (buf == null) off and 0xFF00 shr 8 else 4
        } else {
            // the offset for other instructions is in byte 2 of 'off' for
            // primitive types (buf == null)
            opcode + if (buf == null) off and 0xFF0000 shr 16 else 4
        }
    }

    // ------------------------------------------------------------------------
    // Equals, hashCode and toString
    // ------------------------------------------------------------------------

    /**
     * Tests if the given object is equal to this type.
     *
     * @param o
     * the object to be compared to this type.
     * @return <tt>true</tt> if the given object is equal to this type.
     */
    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Type) {
            return false
        }
        val t = o as Type?
        if (sort != t!!.sort) {
            return false
        }
        if (sort >= ARRAY) {
            if (len != t.len) {
                return false
            }
            var i = off
            var j = t.off
            val end = i + len
            while (i < end) {
                if (buf!![i] != t.buf!![j]) {
                    return false
                }
                i++
                j++
            }
        }
        return true
    }

    /**
     * Returns a hash code value for this type.
     *
     * @return a hash code value for this type.
     */
    override fun hashCode(): Int {
        var hc = 13 * sort
        if (sort >= ARRAY) {
            var i = off
            val end = i + len
            while (i < end) {
                hc = 17 * (hc + buf!![i].toInt())
                i++
            }
        }
        return hc
    }

    /**
     * Returns a string representation of this type.
     *
     * @return the descriptor of this type.
     */
    override fun toString(): String {
        return descriptor
    }

    companion object {

        /**
         * The sort of the <tt>void</tt> type. See [getSort][.getSort].
         */
        val VOID = 0

        /**
         * The sort of the <tt>boolean</tt> type. See [getSort][.getSort].
         */
        val BOOLEAN = 1

        /**
         * The sort of the <tt>char</tt> type. See [getSort][.getSort].
         */
        val CHAR = 2

        /**
         * The sort of the <tt>byte</tt> type. See [getSort][.getSort].
         */
        val BYTE = 3

        /**
         * The sort of the <tt>short</tt> type. See [getSort][.getSort].
         */
        val SHORT = 4

        /**
         * The sort of the <tt>int</tt> type. See [getSort][.getSort].
         */
        val INT = 5

        /**
         * The sort of the <tt>float</tt> type. See [getSort][.getSort].
         */
        val FLOAT = 6

        /**
         * The sort of the <tt>long</tt> type. See [getSort][.getSort].
         */
        val LONG = 7

        /**
         * The sort of the <tt>double</tt> type. See [getSort][.getSort].
         */
        val DOUBLE = 8

        /**
         * The sort of array reference types. See [getSort][.getSort].
         */
        val ARRAY = 9

        /**
         * The sort of object reference types. See [getSort][.getSort].
         */
        val OBJECT = 10

        /**
         * The sort of method types. See [getSort][.getSort].
         */
        val METHOD = 11

        /**
         * The <tt>void</tt> type.
         */
        val VOID_TYPE = Type(
            VOID, null, 'V'.toInt() shl 24
                    or (5 shl 16) or (0 shl 8) or 0, 1
        )

        /**
         * The <tt>boolean</tt> type.
         */
        val BOOLEAN_TYPE = Type(
            BOOLEAN, null, 'Z'.toInt() shl 24
                    or (0 shl 16) or (5 shl 8) or 1, 1
        )

        /**
         * The <tt>char</tt> type.
         */
        val CHAR_TYPE = Type(
            CHAR, null, 'C'.toInt() shl 24
                    or (0 shl 16) or (6 shl 8) or 1, 1
        )

        /**
         * The <tt>byte</tt> type.
         */
        val BYTE_TYPE = Type(
            BYTE, null, 'B'.toInt() shl 24
                    or (0 shl 16) or (5 shl 8) or 1, 1
        )

        /**
         * The <tt>short</tt> type.
         */
        val SHORT_TYPE = Type(
            SHORT, null, 'S'.toInt() shl 24
                    or (0 shl 16) or (7 shl 8) or 1, 1
        )

        /**
         * The <tt>int</tt> type.
         */
        val INT_TYPE = Type(
            INT, null, 'I'.toInt() shl 24
                    or (0 shl 16) or (0 shl 8) or 1, 1
        )

        /**
         * The <tt>float</tt> type.
         */
        val FLOAT_TYPE = Type(
            FLOAT, null, 'F'.toInt() shl 24
                    or (2 shl 16) or (2 shl 8) or 1, 1
        )

        /**
         * The <tt>long</tt> type.
         */
        val LONG_TYPE = Type(
            LONG, null, 'J'.toInt() shl 24
                    or (1 shl 16) or (1 shl 8) or 2, 1
        )

        /**
         * The <tt>double</tt> type.
         */
        val DOUBLE_TYPE = Type(
            DOUBLE, null, 'D'.toInt() shl 24
                    or (3 shl 16) or (3 shl 8) or 2, 1
        )

        /**
         * Returns the Java type corresponding to the given type descriptor.
         *
         * @param typeDescriptor
         * a field or method type descriptor.
         * @return the Java type corresponding to the given type descriptor.
         */
        fun getType(typeDescriptor: String): Type {
            return getType(typeDescriptor.toCharArray(), 0)
        }

        /**
         * Returns the Java type corresponding to the given internal name.
         *
         * @param internalName
         * an internal name.
         * @return the Java type corresponding to the given internal name.
         */
        fun getObjectType(internalName: String): Type {
            val buf = internalName.toCharArray()
            return Type(
                if (buf[0] == '[') ARRAY else OBJECT,
                buf,
                0,
                buf.size
            )
        }

        /**
         * Returns the Java type corresponding to the given method descriptor.
         * Equivalent to `Type.getType(methodDescriptor)`.
         *
         * @param methodDescriptor
         * a method descriptor.
         * @return the Java type corresponding to the given method descriptor.
         */
        fun getMethodType(methodDescriptor: String): Type {
            return getType(methodDescriptor.toCharArray(), 0)
        }

        /**
         * Returns the Java method type corresponding to the given argument and
         * return types.
         *
         * @param returnType
         * the return type of the method.
         * @param argumentTypes
         * the argument types of the method.
         * @return the Java type corresponding to the given argument and return
         * types.
         */
        fun getMethodType(returnType: Type,
                          vararg argumentTypes: Type
        ): Type {
            return getType(
                getMethodDescriptor(
                    returnType,
                    *argumentTypes
                )
            )
        }

        /**
         * Returns the Java type corresponding to the given class.
         *
         * @param c
         * a class.
         * @return the Java type corresponding to the given class.
         */
//        fun getType(c: Class<*>): Type {
//            return if (c.isPrimitive) {
//                if (c == Integer.TYPE) {
//                    INT_TYPE
//                } else if (c == Void.TYPE) {
//                    VOID_TYPE
//                } else if (c == java.lang.Boolean.TYPE) {
//                    BOOLEAN_TYPE
//                } else if (c == java.lang.Byte.TYPE) {
//                    BYTE_TYPE
//                } else if (c == Character.TYPE) {
//                    CHAR_TYPE
//                } else if (c == java.lang.Short.TYPE) {
//                    SHORT_TYPE
//                } else if (c == java.lang.Double.TYPE) {
//                    DOUBLE_TYPE
//                } else if (c == java.lang.Float.TYPE) {
//                    FLOAT_TYPE
//                } else
//                /* if (c == Long.TYPE) */ {
//                    LONG_TYPE
//                }
//            } else {
//                getType(getDescriptor(c))
//            }
//        }

        /**
         * Returns the Java method type corresponding to the given constructor.
         *
         * @param c
         * a [Constructor] object.
         * @return the Java method type corresponding to the given constructor.
         */
//        fun getType(c: Constructor<*>): Type {
//            return getType(getConstructorDescriptor(c))
//        }

        /**
         * Returns the Java method type corresponding to the given method.
         *
         * @param m
         * a [Method] object.
         * @return the Java method type corresponding to the given method.
         */
//        fun getType(m: Method): Type {
//            return getType(getMethodDescriptor(m))
//        }

        /**
         * Returns the Java types corresponding to the argument types of the given
         * method descriptor.
         *
         * @param methodDescriptor
         * a method descriptor.
         * @return the Java types corresponding to the argument types of the given
         * method descriptor.
         */
        fun getArgumentTypes(methodDescriptor: String): Array<Type> {
            val buf = methodDescriptor.toCharArray()
            var off = 1
            var size = 0
            while (true) {
                val car = buf[off++]
                if (car == ')') {
                    break
                } else if (car == 'L') {
                    while (buf[off++] != ';') {
                    }
                    ++size
                } else if (car != '[') {
                    ++size
                }
            }
            val args = arrayOfNulls<Type>(size)
            off = 1
            size = 0
            while (buf[off] != ')') {
                args[size] = getType(buf, off)
                off += args[size]!!.len + if (args[size]!!.sort == OBJECT) 2 else 0
                size += 1
            }
            return args as Array<Type>
        }



        /**
         * Returns the Java type corresponding to the return type of the given
         * method descriptor.
         *
         * @param methodDescriptor
         * a method descriptor.
         * @return the Java type corresponding to the return type of the given
         * method descriptor.
         */
        fun getReturnType(methodDescriptor: String): Type {
            val buf = methodDescriptor.toCharArray()
            return getType(buf, methodDescriptor.indexOf(')') + 1)
        }

        /**
         * Returns the Java type corresponding to the return type of the given
         * method.
         *
         * @param method
         * a method.
         * @return the Java type corresponding to the return type of the given
         * method.
         */
//        fun getReturnType(method: Method): Type {
//            return getType(method.returnType)
//        }

        /**
         * Computes the size of the arguments and of the return value of a method.
         *
         * @param desc
         * the descriptor of a method.
         * @return the size of the arguments of the method (plus one for the
         * implicit this argument), argSize, and the size of its return
         * value, retSize, packed into a single int i =
         * <tt>(argSize << 2) | retSize</tt> (argSize is therefore equal to
         * <tt>i >> 2</tt>, and retSize to <tt>i & 0x03</tt>).
         */
        fun getArgumentsAndReturnSizes(desc: String): Int {
            var n = 1
            var c = 1
            while (true) {
                var car = desc[c++]
                if (car == ')') {
                    car = desc[c]
                    return n shl 2 or if (car == 'V') 0 else if (car == 'D' || car == 'J') 2 else 1
                } else if (car == 'L') {
                    while (desc[c++] != ';') {
                    }
                    n += 1
                } else if (car == '[') {
                    while ({car = desc[c];car}() == '[') {
                        ++c
                    }
                    if (car == 'D' || car == 'J') {
                        n -= 1
                    }
                } else if (car == 'D' || car == 'J') {
                    n += 2
                } else {
                    n += 1
                }
            }
        }

        /**
         * Returns the Java type corresponding to the given type descriptor. For
         * method descriptors, buf is supposed to contain nothing more than the
         * descriptor itself.
         *
         * @param buf
         * a buffer containing a type descriptor.
         * @param off
         * the offset of this descriptor in the previous buffer.
         * @return the Java type corresponding to the given type descriptor.
         */
        private fun getType(buf: CharArray, off: Int): Type {
            var len: Int
            when (buf[off]) {
                'V' -> return VOID_TYPE
                'Z' -> return BOOLEAN_TYPE
                'C' -> return CHAR_TYPE
                'B' -> return BYTE_TYPE
                'S' -> return SHORT_TYPE
                'I' -> return INT_TYPE
                'F' -> return FLOAT_TYPE
                'J' -> return LONG_TYPE
                'D' -> return DOUBLE_TYPE
                '[' -> {
                    len = 1
                    while (buf[off + len] == '[') {
                        ++len
                    }
                    if (buf[off + len] == 'L') {
                        ++len
                        while (buf[off + len] != ';') {
                            ++len
                        }
                    }
                    return Type(ARRAY, buf, off, len + 1)
                }
                'L' -> {
                    len = 1
                    while (buf[off + len] != ';') {
                        ++len
                    }
                    return Type(OBJECT, buf, off + 1, len - 1)
                }
                // case '(':
                else -> return Type(
                    METHOD,
                    buf,
                    off,
                    buf.size - off
                )
            }
        }

        /**
         * Returns the descriptor corresponding to the given argument and return
         * types.
         *
         * @param returnType
         * the return type of the method.
         * @param argumentTypes
         * the argument types of the method.
         * @return the descriptor corresponding to the given argument and return
         * types.
         */
        fun getMethodDescriptor(returnType: Type,
                                vararg argumentTypes: Type
        ): String {
            val buf = StringBuilder()
            buf.append('(')
            for (i in argumentTypes.indices) {
                argumentTypes[i].getDescriptor(buf)
            }
            buf.append(')')
            returnType.getDescriptor(buf)
            return buf.toString()
        }

        // ------------------------------------------------------------------------
        // Direct conversion from classes to type descriptors,
        // without intermediate Type objects
        // ------------------------------------------------------------------------

        /**
         * Returns the internal name of the given class. The internal name of a
         * class is its fully qualified name, as returned by Class.getName(), where
         * '.' are replaced by '/'.
         *
         * @param c
         * an object or array class.
         * @return the internal name of the given class.
         */
//        fun getInternalName(c: Class<*>): String {
//            return c.name.replace('.', '/')
//        }

        /**
         * Returns the descriptor corresponding to the given Java type.
         *
         * @param c
         * an object class, a primitive class or an array class.
         * @return the descriptor corresponding to the given class.
         */
//        fun getDescriptor(c: Class<*>): String {
//            val buf = StringBuilder()
//            getDescriptor(buf, c)
//            return buf.toString()
//        }

        /**
         * Returns the descriptor corresponding to the given constructor.
         *
         * @param c
         * a [Constructor] object.
         * @return the descriptor of the given constructor.
         */
//        fun getConstructorDescriptor(c: Constructor<*>): String {
//            val parameters = c.parameterTypes
//            val buf = StringBuffer()
//            buf.append('(')
//            for (i in parameters.indices) {
//                getDescriptor(buf, parameters[i])
//            }
//            return buf.append(")V").toString()
//        }

        /**
         * Returns the descriptor corresponding to the given method.
         *
         * @param m
         * a [Method] object.
         * @return the descriptor of the given method.
         */
//        fun getMethodDescriptor(m: Method): String {
//            val parameters = m.parameterTypes
//            val buf = StringBuffer()
//            buf.append('(')
//            for (i in parameters.indices) {
//                getDescriptor(buf, parameters[i])
//            }
//            buf.append(')')
//            getDescriptor(buf, m.returnType)
//            return buf.toString()
//        }

        /**
         * Appends the descriptor of the given class to the given string buffer.
         *
         * @param buf
         * the string buffer to which the descriptor must be appended.
         * @param c
         * the class whose descriptor must be computed.
         */
//        private fun getDescriptor(buf: StringBuilder, c: Class<*>) {
//            var d = c
//            while (true) {
//                if (d.isPrimitive) {
//                    val car: Char
//                    if (d == Integer.TYPE) {
//                        car = 'I'
//                    } else if (d == Void.TYPE) {
//                        car = 'V'
//                    } else if (d == Boolean.TYPE) {
//                        car = 'Z'
//                    } else if (d == java.lang.Byte.TYPE) {
//                        car = 'B'
//                    } else if (d == Character.TYPE) {
//                        car = 'C'
//                    } else if (d == java.lang.Short.TYPE) {
//                        car = 'S'
//                    } else if (d == java.lang.Double.TYPE) {
//                        car = 'D'
//                    } else if (d == java.lang.Float.TYPE) {
//                        car = 'F'
//                    } else
//                    /* if (d == Long.TYPE) */ {
//                        car = 'J'
//                    }
//                    buf.append(car)
//                    return
//                } else if (d.isArray) {
//                    buf.append('[')
//                    d = d.componentType
//                } else {
//                    buf.append('L')
//                    val name = d.name
//                    val len = name.length
//                    for (i in 0 until len) {
//                        val car = name[i]
//                        buf.append(if (car == '.') '/' else car)
//                    }
//                    buf.append(';')
//                    return
//                }
//            }
//        }
    }
}
