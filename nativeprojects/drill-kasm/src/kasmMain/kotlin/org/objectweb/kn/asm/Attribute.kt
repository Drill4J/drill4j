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
 * A non standard class, field, method or code attribute.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
class Attribute
/**
 * Constructs a new empty attribute.
 *
 * @param type
 * the type of the attribute.
 */
constructor(
    /**
     * The type of this attribute.
     */
    val type: String
) {

    /**
     * The next attribute in this attribute list. May be <tt>null</tt>.
     */
    internal var next: Attribute? = null

    /**
     * The raw value of this attribute, used only for unknown attributes.
     */
    internal var value: ByteArray? = null

    /**
     * Returns <tt>true</tt> if this type of attribute is unknown. The default
     * implementation of this method always returns <tt>true</tt>.
     *
     * @return <tt>true</tt> if this type of attribute is unknown.
     */
    val isUnknown: Boolean
        get() = true

    /**
     * Returns <tt>true</tt> if this type of attribute is a code attribute.
     *
     * @return <tt>true</tt> if this type of attribute is a code attribute.
     */
    val isCodeAttribute: Boolean
        get() = false

    /**
     * Returns the labels corresponding to this attribute.
     *
     * @return the labels corresponding to this attribute, or <tt>null</tt> if
     * this attribute is not a code attribute that contains labels.
     */
    val labels: Array<Label>?
        get() = null

    /**
     * Returns the length of the attribute list that begins with this attribute.
     *
     * @return the length of the attribute list that begins with this attribute.
     */
    internal val count: Int
        get() {
            var count = 0
            var attr: Attribute? = this
            while (attr != null) {
                count += 1
                attr = attr.next
            }
            return count
        }

    /**
     * Reads a [type][.type] attribute. This method must return a
     * *new* [Attribute] object, of type [type][.type],
     * corresponding to the <tt>len</tt> bytes starting at the given offset, in
     * the given class reader.
     *
     * @param cr
     * the class that contains the attribute to be read.
     * @param off
     * index of the first byte of the attribute's content in
     * [cr.b][ClassReader.b]. The 6 attribute header bytes,
     * containing the type and the length of the attribute, are not
     * taken into account here.
     * @param len
     * the length of the attribute's content.
     * @param buf
     * buffer to be used to call [            readUTF8][ClassReader.readUTF8], [readClass][ClassReader.readClass]
     * or [readConst][ClassReader.readConst].
     * @param codeOff
     * index of the first byte of code's attribute content in
     * [cr.b][ClassReader.b], or -1 if the attribute to be read
     * is not a code attribute. The 6 attribute header bytes,
     * containing the type and the length of the attribute, are not
     * taken into account here.
     * @param labels
     * the labels of the method's code, or <tt>null</tt> if the
     * attribute to be read is not a code attribute.
     * @return a *new* [Attribute] object corresponding to the given
     * bytes.
     */
    fun read(
        cr: ClassReader, off: Int,
        len: Int, buf: CharArray?, codeOff: Int,
        labels: Array<Label?>?
    ): Attribute {
        val attr = Attribute(type)
        attr.value = ByteArray(len)

        cr.b.forEach {
            print("$it,")
        }



        cr.b.copyInto(attr.value!!, 0, off, len)

//        System.arraycopy(cr.b, off, attr.value, 0, len);

        return attr
    }

    /**
     * Returns the byte array form of this attribute.
     *
     * @param cw
     * the class to which this attribute must be added. This
     * parameter can be used to add to the constant pool of this
     * class the items that corresponds to this attribute.
     * @param code
     * the bytecode of the method corresponding to this code
     * attribute, or <tt>null</tt> if this attribute is not a code
     * attributes.
     * @param len
     * the length of the bytecode of the method corresponding to this
     * code attribute, or <tt>null</tt> if this attribute is not a
     * code attribute.
     * @param maxStack
     * the maximum stack size of the method corresponding to this
     * code attribute, or -1 if this attribute is not a code
     * attribute.
     * @param maxLocals
     * the maximum number of local variables of the method
     * corresponding to this code attribute, or -1 if this attribute
     * is not a code attribute.
     * @return the byte array form of this attribute.
     */
    protected fun write(
        cw: ClassWriter, code: ByteArray?,
        len: Int, maxStack: Int, maxLocals: Int
    ): ByteVector {
        val v = ByteVector()
        v.data = (value)!!
        v.length = (value!!.size)
        return v
    }

    /**
     * Returns the size of all the attributes in this attribute list.
     *
     * @param cw
     * the class writer to be used to convert the attributes into
     * byte arrays, with the [write][.write] method.
     * @param code
     * the bytecode of the method corresponding to these code
     * attributes, or <tt>null</tt> if these attributes are not code
     * attributes.
     * @param len
     * the length of the bytecode of the method corresponding to
     * these code attributes, or <tt>null</tt> if these attributes
     * are not code attributes.
     * @param maxStack
     * the maximum stack size of the method corresponding to these
     * code attributes, or -1 if these attributes are not code
     * attributes.
     * @param maxLocals
     * the maximum number of local variables of the method
     * corresponding to these code attributes, or -1 if these
     * attributes are not code attributes.
     * @return the size of all the attributes in this attribute list. This size
     * includes the size of the attribute headers.
     */
    internal fun getSize(
        cw: ClassWriter, code: ByteArray?, len: Int,
        maxStack: Int, maxLocals: Int
    ): Int {
        var attr: Attribute? = this
        var size = 0
        while (attr != null) {
            cw.newUTF8(attr.type)
            size += attr.write(cw, code, len, maxStack, maxLocals).length + 6
            attr = attr.next
        }
        return size
    }

    /**
     * Writes all the attributes of this attribute list in the given byte
     * vector.
     *
     * @param cw
     * the class writer to be used to convert the attributes into
     * byte arrays, with the [write][.write] method.
     * @param code
     * the bytecode of the method corresponding to these code
     * attributes, or <tt>null</tt> if these attributes are not code
     * attributes.
     * @param len
     * the length of the bytecode of the method corresponding to
     * these code attributes, or <tt>null</tt> if these attributes
     * are not code attributes.
     * @param maxStack
     * the maximum stack size of the method corresponding to these
     * code attributes, or -1 if these attributes are not code
     * attributes.
     * @param maxLocals
     * the maximum number of local variables of the method
     * corresponding to these code attributes, or -1 if these
     * attributes are not code attributes.
     * @param out
     * where the attributes must be written.
     */
    internal fun put(
        cw: ClassWriter, code: ByteArray?, len: Int,
        maxStack: Int, maxLocals: Int, out: ByteVector
    ) {
        var attr: Attribute? = this
        while (attr != null) {
            val b = attr.write(cw, code, len, maxStack, maxLocals)
            out.putShort(cw.newUTF8(attr.type)).putInt(b.length)
            out.putByteArray(b.data, 0, b.length)
            attr = attr.next
        }
    }
}
