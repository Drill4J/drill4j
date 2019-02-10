/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2013 INRIA, France Telecom
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
 * The path to a type argument, wildcard bound, array element type, or static
 * inner type within an enclosing type.
 *
 * @author Eric Bruneton
 */
class TypePath
/**
 * Creates a new type path.
 *
 * @param b
 * the byte array containing the type path in Java class file
 * format.
 * @param offset
 * the offset of the first byte of the type path in 'b'.
 */
internal constructor(
        /**
         * The byte array where the path is stored, in Java class file format.
         */
        internal var b: ByteArray,
        /**
         * The offset of the first byte of the type path in 'b'.
         */
        internal var offset: Int) {

    /**
     * Returns the length of this path.
     *
     * @return the length of this path.
     */
    val length: Int
        get() = b[offset].toInt()

    /**
     * Returns the value of the given step of this path.
     *
     * @param index
     * an index between 0 and [.getLength], exclusive.
     * @return [ARRAY_ELEMENT][.ARRAY_ELEMENT], [         INNER_TYPE][.INNER_TYPE], [WILDCARD_BOUND][.WILDCARD_BOUND], or
     * [TYPE_ARGUMENT][.TYPE_ARGUMENT].
     */
    fun getStep(index: Int): Int {
        return b[offset + 2 * index + 1].toInt()
    }

    /**
     * Returns the index of the type argument that the given step is stepping
     * into. This method should only be used for steps whose value is
     * [TYPE_ARGUMENT][.TYPE_ARGUMENT].
     *
     * @param index
     * an index between 0 and [.getLength], exclusive.
     * @return the index of the type argument that the given step is stepping
     * into.
     */
    fun getStepArgument(index: Int): Int {
        return b[offset + 2 * index + 2].toInt()
    }

    /**
     * Returns a string representation of this type path. [ ARRAY_ELEMENT][.ARRAY_ELEMENT] steps are represented with '[', [ INNER_TYPE][.INNER_TYPE] steps with '.', [WILDCARD_BOUND][.WILDCARD_BOUND] steps
     * with '*' and [TYPE_ARGUMENT][.TYPE_ARGUMENT] steps with their type
     * argument index in decimal form.
     */
    override fun toString(): String {
        val length = length
        val result = StringBuilder(length * 2)
        for (i in 0 until length) {
            when (getStep(i)) {
                ARRAY_ELEMENT -> result.append('[')
                INNER_TYPE -> result.append('.')
                WILDCARD_BOUND -> result.append('*')
                TYPE_ARGUMENT -> result.append(getStepArgument(i))
                else -> result.append('_')
            }
        }
        return result.toString()
    }

    companion object {

        /**
         * A type path step that steps into the element type of an array type. See
         * [getStep][.getStep].
         */
        val ARRAY_ELEMENT = 0

        /**
         * A type path step that steps into the nested type of a class type. See
         * [getStep][.getStep].
         */
        val INNER_TYPE = 1

        /**
         * A type path step that steps into the bound of a wildcard type. See
         * [getStep][.getStep].
         */
        val WILDCARD_BOUND = 2

        /**
         * A type path step that steps into a type argument of a generic type. See
         * [getStep][.getStep].
         */
        val TYPE_ARGUMENT = 3

        /**
         * Converts a type path in string form, in the format used by
         * [.toString], into a TypePath object.
         *
         * @param typePath
         * a type path in string form, in the format used by
         * [.toString]. May be null or empty.
         * @return the corresponding TypePath object, or null if the path is empty.
         */
        fun fromString(typePath: String?): TypePath? {
            if (typePath == null || typePath.length == 0) {
                return null
            }
            val n = typePath.length
            val out = ByteVector(n)
            out.putByte(0)
            var i = 0
            while (i < n) {
                var c = typePath[i++]
                if (c == '[') {
                    out.put11(ARRAY_ELEMENT, 0)
                } else if (c == '.') {
                    out.put11(INNER_TYPE, 0)
                } else if (c == '*') {
                    out.put11(WILDCARD_BOUND, 0)
                } else if (c >= '0' && c <= '9') {
                    var typeArg = c - '0'
                    while (i < n && {c = typePath[i];c}() >= '0' && c <= '9') {
                        typeArg = typeArg * 10 + c.toInt() - '0'.toInt()
                        i += 1
                    }
                    out.put11(TYPE_ARGUMENT, typeArg)
                }
            }
            out.data[0] = (out.length / 2).toByte()
            return TypePath(out.data, 0)
        }
    }
}
