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
 * An [AnnotationVisitor] that generates annotations in bytecode form.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
internal class AnnotationWriter
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------

/**
 * Constructs a new [AnnotationWriter].
 *
 * @param cw
 * the class writer to which this annotation must be added.
 * @param named
 * <tt>true<tt> if values are named, <tt>false</tt> otherwise.
 * @param bv
 * where the annotation values must be stored.
 * @param parent
 * where the number of annotation values must be stored.
 * @param offset
 * where in <tt>parent</tt> the number of annotation values must
 * be stored.
</tt></tt> */
(
    /**
         * The class writer to which this annotation must be added.
         */
        private val cw: ClassWriter,
    /**
         * <tt>true<tt> if values are named, <tt>false</tt> otherwise. Annotation
         * writers used for annotation default and annotation arrays use unnamed
         * values.
        </tt></tt> */
        private val named: Boolean,
    /**
         * The annotation values in bytecode form. This byte vector only contains
         * the values themselves, i.e. the number of values must be stored as a
         * unsigned short just before these bytes.
         */
        private val bv: ByteVector,
    /**
         * The byte vector to be used to store the number of values of this
         * annotation. See [.bv].
         */
        private val parent: ByteVector?,
    /**
         * Where the number of values of this annotation must be stored in
         * [.parent].
         */
        private val offset: Int) : AnnotationVisitor(Opcodes.ASM5) {

    /**
     * The number of values in this annotation.
     */
    private var size: Int = 0
    get

    /**
     * Next annotation writer. This field is used to store annotation lists.
     */
    var next: AnnotationWriter? = null

    /**
     * Previous annotation writer. This field is used to store annotation lists.
     */
    var prev: AnnotationWriter? = null

    // ------------------------------------------------------------------------
    // Implementation of the AnnotationVisitor abstract class
    // ------------------------------------------------------------------------

    override fun visit(name: String, value: Any) {
        ++size
        if (named) {
            bv.putShort(cw.newUTF8(name))
        }
        if (value is String) {
            bv.put12('s'.toInt(), cw.newUTF8(value))
        } else if (value is Byte) {
            bv.put12('B'.toInt(), cw.newInteger(value.toByte().toInt()).index)
        } else if (value is Boolean) {
            val v = if (value) 1 else 0
            bv.put12('Z'.toInt(), cw.newInteger(v).index)
        } else if (value is Char) {
            bv.put12('C'.toInt(), cw.newInteger(value.toChar().toInt()).index)
        } else if (value is Short) {
            bv.put12('S'.toInt(), cw.newInteger(value.toShort().toInt()).index)
        } else if (value is Type) {
            bv.put12('c'.toInt(), cw.newUTF8(value.descriptor))
        } else if (value is ByteArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('B'.toInt(), cw.newInteger(v[i].toInt()).index)
            }
        } else if (value is BooleanArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('Z'.toInt(), cw.newInteger(if (v[i]) 1 else 0).index)
            }
        } else if (value is ShortArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('S'.toInt(), cw.newInteger(v[i].toInt()).index)
            }
        } else if (value is CharArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('C'.toInt(), cw.newInteger(v[i].toInt()).index)
            }
        } else if (value is IntArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('I'.toInt(), cw.newInteger(v[i].toInt()).index)
            }
        } else if (value is LongArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('J'.toInt(), cw.newLong(v[i]).index)
            }
        } else if (value is FloatArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('F'.toInt(), cw.newFloat(v[i]).index)
            }
        } else if (value is DoubleArray) {
            val v = value
            bv.put12('['.toInt(), v.size)
            for (i in v.indices) {
                bv.put12('D'.toInt(), cw.newDouble(v[i]).index)
            }
        } else {
            val i = cw.newConstItem(value)
            bv.put12(".s.IFJDCS"[i.type].toInt(), i.index)
        }
    }

    override fun visitEnum(name: String, desc: String,
                           value: String) {
        ++size
        if (named) {
            bv.putShort(cw.newUTF8(name))
        }
        bv.put12('e'.toInt(), cw.newUTF8(desc)).putShort(cw.newUTF8(value))
    }

    override fun visitAnnotation(name: String,
                                 desc: String): AnnotationVisitor {
        ++size
        if (named) {
            bv.putShort(cw.newUTF8(name))
        }
        // write tag and type, and reserve space for values count
        bv.put12('@'.toInt(), cw.newUTF8(desc)).putShort(0)
        return AnnotationWriter(cw, true, bv, bv, bv.length - 2)
    }

    override fun visitArray(name: String): AnnotationVisitor {
        ++size
        if (named) {
            bv.putShort(cw.newUTF8(name))
        }
        // write tag, and reserve space for array size
        bv.put12('['.toInt(), 0)
        return AnnotationWriter(cw, false, bv, bv, bv.length - 2)
    }

    override fun visitEnd() {
        if (parent != null) {
            val data = parent.data
            data[offset] = size.ushr(8).toByte()
            data[offset + 1] = size.toByte()
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Returns the size of this annotation writer list.
     *
     * @return the size of this annotation writer list.
     */
    fun getSize(): Int {
        var size = 0
        var aw: AnnotationWriter? = this
        while (aw != null) {
            size += aw.bv.length
            aw = aw.next
        }
        return size
    }

    /**
     * Puts the annotations of this annotation writer list into the given byte
     * vector.
     *
     * @param out
     * where the annotations must be put.
     */
    fun put(out: ByteVector) {
        var n = 0
        var size = 2
        var aw: AnnotationWriter? = this
        var last: AnnotationWriter? = null
        while (aw != null) {
            ++n
            size += aw.bv.length
            aw.visitEnd() // in case user forgot to call visitEnd
            aw.prev = last
            last = aw
            aw = aw.next
        }
        out.putInt(size)
        out.putShort(n)
        aw = last
        while (aw != null) {
            out.putByteArray(aw.bv.data, 0, aw.bv.length)
            aw = aw.prev
        }
    }

    companion object {

        /**
         * Puts the given annotation lists into the given byte vector.
         *
         * @param panns
         * an array of annotation writer lists.
         * @param off
         * index of the first annotation to be written.
         * @param out
         * where the annotations must be put.
         */
        fun put(panns: Array<AnnotationWriter?>, off: Int,
                out: ByteVector
        ) {
            var size = 1 + 2 * (panns.size - off)
            for (i in off until panns.size) {
                size += if (panns[i] == null) 0 else panns[i]!!.getSize()
            }
            out.putInt(size).putByte(panns.size - off)
            for (i in off until panns.size) {
                var aw: AnnotationWriter? = panns[i]
                var last: AnnotationWriter? = null
                var n = 0
                while (aw != null) {
                    ++n
                    aw.visitEnd() // in case user forgot to call visitEnd
                    aw.prev = last
                    last = aw
                    aw = aw.next
                }
                out.putShort(n)
                aw = last
                while (aw != null) {
                    out.putByteArray(aw.bv.data, 0, aw.bv.length)
                    aw = aw.prev
                }
            }
        }

        /**
         * Puts the given type reference and type path into the given bytevector.
         * LOCAL_VARIABLE and RESOURCE_VARIABLE target types are not supported.
         *
         * @param typeRef
         * a reference to the annotated type. See [TypeReference].
         * @param typePath
         * the path to the annotated type argument, wildcard bound, array
         * element type, or static inner type within 'typeRef'. May be
         * <tt>null</tt> if the annotation targets 'typeRef' as a whole.
         * @param out
         * where the type reference and type path must be put.
         */
        fun putTarget(typeRef: Int, typePath: TypePath?, out: ByteVector) {
            when (typeRef.ushr(24)) {
                0x00 // CLASS_TYPE_PARAMETER
                    , 0x01 // METHOD_TYPE_PARAMETER
                    , 0x16 // METHOD_FORMAL_PARAMETER
                -> out.putShort(typeRef.ushr(16))
                0x13 // FIELD
                    , 0x14 // METHOD_RETURN
                    , 0x15 // METHOD_RECEIVER
                -> out.putByte(typeRef.ushr(24))
                0x47 // CAST
                    , 0x48 // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                    , 0x49 // METHOD_INVOCATION_TYPE_ARGUMENT
                    , 0x4A // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                    , 0x4B // METHOD_REFERENCE_TYPE_ARGUMENT
                -> out.putInt(typeRef)
                // case 0x10: // CLASS_EXTENDS
                // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
                // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
                // case 0x17: // THROWS
                // case 0x42: // EXCEPTION_PARAMETER
                // case 0x43: // INSTANCEOF
                // case 0x44: // NEW
                // case 0x45: // CONSTRUCTOR_REFERENCE_RECEIVER
                // case 0x46: // METHOD_REFERENCE_RECEIVER
                else -> out.put12(typeRef.ushr(24), typeRef and 0xFFFF00 shr 8)
            }
            if (typePath == null) {
                out.putByte(0)
            } else {
                val length = typePath.b[typePath.offset] * 2 + 1
                out.putByteArray(typePath.b, typePath.offset, length)
            }
        }
    }
}
