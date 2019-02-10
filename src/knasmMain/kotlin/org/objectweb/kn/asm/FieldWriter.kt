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
 * An [FieldVisitor] that generates Java fields in bytecode form.
 *
 * @author Eric Bruneton
 */
internal class FieldWriter
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------

/**
 * Constructs a new [FieldWriter].
 *
 * @param cw
 * the class writer to which this field must be added.
 * @param access
 * the field's access flags (see [Opcodes]).
 * @param name
 * the field's name.
 * @param desc
 * the field's descriptor (see [Type]).
 * @param signature
 * the field's signature. May be <tt>null</tt>.
 * @param value
 * the field's constant value. May be <tt>null</tt>.
 */
(
    /**
         * The class writer to which this field must be added.
         */
        private val cw: ClassWriter,
    /**
         * Access flags of this field.
         */
        private val access: Int, name: String,
    desc: String, signature: String?, value: Any?) : FieldVisitor(Opcodes.ASM5) {

    /**
     * The index of the constant pool item that contains the name of this
     * method.
     */
    private val name: Int

    /**
     * The index of the constant pool item that contains the descriptor of this
     * field.
     */
    private val desc: Int

    /**
     * The index of the constant pool item that contains the signature of this
     * field.
     */
    private var signature: Int = 0

    /**
     * The index of the constant pool item that contains the constant value of
     * this field.
     */
    private var value: Int = 0

    /**
     * The runtime visible annotations of this field. May be <tt>null</tt>.
     */
    private var anns: AnnotationWriter? = null

    /**
     * The runtime invisible annotations of this field. May be <tt>null</tt>.
     */
    private var ianns: AnnotationWriter? = null

    /**
     * The runtime visible type annotations of this field. May be <tt>null</tt>.
     */
    private var tanns: AnnotationWriter? = null

    /**
     * The runtime invisible type annotations of this field. May be
     * <tt>null</tt>.
     */
    private var itanns: AnnotationWriter? = null

    /**
     * The non standard attributes of this field. May be <tt>null</tt>.
     */
    private var attrs: Attribute? = null

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Returns the size of this field.
     *
     * @return the size of this field.
     */
    val size: Int
        get() {
            var size = 8
            if (value != 0) {
                cw.newUTF8("ConstantValue")
                size += 8
            }
            if (access and Opcodes.ACC_SYNTHETIC != 0) {
                if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                    cw.newUTF8("Synthetic")
                    size += 6
                }
            }
            if (access and Opcodes.ACC_DEPRECATED != 0) {
                cw.newUTF8("Deprecated")
                size += 6
            }
            if (ClassReader.SIGNATURES && signature != 0) {
                cw.newUTF8("Signature")
                size += 8
            }
            if (ClassReader.ANNOTATIONS && anns != null) {
                cw.newUTF8("RuntimeVisibleAnnotations")
                size += 8 + anns!!.getSize()
            }
            if (ClassReader.ANNOTATIONS && ianns != null) {
                cw.newUTF8("RuntimeInvisibleAnnotations")
                size += 8 + ianns!!.getSize()
            }
            if (ClassReader.ANNOTATIONS && tanns != null) {
                cw.newUTF8("RuntimeVisibleTypeAnnotations")
                size += 8 + tanns!!.getSize()
            }
            if (ClassReader.ANNOTATIONS && itanns != null) {
                cw.newUTF8("RuntimeInvisibleTypeAnnotations")
                size += 8 + itanns!!.getSize()
            }
            if (attrs != null) {
                size += attrs!!.getSize(cw, null, 0, -1, -1)
            }
            return size
        }

    init {
        if (cw.firstField == null) {
            cw.firstField = this
        } else {
            cw.lastField!!.fv = this
        }
        cw.lastField = this
        this.name = cw.newUTF8(name)
        this.desc = cw.newUTF8(desc)
        if (ClassReader.SIGNATURES && signature != null) {
            this.signature = cw.newUTF8(signature)
        }
        if (value != null) {
            this.value = cw.newConstItem(value).index
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the FieldVisitor abstract class
    // ------------------------------------------------------------------------

    override fun visitAnnotation(desc: String,
                                 visible: Boolean): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(cw, true, bv, bv, 2)
        if (visible) {
            aw.next = anns
            anns = aw
        } else {
            aw.next = ianns
            ianns = aw
        }
        return aw
    }

    override fun visitTypeAnnotation(typeRef: Int,
                                     typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        // write target_type and target_info
        AnnotationWriter.putTarget(typeRef, typePath, bv)
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(
            cw, true, bv, bv,
            bv.length - 2
        )
        if (visible) {
            aw.next = tanns
            tanns = aw
        } else {
            aw.next = itanns
            itanns = aw
        }
        return aw
    }

    override fun visitAttribute(attr: Attribute) {
        attr.next = attrs
        attrs = attr
    }

    override fun visitEnd() {}

    /**
     * Puts the content of this field into the given byte vector.
     *
     * @param out
     * where the content of this field must be put.
     */
    fun put(out: ByteVector) {
        val FACTOR = ClassWriter.TO_ACC_SYNTHETIC
        val mask = (Opcodes.ACC_DEPRECATED or ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                or (access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR)
        out.putShort(access and mask.inv()).putShort(name).putShort(desc)
        var attributeCount = 0
        if (value != 0) {
            ++attributeCount
        }
        if (access and Opcodes.ACC_SYNTHETIC != 0) {
            if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                ++attributeCount
            }
        }
        if (access and Opcodes.ACC_DEPRECATED != 0) {
            ++attributeCount
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount
        }
        if (attrs != null) {
            attributeCount += attrs!!.count
        }
        out.putShort(attributeCount)
        if (value != 0) {
            out.putShort(cw.newUTF8("ConstantValue"))
            out.putInt(2).putShort(value)
        }
        if (access and Opcodes.ACC_SYNTHETIC != 0) {
            if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                out.putShort(cw.newUTF8("Synthetic")).putInt(0)
            }
        }
        if (access and Opcodes.ACC_DEPRECATED != 0) {
            out.putShort(cw.newUTF8("Deprecated")).putInt(0)
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            out.putShort(cw.newUTF8("Signature"))
            out.putInt(2).putShort(signature)
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleAnnotations"))
            anns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleAnnotations"))
            ianns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"))
            tanns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"))
            itanns!!.put(out)
        }
        if (attrs != null) {
            attrs!!.put(cw, null, 0, -1, -1, out)
        }
    }
}
