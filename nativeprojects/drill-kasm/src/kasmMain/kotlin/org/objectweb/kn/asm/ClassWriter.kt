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
 * A [ClassVisitor] that generates classes in bytecode form. More
 * precisely this visitor generates a byte array conforming to the Java class
 * file format. It can be used alone, to generate a Java class "from scratch",
 * or with one or more [ClassReader] and adapter class visitor
 * to generate a modified class from one or more existing Java classes.
 *
 * @author Eric Bruneton
 */
open class ClassWriter
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------

/**
 * Constructs a new [ClassWriter] object.
 *
 * @param flags
 * option flags that can be used to modify the default behavior
 * of this class. See [.COMPUTE_MAXS],
 * [.COMPUTE_FRAMES].
 */
(flags: Int) : ClassVisitor(Opcodes.ASM5) {

    /**
     * The class reader from which this class writer was constructed, if any.
     */
    internal var cr: ClassReader?=null

    /**
     * Minor and major version numbers of the class to be generated.
     */
    internal var version: Int = 0

    /**
     * Index of the next item to be added in the constant pool.
     */
    internal var index: Int = 0

    /**
     * The constant pool of this class.
     */
    internal val pool: ByteVector

    /**
     * The constant pool's hash table data.
     */
    internal var items: Array<Item?>

    /**
     * The threshold of the constant pool's hash table.
     */
    internal var threshold: Int = 0

    /**
     * A reusable key used to look for items in the [.items] hash table.
     */
    internal val key: Item

    /**
     * A reusable key used to look for items in the [.items] hash table.
     */
    internal val key2: Item

    /**
     * A reusable key used to look for items in the [.items] hash table.
     */
    internal val key3: Item

    /**
     * A reusable key used to look for items in the [.items] hash table.
     */
    internal val key4: Item

    /**
     * A type table used to temporarily store internal names that will not
     * necessarily be stored in the constant pool. This type table is used by
     * the control flow and data flow analysis algorithm used to compute stack
     * map frames from scratch. This array associates to each index <tt>i</tt>
     * the Item whose index is <tt>i</tt>. All Item objects stored in this array
     * are also stored in the [.items] hash table. These two arrays allow
     * to retrieve an Item from its index or, conversely, to get the index of an
     * Item from its value. Each Item stores an internal name in its
     * [Item.strVal1] field.
     */
    internal var typeTable: Array<Item?>? = null

    /**
     * Number of elements in the [.typeTable] array.
     */
    private var typeCount: Short = 0

    /**
     * The access flags of this class.
     */
    private var access: Int = 0

    /**
     * The constant pool item that contains the internal name of this class.
     */
    private var name: Int = 0

    /**
     * The internal name of this class.
     */
    internal var thisName: String?=null

    /**
     * The constant pool item that contains the signature of this class.
     */
    private var signature: Int = 0

    /**
     * The constant pool item that contains the internal name of the super class
     * of this class.
     */
    private var superName: Int = 0

    /**
     * Number of interfaces implemented or extended by this class or interface.
     */
    private var interfaceCount: Int = 0

    /**
     * The interfaces implemented or extended by this class or interface. More
     * precisely, this array contains the indexes of the constant pool items
     * that contain the internal names of these interfaces.
     */
    private var interfaces: IntArray? = null

    /**
     * The index of the constant pool item that contains the name of the source
     * file from which this class was compiled.
     */
    private var sourceFile: Int = 0

    /**
     * The SourceDebug attribute of this class.
     */
    private var sourceDebug: ByteVector? = null

    /**
     * The constant pool item that contains the name of the enclosing class of
     * this class.
     */
    private var enclosingMethodOwner: Int = 0

    /**
     * The constant pool item that contains the name and descriptor of the
     * enclosing method of this class.
     */
    private var enclosingMethod: Int = 0

    /**
     * The runtime visible annotations of this class.
     */
    private var anns: AnnotationWriter? = null

    /**
     * The runtime invisible annotations of this class.
     */
    private var ianns: AnnotationWriter? = null

    /**
     * The runtime visible type annotations of this class.
     */
    private var tanns: AnnotationWriter? = null

    /**
     * The runtime invisible type annotations of this class.
     */
    private var itanns: AnnotationWriter? = null

    /**
     * The non standard attributes of this class.
     */
    private var attrs: Attribute? = null

    /**
     * The number of entries in the InnerClasses attribute.
     */
    private var innerClassesCount: Int = 0

    /**
     * The InnerClasses attribute.
     */
    private var innerClasses: ByteVector? = null

    /**
     * The number of entries in the BootstrapMethods attribute.
     */
    internal var bootstrapMethodsCount: Int = 0

    /**
     * The BootstrapMethods attribute.
     */
    internal var bootstrapMethods: ByteVector? = null

    /**
     * The fields of this class. These fields are stored in a linked list of
     * [FieldWriter] objects, linked to each other by their
     * [FieldWriter.fv] field. This field stores the first element of this
     * list.
     */
    internal var firstField: FieldWriter? = null

    /**
     * The fields of this class. These fields are stored in a linked list of
     * [FieldWriter] objects, linked to each other by their
     * [FieldWriter.fv] field. This field stores the last element of this
     * list.
     */
    internal var lastField: FieldWriter? = null

    /**
     * The methods of this class. These methods are stored in a linked list of
     * [MethodWriter] objects, linked to each other by their
     * [MethodWriter.mv] field. This field stores the first element of
     * this list.
     */
    internal var firstMethod: MethodWriter? = null

    /**
     * The methods of this class. These methods are stored in a linked list of
     * [MethodWriter] objects, linked to each other by their
     * [MethodWriter.mv] field. This field stores the last element of this
     * list.
     */
    internal var lastMethod: MethodWriter? = null

    /**
     * <tt>true</tt> if the maximum stack size and number of local variables
     * must be automatically computed.
     */
    val computeMaxs: Boolean

    /**
     * <tt>true</tt> if the stack map frames must be recomputed from scratch.
     */
    val computeFrames: Boolean

    /**
     * <tt>true</tt> if the stack map tables of this class are invalid. The
     * [MethodWriter.resizeInstructions] method cannot transform existing
     * stack map tables, and so produces potentially invalid classes when it is
     * executed. In this case the class is reread and rewritten with the
     * [.COMPUTE_FRAMES] option (the resizeInstructions method can resize
     * stack map tables when this option is used).
     */
    internal var invalidFrames: Boolean = false

    init {
        index = 1
        pool = ByteVector()
        items = arrayOfNulls(256)
        threshold = (0.75 * items.size).toInt()
        key = Item()
        key2 = Item()
        key3 = Item()
        key4 = Item()
        this.computeMaxs = flags and COMPUTE_MAXS != 0
        this.computeFrames = flags and COMPUTE_FRAMES != 0
    }

    /**
     * Constructs a new [ClassWriter] object and enables optimizations for
     * "mostly add" bytecode transformations. These optimizations are the
     * following:
     *
     *
     *  * The constant pool from the original class is copied as is in the new
     * class, which saves time. New constant pool entries will be added at the
     * end if necessary, but unused constant pool entries *won't be
     * removed*.
     *  * Methods that are not transformed are copied as is in the new class,
     * directly from the original class bytecode (i.e. without emitting visit
     * events for all the method instructions), which saves a *lot* of
     * time. Untransformed methods are detected by the fact that the
     * [ClassReader] receives [MethodVisitor] objects that come from
     * a [ClassWriter] (and not from any other [ClassVisitor]
     * instance).
     *
     *
     * @param classReader
     * the [ClassReader] used to read the original class. It
     * will be used to copy the entire constant pool from the
     * original class and also to copy other fragments of original
     * bytecode where applicable.
     * @param flags
     * option flags that can be used to modify the default behavior
     * of this class. *These option flags do not affect methods
     * that are copied as is in the new class. This means that the
     * maximum stack size nor the stack frames will be computed for
     * these methods*. See [.COMPUTE_MAXS],
     * [.COMPUTE_FRAMES].
     */
    constructor(classReader: ClassReader, flags: Int) : this(flags) {
        classReader.copyPool(this)
        this.cr = classReader
    }

    // ------------------------------------------------------------------------
    // Implementation of the ClassVisitor abstract class
    // ------------------------------------------------------------------------

    override fun visit(version: Int, access: Int, name: String, signature: String?,
                   superName: String, interfaces: Array<String?>?) {
        this.version = version
        this.access = access
        this.name = newClass(name)
        thisName = name
        if (ClassReader.SIGNATURES && signature != null) {
            this.signature = newUTF8(signature)
        }
        this.superName = if (superName == null) 0 else newClass(superName)
        if (interfaces != null && interfaces.size > 0) {
            interfaceCount = interfaces.size
            this.interfaces = IntArray(interfaceCount)
            for (i in 0 until interfaceCount) {
                this.interfaces!![i] = newClass(interfaces[i]!!)
            }
        }
    }

    override fun visitSource(file: String, debug: String?) {
        if (file != null) {
            sourceFile = newUTF8(file)
        }
        if (debug != null) {
            sourceDebug = ByteVector().putUTF8(debug)
        }
    }

    override fun visitOuterClass(owner: String, name: String,
                                 desc: String) {
        enclosingMethodOwner = newClass(owner)
        if (name != null && desc != null) {
            enclosingMethod = newNameType(name, desc)
        }
    }

    override fun visitAnnotation(desc: String,
                                 visible: Boolean): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        // write type, and reserve space for values count
        bv.putShort(newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(this, true, bv, bv, 2)
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
        bv.putShort(newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(
            this, true, bv, bv,
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

    override fun visitInnerClass(name: String,
                                 outerName: String?, innerName: String?, access: Int) {
        if (innerClasses == null) {
            innerClasses = ByteVector()
        }
        ++innerClassesCount
        innerClasses!!.putShort(if (name == null) 0 else newClass(name))
        innerClasses!!.putShort(if (outerName == null) 0 else newClass(outerName))
        innerClasses!!.putShort(if (innerName == null) 0 else newUTF8(innerName))
        innerClasses!!.putShort(access)
    }

    override fun visitField(access: Int, name: String,
                            desc: String, signature: String?, value: Any?): FieldVisitor {
        return FieldWriter(this, access, name, desc, signature, value)
    }

    override fun visitMethod(access: Int, name: String,
                             desc: String, signature: String?, exceptions: Array<String?>?): MethodVisitor {
        return MethodWriter(
            this, access, name, desc, signature,
            exceptions, computeMaxs, computeFrames
        )
    }

    override fun visitEnd() {}

    // ------------------------------------------------------------------------
    // Other public methods
    // ------------------------------------------------------------------------

    /**
     * Returns the bytecode of the class that was build with this class writer.
     *
     * @return the bytecode of the class that was build with this class writer.
     */
    fun toByteArray(): ByteArray {
        if (index > 0xFFFF) {
            throw RuntimeException("Class file too large!")
        }
        // computes the real size of the bytecode of this class
        var size = 24 + 2 * interfaceCount
        var nbFields = 0
        var fb = firstField
        while (fb != null) {
            ++nbFields
            size += fb.size
            fb = fb.fv as FieldWriter?
        }
        var nbMethods = 0
        var mb = firstMethod
        while (mb != null) {
            ++nbMethods
            size += mb.size
            mb = mb.mv as MethodWriter?
        }
        var attributeCount = 0
        if (bootstrapMethods != null) {
            // we put it as first attribute in order to improve a bit
            // ClassReader.copyBootstrapMethods
            ++attributeCount
            size += 8 + bootstrapMethods!!.length
            newUTF8("BootstrapMethods")
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            ++attributeCount
            size += 8
            newUTF8("Signature")
        }
        if (sourceFile != 0) {
            ++attributeCount
            size += 8
            newUTF8("SourceFile")
        }
        if (sourceDebug != null) {
            ++attributeCount
            size += sourceDebug!!.length + 4
            newUTF8("SourceDebugExtension")
        }
        if (enclosingMethodOwner != 0) {
            ++attributeCount
            size += 10
            newUTF8("EnclosingMethod")
        }
        if (access and Opcodes.ACC_DEPRECATED != 0) {
            ++attributeCount
            size += 6
            newUTF8("Deprecated")
        }
        if (access and Opcodes.ACC_SYNTHETIC != 0) {
            if (version and 0xFFFF < Opcodes.V1_5 || access and ACC_SYNTHETIC_ATTRIBUTE != 0) {
                ++attributeCount
                size += 6
                newUTF8("Synthetic")
            }
        }
        if (innerClasses != null) {
            ++attributeCount
            size += 8 + innerClasses!!.length
            newUTF8("InnerClasses")
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            ++attributeCount
            size += 8 + anns!!.getSize()
            newUTF8("RuntimeVisibleAnnotations")
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            ++attributeCount
            size += 8 + ianns!!.getSize()
            newUTF8("RuntimeInvisibleAnnotations")
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            ++attributeCount
            size += 8 + tanns!!.getSize()
            newUTF8("RuntimeVisibleTypeAnnotations")
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            ++attributeCount
            size += 8 + itanns!!.getSize()
            newUTF8("RuntimeInvisibleTypeAnnotations")
        }
        if (attrs != null) {
            attributeCount += attrs!!.count
            size += attrs!!.getSize(this, null, 0, -1, -1)
        }
        size += pool.length
        // allocates a byte vector of this size, in order to avoid unnecessary
        // arraycopy operations in the ByteVector.enlarge() method
        val out = ByteVector(size)
        out.putInt(-0x35014542).putInt(version)
        out.putShort(index).putByteArray(pool.data, 0, pool.length)
        val mask = (Opcodes.ACC_DEPRECATED or ACC_SYNTHETIC_ATTRIBUTE
                or (access and ACC_SYNTHETIC_ATTRIBUTE) / TO_ACC_SYNTHETIC)
        out.putShort(access and mask.inv()).putShort(name).putShort(superName)
        out.putShort(interfaceCount)
        for (i in 0 until interfaceCount) {
            out.putShort(interfaces!![i])
        }
        out.putShort(nbFields)
        fb = firstField
        while (fb != null) {
            fb.put(out)
            fb = fb.fv as FieldWriter?
        }
        out.putShort(nbMethods)
        mb = firstMethod
        while (mb != null) {
            mb.put(out)
            mb = mb.mv as MethodWriter?
        }
        out.putShort(attributeCount)
        if (bootstrapMethods != null) {
            out.putShort(newUTF8("BootstrapMethods"))
            out.putInt(bootstrapMethods!!.length + 2).putShort(
                    bootstrapMethodsCount)
            out.putByteArray(bootstrapMethods!!.data, 0, bootstrapMethods!!.length)
        }
        if (ClassReader.SIGNATURES && signature != 0) {
            out.putShort(newUTF8("Signature")).putInt(2).putShort(signature)
        }
        if (sourceFile != 0) {
            out.putShort(newUTF8("SourceFile")).putInt(2).putShort(sourceFile)
        }
        if (sourceDebug != null) {
            val len = sourceDebug!!.length - 2
            out.putShort(newUTF8("SourceDebugExtension")).putInt(len)
            out.putByteArray(sourceDebug!!.data, 2, len)
        }
        if (enclosingMethodOwner != 0) {
            out.putShort(newUTF8("EnclosingMethod")).putInt(4)
            out.putShort(enclosingMethodOwner).putShort(enclosingMethod)
        }
        if (access and Opcodes.ACC_DEPRECATED != 0) {
            out.putShort(newUTF8("Deprecated")).putInt(0)
        }
        if (access and Opcodes.ACC_SYNTHETIC != 0) {
            if (version and 0xFFFF < Opcodes.V1_5 || access and ACC_SYNTHETIC_ATTRIBUTE != 0) {
                out.putShort(newUTF8("Synthetic")).putInt(0)
            }
        }
        if (innerClasses != null) {
            out.putShort(newUTF8("InnerClasses"))
            out.putInt(innerClasses!!.length + 2).putShort(innerClassesCount)
            out.putByteArray(innerClasses!!.data, 0, innerClasses!!.length)
        }
        if (ClassReader.ANNOTATIONS && anns != null) {
            out.putShort(newUTF8("RuntimeVisibleAnnotations"))
            anns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && ianns != null) {
            out.putShort(newUTF8("RuntimeInvisibleAnnotations"))
            ianns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && tanns != null) {
            out.putShort(newUTF8("RuntimeVisibleTypeAnnotations"))
            tanns!!.put(out)
        }
        if (ClassReader.ANNOTATIONS && itanns != null) {
            out.putShort(newUTF8("RuntimeInvisibleTypeAnnotations"))
            itanns!!.put(out)
        }
        if (attrs != null) {
            attrs!!.put(this, null, 0, -1, -1, out)
        }
        if (invalidFrames) {
            val cw = ClassWriter(COMPUTE_FRAMES)
            ClassReader(out.data)
                .accept(cw, ClassReader.SKIP_FRAMES)
            return cw.toByteArray()
        }
        return out.data
    }

    // ------------------------------------------------------------------------
    // Utility methods: constant pool management
    // ------------------------------------------------------------------------

    /**
     * Adds a number or string constant to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     *
     * @param cst
     * the value of the constant to be added to the constant pool.
     * This parameter must be an [Integer], a [Float], a
     * [Long], a [Double], a [String] or a
     * [Type].
     * @return a new or already existing constant item with the given value.
     */
    internal fun newConstItem(cst: Any): Item {
        if (cst is Int) {
            val `val` = cst.toInt()
            return newInteger(`val`)
        } else if (cst is Byte) {
            val `val` = cst.toInt()
            return newInteger(`val`)
        } else if (cst is Char) {
            val `val` = cst.toChar().toInt()
            return newInteger(`val`)
        } else if (cst is Short) {
            val `val` = cst.toInt()
            return newInteger(`val`)
        } else if (cst is Boolean) {
            val `val` = if (cst) 1 else 0
            return newInteger(`val`)
        } else if (cst is Float) {
            val `val` = cst.toFloat()
            return newFloat(`val`)
        } else if (cst is Long) {
            val `val` = cst.toLong()
            return newLong(`val`)
        } else if (cst is Double) {
            val `val` = cst.toDouble()
            return newDouble(`val`)
        } else if (cst is String) {
            return newString(cst)
        } else if (cst is Type) {
            val t = cst
            val s = t.sort
            return if (s == Type.OBJECT) {
                newClassItem(t.internalName)
            } else if (s == Type.METHOD) {
                newMethodTypeItem(t.descriptor)
            } else { // s == primitive type or array
                newClassItem(t.descriptor)
            }
        } else if (cst is Handle) {
            val h = cst
            return newHandleItem(h.tag, h.owner, h.name, h.desc)
        } else {
            throw IllegalArgumentException("value $cst")
        }
    }

    /**
     * Adds a number or string constant to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param cst
     * the value of the constant to be added to the constant pool.
     * This parameter must be an [Integer], a [Float], a
     * [Long], a [Double] or a [String].
     * @return the index of a new or already existing constant item with the
     * given value.
     */
    fun newConst(cst: Any): Int {
        return newConstItem(cst).index
    }

    /**
     * Adds an UTF8 string to the constant pool of the class being build. Does
     * nothing if the constant pool already contains a similar item. *This
     * method is intended for [Attribute] sub classes, and is normally not
     * needed by class generators or adapters.*
     *
     * @param value
     * the String value.
     * @return the index of a new or already existing UTF8 item.
     */
    fun newUTF8(value: String?): Int {
        key[UTF8, value, null] = null
        var result = get(key)
        if (result == null) {
            pool.putByte(UTF8).putUTF8(value!!)
            result = Item(index++, key)
            put(result)
        }
        return result.index
    }

    /**
     * Adds a class reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param value
     * the internal name of the class.
     * @return a new or already existing class reference item.
     */
    internal fun newClassItem(value: String?): Item {
        key2.set(CLASS, value, null,null)
        var result = get(key2)
        if (result == null) {
            pool.put12(CLASS, newUTF8(value))
            result = Item(index++, key2)
            put(result)
        }
        return result
    }

    /**
     * Adds a class reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param value
     * the internal name of the class.
     * @return the index of a new or already existing class reference item.
     */
    fun newClass(value: String?): Int {
        return newClassItem(value).index
    }

    /**
     * Adds a method type reference to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param methodDesc
     * method descriptor of the method type.
     * @return a new or already existing method type reference item.
     */
    internal fun newMethodTypeItem(methodDesc: String): Item {
        key2[MTYPE, methodDesc, null] = null
        var result = get(key2)
        if (result == null) {
            pool.put12(MTYPE, newUTF8(methodDesc))
            result = Item(index++, key2)
            put(result)
        }
        return result
    }

    /**
     * Adds a method type reference to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param methodDesc
     * method descriptor of the method type.
     * @return the index of a new or already existing method type reference
     * item.
     */
    fun newMethodType(methodDesc: String): Int {
        return newMethodTypeItem(methodDesc).index
    }

    /**
     * Adds a handle to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item. *This method is
     * intended for [Attribute] sub classes, and is normally not needed by
     * class generators or adapters.*
     *
     * @param tag
     * the kind of this handle. Must be [Opcodes.H_GETFIELD],
     * [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD],
     * [Opcodes.H_PUTSTATIC], [Opcodes.H_INVOKEVIRTUAL],
     * [Opcodes.H_INVOKESTATIC],
     * [Opcodes.H_INVOKESPECIAL],
     * [Opcodes.H_NEWINVOKESPECIAL] or
     * [Opcodes.H_INVOKEINTERFACE].
     * @param owner
     * the internal name of the field or method owner class.
     * @param name
     * the name of the field or method.
     * @param desc
     * the descriptor of the field or method.
     * @return a new or an already existing method type reference item.
     */
    internal fun newHandleItem(tag: Int, owner: String, name: String,
                               desc: String): Item {
        key4[HANDLE_BASE + tag, owner, name] = desc
        var result = get(key4)
        if (result == null) {
            if (tag <= Opcodes.H_PUTSTATIC) {
                put112(HANDLE, tag, newField(owner, name, desc))
            } else {
                put112(
                    HANDLE,
                        tag,
                        newMethod(owner, name, desc,
                                tag == Opcodes.H_INVOKEINTERFACE
                        ))
            }
            result = Item(index++, key4)
            put(result)
        }
        return result
    }

    /**
     * Adds a handle to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item. *This method is
     * intended for [Attribute] sub classes, and is normally not needed by
     * class generators or adapters.*
     *
     * @param tag
     * the kind of this handle. Must be [Opcodes.H_GETFIELD],
     * [Opcodes.H_GETSTATIC], [Opcodes.H_PUTFIELD],
     * [Opcodes.H_PUTSTATIC], [Opcodes.H_INVOKEVIRTUAL],
     * [Opcodes.H_INVOKESTATIC],
     * [Opcodes.H_INVOKESPECIAL],
     * [Opcodes.H_NEWINVOKESPECIAL] or
     * [Opcodes.H_INVOKEINTERFACE].
     * @param owner
     * the internal name of the field or method owner class.
     * @param name
     * the name of the field or method.
     * @param desc
     * the descriptor of the field or method.
     * @return the index of a new or already existing method type reference
     * item.
     */
    fun newHandle(tag: Int, owner: String, name: String,
                  desc: String): Int {
        return newHandleItem(tag, owner, name, desc).index
    }

    /**
     * Adds an invokedynamic reference to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param name
     * name of the invoked method.
     * @param desc
     * descriptor of the invoke method.
     * @param bsm
     * the bootstrap method.
     * @param bsmArgs
     * the bootstrap method constant arguments.
     *
     * @return a new or an already existing invokedynamic type reference item.
     */
    internal fun newInvokeDynamicItem(name: String, desc: String,
                                      bsm: Handle, vararg bsmArgs: Any): Item {
        // cache for performance
        var bootstrapMethods = this.bootstrapMethods
        if (bootstrapMethods == null) {
            this.bootstrapMethods = ByteVector()
            bootstrapMethods = this.bootstrapMethods
        }

        val position = bootstrapMethods!!.length // record current position

        var hashCode = bsm.hashCode()
        bootstrapMethods.putShort(newHandle(bsm.tag, bsm.owner, bsm.name,
                bsm.desc))

        val argsLength = bsmArgs.size
        bootstrapMethods.putShort(argsLength)

        for (i in 0 until argsLength) {
            val bsmArg = bsmArgs[i]
            hashCode = hashCode xor bsmArg.hashCode()
            bootstrapMethods.putShort(newConst(bsmArg))
        }

        val data = bootstrapMethods.data
        val length = 1 + 1 + argsLength shl 1 // (bsm + argCount + arguments)
        hashCode = hashCode and 0x7FFFFFFF
        var result: Item? = items[hashCode % items.size]
        loop@ while (result != null) {
            if (result.type != BSM || result.hashCode != hashCode) {
                result = result.next
                continue
            }

            // because the data encode the size of the argument
            // we don't need to test if these size are equals
            val resultPosition = result.intVal
            for (p in 0 until length) {
                if (data[position + p] != data[resultPosition + p]) {
                    result = result!!.next
                    continue@loop
                }
            }
            break
        }

        val bootstrapMethodIndex: Int
        if (result != null) {
            bootstrapMethodIndex = result.index
            bootstrapMethods.length = position // revert to old position
        } else {
            bootstrapMethodIndex = bootstrapMethodsCount++
            result = Item(bootstrapMethodIndex)
            result[position] = hashCode
            put(result)
        }

        // now, create the InvokeDynamic constant
        key3[name, desc] = bootstrapMethodIndex
        result = get(key3)
        if (result == null) {
            put122(INDY, bootstrapMethodIndex, newNameType(name, desc))
            result = Item(index++, key3)
            put(result)
        }
        return result
    }

    /**
     * Adds an invokedynamic reference to the constant pool of the class being
     * build. Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param name
     * name of the invoked method.
     * @param desc
     * descriptor of the invoke method.
     * @param bsm
     * the bootstrap method.
     * @param bsmArgs
     * the bootstrap method constant arguments.
     *
     * @return the index of a new or already existing invokedynamic reference
     * item.
     */
    fun newInvokeDynamic(name: String, desc: String,
                         bsm: Handle, vararg bsmArgs: Any): Int {
        return newInvokeDynamicItem(name, desc, bsm, *bsmArgs).index
    }

    /**
     * Adds a field reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     *
     * @param owner
     * the internal name of the field's owner class.
     * @param name
     * the field's name.
     * @param desc
     * the field's descriptor.
     * @return a new or already existing field reference item.
     */
    internal fun newFieldItem(owner: String, name: String, desc: String): Item {
        key3[FIELD, owner, name] = desc
        var result = get(key3)
        if (result == null) {
            put122(FIELD, newClass(owner), newNameType(name, desc))
            result = Item(index++, key3)
            put(result)
        }
        return result
    }

    /**
     * Adds a field reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param owner
     * the internal name of the field's owner class.
     * @param name
     * the field's name.
     * @param desc
     * the field's descriptor.
     * @return the index of a new or already existing field reference item.
     */
    fun newField(owner: String, name: String, desc: String): Int {
        return newFieldItem(owner, name, desc).index
    }

    /**
     * Adds a method reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     *
     * @param owner
     * the internal name of the method's owner class.
     * @param name
     * the method's name.
     * @param desc
     * the method's descriptor.
     * @param itf
     * <tt>true</tt> if <tt>owner</tt> is an interface.
     * @return a new or already existing method reference item.
     */
    internal fun newMethodItem(owner: String, name: String,
                               desc: String, itf: Boolean): Item {
        val type = if (itf) IMETH else METH
        key3[type, owner, name] = desc
        var result = get(key3)
        if (result == null) {
            put122(type, newClass(owner), newNameType(name, desc))
            result = Item(index++, key3)
            put(result)
        }
        return result
    }

    /**
     * Adds a method reference to the constant pool of the class being build.
     * Does nothing if the constant pool already contains a similar item.
     * *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param owner
     * the internal name of the method's owner class.
     * @param name
     * the method's name.
     * @param desc
     * the method's descriptor.
     * @param itf
     * <tt>true</tt> if <tt>owner</tt> is an interface.
     * @return the index of a new or already existing method reference item.
     */
    fun newMethod(owner: String, name: String,
                  desc: String, itf: Boolean): Int {
        return newMethodItem(owner, name, desc, itf).index
    }

    /**
     * Adds an integer to the constant pool of the class being build. Does
     * nothing if the constant pool already contains a similar item.
     *
     * @param value
     * the int value.
     * @return a new or already existing int item.
     */
    internal fun newInteger(value: Int): Item {
        key.set(value)
        var result = get(key)
        if (result == null) {
            pool.putByte(INT).putInt(value)
            result = Item(index++, key)
            put(result)
        }
        return result
    }

    /**
     * Adds a float to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item.
     *
     * @param value
     * the float value.
     * @return a new or already existing float item.
     */
    internal fun newFloat(value: Float): Item {
        key.set(value)
        var result = get(key)
        if (result == null) {
            pool.putByte(FLOAT).putInt(key.intVal)
            result = Item(index++, key)
            put(result)
        }
        return result
    }

    /**
     * Adds a long to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item.
     *
     * @param value
     * the long value.
     * @return a new or already existing long item.
     */
    internal fun newLong(value: Long): Item {
        key.set(value)
        var result = get(key)
        if (result == null) {
            pool.putByte(LONG).putLong(value)
            result = Item(index, key)
            index += 2
            put(result)
        }
        return result
    }

    /**
     * Adds a double to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item.
     *
     * @param value
     * the double value.
     * @return a new or already existing double item.
     */
    internal fun newDouble(value: Double): Item {
        key.set(value)
        var result = get(key)
        if (result == null) {
            pool.putByte(DOUBLE).putLong(key.longVal)
            result = Item(index, key)
            index += 2
            put(result)
        }
        return result
    }

    /**
     * Adds a string to the constant pool of the class being build. Does nothing
     * if the constant pool already contains a similar item.
     *
     * @param value
     * the String value.
     * @return a new or already existing string item.
     */
    private fun newString(value: String): Item {
        key2[STR, value, null] = null
        var result = get(key2)
        if (result == null) {
            pool.put12(STR, newUTF8(value))
            result = Item(index++, key2)
            put(result)
        }
        return result
    }

    /**
     * Adds a name and type to the constant pool of the class being build. Does
     * nothing if the constant pool already contains a similar item. *This
     * method is intended for [Attribute] sub classes, and is normally not
     * needed by class generators or adapters.*
     *
     * @param name
     * a name.
     * @param desc
     * a type descriptor.
     * @return the index of a new or already existing name and type item.
     */
    fun newNameType(name: String, desc: String): Int {
        return newNameTypeItem(name, desc).index
    }

    /**
     * Adds a name and type to the constant pool of the class being build. Does
     * nothing if the constant pool already contains a similar item.
     *
     * @param name
     * a name.
     * @param desc
     * a type descriptor.
     * @return a new or already existing name and type item.
     */
    internal fun newNameTypeItem(name: String, desc: String): Item {
        key2[NAME_TYPE, name, desc] = null
        var result = get(key2)
        if (result == null) {
            put122(NAME_TYPE, newUTF8(name), newUTF8(desc))
            result = Item(index++, key2)
            put(result)
        }
        return result
    }

    /**
     * Adds the given internal name to [.typeTable] and returns its index.
     * Does nothing if the type table already contains this internal name.
     *
     * @param type
     * the internal name to be added to the type table.
     * @return the index of this internal name in the type table.
     */
    internal fun addType(type: String?): Int {
        key[TYPE_NORMAL, type, null] = null
        var result = get(key)
        if (result == null) {
            result = addType(key)
        }
        return result.index
    }

    /**
     * Adds the given "uninitialized" type to [.typeTable] and returns its
     * index. This method is used for UNINITIALIZED types, made of an internal
     * name and a bytecode offset.
     *
     * @param type
     * the internal name to be added to the type table.
     * @param offset
     * the bytecode offset of the NEW instruction that created this
     * UNINITIALIZED type value.
     * @return the index of this internal name in the type table.
     */
    internal fun addUninitializedType(type: String?, offset: Int): Int {
        key.type = TYPE_UNINIT
        key.intVal = offset
        key.strVal1 = type
        key.hashCode = 0x7FFFFFFF and TYPE_UNINIT + type.hashCode() + offset
        var result = get(key)
        if (result == null) {
            result = addType(key)
        }
        return result.index
    }

    /**
     * Adds the given Item to [.typeTable].
     *
     * @param item
     * the value to be added to the type table.
     * @return the added Item, which a new Item instance with the same value as
     * the given Item.
     */
    private fun addType(item: Item): Item {
        ++typeCount
        val result = Item(typeCount.toInt(), key)
        put(result)
        if (typeTable == null) {
            typeTable = arrayOfNulls(16)
        }
        if (typeCount.toInt() == typeTable!!.size) {
            val newTable = arrayOfNulls<Item>(2 * typeTable!!.size)
            typeTable!!.copyInto(newTable,0,0,typeTable!!.size)
            typeTable = newTable
        }
        typeTable!![typeCount.toInt()] = result
        return result
    }

    /**
     * Returns the index of the common super type of the two given types. This
     * method calls [.getCommonSuperClass] and caches the result in the
     * [.items] hash table to speedup future calls with the same
     * parameters.
     *
     * @param type1
     * index of an internal name in [.typeTable].
     * @param type2
     * index of an internal name in [.typeTable].
     * @return the index of the common super type of the two given types.
     */
    internal fun getMergedType(type1: Int, type2: Int): Int {
        key2.type = TYPE_MERGED
        key2.longVal = (type1 or ((type2.toLong() shl 32).toInt())).toLong()
        key2.hashCode = 0x7FFFFFFF and TYPE_MERGED + type1 + type2
        var result = get(key2)
        if (result == null) {
            val t = typeTable!![type1]?.strVal1
            val u = typeTable!![type2]?.strVal1
            key2.intVal = addType(getCommonSuperClass(t, u))
            result = Item(0.toShort().toInt(), key2)
            put(result)
        }
        return result.intVal
    }

    /**
     * Returns the common super type of the two given types. The default
     * implementation of this method *loads* the two given classes and uses
     * the java.lang.Class methods to find the common super class. It can be
     * overridden to compute this common super type in other ways, in particular
     * without actually loading any class, or to take into account the class
     * that is currently being generated by this ClassWriter, which can of
     * course not be loaded since it is under construction.
     *
     * @param type1
     * the internal name of a class.
     * @param type2
     * the internal name of another class.
     * @return the internal name of the common super class of the two given
     * classes.
     ** */
    protected fun getCommonSuperClass(type1: String?, type2: String?): String {
//        var c: Class<*>
//        val d: Class<*>
//        val classLoader = javaClass.classLoader
//        try {
//            c = Class.forName(type1!!.replace('/', '.'), false, classLoader)
//            d = Class.forName(type2!!.replace('/', '.'), false, classLoader)
//        } catch (e: Exception) {
//            throw RuntimeException(e.toString())
//        }
//
//        if (c.isAssignableFrom(d)) {
            return type1!!
//        }
//        if (d.isAssignableFrom(c)) {
//            return type2
//        }
//        if (c.isInterface || d.isInterface) {
//            return "java/lang/Object"
//        } else {
//            do {
//                c = c.superclass
//            } while (!c.isAssignableFrom(d))
//            return c.name.replace('.', '/')
//        }
        throw RuntimeException("oooops")
    }

    /**
     * Returns the constant pool's hash table item which is equal to the given
     * item.
     *
     * @param key
     * a constant pool item.
     * @return the constant pool's hash table item which is equal to the given
     * item, or <tt>null</tt> if there is no such item.
     */
    private operator fun get(key: Item): Item? {
        var i: Item? = items[key.hashCode % items.size]
        while (i != null && (i.type != key.type || !key.isEqualTo(i))) {
            i = i.next
        }
        return i
    }

    /**
     * Puts the given item in the constant pool's hash table. The hash table
     * *must* not already contains this item.
     *
     * @param i
     * the item to be added to the constant pool's hash table.
     */
    private fun put(i: Item) {
        if (index + typeCount > threshold) {
            val ll = items.size
            val nl = ll * 2 + 1
            val newItems = arrayOfNulls<Item>(nl)
            for (l in ll - 1 downTo 0) {
                var j: Item? = items[l]
                while (j != null) {
                    val index = j.hashCode % newItems.size
                    val k = j.next
                    j.next = newItems[index]
                    newItems[index] = j
                    j = k
                }
            }
            items = newItems
            threshold = (nl * 0.75).toInt()
        }
        val index = i.hashCode % items.size
        i.next = items[index]
        items[index] = i
    }

    /**
     * Puts one byte and two shorts into the constant pool.
     *
     * @param b
     * a byte.
     * @param s1
     * a short.
     * @param s2
     * another short.
     */
    private fun put122(b: Int, s1: Int, s2: Int) {
        pool.put12(b, s1).putShort(s2)
    }

    /**
     * Puts two bytes and one short into the constant pool.
     *
     * @param b1
     * a byte.
     * @param b2
     * another byte.
     * @param s
     * a short.
     */
    private fun put112(b1: Int, b2: Int, s: Int) {
        pool.put11(b1, b2).putShort(s)
    }

    companion object {

        /**
         * Flag to automatically compute the maximum stack size and the maximum
         * number of local variables of methods. If this flag is set, then the
         * arguments of the [visitMaxs][MethodVisitor.visitMaxs] method of the
         * [MethodVisitor] returned by the [visitMethod][.visitMethod]
         * method will be ignored, and computed automatically from the signature and
         * the bytecode of each method.
         *
         * @see .ClassWriter
         */
        val COMPUTE_MAXS = 1

        /**
         * Flag to automatically compute the stack map frames of methods from
         * scratch. If this flag is set, then the calls to the
         * [MethodVisitor.visitFrame] method are ignored, and the stack map
         * frames are recomputed from the methods bytecode. The arguments of the
         * [visitMaxs][MethodVisitor.visitMaxs] method are also ignored and
         * recomputed from the bytecode. In other words, computeFrames implies
         * computeMaxs.
         *
         * @see .ClassWriter
         */
        val COMPUTE_FRAMES = 2

        /**
         * Pseudo access flag to distinguish between the synthetic attribute and the
         * synthetic access flag.
         */
        internal val ACC_SYNTHETIC_ATTRIBUTE = 0x40000

        /**
         * Factor to convert from ACC_SYNTHETIC_ATTRIBUTE to Opcode.ACC_SYNTHETIC.
         */
        internal val TO_ACC_SYNTHETIC = ACC_SYNTHETIC_ATTRIBUTE / Opcodes.ACC_SYNTHETIC

        /**
         * The type of instructions without any argument.
         */
        internal val NOARG_INSN = 0

        /**
         * The type of instructions with an signed byte argument.
         */
        internal val SBYTE_INSN = 1

        /**
         * The type of instructions with an signed short argument.
         */
        internal val SHORT_INSN = 2

        /**
         * The type of instructions with a local variable index argument.
         */
        internal val VAR_INSN = 3

        /**
         * The type of instructions with an implicit local variable index argument.
         */
        internal val IMPLVAR_INSN = 4

        /**
         * The type of instructions with a type descriptor argument.
         */
        internal val TYPE_INSN = 5

        /**
         * The type of field and method invocations instructions.
         */
        internal val FIELDORMETH_INSN = 6

        /**
         * The type of the INVOKEINTERFACE/INVOKEDYNAMIC instruction.
         */
        internal val ITFMETH_INSN = 7

        /**
         * The type of the INVOKEDYNAMIC instruction.
         */
        internal val INDYMETH_INSN = 8

        /**
         * The type of instructions with a 2 bytes bytecode offset label.
         */
        internal val LABEL_INSN = 9

        /**
         * The type of instructions with a 4 bytes bytecode offset label.
         */
        internal val LABELW_INSN = 10

        /**
         * The type of the LDC instruction.
         */
        internal val LDC_INSN = 11

        /**
         * The type of the LDC_W and LDC2_W instructions.
         */
        internal val LDCW_INSN = 12

        /**
         * The type of the IINC instruction.
         */
        internal val IINC_INSN = 13

        /**
         * The type of the TABLESWITCH instruction.
         */
        internal val TABL_INSN = 14

        /**
         * The type of the LOOKUPSWITCH instruction.
         */
        internal val LOOK_INSN = 15

        /**
         * The type of the MULTIANEWARRAY instruction.
         */
        internal val MANA_INSN = 16

        /**
         * The type of the WIDE instruction.
         */
        internal val WIDE_INSN = 17

        /**
         * The instruction types of all JVM opcodes.
         */
        internal val TYPE: ByteArray

        /**
         * The type of CONSTANT_Class constant pool items.
         */
        internal val CLASS = 7

        /**
         * The type of CONSTANT_Fieldref constant pool items.
         */
        internal val FIELD = 9

        /**
         * The type of CONSTANT_Methodref constant pool items.
         */
        internal val METH = 10

        /**
         * The type of CONSTANT_InterfaceMethodref constant pool items.
         */
        internal val IMETH = 11

        /**
         * The type of CONSTANT_String constant pool items.
         */
        internal val STR = 8

        /**
         * The type of CONSTANT_Integer constant pool items.
         */
        internal val INT = 3

        /**
         * The type of CONSTANT_Float constant pool items.
         */
        internal val FLOAT = 4

        /**
         * The type of CONSTANT_Long constant pool items.
         */
        internal val LONG = 5

        /**
         * The type of CONSTANT_Double constant pool items.
         */
        internal val DOUBLE = 6

        /**
         * The type of CONSTANT_NameAndType constant pool items.
         */
        internal val NAME_TYPE = 12

        /**
         * The type of CONSTANT_Utf8 constant pool items.
         */
        internal val UTF8 = 1

        /**
         * The type of CONSTANT_MethodType constant pool items.
         */
        internal val MTYPE = 16

        /**
         * The type of CONSTANT_MethodHandle constant pool items.
         */
        internal val HANDLE = 15

        /**
         * The type of CONSTANT_InvokeDynamic constant pool items.
         */
        internal val INDY = 18

        /**
         * The base value for all CONSTANT_MethodHandle constant pool items.
         * Internally, ASM store the 9 variations of CONSTANT_MethodHandle into 9
         * different items.
         */
        internal val HANDLE_BASE = 20

        /**
         * Normal type Item stored in the ClassWriter [ClassWriter.typeTable],
         * instead of the constant pool, in order to avoid clashes with normal
         * constant pool items in the ClassWriter constant pool's hash table.
         */
        internal val TYPE_NORMAL = 30

        /**
         * Uninitialized type Item stored in the ClassWriter
         * [ClassWriter.typeTable], instead of the constant pool, in order to
         * avoid clashes with normal constant pool items in the ClassWriter constant
         * pool's hash table.
         */
        internal val TYPE_UNINIT = 31

        /**
         * Merged type Item stored in the ClassWriter [ClassWriter.typeTable],
         * instead of the constant pool, in order to avoid clashes with normal
         * constant pool items in the ClassWriter constant pool's hash table.
         */
        internal val TYPE_MERGED = 32

        /**
         * The type of BootstrapMethods items. These items are stored in a special
         * class attribute named BootstrapMethods and not in the constant pool.
         */
        internal val BSM = 33

        // ------------------------------------------------------------------------
        // Static initializer
        // ------------------------------------------------------------------------

        /**
         * Computes the instruction types of JVM opcodes.
         */
        init {
            var i: Int
            val b = ByteArray(220)
            val s = ("AAAAAAAAAAAAAAAABCLMMDDDDDEEEEEEEEEEEEEEEEEEEEAAAAAAAADD"
                    + "DDDEEEEEEEEEEEEEEEEEEEEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAANAAAAAAAAAAAAAAAAAAAAJJJJJJJJJJJJJJJJDOPAA"
                    + "AAAAGGGGGGGHIFBFAAFFAARQJJKKJJJJJJJJJJJJJJJJJJ")
            i = 0
            while (i < b.size) {
                b[i] = (s[i] - 'A').toByte()
                ++i
            }
            TYPE = b

            // code to generate the above string
            //
            // // SBYTE_INSN instructions
            // b[Constants.NEWARRAY] = SBYTE_INSN;
            // b[Constants.BIPUSH] = SBYTE_INSN;
            //
            // // SHORT_INSN instructions
            // b[Constants.SIPUSH] = SHORT_INSN;
            //
            // // (IMPL)VAR_INSN instructions
            // b[Constants.RET] = VAR_INSN;
            // for (i = Constants.ILOAD; i <= Constants.ALOAD; ++i) {
            // b[i] = VAR_INSN;
            // }
            // for (i = Constants.ISTORE; i <= Constants.ASTORE; ++i) {
            // b[i] = VAR_INSN;
            // }
            // for (i = 26; i <= 45; ++i) { // ILOAD_0 to ALOAD_3
            // b[i] = IMPLVAR_INSN;
            // }
            // for (i = 59; i <= 78; ++i) { // ISTORE_0 to ASTORE_3
            // b[i] = IMPLVAR_INSN;
            // }
            //
            // // TYPE_INSN instructions
            // b[Constants.NEW] = TYPE_INSN;
            // b[Constants.ANEWARRAY] = TYPE_INSN;
            // b[Constants.CHECKCAST] = TYPE_INSN;
            // b[Constants.INSTANCEOF] = TYPE_INSN;
            //
            // // (Set)FIELDORMETH_INSN instructions
            // for (i = Constants.GETSTATIC; i <= Constants.INVOKESTATIC; ++i) {
            // b[i] = FIELDORMETH_INSN;
            // }
            // b[Constants.INVOKEINTERFACE] = ITFMETH_INSN;
            // b[Constants.INVOKEDYNAMIC] = INDYMETH_INSN;
            //
            // // LABEL(W)_INSN instructions
            // for (i = Constants.IFEQ; i <= Constants.JSR; ++i) {
            // b[i] = LABEL_INSN;
            // }
            // b[Constants.IFNULL] = LABEL_INSN;
            // b[Constants.IFNONNULL] = LABEL_INSN;
            // b[200] = LABELW_INSN; // GOTO_W
            // b[201] = LABELW_INSN; // JSR_W
            // // temporary opcodes used internally by ASM - see Label and
            // MethodWriter
            // for (i = 202; i < 220; ++i) {
            // b[i] = LABEL_INSN;
            // }
            //
            // // LDC(_W) instructions
            // b[Constants.LDC] = LDC_INSN;
            // b[19] = LDCW_INSN; // LDC_W
            // b[20] = LDCW_INSN; // LDC2_W
            //
            // // special instructions
            // b[Constants.IINC] = IINC_INSN;
            // b[Constants.TABLESWITCH] = TABL_INSN;
            // b[Constants.LOOKUPSWITCH] = LOOK_INSN;
            // b[Constants.MULTIANEWARRAY] = MANA_INSN;
            // b[196] = WIDE_INSN; // WIDE
            //
            // for (i = 0; i < b.length; ++i) {
            // System.err.print((char)('A' + b[i]));
            // }
            // System.err.
        }
    }
}
