package org.objectweb.kn.asm

class ClassReader
constructor(val b: ByteArray, off: Int = 0, len: Int = b.size) {
    private val items: IntArray
    private val strings: Array<String?>
    val maxStringLength: Int
    val header: Int
    val access: Int
        get() = readUnsignedShort(header)
    val className: String?
        get() = readClass(header + 2, CharArray(maxStringLength))
    val superName: String?
        get() = readClass(header + 4, CharArray(maxStringLength))

    /**
     * Returns the internal names of the class's interfaces (see
     * [getInternalName][Type.getInternalName]).
     *
     * @return the array of internal names for all implemented interfaces or
     * <tt>null</tt>.
     *
     * @see ClassVisitor.visit
     */
    val interfaces: Array<String>
        get() {
//            var index = header + 6
//            val n = readUnsignedShort(index)
//            val interfaces = arrayOfNulls<String>(n)
//            if (n > 0) {
//                val buf = CharArray(maxStringLength)
//                for (i in 0 until n) {
//                    index += 2
//                    interfaces[i] = readClass(index, buf)
//                }
//            }
//            return interfaces
            throw RuntimeException("oops")
        }

    /**
     * Returns the start index of the attribute_info structure of this class.
     *
     * @return the start index of the attribute_info structure of this class.
     */
    private// skips the header
    // skips fields and methods
    // the attribute_info structure starts just after the methods
    val attributes: Int
        get() {
            var u = header + 8 + readUnsignedShort(header + 6) * 2
            for (i in readUnsignedShort(u) downTo 1) {
                for (j in readUnsignedShort(u + 8) downTo 1) {
                    u += 6 + readInt(u + 12)
                }
                u += 8
            }
            u += 2
            for (i in readUnsignedShort(u) downTo 1) {
                for (j in readUnsignedShort(u + 8) downTo 1) {
                    u += 6 + readInt(u + 12)
                }
                u += 8
            }
            return u + 2
        }

    // ------------------------------------------------------------------------
    // Utility methods: low level parsing
    // ------------------------------------------------------------------------

    /**
     * Returns the number of constant pool items in [b][.b].
     *
     * @return the number of constant pool items in [b][.b].
     */
    val itemCount: Int
        get() = items.size

    init {
        // checks the class version
        if (readShort(off + 6) > Opcodes.V1_8) {
            throw IllegalArgumentException()
        }
        // parses the constant pool
        items = IntArray(readUnsignedShort(off + 8))
        val n = items.size
        strings = arrayOfNulls(n)
        var max = 0
        var index = off + 10
        var i = 1
        while (i < n) {
            items[i] = index + 1
            val size: Int
            when (b[index].toInt()) {
                ClassWriter.FIELD, ClassWriter.METH, ClassWriter.IMETH, ClassWriter.INT, ClassWriter.FLOAT, ClassWriter.NAME_TYPE, ClassWriter.INDY -> size =
                        5
                ClassWriter.LONG, ClassWriter.DOUBLE -> {
                    size = 9
                    ++i
                }
                ClassWriter.UTF8 -> {
                    size = 3 + readUnsignedShort(index + 1)
                    if (size > max) {
                        max = size
                    }
                }
                ClassWriter.HANDLE -> size = 4
                // case ClassWriter.CLASS:
                // case ClassWriter.STR:
                // case ClassWriter.MTYPE
                else -> size = 3
            }
            index += size
            ++i
        }
        maxStringLength = max
        // the class header information starts just after the constant pool
        header = index
    }

    /**
     * Copies the constant pool data into the given [ClassWriter]. Should
     * be called before the [.accept] method.
     *
     * @param classWriter
     * the [ClassWriter] to copy constant pool into.
     */
    internal fun copyPool(classWriter: ClassWriter) {
        val buf = CharArray(maxStringLength)
        val ll = items.size
        val items2 = arrayOfNulls<Item>(ll)
        var i = 1
        while (i < ll) {
            var index = items[i]
            val tag = b[index - 1].toInt()
            val item = Item(i)
            val nameType: Int
            when (tag) {
                ClassWriter.FIELD, ClassWriter.METH, ClassWriter.IMETH -> {
                    nameType = items[readUnsignedShort(index + 2)]
                    item[tag, readClass(index, buf)!!, readUTF8(nameType, buf)!!] = readUTF8(nameType + 2, buf)!!
                }
                ClassWriter.INT -> item.set(readInt(index))
                ClassWriter.FLOAT -> item.set(readInt(index).toFloat().toBits().toFloat())
                ClassWriter.NAME_TYPE -> item[tag, readUTF8(index, buf)!!, readUTF8(index + 2, buf)!!] = null
                ClassWriter.LONG -> {
                    item.set(readLong(index))
                    ++i
                }
                ClassWriter.DOUBLE -> {
                    item.set(readLong(index).toDouble().toBits().toDouble())
                    ++i
                }
                ClassWriter.UTF8 -> {
                    var s: String? = strings[i]
                    if (s == null) {
                        index = items[i]
                        strings[i] = readUTF(
                            index + 2,
                            readUnsignedShort(index), buf
                        )
                        s = strings[i]
                    }
                    item[tag, s!!, null] = null
                }
                ClassWriter.HANDLE -> {
                    val fieldOrMethodRef = items[readUnsignedShort(index + 1)]
                    nameType = items[readUnsignedShort(fieldOrMethodRef + 2)]
                    item[ClassWriter.HANDLE_BASE + readByte(index), readClass(fieldOrMethodRef, buf)!!, readUTF8(
                        nameType,
                        buf
                    )!!] = readUTF8(nameType + 2, buf)!!
                }
                ClassWriter.INDY -> {
                    if (classWriter.bootstrapMethods == null) {
                        copyBootstrapMethods(classWriter, items2, buf)
                    }
                    nameType = items[readUnsignedShort(index + 2)]
                    item[readUTF8(nameType, buf)!!, readUTF8(nameType + 2, buf)!!] = readUnsignedShort(index)
                }
                // case ClassWriter.STR:
                // case ClassWriter.CLASS:
                // case ClassWriter.MTYPE
                else -> item[tag, readUTF8(index, buf), null] = null
            }

            val index2 = item.hashCode % items2.size
            item.next = items2[index2]
            items2[index2] = item
            i++
        }

        val off = items[1] - 1
        classWriter.pool.putByteArray(b, off, header - off)
        classWriter.items = items2
        classWriter.threshold = (0.75 * ll).toInt()
        classWriter.index = ll
    }

    /**
     * Copies the bootstrap method data into the given [ClassWriter].
     * Should be called before the [.accept] method.
     *
     * @param classWriter
     * the [ClassWriter] to copy bootstrap methods into.
     */
    private fun copyBootstrapMethods(
        classWriter: ClassWriter,
        items: Array<Item?>, c: CharArray
    ) {
        // finds the "BootstrapMethods" attribute
        var u = attributes
        var found = false
        for (i in readUnsignedShort(u) downTo 1) {
            val attrName = readUTF8(u + 2, c)
            if ("BootstrapMethods" == attrName) {
                found = true
                break
            }
            u += 6 + readInt(u + 4)
        }
        if (!found) {
            return
        }
        // copies the bootstrap methods in the class writer
        val boostrapMethodCount = readUnsignedShort(u + 8)
        var j = 0
        var v = u + 10
        while (j < boostrapMethodCount) {
            val position = v - u - 10
            var hashCode = readConst(readUnsignedShort(v), c)!!.hashCode()
            for (k in readUnsignedShort(v + 2) downTo 1) {
                hashCode = hashCode xor readConst(readUnsignedShort(v + 4), c)!!.hashCode()
                v += 2
            }
            v += 4
            val item = Item(j)
            item[position] = hashCode and 0x7FFFFFFF
            val index = item.hashCode % items.size
            item.next = items[index]
            items[index] = item
            j++
        }
        val attrSize = readInt(u + 4)
        val bootstrapMethods = ByteVector(attrSize + 62)
        bootstrapMethods.putByteArray(b, u + 10, attrSize - 2)
        classWriter.bootstrapMethodsCount = boostrapMethodCount
        classWriter.bootstrapMethods = bootstrapMethods
    }


    /**
     * Constructs a new [ClassReader] object.
     *
     * @param name
     * the binary qualified name of the class to be read.
     * @throws IOException
     * if an exception occurs during reading.
     */


    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------

    /**
     * Makes the given visitor visit the Java class of this [ClassReader]
     * . This class is the one specified in the constructor (see
     * [ClassReader][.ClassReader]).
     *
     * @param classVisitor
     * the visitor that must visit this class.
     * @param flags
     * option flags that can be used to modify the default behavior
     * of this class. See [.SKIP_DEBUG], [.EXPAND_FRAMES]
     * , [.SKIP_FRAMES], [.SKIP_CODE].
     */
    fun accept(classVisitor: ClassVisitor, flags: Int) {
        accept(classVisitor, arrayOfNulls(0), flags)
    }

    /**
     * Makes the given visitor visit the Java class of this [ClassReader].
     * This class is the one specified in the constructor (see
     * [ClassReader][.ClassReader]).
     *
     * @param classVisitor
     * the visitor that must visit this class.
     * @param attrs
     * prototypes of the attributes that must be parsed during the
     * visit of the class. Any attribute whose type is not equal to
     * the type of one the prototypes will not be parsed: its byte
     * array value will be passed unchanged to the ClassWriter.
     * *This may corrupt it if this value contains references to
     * the constant pool, or has syntactic or semantic links with a
     * class element that has been transformed by a class adapter
     * between the reader and the writer*.
     * @param flags
     * option flags that can be used to modify the default behavior
     * of this class. See [.SKIP_DEBUG], [.EXPAND_FRAMES]
     * , [.SKIP_FRAMES], [.SKIP_CODE].
     */
    fun accept(
        classVisitor: ClassVisitor,
        attrs: Array<Attribute?>, flags: Int
    ) {
        var u = header // current offset in the class file
        val c = CharArray(maxStringLength) // buffer used to read strings

        val context = Context()
        context.attrs = attrs
        context.flags = flags
        context.buffer = c

        // reads the class declaration
        var access = readUnsignedShort(u)
        val name = readClass(u + 2, c)
        val superClass = readClass(u + 4, c)
        val interfaces = arrayOfNulls<String>(readUnsignedShort(u + 6))
        u += 8
        for (i in interfaces.indices) {
            interfaces[i] = readClass(u, c)
            u += 2
        }

        // reads the class attributes
        var signature: String? = null
        var sourceFile: String? = null
        var sourceDebug: String? = null
        var enclosingOwner: String? = null
        var enclosingName: String? = null
        var enclosingDesc: String? = null
        var anns = 0
        var ianns = 0
        var tanns = 0
        var itanns = 0
        var innerClasses = 0
        var attributes: Attribute? = null

        u = this.attributes
        for (i in readUnsignedShort(u) downTo 1) {
            val attrName = readUTF8(u + 2, c)
            if ("SourceFile" == attrName) {
                sourceFile = readUTF8(u + 8, c)
            } else if ("InnerClasses" == attrName) {
                innerClasses = u + 8
            } else if ("EnclosingMethod" == attrName) {
                enclosingOwner = readClass(u + 8, c)
                val item = readUnsignedShort(u + 10)
                if (item != 0) {
                    enclosingName = readUTF8(items[item], c)
                    enclosingDesc = readUTF8(items[item] + 2, c)
                }
            } else if (SIGNATURES && "Signature" == attrName) {
                signature = readUTF8(u + 8, c)
            } else if (ANNOTATIONS && "RuntimeVisibleAnnotations" == attrName) {
                anns = u + 8
            } else if (ANNOTATIONS && "RuntimeVisibleTypeAnnotations" == attrName) {
                tanns = u + 8
            } else if ("Deprecated" == attrName) {
                access = access or Opcodes.ACC_DEPRECATED
            } else if ("Synthetic" == attrName) {
                access = access or (Opcodes.ACC_SYNTHETIC or ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)
            } else if ("SourceDebugExtension" == attrName) {
                val len = readInt(u + 4)
                sourceDebug = readUTF(u + 8, len, CharArray(len))
            } else if (ANNOTATIONS && "RuntimeInvisibleAnnotations" == attrName) {
                ianns = u + 8
            } else if (ANNOTATIONS && "RuntimeInvisibleTypeAnnotations" == attrName) {
                itanns = u + 8
            } else if ("BootstrapMethods" == attrName) {
                val bootstrapMethods = IntArray(readUnsignedShort(u + 8))
                var j = 0
                var v = u + 10
                while (j < bootstrapMethods.size) {
                    bootstrapMethods[j] = v
                    v += 2 + readUnsignedShort(v + 2) shl 1
                    j++
                }
                context.bootstrapMethods = bootstrapMethods
            } else {

                val attr = readAttribute(
                    attrs, attrName, u + 8,
                    readInt(u + 4), c, -1, null
                )
                if (attr != null) {
                    attr.next = attributes
                    attributes = attr
                }
            }
            u += 6 + readInt(u + 4)
        }


        // visits the class declaration
        classVisitor.visit(
            readInt(items[1] - 7), access, name!!, signature,
            superClass!!, interfaces
        )

        // visits the source and debug info
        if (flags and SKIP_DEBUG == 0 && (sourceFile != null || sourceDebug != null)) {
            classVisitor.visitSource(sourceFile!!, sourceDebug)
        }

        // visits the outer class
        if (enclosingOwner != null) {
            classVisitor.visitOuterClass(
                enclosingOwner, enclosingName!!,
                enclosingDesc!!
            )
        }

        // visits the class annotations and type annotations
        if (ANNOTATIONS && anns != 0) {
            var i = readUnsignedShort(anns)
            var v = anns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    classVisitor.visitAnnotation(readUTF8(v, c)!!, true)
                )
                --i
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            var i = readUnsignedShort(ianns)
            var v = ianns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    classVisitor.visitAnnotation(readUTF8(v, c)!!, false)
                )
                --i
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            var i = readUnsignedShort(tanns)
            var v = tanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    classVisitor.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, true
                    )
                )
                --i
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            var i = readUnsignedShort(itanns)
            var v = itanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    classVisitor.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, false
                    )
                )
                --i
            }
        }

        // visits the attributes
        while (attributes != null) {
            val attr = attributes.next
            attributes.next = null
            classVisitor.visitAttribute(attributes)
            attributes = attr
        }

        // visits the inner classes
        if (innerClasses != 0) {
            var v = innerClasses + 2
            for (i in readUnsignedShort(innerClasses) downTo 1) {
                classVisitor.visitInnerClass(
                    readClass(v, c)!!,
                    readClass(v + 2, c), readUTF8(v + 4, c),
                    readUnsignedShort(v + 6)
                )
                v += 8
            }
        }

        // visits the fields and methods
        u = header + 10 + 2 * interfaces.size
        for (i in readUnsignedShort(u - 2) downTo 1) {
            u = readField(classVisitor, context, u)
        }
        u += 2
        for (i in readUnsignedShort(u - 2) downTo 1) {
            u = readMethod(classVisitor, context, u)
        }

        // visits the end of the class
        classVisitor.visitEnd()
    }

    /**
     * Reads a field and makes the given visitor visit it.
     *
     * @param classVisitor
     * the visitor that must visit the field.
     * @param context
     * information about the class being parsed.
     * @param u
     * the start offset of the field in the class file.
     * @return the offset of the first byte following the field in the class.
     */
    private fun readField(
        classVisitor: ClassVisitor,
        context: Context, u: Int
    ): Int {
        var u = u
        // reads the field declaration
        val c = context.buffer
        var access = readUnsignedShort(u)
        val name = readUTF8(u + 2, c)
        val desc = readUTF8(u + 4, c)
        u += 6

        // reads the field attributes
        var signature: String? = null
        var anns = 0
        var ianns = 0
        var tanns = 0
        var itanns = 0
        var value: Any? = null
        var attributes: Attribute? = null

        for (i in readUnsignedShort(u) downTo 1) {
            val attrName = readUTF8(u + 2, c)
            // tests are sorted in decreasing frequency order
            // (based on frequencies observed on typical classes)
            if ("ConstantValue" == attrName) {
                val item = readUnsignedShort(u + 8)
                value = if (item == 0) null else readConst(item, c)
            } else if (SIGNATURES && "Signature" == attrName) {
                signature = readUTF8(u + 8, c)
            } else if ("Deprecated" == attrName) {
                access = access or Opcodes.ACC_DEPRECATED
            } else if ("Synthetic" == attrName) {
                access = access or (Opcodes.ACC_SYNTHETIC or ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)
            } else if (ANNOTATIONS && "RuntimeVisibleAnnotations" == attrName) {
                anns = u + 8
            } else if (ANNOTATIONS && "RuntimeVisibleTypeAnnotations" == attrName) {
                tanns = u + 8
            } else if (ANNOTATIONS && "RuntimeInvisibleAnnotations" == attrName) {
                ianns = u + 8
            } else if (ANNOTATIONS && "RuntimeInvisibleTypeAnnotations" == attrName) {
                itanns = u + 8
            } else {
                val attr = readAttribute(
                    context.attrs!!, attrName, u + 8,
                    readInt(u + 4), c, -1, null
                )
                if (attr != null) {
                    attr.next = attributes
                    attributes = attr
                }
            }
            u += 6 + readInt(u + 4)
        }
        u += 2

        // visits the field declaration

        val fv = classVisitor.visitField(
            access, name!!, desc!!,
            signature, value
        ) ?: return u

        // visits the field annotations and type annotations
        if (ANNOTATIONS && anns != 0) {
            var i = readUnsignedShort(anns)
            var v = anns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    fv.visitAnnotation(readUTF8(v, c)!!, true)
                )
                --i
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            var i = readUnsignedShort(ianns)
            var v = ianns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    fv.visitAnnotation(readUTF8(v, c)!!, false)
                )
                --i
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            var i = readUnsignedShort(tanns)
            var v = tanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    fv.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, true
                    )
                )
                --i
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            var i = readUnsignedShort(itanns)
            var v = itanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    fv.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, false
                    )
                )
                --i
            }
        }

        // visits the field attributes
        while (attributes != null) {
            val attr = attributes.next
            attributes.next = null
            fv.visitAttribute(attributes)
            attributes = attr
        }

        // visits the end of the field
        fv.visitEnd()

        return u
    }

    /**
     * Reads a method and makes the given visitor visit it.
     *
     * @param classVisitor
     * the visitor that must visit the method.
     * @param context
     * information about the class being parsed.
     * @param u
     * the start offset of the method in the class file.
     * @return the offset of the first byte following the method in the class.
     */
    private fun readMethod(
        classVisitor: ClassVisitor,
        context: Context, u: Int
    ): Int {
        var u = u
        // reads the method declaration
        val c = context.buffer
        context.access = readUnsignedShort(u)
        context.name = readUTF8(u + 2, c)
        context.desc = readUTF8(u + 4, c)
        u += 6

        // reads the method attributes
        var code = 0
        var exception = 0
        var exceptions: Array<String?>? = null
        var signature: String? = null
        var methodParameters = 0
        var anns = 0
        var ianns = 0
        var tanns = 0
        var itanns = 0
        var dann = 0
        var mpanns = 0
        var impanns = 0
        val firstAttribute = u
        var attributes: Attribute? = null

        for (i in readUnsignedShort(u) downTo 1) {
            val attrName = readUTF8(u + 2, c)
            // tests are sorted in decreasing frequency order
            // (based on frequencies observed on typical classes)
            if ("Code" == attrName) {
                if (context.flags and SKIP_CODE == 0) {
                    code = u + 8
                }
            } else if ("Exceptions" == attrName) {
                exceptions = arrayOfNulls(readUnsignedShort(u + 8))
                exception = u + 10
                for (j in exceptions.indices) {
                    exceptions[j] = readClass(exception, c)
                    exception += 2
                }
            } else if (SIGNATURES && "Signature" == attrName) {
                signature = readUTF8(u + 8, c)
            } else if ("Deprecated" == attrName) {
                context.access = context.access or Opcodes.ACC_DEPRECATED
            } else if (ANNOTATIONS && "RuntimeVisibleAnnotations" == attrName) {
                anns = u + 8
            } else if (ANNOTATIONS && "RuntimeVisibleTypeAnnotations" == attrName) {
                tanns = u + 8
            } else if (ANNOTATIONS && "AnnotationDefault" == attrName) {
                dann = u + 8
            } else if ("Synthetic" == attrName) {
                context.access = context.access or (Opcodes.ACC_SYNTHETIC or ClassWriter.ACC_SYNTHETIC_ATTRIBUTE)
            } else if (ANNOTATIONS && "RuntimeInvisibleAnnotations" == attrName) {
                ianns = u + 8
            } else if (ANNOTATIONS && "RuntimeInvisibleTypeAnnotations" == attrName) {
                itanns = u + 8
            } else if (ANNOTATIONS && "RuntimeVisibleParameterAnnotations" == attrName) {
                mpanns = u + 8
            } else if (ANNOTATIONS && "RuntimeInvisibleParameterAnnotations" == attrName) {
                impanns = u + 8
            } else if ("MethodParameters" == attrName) {
                methodParameters = u + 8
            } else {
                val attr = readAttribute(
                    context.attrs!!, attrName, u + 8,
                    readInt(u + 4), c, -1, null
                )
                if (attr != null) {
                    attr.next = attributes
                    attributes = attr
                }
            }
            u += 6 + readInt(u + 4)
        }
        u += 2

        // visits the method declaration

        val mv = classVisitor.visitMethod(
            context.access,
            context.name!!, context.desc!!, signature, exceptions
        ) ?: return u

        /*
         * if the returned MethodVisitor is in fact a MethodWriter, it means
         * there is no method adapter between the reader and the writer. If, in
         * addition, the writer's constant pool was copied from this reader
         * (mw.cw.cr == this), and the signature and exceptions of the method
         * have not been changed, then it is possible to skip all visit events
         * and just copy the original code of the method to the writer (the
         * access, name and descriptor can have been changed, this is not
         * important since they are not copied as is from the reader).
         */
        if (WRITER && mv is MethodWriter) {
            val mw = mv
            if (mw.cw.cr === this && signature === mw.signature) {
                var sameExceptions = false
                if (exceptions == null) {
                    sameExceptions = mw.exceptionCount == 0
                } else if (exceptions.size == mw.exceptionCount) {
                    sameExceptions = true
                    for (j in exceptions.indices.reversed()) {
                        exception -= 2
                        if (mw.exceptions[j] != readUnsignedShort(exception)) {
                            sameExceptions = false
                            break
                        }
                    }
                }
                if (sameExceptions) {
                    /*
                     * we do not copy directly the code into MethodWriter to
                     * save a byte array copy operation. The real copy will be
                     * done in ClassWriter.toByteArray().
                     */
                    mw.classReaderOffset = firstAttribute
                    mw.classReaderLength = u - firstAttribute
                    return u
                }
            }
        }

        // visit the method parameters
        if (methodParameters != 0) {
            var i = b[methodParameters].toInt() and 0xFF
            var v = methodParameters + 1
            while (i > 0) {
                mv.visitParameter(readUTF8(v, c)!!, readUnsignedShort(v + 2))
                --i
                v = v + 4
            }
        }

        // visits the method annotations
        if (ANNOTATIONS && dann != 0) {
            val dv = mv.visitAnnotationDefault()
            readAnnotationValue(dann, c, null, dv)
            dv?.visitEnd()
        }
        if (ANNOTATIONS && anns != 0) {
            var i = readUnsignedShort(anns)
            var v = anns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    mv.visitAnnotation(readUTF8(v, c)!!, true)
                )
                --i
            }
        }
        if (ANNOTATIONS && ianns != 0) {
            var i = readUnsignedShort(ianns)
            var v = ianns + 2
            while (i > 0) {
                v = readAnnotationValues(
                    v + 2, c, true,
                    mv.visitAnnotation(readUTF8(v, c)!!, false)
                )
                --i
            }
        }
        if (ANNOTATIONS && tanns != 0) {
            var i = readUnsignedShort(tanns)
            var v = tanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    mv.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, true
                    )
                )
                --i
            }
        }
        if (ANNOTATIONS && itanns != 0) {
            var i = readUnsignedShort(itanns)
            var v = itanns + 2
            while (i > 0) {
                v = readAnnotationTarget(context, v)
                v = readAnnotationValues(
                    v + 2, c, true,
                    mv.visitTypeAnnotation(
                        context.typeRef,
                        context.typePath!!, readUTF8(v, c)!!, false
                    )
                )
                --i
            }
        }
        if (ANNOTATIONS && mpanns != 0) {
            readParameterAnnotations(mv, context, mpanns, true)
        }
        if (ANNOTATIONS && impanns != 0) {
            readParameterAnnotations(mv, context, impanns, false)
        }

        // visits the method attributes
        while (attributes != null) {
            val attr = attributes.next
            attributes.next = null
            mv.visitAttribute(attributes)
            attributes = attr
        }

        // visits the method code
        if (code != 0) {
            mv.visitCode()
            readCode(mv, context, code)
        }

        // visits the end of the method
        mv.visitEnd()

        return u
    }

    /**
     * Reads the bytecode of a method and makes the given visitor visit it.
     *
     * @param mv
     * the visitor that must visit the method's code.
     * @param context
     * information about the class being parsed.
     * @param u
     * the start offset of the code attribute in the class file.
     */
    private fun readCode(mv: MethodVisitor, context: Context, u: Int) {
        var u = u
        // reads the header
        val b = this.b
        val c = context.buffer
        val maxStack = readUnsignedShort(u)
        val maxLocals = readUnsignedShort(u + 2)
        val codeLength = readInt(u + 4)
        u += 8

        // reads the bytecode to find the labels
        val codeStart = u
        val codeEnd = u + codeLength
        context.labels = arrayOfNulls(codeLength + 2)
        val labels = context.labels
        readLabel(codeLength + 1, labels)
        while (u < codeEnd) {
            val offset = u - codeStart
            var opcode = b[u].toInt() and 0xFF
            when (ClassWriter.TYPE[opcode].toInt()) {
                ClassWriter.NOARG_INSN, ClassWriter.IMPLVAR_INSN -> u += 1
                ClassWriter.LABEL_INSN -> {
                    readLabel(offset + readShort(u + 1), labels)
                    u += 3
                }
                ClassWriter.LABELW_INSN -> {
                    readLabel(offset + readInt(u + 1), labels)
                    u += 5
                }
                ClassWriter.WIDE_INSN -> {
                    opcode = b[u + 1].toInt() and 0xFF
                    if (opcode == Opcodes.IINC) {
                        u += 6
                    } else {
                        u += 4
                    }
                }
                ClassWriter.TABL_INSN -> {
                    // skips 0 to 3 padding bytes
                    u = u + 4 - (offset and 3)
                    // reads instruction
                    readLabel(offset + readInt(u), labels)
                    for (i in readInt(u + 8) - readInt(u + 4) + 1 downTo 1) {
                        readLabel(offset + readInt(u + 12), labels)
                        u += 4
                    }
                    u += 12
                }
                ClassWriter.LOOK_INSN -> {
                    // skips 0 to 3 padding bytes
                    u = u + 4 - (offset and 3)
                    // reads instruction
                    readLabel(offset + readInt(u), labels)
                    for (i in readInt(u + 4) downTo 1) {
                        readLabel(offset + readInt(u + 12), labels)
                        u += 8
                    }
                    u += 8
                }
                ClassWriter.VAR_INSN, ClassWriter.SBYTE_INSN, ClassWriter.LDC_INSN -> u += 2
                ClassWriter.SHORT_INSN, ClassWriter.LDCW_INSN, ClassWriter.FIELDORMETH_INSN, ClassWriter.TYPE_INSN, ClassWriter.IINC_INSN -> u += 3
                ClassWriter.ITFMETH_INSN, ClassWriter.INDYMETH_INSN -> u += 5
                // case MANA_INSN:
                else -> u += 4
            }
        }

        // reads the try catch entries to find the labels, and also visits them
        for (i in readUnsignedShort(u) downTo 1) {
            val start = readLabel(readUnsignedShort(u + 2), labels)
            val end = readLabel(readUnsignedShort(u + 4), labels)
            val handler = readLabel(readUnsignedShort(u + 6), labels)
            val type = readUTF8(items[readUnsignedShort(u + 8)], c)
            mv.visitTryCatchBlock(start, end, handler, type)
            u += 8
        }
        u += 2

        // reads the code attributes
        var tanns: IntArray? = null // start index of each visible type annotation
        var itanns: IntArray? = null // start index of each invisible type annotation
        var tann = 0 // current index in tanns array
        var itann = 0 // current index in itanns array
        var ntoff = -1 // next visible type annotation code offset
        var nitoff = -1 // next invisible type annotation code offset
        var varTable = 0
        var varTypeTable = 0
        var zip = true
        val unzip = context.flags and EXPAND_FRAMES != 0
        var stackMap = 0
        var stackMapSize = 0
        var frameCount = 0
        var frame: Context? = null
        var attributes: Attribute? = null

        for (i in readUnsignedShort(u) downTo 1) {
            val attrName = readUTF8(u + 2, c)
            if ("LocalVariableTable" == attrName) {
                if (context.flags and SKIP_DEBUG == 0) {
                    varTable = u + 8
                    var j = readUnsignedShort(u + 8)
                    var v = u
                    while (j > 0) {
                        var label = readUnsignedShort(v + 10)
                        if (labels!![label] == null) {
                            readLabel(label, labels).status = readLabel(label, labels).status or
                                    Label.DEBUG
                        }
                        label += readUnsignedShort(v + 12)
                        if (labels[label] == null) {
                            readLabel(label, labels).status = readLabel(label, labels).status or
                                    Label.DEBUG
                        }
                        v += 10
                        --j
                    }
                }
            } else if ("LocalVariableTypeTable" == attrName) {
                varTypeTable = u + 8
            } else if ("LineNumberTable" == attrName) {
                if (context.flags and SKIP_DEBUG == 0) {
                    var j = readUnsignedShort(u + 8)
                    var v = u
                    while (j > 0) {
                        val label = readUnsignedShort(v + 10)
                        if (labels!![label] == null) {
                            readLabel(label, labels).status = readLabel(label, labels).status or
                                    Label.DEBUG
                        }
                        labels[label]?.line = readUnsignedShort(v + 12)
                        v += 4
                        --j
                    }
                }
            } else if (ANNOTATIONS && "RuntimeVisibleTypeAnnotations" == attrName) {
                tanns = readTypeAnnotations(mv, context, u + 8, true)
                ntoff = if (tanns.size == 0 || readByte(tanns[0]) < 0x43)
                    -1
                else
                    readUnsignedShort(tanns[0] + 1)
            } else if (ANNOTATIONS && "RuntimeInvisibleTypeAnnotations" == attrName) {
                itanns = readTypeAnnotations(mv, context, u + 8, false)
                nitoff = if (itanns.size == 0 || readByte(itanns[0]) < 0x43)
                    -1
                else
                    readUnsignedShort(itanns[0] + 1)
            } else if (FRAMES && "StackMapTable" == attrName) {
                if (context.flags and SKIP_FRAMES == 0) {
                    stackMap = u + 10
                    stackMapSize = readInt(u + 4)
                    frameCount = readUnsignedShort(u + 8)
                }
                /*
                 * here we do not extract the labels corresponding to the
                 * attribute content. This would require a full parsing of the
                 * attribute, which would need to be repeated in the second
                 * phase (see below). Instead the content of the attribute is
                 * read one frame at a time (i.e. after a frame has been
                 * visited, the next frame is read), and the labels it contains
                 * are also extracted one frame at a time. Thanks to the
                 * ordering of frames, having only a "one frame lookahead" is
                 * not a problem, i.e. it is not possible to see an offset
                 * smaller than the offset of the current insn and for which no
                 * Label exist.
                 */
                /*
                 * This is not true for UNINITIALIZED type offsets. We solve
                 * this by parsing the stack map table without a full decoding
                 * (see below).
                 */
            } else if (FRAMES && "StackMap" == attrName) {
                if (context.flags and SKIP_FRAMES == 0) {
                    zip = false
                    stackMap = u + 10
                    stackMapSize = readInt(u + 4)
                    frameCount = readUnsignedShort(u + 8)
                }
                /*
                 * IMPORTANT! here we assume that the frames are ordered, as in
                 * the StackMapTable attribute, although this is not guaranteed
                 * by the attribute format.
                 */
            } else {
                for (j in context.attrs!!.indices) {
                    if (context.attrs!![j]!!.type == attrName) {
                        val attr = context.attrs!![j]?.read(
                            this, u + 8,
                            readInt(u + 4), c, codeStart - 8, labels
                        )
                        if (attr != null) {
                            attr.next = attributes
                            attributes = attr
                        }
                    }
                }
            }
            u += 6 + readInt(u + 4)
        }
        u += 2

        // generates the first (implicit) stack map frame
        if (FRAMES && (stackMap != 0 || unzip)) {
            /*
             * for the first explicit frame the offset is not offset_delta + 1
             * but only offset_delta; setting the implicit frame offset to -1
             * allow the use of the "offset_delta + 1" rule in all cases
             */
            frame = context
            frame.offset = -1
            frame.mode = 0
            frame.localCount = 0
            frame.localDiff = 0
            frame.stackCount = 0
            frame.local = arrayOfNulls(maxLocals)
            frame.stack = arrayOfNulls(maxStack)
            if (unzip) {
                getImplicitFrame(context)
            }
        }
        if (FRAMES && stackMap != 0) {
            /*
             * Finds labels for UNINITIALIZED frame types. Instead of decoding
             * each element of the stack map table, we look for 3 consecutive
             * bytes that "look like" an UNINITIALIZED type (tag 8, offset
             * within code bounds, NEW instruction at this offset). We may find
             * false positives (i.e. not real UNINITIALIZED types), but this
             * should be rare, and the only consequence will be the creation of
             * an unneeded label. This is better than creating a label for each
             * NEW instruction, and faster than fully decoding the whole stack
             * map table.
             */
            for (i in stackMap until stackMap + stackMapSize - 2) {
                if (b[i].toInt() == 8) { // UNINITIALIZED FRAME TYPE
                    val v = readUnsignedShort(i + 1)
                    if (v >= 0 && v < codeLength) {
                        if (b[codeStart + v].toInt() and 0xFF == Opcodes.NEW) {
                            readLabel(v, labels)
                        }
                    }
                }
            }
        }

        // visits the instructions
        u = codeStart
        u = igrtest(
            u,
            codeEnd,
            codeStart,
            labels,
            mv,
            context,
            frame,
            zip,
            unzip,
            frameCount,
            stackMap,
            b,
            c,
            tanns,
            tann,
            ntoff,
            itanns,
            itann,
            nitoff
        )

        if (labels!![codeLength] != null) {
            mv.visitLabel(labels[codeLength]!!)
        }

        // visits the local variable tables
        if (context.flags and SKIP_DEBUG == 0 && varTable != 0) {
            var typeTable: IntArray? = null
            if (varTypeTable != 0) {
                u = varTypeTable + 2
                typeTable = IntArray(readUnsignedShort(varTypeTable) * 3)
                var i = typeTable.size
                while (i > 0) {
                    typeTable[--i] = u + 6 // signature
                    typeTable[--i] = readUnsignedShort(u + 8) // index
                    typeTable[--i] = readUnsignedShort(u) // start
                    u += 10
                }
            }
            u = varTable + 2
            for (i in readUnsignedShort(varTable) downTo 1) {
                val start = readUnsignedShort(u)
                val length = readUnsignedShort(u + 2)
                val index = readUnsignedShort(u + 8)
                var vsignature: String? = null
                if (typeTable != null) {
                    var j = 0
                    while (j < typeTable.size) {
                        if (typeTable[j] == start && typeTable[j + 1] == index) {
                            vsignature = readUTF8(typeTable[j + 2], c)
                            break
                        }
                        j += 3
                    }
                }

                mv.visitLocalVariable(
                    readUTF8(u + 4, c)!!, readUTF8(u + 6, c)!!,
                    vsignature, labels[start]!!, labels[start + length]!!,
                    index
                )

                u += 10
            }
        }

        // visits the local variables type annotations
        if (tanns != null) {
            for (i in tanns.indices) {
                if (readByte(tanns[i]) shr 1 == 0x40 shr 1) {
                    var v = readAnnotationTarget(context, tanns[i])

                    v = readAnnotationValues(
                        v + 2, c, true,
                        mv.visitLocalVariableAnnotation(
                            context.typeRef,
                            context.typePath!!, context.start!!,
                            context.end!!, context.index!!, readUTF8(v, c)!!,
                            true
                        )
                    )

                }
            }
        }
        if (itanns != null) {
            for (i in itanns.indices) {
                if (readByte(itanns[i]) shr 1 == 0x40 shr 1) {
                    var v = readAnnotationTarget(context, itanns[i])

                    v = readAnnotationValues(
                        v + 2, c, true,
                        mv.visitLocalVariableAnnotation(
                            context.typeRef,
                            context.typePath!!, context.start!!,
                            context.end!!, context.index!!, readUTF8(v, c)!!,
                            false
                        )
                    )

                }
            }
        }

        // visits the code attributes
        while (attributes != null) {
            val attr = attributes.next
            attributes.next = null
            mv.visitAttribute(attributes)
            attributes = attr
        }

        // visits the max stack and max locals values
        mv.visitMaxs(maxStack, maxLocals)
    }

    private fun igrtest(
        u: Int,
        codeEnd: Int,
        codeStart: Int,
        labels: Array<Label?>?,
        mv: MethodVisitor,
        context: Context,
        frame: Context?,
        zip: Boolean,
        unzip: Boolean,
        frameCount: Int,
        stackMap: Int,
        b: ByteArray,
        c: CharArray?,
        tanns: IntArray?,
        tann: Int,
        ntoff: Int,
        itanns: IntArray?,
        itann: Int,
        nitoff: Int
    ): Int {
        var u1 = u
        var frame1 = frame
        var frameCount1 = frameCount
        var stackMap1 = stackMap
        var tann1 = tann
        var ntoff1 = ntoff
        var itann1 = itann
        var nitoff1 = nitoff
        while (u1 < codeEnd) {
            val offset = u1 - codeStart

            // visits the label and line number for this offset, if any
            val l = labels!![offset]
            if (l != null) {
                mv.visitLabel(l)
                if (context.flags and SKIP_DEBUG == 0 && l.line > 0) {
                    mv.visitLineNumber(l.line, l)
                }
            }

            // visits the frame(s) for this offset, if any
            while (FRAMES && frame1 != null
                && (frame1.offset == offset || frame1.offset == -1)
            ) {
                // if there is a frame for this offset, makes the visitor visit
                // it, and reads the next frame if there is one.
                if (!zip || unzip) {

                    mv.visitFrame(
                        Opcodes.F_NEW, frame1.localCount, frame1.local!!,
                        frame1.stackCount, frame1.stack!!
                    )

                } else if (frame1.offset != -1) {

                    mv.visitFrame(
                        frame1.mode, frame1.localDiff, frame1.local!!,
                        frame1.stackCount, frame1.stack!!
                    )

                }
                if (frameCount1 > 0) {
                    stackMap1 = readFrame(stackMap1, zip, unzip, frame1)
                    --frameCount1
                } else {
                    frame1 = null
                }
            }

            // visits the instruction at this offset
            var opcode = b[u1].toInt() and 0xFF
            when (ClassWriter.TYPE[opcode].toInt()) {
                ClassWriter.NOARG_INSN -> {
                    mv.visitInsn(opcode)
                    u1 += 1
                }
                ClassWriter.IMPLVAR_INSN -> {
                    if (opcode > Opcodes.ISTORE) {
                        opcode -= 59 // ISTORE_0
                        mv.visitVarInsn(
                            Opcodes.ISTORE + (opcode shr 2),
                            opcode and 0x3
                        )
                    } else {
                        opcode -= 26 // ILOAD_0
                        mv.visitVarInsn(Opcodes.ILOAD + (opcode shr 2), opcode and 0x3)
                    }
                    u1 += 1
                }
                ClassWriter.LABEL_INSN -> {
                    mv.visitJumpInsn(opcode, labels!![offset + readShort(u1 + 1)]!!)
                    u1 += 3
                }
                ClassWriter.LABELW_INSN -> {
                    mv.visitJumpInsn(opcode - 33, labels!![offset + readInt(u1 + 1)]!!)
                    u1 += 5
                }
                ClassWriter.WIDE_INSN -> {
                    opcode = b[u1 + 1].toInt() and 0xFF
                    if (opcode == Opcodes.IINC) {
                        mv.visitIincInsn(readUnsignedShort(u1 + 2), readShort(u1 + 4).toInt())
                        u1 += 6
                    } else {
                        mv.visitVarInsn(opcode, readUnsignedShort(u1 + 2))
                        u1 += 4
                    }
                }
                ClassWriter.TABL_INSN -> {
                    // skips 0 to 3 padding bytes
                    u1 = u1 + 4 - (offset and 3)
                    // reads instruction
                    val label = offset + readInt(u1)
                    val min = readInt(u1 + 4)
                    val max = readInt(u1 + 8)
                    val table = arrayOfNulls<Label>(max - min + 1)
                    u1 += 12
                    for (i in table.indices) {
                        table[i] = labels[offset + readInt(u1)]
                        u1 += 4
                    }
                    mv.visitTableSwitchInsn(min, max, labels[label]!!, table)
                }
                ClassWriter.LOOK_INSN -> {
                    // skips 0 to 3 padding bytes
                    u1 = u1 + 4 - (offset and 3)
                    // reads instruction
                    val label = offset + readInt(u1)
                    val len = readInt(u1 + 4)
                    val keys = IntArray(len)
                    val values = arrayOfNulls<Label>(len)
                    u1 += 8
                    for (i in 0 until len) {
                        keys[i] = readInt(u1)
                        values[i] = labels[offset + readInt(u1 + 4)]
                        u1 += 8
                    }
                    mv.visitLookupSwitchInsn(labels[label]!!, keys, values)
                }
                ClassWriter.VAR_INSN -> {
                    mv.visitVarInsn(opcode, b[u1 + 1].toInt() and 0xFF)
                    u1 += 2
                }
                ClassWriter.SBYTE_INSN -> {
                    mv.visitIntInsn(opcode, b[u1 + 1].toInt())
                    u1 += 2
                }
                ClassWriter.SHORT_INSN -> {
                    mv.visitIntInsn(opcode, readShort(u1 + 1).toInt())
                    u1 += 3
                }
                ClassWriter.LDC_INSN -> {

                    val readConst = readConst(b[u1 + 1].toInt() and 0xFF, c)



                    mv.visitLdcInsn(readConst!!)

                    u1 += 2
                }
                ClassWriter.LDCW_INSN -> {
                    mv.visitLdcInsn(readConst(readUnsignedShort(u1 + 1), c)!!)
                    u1 += 3
                }
                ClassWriter.FIELDORMETH_INSN, ClassWriter.ITFMETH_INSN -> {

                    var cpIndex = items[readUnsignedShort(u1 + 1)]
                    val iowner = readClass(cpIndex, c)
                    cpIndex = items[readUnsignedShort(cpIndex + 2)]
                    val iname = readUTF8(cpIndex, c)
                    val idesc = readUTF8(cpIndex + 2, c)
                    if (opcode < Opcodes.INVOKEVIRTUAL) {
                        mv.visitFieldInsn(opcode, iowner!!, iname!!, idesc!!)
                    } else {
                        mv.visitMethodInsn(opcode, iowner!!, iname!!, idesc!!)
                    }
                    if (opcode == Opcodes.INVOKEINTERFACE) {
                        u1 += 5
                    } else {
                        u1 += 3
                    }
                }
                ClassWriter.INDYMETH_INSN -> {
                    var cpIndex = items[readUnsignedShort(u1 + 1)]
                    var bsmIndex = context.bootstrapMethods!![readUnsignedShort(cpIndex)]
                    val bsm = readConst(readUnsignedShort(bsmIndex), c) as Handle?
                    val bsmArgCount = readUnsignedShort(bsmIndex + 2)
                    val bsmArgs = arrayOfNulls<Any>(bsmArgCount)
                    bsmIndex += 4
                    for (i in 0 until bsmArgCount) {
                        bsmArgs[i] = readConst(readUnsignedShort(bsmIndex), c)
                        bsmIndex += 2
                    }
                    cpIndex = items[readUnsignedShort(cpIndex + 2)]
                    val iname = readUTF8(cpIndex, c)
                    val idesc = readUTF8(cpIndex + 2, c)
                    mv.visitInvokeDynamicInsn(iname!!, idesc!!, bsm!!, bsmArgs)
                    u1 += 5
                }
                ClassWriter.TYPE_INSN -> {
                    mv.visitTypeInsn(opcode, readClass(u1 + 1, c)!!)
                    u1 += 3
                }
                ClassWriter.IINC_INSN -> {
                    mv.visitIincInsn(b[u1 + 1].toInt() and 0xFF, b[u1 + 2].toInt())
                    u1 += 3
                }
                // case MANA_INSN:
                else -> {
                    mv.visitMultiANewArrayInsn(readClass(u1 + 1, c)!!, b[u1 + 3].toInt() and 0xFF)
                    u1 += 4
                }
            }

            // visit the instruction annotations, if any
            while (tanns != null && tann1 < tanns.size && ntoff1 <= offset) {
                if (ntoff1 == offset) {
                    val v = readAnnotationTarget(context, tanns[tann1])
                    readAnnotationValues(
                        v + 2, c, true,
                        mv.visitInsnAnnotation(
                            context.typeRef,
                            context.typePath!!, readUTF8(v, c)!!, true
                        )
                    )
                }
                ntoff1 = if (++tann1 >= tanns.size || readByte(tanns[tann1]) < 0x43)
                    -1
                else
                    readUnsignedShort(tanns[tann1] + 1)
            }
            while (itanns != null && itann1 < itanns.size && nitoff1 <= offset) {
                if (nitoff1 == offset) {
                    val v = readAnnotationTarget(context, itanns[itann1])
                    readAnnotationValues(
                        v + 2, c, true,
                        mv.visitInsnAnnotation(
                            context.typeRef,
                            context.typePath!!,
                            readUTF8(v, c),
                            false
                        )
                    )
                }
                nitoff1 = if (++itann1 >= itanns.size || readByte(itanns[itann1]) < 0x43)
                    -1
                else
                    readUnsignedShort(itanns[itann1] + 1)
            }
        }
        return u1
    }

    /**
     * Parses a type annotation table to find the labels, and to visit the try
     * catch block annotations.
     *
     * @param u
     * the start offset of a type annotation table.
     * @param mv
     * the method visitor to be used to visit the try catch block
     * annotations.
     * @param context
     * information about the class being parsed.
     * @param visible
     * if the type annotation table to parse contains runtime visible
     * annotations.
     * @return the start offset of each type annotation in the parsed table.
     */
    private fun readTypeAnnotations(
        mv: MethodVisitor,
        context: Context, u: Int, visible: Boolean
    ): IntArray {
        var u = u
        val c = context.buffer
        val offsets = IntArray(readUnsignedShort(u))
        u += 2
        for (i in offsets.indices) {
            offsets[i] = u
            val target = readInt(u)
            when (target.ushr(24)) {
                0x00 // CLASS_TYPE_PARAMETER
                    , 0x01 // METHOD_TYPE_PARAMETER
                    , 0x16 // METHOD_FORMAL_PARAMETER
                -> u += 2
                0x13 // FIELD
                    , 0x14 // METHOD_RETURN
                    , 0x15 // METHOD_RECEIVER
                -> u += 1
                0x40 // LOCAL_VARIABLE
                    , 0x41 // RESOURCE_VARIABLE
                -> {
                    for (j in readUnsignedShort(u + 1) downTo 1) {
                        val start = readUnsignedShort(u + 3)
                        val length = readUnsignedShort(u + 5)
                        readLabel(start, context.labels)
                        readLabel(start + length, context.labels)
                        u += 6
                    }
                    u += 3
                }
                0x47 // CAST
                    , 0x48 // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                    , 0x49 // METHOD_INVOCATION_TYPE_ARGUMENT
                    , 0x4A // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                    , 0x4B // METHOD_REFERENCE_TYPE_ARGUMENT
                -> u += 4
                // case 0x10: // CLASS_EXTENDS
                // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
                // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
                // case 0x17: // THROWS
                // case 0x42: // EXCEPTION_PARAMETER
                // case 0x43: // INSTANCEOF
                // case 0x44: // NEW
                // case 0x45: // CONSTRUCTOR_REFERENCE_RECEIVER
                // case 0x46: // METHOD_REFERENCE_RECEIVER
                else -> u += 3
            }
            val pathLength = readByte(u)
            if (target.ushr(24) == 0x42) {
                val path = if (pathLength == 0) null else TypePath(b, u)
                u += 1 + 2 * pathLength
                u = readAnnotationValues(
                    u + 2, c, true,
                    mv.visitTryCatchAnnotation(
                        target, path!!,
                        readUTF8(u, c)!!, visible
                    )
                )
            } else {
                u = readAnnotationValues(u + 3 + 2 * pathLength, c, true, null)
            }
        }
        return offsets
    }

    /**
     * Parses the header of a type annotation to extract its target_type and
     * target_path (the result is stored in the given context), and returns the
     * start offset of the rest of the type_annotation structure (i.e. the
     * offset to the type_index field, which is followed by
     * num_element_value_pairs and then the name,value pairs).
     *
     * @param context
     * information about the class being parsed. This is where the
     * extracted target_type and target_path must be stored.
     * @param u
     * the start offset of a type_annotation structure.
     * @return the start offset of the rest of the type_annotation structure.
     */
    private fun readAnnotationTarget(context: Context, u: Int): Int {
        var u = u
        var target = readInt(u)
        when (target.ushr(24)) {
            0x00 // CLASS_TYPE_PARAMETER
                , 0x01 // METHOD_TYPE_PARAMETER
                , 0x16 // METHOD_FORMAL_PARAMETER
            -> {
                target = target and -0x10000
                u += 2
            }
            0x13 // FIELD
                , 0x14 // METHOD_RETURN
                , 0x15 // METHOD_RECEIVER
            -> {
                target = target and -0x1000000
                u += 1
            }
            0x40 // LOCAL_VARIABLE
                , 0x41 -> { // RESOURCE_VARIABLE
                target = target and -0x1000000
                val n = readUnsignedShort(u + 1)
                context.start = arrayOfNulls(n)
                context.end = arrayOfNulls(n)
                context.index = IntArray(n)
                u += 3
                for (i in 0 until n) {
                    val start = readUnsignedShort(u)
                    val length = readUnsignedShort(u + 2)
                    context.start!![i] = readLabel(start, context.labels)
                    context.end!![i] = readLabel(start + length, context.labels)
                    context.index!![i] = readUnsignedShort(u + 4)
                    u += 6
                }
            }
            0x47 // CAST
                , 0x48 // CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                , 0x49 // METHOD_INVOCATION_TYPE_ARGUMENT
                , 0x4A // CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                , 0x4B // METHOD_REFERENCE_TYPE_ARGUMENT
            -> {
                target = target and -0xffff01
                u += 4
            }
            // case 0x10: // CLASS_EXTENDS
            // case 0x11: // CLASS_TYPE_PARAMETER_BOUND
            // case 0x12: // METHOD_TYPE_PARAMETER_BOUND
            // case 0x17: // THROWS
            // case 0x42: // EXCEPTION_PARAMETER
            // case 0x43: // INSTANCEOF
            // case 0x44: // NEW
            // case 0x45: // CONSTRUCTOR_REFERENCE_RECEIVER
            // case 0x46: // METHOD_REFERENCE_RECEIVER
            else -> {
                target = target and if (target.ushr(24) < 0x43) -0x100 else -0x1000000
                u += 3
            }
        }
        val pathLength = readByte(u)
        context.typeRef = target
        context.typePath = if (pathLength == 0) null else TypePath(b, u)
        return u + 1 + 2 * pathLength
    }

    /**
     * Reads parameter annotations and makes the given visitor visit them.
     *
     * @param mv
     * the visitor that must visit the annotations.
     * @param context
     * information about the class being parsed.
     * @param v
     * start offset in [b][.b] of the annotations to be read.
     * @param visible
     * <tt>true</tt> if the annotations to be read are visible at
     * runtime.
     */
    private fun readParameterAnnotations(
        mv: MethodVisitor,
        context: Context, v: Int, visible: Boolean
    ) {
        var v = v
        var i: Int
        val n = b[v++].toInt() and 0xFF
        // workaround for a bug in javac (javac compiler generates a parameter
        // annotation array whose size is equal to the number of parameters in
        // the Java source file, while it should generate an array whose size is
        // equal to the number of parameters in the method descriptor - which
        // includes the synthetic parameters added by the compiler). This work-
        // around supposes that the synthetic parameters are the first ones.
        val synthetics = Type.getArgumentTypes(context.desc!!).size - n
        var av: AnnotationVisitor?
        i = 0
        while (i < synthetics) {
            // virtual annotation to detect synthetic parameters in MethodWriter
            av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", false)
            av?.visitEnd()
            ++i
        }
        val c = context.buffer
        while (i < n + synthetics) {
            var j = readUnsignedShort(v)
            v += 2
            while (j > 0) {
                av = mv.visitParameterAnnotation(i, readUTF8(v, c)!!, visible)
                v = readAnnotationValues(v + 2, c, true, av)
                --j
            }
            ++i
        }
    }

    /**
     * Reads the values of an annotation and makes the given visitor visit them.
     *
     * @param v
     * the start offset in [b][.b] of the values to be read
     * (including the unsigned short that gives the number of
     * values).
     * @param buf
     * buffer to be used to call [readUTF8][.readUTF8],
     * [readClass][.readClass] or [            readConst][.readConst].
     * @param named
     * if the annotation values are named or not.
     * @param av
     * the visitor that must visit the values.
     * @return the end offset of the annotation values.
     */
    private fun readAnnotationValues(
        v: Int, buf: CharArray?,
        named: Boolean, av: AnnotationVisitor?
    ): Int {
        var v = v
        var i = readUnsignedShort(v)
        v += 2
        if (named) {
            while (i > 0) {
                v = readAnnotationValue(v + 2, buf, readUTF8(v, buf), av)
                --i
            }
        } else {
            while (i > 0) {
                v = readAnnotationValue(v, buf, null, av)
                --i
            }
        }
        av?.visitEnd()
        return v
    }

    /**
     * Reads a value of an annotation and makes the given visitor visit it.
     *
     * @param v
     * the start offset in [b][.b] of the value to be read
     * (*not including the value name constant pool index*).
     * @param buf
     * buffer to be used to call [readUTF8][.readUTF8],
     * [readClass][.readClass] or [            readConst][.readConst].
     * @param name
     * the name of the value to be read.
     * @param av
     * the visitor that must visit the value.
     * @return the end offset of the annotation value.
     */
    private fun readAnnotationValue(
        v: Int, buf: CharArray?, name: String?,
        av: AnnotationVisitor?
    ): Int {
        var v = v
        var i: Int
        if (av == null) {
            when ((b[v].toInt() and 0xFF).toChar()) {
                'e' // enum_const_value
                -> return v + 5
                '@' // annotation_value
                -> return readAnnotationValues(v + 3, buf, true, null)
                '[' // array_value
                -> return readAnnotationValues(v + 1, buf, false, null)
                else -> return v + 3
            }
        }
        when ((b[v++].toInt() and 0xFF).toChar()) {
            'I' // pointer to CONSTANT_Integer
                , 'J' // pointer to CONSTANT_Long
                , 'F' // pointer to CONSTANT_Float
                , 'D' // pointer to CONSTANT_Double
            -> {
                av.visit(name!!, readConst(readUnsignedShort(v), buf)!!)
                v += 2
            }
            'B' // pointer to CONSTANT_Byte
            -> {
                av.visit(
                    name!!,
                    readInt(items[readUnsignedShort(v)]).toByte()
                )
                v += 2
            }
            'Z' // pointer to CONSTANT_Boolean
            -> {
                av.visit(
                    name!!,
                    if (readInt(items[readUnsignedShort(v)]) == 0)
                        false
                    else
                        true
                )
                v += 2
            }
            'S' // pointer to CONSTANT_Short
            -> {
                av.visit(name!!, readInt(items[readUnsignedShort(v)]).toShort())
                v += 2
            }
            'C' // pointer to CONSTANT_Char
            -> {
                av.visit(name!!, readInt(items[readUnsignedShort(v)]).toChar())
                v += 2
            }
            's' // pointer to CONSTANT_Utf8
            -> {
                av.visit(name!!, readUTF8(v, buf)!!)
                v += 2
            }
            'e' // enum_const_value
            -> {
                av.visitEnum(name!!, readUTF8(v, buf)!!, readUTF8(v + 2, buf)!!)
                v += 4
            }
            'c' // class_info
            -> {
                throw RuntimeException("GmMMMMM")
//                av.visit(name!!, Type.getType(readUTF8(v, buf)!!))
                v += 2
            }
            '@' // annotation_value
            -> v = readAnnotationValues(
                v + 2, buf, true,
                av.visitAnnotation(name!!, readUTF8(v, buf)!!)
            )
            '[' // array_value
            -> {
                val size = readUnsignedShort(v)
                v += 2
                if (size == 0) {
                    return readAnnotationValues(
                        v - 2, buf, false,
                        av.visitArray(name!!)
                    )
                }
                when ((this.b[v++].toInt() and 0xFF).toChar()) {
                    'B' -> {
                        val bv = ByteArray(size)
                        i = 0
                        while (i < size) {
                            bv[i] = readInt(items[readUnsignedShort(v)]).toByte()
                            v += 3
                            i++
                        }
                        av.visit(name!!, bv)
                        --v
                    }
                    'Z' -> {
                        val zv = BooleanArray(size)
                        i = 0
                        while (i < size) {
                            zv[i] = readInt(items[readUnsignedShort(v)]) != 0
                            v += 3
                            i++
                        }
                        av.visit(name!!, zv)
                        --v
                    }
                    'S' -> {
                        val sv = ShortArray(size)
                        i = 0
                        while (i < size) {
                            sv[i] = readInt(items[readUnsignedShort(v)]).toShort()
                            v += 3
                            i++
                        }
                        av.visit(name!!, sv)
                        --v
                    }
                    'C' -> {
                        val cv = CharArray(size)
                        i = 0
                        while (i < size) {
                            cv[i] = readInt(items[readUnsignedShort(v)]).toChar()
                            v += 3
                            i++
                        }
                        av.visit(name!!, cv)
                        --v
                    }
                    'I' -> {
                        val iv = IntArray(size)
                        i = 0
                        while (i < size) {
                            iv[i] = readInt(items[readUnsignedShort(v)])
                            v += 3
                            i++
                        }
                        av.visit(name!!, iv)
                        --v
                    }
                    'J' -> {
                        val lv = LongArray(size)
                        i = 0
                        while (i < size) {
                            lv[i] = readLong(items[readUnsignedShort(v)])
                            v += 3
                            i++
                        }
                        av.visit(name!!, lv)
                        --v
                    }
                    'F' -> {
                        val fv = FloatArray(size)
                        i = 0
                        while (i < size) {
                            fv[i] = readInt(items[readUnsignedShort(v)]).toFloat().toBits().toFloat()
                            v += 3
                            i++
                        }
                        av.visit(name!!, fv)
                        --v
                    }
                    'D' -> {
                        val dv = DoubleArray(size)
                        i = 0
                        while (i < size) {
                            dv[i] = readLong(items[readUnsignedShort(v)]).toDouble().toBits().toDouble()
                            v += 3
                            i++
                        }
                        av.visit(name!!, dv)
                        --v
                    }
                    else -> v = readAnnotationValues(v - 3, buf, false, av.visitArray(name!!))
                }
            }
        }
        return v
    }

    /**
     * Computes the implicit frame of the method currently being parsed (as
     * defined in the given [Context]) and stores it in the given context.
     *
     * @param frame
     * information about the class being parsed.
     */
    private fun getImplicitFrame(frame: Context) {
        val desc = frame.desc
        val locals = frame.local
        var local = 0
        if (frame.access and Opcodes.ACC_STATIC === 0) {
            if ("<init>" == frame.name) {
                locals!![local++] = Opcodes.UNINITIALIZED_THIS
            } else {
                locals!![local++] = readClass(header + 2, frame.buffer)!!
            }
        }
        var i = 1
        loop@ while (true) {
            val j = i
            when (desc!![i++]) {
                'Z', 'C', 'B', 'S', 'I' -> locals!![local++] = Opcodes.INTEGER
                'F' -> locals!![local++] = Opcodes.FLOAT
                'J' -> locals!![local++] = Opcodes.LONG
                'D' -> locals!![local++] = Opcodes.DOUBLE
                '[' -> {
                    while (desc[i] == '[') {
                        ++i
                    }
                    if (desc[i] == 'L') {
                        ++i
                        while (desc[i] != ';') {
                            ++i
                        }
                    }
                    locals!![local++] = desc.substring(j, ++i)
                }
                'L' -> {
                    while (desc[i] != ';') {
                        ++i
                    }
                    locals!![local++] = desc.substring(j + 1, i++)
                }
                else -> break@loop
            }
        }
        frame.localCount = local
    }

    /**
     * Reads a stack map frame and stores the result in the given
     * [Context] object.
     *
     * @param stackMap
     * the start offset of a stack map frame in the class file.
     * @param zip
     * if the stack map frame at stackMap is compressed or not.
     * @param unzip
     * if the stack map frame must be uncompressed.
     * @param frame
     * where the parsed stack map frame must be stored.
     * @return the offset of the first byte following the parsed frame.
     */
    private fun readFrame(
        stackMap: Int, zip: Boolean, unzip: Boolean,
        frame: Context
    ): Int {
        var stackMap = stackMap
        val c = frame.buffer
        val labels = frame.labels
        val tag: Int
        val delta: Int
        if (zip) {
            tag = b[stackMap++].toInt() and 0xFF
        } else {
            tag = MethodWriter.FULL_FRAME
            frame.offset = -1
        }
        frame.localDiff = 0
        if (tag < MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME) {
            delta = tag
            frame.mode = Opcodes.F_SAME
            frame.stackCount = 0
        } else if (tag < MethodWriter.RESERVED) {
            delta = tag - MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME
            stackMap = readFrameType(frame.stack, 0, stackMap, c, labels)
            frame.mode = Opcodes.F_SAME1
            frame.stackCount = 1
        } else {
            delta = readUnsignedShort(stackMap)
            stackMap += 2
            if (tag == MethodWriter.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
                stackMap = readFrameType(frame.stack, 0, stackMap, c, labels)
                frame.mode = Opcodes.F_SAME1
                frame.stackCount = 1
            } else if (tag >= MethodWriter.CHOP_FRAME && tag < MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_CHOP
                frame.localDiff = MethodWriter.SAME_FRAME_EXTENDED - tag
                frame.localCount -= frame.localDiff
                frame.stackCount = 0
            } else if (tag == MethodWriter.SAME_FRAME_EXTENDED) {
                frame.mode = Opcodes.F_SAME
                frame.stackCount = 0
            } else if (tag < MethodWriter.FULL_FRAME) {
                var local = if (unzip) frame.localCount else 0
                for (i in tag - MethodWriter.SAME_FRAME_EXTENDED downTo 1) {
                    stackMap = readFrameType(
                        frame.local, local++, stackMap, c,
                        labels
                    )
                }
                frame.mode = Opcodes.F_APPEND
                frame.localDiff = tag - MethodWriter.SAME_FRAME_EXTENDED
                frame.localCount += frame.localDiff
                frame.stackCount = 0
            } else { // if (tag == FULL_FRAME) {
                frame.mode = Opcodes.F_FULL
                var n = readUnsignedShort(stackMap)
                stackMap += 2
                frame.localDiff = n
                frame.localCount = n
                var local = 0
                while (n > 0) {
                    stackMap = readFrameType(
                        frame.local, local++, stackMap, c,
                        labels
                    )
                    n--
                }
                n = readUnsignedShort(stackMap)
                stackMap += 2
                frame.stackCount = n
                var stack = 0
                while (n > 0) {
                    stackMap = readFrameType(
                        frame.stack, stack++, stackMap, c,
                        labels
                    )
                    n--
                }
            }
        }
        frame.offset += delta + 1
        readLabel(frame.offset, labels)
        return stackMap
    }

    /**
     * Reads a stack map frame type and stores it at the given index in the
     * given array.
     *
     * @param frame
     * the array where the parsed type must be stored.
     * @param index
     * the index in 'frame' where the parsed type must be stored.
     * @param v
     * the start offset of the stack map frame type to read.
     * @param buf
     * a buffer to read strings.
     * @param labels
     * the labels of the method currently being parsed, indexed by
     * their offset. If the parsed type is an Uninitialized type, a
     * new label for the corresponding NEW instruction is stored in
     * this array if it does not already exist.
     * @return the offset of the first byte after the parsed type.
     */
    private fun readFrameType(
        frame: Array<Any?>?, index: Int, v: Int,
        buf: CharArray?, labels: Array<Label?>?
    ): Int {
        var v = v
        val type = b[v++].toInt() and 0xFF
        when (type) {
            0 -> frame!![index] = Opcodes.TOP
            1 -> frame!![index] = Opcodes.INTEGER
            2 -> frame!![index] = Opcodes.FLOAT
            3 -> frame!![index] = Opcodes.DOUBLE
            4 -> frame!![index] = Opcodes.LONG
            5 -> frame!![index] = Opcodes.NULL
            6 -> frame!![index] = Opcodes.UNINITIALIZED_THIS
            7 // Object
            -> {
                frame!![index] = readClass(v, buf)!!
                v += 2
            }
            else // Uninitialized
            -> {
                frame!![index] = readLabel(readUnsignedShort(v), labels)
                v += 2
            }
        }
        return v
    }

    /**
     * Returns the label corresponding to the given offset. The default
     * implementation of this method creates a label for the given offset if it
     * has not been already created.
     *
     * @param offset
     * a bytecode offset in a method.
     * @param labels
     * the already created labels, indexed by their offset. If a
     * label already exists for offset this method must not create a
     * new one. Otherwise it must store the new label in this array.
     * @return a non null Label, which must be equal to labels[offset].
     */
    protected fun readLabel(offset: Int, labels: Array<Label?>?): Label {
        if (labels!![offset] == null) {
            labels[offset] = Label()
        }
        return labels!![offset]!!
    }

    /**
     * Reads an attribute in [b][.b].
     *
     * @param attrs
     * prototypes of the attributes that must be parsed during the
     * visit of the class. Any attribute whose type is not equal to
     * the type of one the prototypes is ignored (i.e. an empty
     * [Attribute] instance is returned).
     * @param type
     * the type of the attribute.
     * @param off
     * index of the first byte of the attribute's content in
     * [b][.b]. The 6 attribute header bytes, containing the
     * type and the length of the attribute, are not taken into
     * account here (they have already been read).
     * @param len
     * the length of the attribute's content.
     * @param buf
     * buffer to be used to call [readUTF8][.readUTF8],
     * [readClass][.readClass] or [            readConst][.readConst].
     * @param codeOff
     * index of the first byte of code's attribute content in
     * [b][.b], or -1 if the attribute to be read is not a code
     * attribute. The 6 attribute header bytes, containing the type
     * and the length of the attribute, are not taken into account
     * here.
     * @param labels
     * the labels of the method's code, or <tt>null</tt> if the
     * attribute to be read is not a code attribute.
     * @return the attribute that has been read, or <tt>null</tt> to skip this
     * attribute.
     */
    private fun readAttribute(
        attrs: Array<Attribute?>, type: String?,
        off: Int, len: Int, buf: CharArray?, codeOff: Int,
        labels: Array<Label?>?
    ): Attribute {
        for (i in attrs.indices) {
            if (attrs[i]!!.type == type) {
                return attrs[i]!!.read(this, off, len, buf!!, codeOff, labels!!)
            }
        }

        return Attribute(type!!).read(this, off, len, null, -1, null)
    }

    /**
     * Returns the start index of the constant pool item in [b][.b], plus
     * one. *This method is intended for [Attribute] sub classes, and is
     * normally not needed by class generators or adapters.*
     *
     * @param item
     * the index a constant pool item.
     * @return the start index of the constant pool item in [b][.b], plus
     * one.
     */
    fun getItem(item: Int): Int {
        return items[item]
    }

    /**
     * Reads a byte value in [b][.b]. *This method is intended for
     * [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @param index
     * the start index of the value to be read in [b][.b].
     * @return the read value.
     */
    fun readByte(index: Int): Int {
        return b[index].toInt() and 0xFF
    }

    /**
     * Reads an unsigned short value in [b][.b]. *This method is intended
     * for [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @param index
     * the start index of the value to be read in [b][.b].
     * @return the read value.
     */
    fun readUnsignedShort(index: Int): Int {
        val b = this.b
        return b[index].toInt() and 0xFF shl 8 or (b[index + 1].toInt() and 0xFF)
    }

    /**
     * Reads a signed short value in [b][.b]. *This method is intended
     * for [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @param index
     * the start index of the value to be read in [b][.b].
     * @return the read value.
     */
    fun readShort(index: Int): Short {
        val b = this.b
        return (b[index].toInt() and 0xFF shl 8 or (b[index + 1].toInt() and 0xFF)).toShort()
    }

    /**
     * Reads a signed int value in [b][.b]. *This method is intended for
     * [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @param index
     * the start index of the value to be read in [b][.b].
     * @return the read value.
     */
    fun readInt(index: Int): Int {
        val b = this.b
        return (b[index].toInt() and 0xFF shl 24 or (b[index + 1].toInt() and 0xFF shl 16)
                or (b[index + 2].toInt() and 0xFF shl 8) or (b[index + 3].toInt() and 0xFF))
    }

    /**
     * Reads a signed long value in [b][.b]. *This method is intended for
     * [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @param index
     * the start index of the value to be read in [b][.b].
     * @return the read value.
     */
    fun readLong(index: Int): Long {
        val l1 = readInt(index).toLong()
        val l0 = readInt(index + 4) and 0xFFFFFFFFL.toInt()
        return (l1.toInt() shl 32 or l0).toLong()
    }

    /**
     * Reads an UTF8 string constant pool item in [b][.b]. *This method
     * is intended for [Attribute] sub classes, and is normally not needed
     * by class generators or adapters.*
     *
     * @param index
     * the start index of an unsigned short value in [b][.b],
     * whose value is the index of an UTF8 constant pool item.
     * @param buf
     * buffer to be used to read the item. This buffer must be
     * sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 item.
     */
    fun readUTF8(index: Int, buf: CharArray?): String? {

        var index = index

        val item = readUnsignedShort(index)

        if (index == 0 || item == 0) {
            return null
        }
        val s = strings[item]

        if (s != null) {
            return s
        }
        index = items[item]

        strings[item] = readUTF(index + 2, readUnsignedShort(index), buf)


        return strings[item]
    }

    /**
     * Reads UTF8 string in [b][.b].
     *
     * @param index
     * start offset of the UTF8 string to be read.
     * @param utfLen
     * length of the UTF8 string to be read.
     * @param buf
     * buffer to be used to read the string. This buffer must be
     * sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private fun readUTF(index: Int, utfLen: Int, buf: CharArray?): String {
        var index = index
        val endIndex = index + utfLen
        val b = this.b
        var strLen = 0
        var c: Int
        var st = 0
        var cc: Char = 0.toChar()
        while (index < endIndex) {
            c = b[index++].toInt()
            when (st) {
                0 -> {
                    c = c and 0xFF
                    if (c < 0x80) { // 0xxxxxxx
                        buf!![strLen++] = c.toChar()
                    } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                        cc = (c and 0x1F).toChar()
                        st = 1
                    } else { // 1110 xxxx 10xx xxxx 10xx xxxx
                        cc = (c and 0x0F).toChar()
                        st = 2
                    }
                }

                1 // byte 2 of 2-byte char or byte 3 of 3-byte char
                -> {
                    buf!![strLen++] = (cc.toInt() shl 6 or (c and 0x3F)).toChar()
                    st = 0
                }

                2 // byte 2 of 3-byte char
                -> {
                    cc = (cc.toInt() shl 6 or (c and 0x3F)).toChar()
                    st = 1
                }
            }
        }
        return String(buf!!, 0, strLen)
    }

    /**
     * Reads a class constant pool item in [b][.b]. *This method is
     * intended for [Attribute] sub classes, and is normally not needed by
     * class generators or adapters.*
     *
     * @param index
     * the start index of an unsigned short value in [b][.b],
     * whose value is the index of a class constant pool item.
     * @param buf
     * buffer to be used to read the item. This buffer must be
     * sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified class item.
     */
    fun readClass(index: Int, buf: CharArray?): String? {
        // computes the start index of the CONSTANT_Class item in b
        // and reads the CONSTANT_Utf8 item designated by
        // the first two bytes of this CONSTANT_Class item
        return readUTF8(items[readUnsignedShort(index)], buf)
    }

    /**
     * Reads a numeric or string constant pool item in [b][.b]. *This
     * method is intended for [Attribute] sub classes, and is normally not
     * needed by class generators or adapters.*
     *
     * @param item
     * the index of a constant pool item.
     * @param buf
     * buffer to be used to read the item. This buffer must be
     * sufficiently large. It is not automatically resized.
     * @return the [Integer], [Float], [Long], [Double],
     * [String], [Type] or [Handle] corresponding to
     * the given constant pool item.
     */
    fun readConst(item: Int, buf: CharArray?): Any? {
        val index = items[item]
        when (b[index - 1].toInt()) {
            ClassWriter.INT -> return readInt(index)
            ClassWriter.FLOAT -> return readInt(index).toFloat().toBits()
            ClassWriter.LONG -> return readLong(index)
            ClassWriter.DOUBLE -> return readLong(index).toDouble().toBits()
            ClassWriter.CLASS -> return Type.getObjectType(
                readUTF8(index, buf)!!
            )
            ClassWriter.STR -> {

                return readUTF8(index, buf)}
            ClassWriter.MTYPE -> return Type.getMethodType(
                readUTF8(index, buf)!!
            )
            else // case ClassWriter.HANDLE_BASE + [1..9]:
            -> {
                val tag = readByte(index)
                val items = this.items
                var cpIndex = items[readUnsignedShort(index + 1)]
                val owner = readClass(cpIndex, buf)
                cpIndex = items[readUnsignedShort(cpIndex + 2)]
                val name = readUTF8(cpIndex, buf)
                val desc = readUTF8(cpIndex + 2, buf)
                return Handle(tag, owner!!, name!!, desc!!)
            }
        }
    }

    companion object {

        /**
         * True to enable signatures support.
         */
        internal val SIGNATURES = true

        /**
         * True to enable annotations support.
         */
        internal val ANNOTATIONS = true

        /**
         * True to enable stack map frames support.
         */
        internal val FRAMES = true

        /**
         * True to enable bytecode writing support.
         */
        internal val WRITER = true

        /**
         * True to enable JSR_W and GOTO_W support.
         */
        internal val RESIZE = true

        /**
         * Flag to skip method code. If this class is set `CODE`
         * attribute won't be visited. This can be used, for example, to retrieve
         * annotations for methods and method parameters.
         */
        val SKIP_CODE = 1

        /**
         * Flag to skip the debug information in the class. If this flag is set the
         * debug information of the class is not visited, i.e. the
         * [visitLocalVariable][MethodVisitor.visitLocalVariable] and
         * [visitLineNumber][MethodVisitor.visitLineNumber] methods will not be
         * called.
         */
        val SKIP_DEBUG = 2

        /**
         * Flag to skip the stack map frames in the class. If this flag is set the
         * stack map frames of the class is not visited, i.e. the
         * [visitFrame][MethodVisitor.visitFrame] method will not be called.
         * This flag is useful when the [ClassWriter.COMPUTE_FRAMES] option is
         * used: it avoids visiting frames that will be ignored and recomputed from
         * scratch in the class writer.
         */
        val SKIP_FRAMES = 4

        /**
         * Flag to expand the stack map frames. By default stack map frames are
         * visited in their original format (i.e. "expanded" for classes whose
         * version is less than V1_6, and "compressed" for the other classes). If
         * this flag is set, stack map frames are always visited in expanded format
         * (this option adds a decompression/recompression step in ClassReader and
         * ClassWriter which degrades performances quite a lot).
         */
        val EXPAND_FRAMES = 8

        /**
         * Reads the bytecode of a class.
         *
         * @param is
         * an input stream from which to read the class.
         * @param close
         * true to close the input stream after reading.
         * @return the bytecode read from the given input stream.
         * @throws IOException
         * if a problem occurs during reading.
         */

    }
}// ------------------------------------------------------------------------
// Constructors
// ------------------------------------------------------------------------
/**
 * Constructs a new [ClassReader] object.
 *
 * @param b
 * the bytecode of the class to be read.
 */
