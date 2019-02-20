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
 * A dynamically extensible vector of bytes. This class is roughly equivalent to
 * a DataOutputStream on top of a ByteArrayOutputStream, but is more efficient.
 *
 * @author Eric Bruneton
 */
class ByteVector {

    /**
     * The content of this vector.
     */
    internal var data: ByteArray

    /**
     * Actual number of bytes in this vector.
     */
    internal var length: Int = 0

    /**
     * Constructs a new [ByteVector] with a default initial
     * size.
     */
    constructor() {
        data = ByteArray(64)
    }

    /**
     * Constructs a new [ByteVector] with the given initial
     * size.
     *
     * @param initialSize
     * the initial size of the byte vector to be constructed.
     */
    constructor(initialSize: Int) {
        data = ByteArray(initialSize)
    }

    /**
     * Puts a byte into this byte vector. The byte vector is automatically
     * enlarged if necessary.
     *
     * @param b
     * a byte.
     * @return this byte vector.
     */
    fun putByte(b: Int): ByteVector {
        var length = this.length
        if (length + 1 > data.size) {
            enlarge(1)
        }
        data[length++] = b.toByte()
        this.length = length
        return this
    }

    /**
     * Puts two bytes into this byte vector. The byte vector is automatically
     * enlarged if necessary.
     *
     * @param b1
     * a byte.
     * @param b2
     * another byte.
     * @return this byte vector.
     */
    internal fun put11(b1: Int, b2: Int): ByteVector {
        var length = this.length
        if (length + 2 > data.size) {
            enlarge(2)
        }
        val data = this.data
        data[length++] = b1.toByte()
        data[length++] = b2.toByte()
        this.length = length
        return this
    }

    /**
     * Puts a short into this byte vector. The byte vector is automatically
     * enlarged if necessary.
     *
     * @param s
     * a short.
     * @return this byte vector.
     */
    fun putShort(s: Int): ByteVector {
        var length = this.length
        if (length + 2 > data.size) {
            enlarge(2)
        }
        val data = this.data
        data[length++] = s.ushr(8).toByte()
        data[length++] = s.toByte()
        this.length = length
        return this
    }

    /**
     * Puts a byte and a short into this byte vector. The byte vector is
     * automatically enlarged if necessary.
     *
     * @param b
     * a byte.
     * @param s
     * a short.
     * @return this byte vector.
     */
    internal fun put12(b: Int, s: Int): ByteVector {
        var length = this.length
        if (length + 3 > data.size) {
            enlarge(3)
        }
        val data = this.data
        data[length++] = b.toByte()
        data[length++] = s.ushr(8).toByte()
        data[length++] = s.toByte()
        this.length = length
        return this
    }

    /**
     * Puts an int into this byte vector. The byte vector is automatically
     * enlarged if necessary.
     *
     * @param i
     * an int.
     * @return this byte vector.
     */
    fun putInt(i: Int): ByteVector {
        var length = this.length
        if (length + 4 > data.size) {
            enlarge(4)
        }
        val data = this.data
        data[length++] = i.ushr(24).toByte()
        data[length++] = i.ushr(16).toByte()
        data[length++] = i.ushr(8).toByte()
        data[length++] = i.toByte()
        this.length = length
        return this
    }

    /**
     * Puts a long into this byte vector. The byte vector is automatically
     * enlarged if necessary.
     *
     * @param l
     * a long.
     * @return this byte vector.
     */
    fun putLong(l: Long): ByteVector {
        var length = this.length
        if (length + 8 > data.size) {
            enlarge(8)
        }
        val data = this.data
        var i = l.ushr(32).toInt()
        data[length++] = i.ushr(24).toByte()
        data[length++] = i.ushr(16).toByte()
        data[length++] = i.ushr(8).toByte()
        data[length++] = i.toByte()
        i = l.toInt()
        data[length++] = i.ushr(24).toByte()
        data[length++] = i.ushr(16).toByte()
        data[length++] = i.ushr(8).toByte()
        data[length++] = i.toByte()
        this.length = length
        return this
    }

    /**
     * Puts an UTF8 string into this byte vector. The byte vector is
     * automatically enlarged if necessary.
     *
     * @param s
     * a String.
     * @return this byte vector.
     */
    fun putUTF8(s: String): ByteVector {
        val charLength = s.length
        var len = length
        if (len + 2 + charLength > data.size) {
            enlarge(2 + charLength)
        }
        var data = this.data
        // optimistic algorithm: instead of computing the byte length and then
        // serializing the string (which requires two loops), we assume the byte
        // length is equal to char length (which is the most frequent case), and
        // we start serializing the string right away. During the serialization,
        // if we find that this assumption is wrong, we continue with the
        // general method.
        data[len++] = charLength.ushr(8).toByte()
        data[len++] = charLength.toByte()
        for (i in 0 until charLength) {
            var c = s[i]
            if (c >= '\u0001' && c <= '\u007f') {
                data[len++] = c.toByte()
            } else {
                var byteLength = i
                for (j in i until charLength) {
                    c = s[j]
                    if (c >= '\u0001' && c <= '\u007f') {
                        byteLength++
                    } else if (c > '\u07FF') {
                        byteLength += 3
                    } else {
                        byteLength += 2
                    }
                }
                data[length] = byteLength.ushr(8).toByte()
                data[length + 1] = byteLength.toByte()
                if (length + 2 + byteLength > data.size) {
                    length = len
                    enlarge(2 + byteLength)
                    data = this.data
                }
                for (j in i until charLength) {
                    c = s[j]
                    if (c >= '\u0001' && c <= '\u007f') {
                        data[len++] = c.toByte()
                    } else if (c > '\u07FF') {
                        data[len++] = (0xE0 or (c.toInt() shr 12 and 0xF)).toByte()
                        data[len++] = (0x80 or (c.toInt() shr 6 and 0x3F)).toByte()
                        data[len++] = (0x80 or (c.toInt() and 0x3F)).toByte()
                    } else {
                        data[len++] = (0xC0 or (c.toInt() shr 6 and 0x1F)).toByte()
                        data[len++] = (0x80 or (c.toInt() and 0x3F)).toByte()
                    }
                }
                break
            }
        }
        length = len
        return this
    }

    /**
     * Puts an array of bytes into this byte vector. The byte vector is
     * automatically enlarged if necessary.
     *
     * @param b
     * an array of bytes. May be <tt>null</tt> to put <tt>len</tt>
     * null bytes into this byte vector.
     * @param off
     * index of the fist byte of b that must be copied.
     * @param len
     * number of bytes of b that must be copied.
     * @return this byte vector.
     */
    fun putByteArray(b: ByteArray?, off: Int, len: Int): ByteVector {
        if (length + len > data.size) {
            enlarge(len)
        }
        if (b != null) {


            b.copyInto(data,  length,off, len)
//            System.arraycopy(b, off, data, length, len)
        }
        length += len
        return this
    }

    /**
     * Enlarge this byte vector so that it can receive n more bytes.
     *
     * @param size
     * number of additional bytes that this byte vector should be
     * able to receive.
     */
    private fun enlarge(size: Int) {
        val length1 = 2 * data.size
        val length2 = length + size
        val newData = ByteArray(if (length1 > length2) length1 else length2)


        data.copyInto(newData, 0, 0, length)
        data = newData
    }
}
