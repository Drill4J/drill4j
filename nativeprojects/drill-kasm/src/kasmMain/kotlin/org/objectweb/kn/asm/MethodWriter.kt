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

import kotlin.math.max


/**
 * A [MethodVisitor] that generates methods in bytecode form. Each visit
 * method of this class appends the bytecode corresponding to the visited
 * instruction to a byte vector, in the order these methods are called.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
open class MethodWriter
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------

/**
 * Constructs a new [MethodWriter].
 *
 * @param cw
 * the class writer in which the method must be added.
 * @param access
 * the method's access flags (see [Opcodes]).
 * @param name
 * the method's name.
 * @param desc
 * the method's descriptor (see [Type]).
 * @param signature
 * the method's signature. May be <tt>null</tt>.
 * @param exceptions
 * the internal names of the method's exceptions. May be
 * <tt>null</tt>.
 * @param computeMaxs
 * <tt>true</tt> if the maximum stack size and number of local
 * variables must be automatically computed.
 * @param computeFrames
 * <tt>true</tt> if the stack map tables must be recomputed from
 * scratch.
 */
    (
    /**
     * The class writer to which this method must be added.
     */
    val cw: ClassWriter, access: Int, name: String,
    /**
     * The descriptor of this method.
     */
    private val descriptor: String, signature: String?,
    exceptions: Array<String?>?, computeMaxs: Boolean,
    computeFrames: Boolean
) : MethodVisitor(Opcodes.ASM5) {

    /**
     * Access flags of this method.
     */
    private var access: Int = 0

    /**
     * The index of the constant pool item that contains the name of this
     * method.
     */
    private val name: Int

    /**
     * The index of the constant pool item that contains the descriptor of this
     * method.
     */
    private val desc: Int

    /**
     * The signature of this method.
     */
    var signature: String? = null

    /**
     * If not zero, indicates that the code of this method must be copied from
     * the ClassReader associated to this writer in `cw.cr`. More
     * precisely, this field gives the index of the first byte to copied from
     * `cw.cr.b`.
     */
    var classReaderOffset: Int = 0

    /**
     * If not zero, indicates that the code of this method must be copied from
     * the ClassReader associated to this writer in `cw.cr`. More
     * precisely, this field gives the number of bytes to copied from
     * `cw.cr.b`.
     */
    var classReaderLength: Int = 0

    /**
     * Number of exceptions that can be thrown by this method.
     */
    var exceptionCount: Int = 0

    /**
     * The exceptions that can be thrown by this method. More precisely, this
     * array contains the indexes of the constant pool items that contain the
     * internal names of these exception classes.
     */
    lateinit var exceptions: IntArray

    /**
     * The annotation default attribute of this method. May be <tt>null</tt>.
     */
    private var annd: ByteVector? = null

    /**
     * The runtime visible annotations of this method. May be <tt>null</tt>.
     */
    private var anns: AnnotationWriter? = null

    /**
     * The runtime invisible annotations of this method. May be <tt>null</tt>.
     */
    private var ianns: AnnotationWriter? = null

    /**
     * The runtime visible type annotations of this method. May be <tt>null</tt>
     * .
     */
    private var tanns: AnnotationWriter? = null

    /**
     * The runtime invisible type annotations of this method. May be
     * <tt>null</tt>.
     */
    private var itanns: AnnotationWriter? = null

    /**
     * The runtime visible parameter annotations of this method. May be
     * <tt>null</tt>.
     */
    private var panns: Array<AnnotationWriter?>? = null

    /**
     * The runtime invisible parameter annotations of this method. May be
     * <tt>null</tt>.
     */
    private var ipanns: Array<AnnotationWriter?>? = null

    /**
     * The number of synthetic parameters of this method.
     */
    private var synthetics: Int = 0

    /**
     * The non standard attributes of the method.
     */
    private var attrs: Attribute? = null

    /**
     * The bytecode of this method.
     */
    private var code = ByteVector()

    /**
     * Maximum stack size of this method.
     */
    private var maxStack: Int = 0

    /**
     * Maximum number of local variables for this method.
     */
    private var maxLocals: Int = 0

    /**
     * Number of local variables in the current stack map frame.
     */
    private var currentLocals: Int = 0

    /**
     * Number of stack map frames in the StackMapTable attribute.
     */
    private var frameCount: Int = 0

    /**
     * The StackMapTable attribute.
     */
    private var stackMap: ByteVector? = null

    /**
     * The offset of the last frame that was written in the StackMapTable
     * attribute.
     */
    private var previousFrameOffset: Int = 0

    /**
     * The last frame that was written in the StackMapTable attribute.
     *
     * @see .frame
     */
    private var previousFrame: IntArray? = null

    /**
     * Index of the next element to be added in [.frame].
     */
    private var frameIndex: Int = 0

    /**
     * The current stack map frame. The first element contains the offset of the
     * instruction to which the frame corresponds, the second element is the
     * number of locals and the third one is the number of stack elements. The
     * local variables start at index 3 and are followed by the operand stack
     * values. In summary frame!![0] = offset, frame!![1] = nLocal, frame!![2] =
     * nStack, frame!![3] = nLocal. All types are encoded as integers, with the
     * same format as the one used in [Label], but limited to BASE types.
     */
    private var frame: IntArray? = null

    /**
     * Number of elements in the exception handler list.
     */
    private var handlerCount: Int = 0

    /**
     * The first element in the exception handler list.
     */
    private var firstHandler: Handler? = null

    /**
     * The last element in the exception handler list.
     */
    private var lastHandler: Handler? = null

    /**
     * Number of entries in the MethodParameters attribute.
     */
    private var methodParametersCount: Int = 0

    /**
     * The MethodParameters attribute.
     */
    private var methodParameters: ByteVector? = null

    /**
     * Number of entries in the LocalVariableTable attribute.
     */
    private var localVarCount: Int = 0

    /**
     * The LocalVariableTable attribute.
     */
    private var localVar: ByteVector? = null

    /**
     * Number of entries in the LocalVariableTypeTable attribute.
     */
    private var localVarTypeCount: Int = 0

    /**
     * The LocalVariableTypeTable attribute.
     */
    private var localVarType: ByteVector? = null

    /**
     * Number of entries in the LineNumberTable attribute.
     */
    private var lineNumberCount: Int = 0

    /**
     * The LineNumberTable attribute.
     */
    private var lineNumber: ByteVector? = null

    /**
     * The start offset of the last visited instruction.
     */
    private var lastCodeOffset: Int = 0

    /**
     * The runtime visible type annotations of the code. May be <tt>null</tt>.
     */
    private var ctanns: AnnotationWriter? = null

    /**
     * The runtime invisible type annotations of the code. May be <tt>null</tt>.
     */
    private var ictanns: AnnotationWriter? = null

    /**
     * The non standard attributes of the method's code.
     */
    private var cattrs: Attribute? = null

    /**
     * Indicates if some jump instructions are too small and need to be resized.
     */
    private var resize: Boolean = false

    /**
     * The number of subroutines in this method.
     */
    private var subroutines: Int = 0

    // ------------------------------------------------------------------------

    /*
     * Fields for the control flow graph analysis algorithm (used to compute the
     * maximum stack size). A control flow graph contains one node per "basic
     * block", and one edge per "jump" from one basic block to another. Each
     * node (i.e., each basic block) is represented by the Label object that
     * corresponds to the first instruction of this basic block. Each node also
     * stores the list of its successors in the graph, as a linked list of Edge
     * objects.
     */

    /**
     * Indicates what must be automatically computed.
     *
     * @see .FRAMES
     *
     * @see .MAXS
     *
     * @see .NOTHING
     */
    private val compute: Int

    /**
     * A list of labels. This list is the list of basic blocks in the method,
     * i.e. a list of Label objects linked to each other by their
     * [Label.successor] field, in the order they are visited by
     * [MethodVisitor.visitLabel], and starting with the first basic
     * block.
     */
    private var labels: Label? = null

    /**
     * The previous basic block.
     */
    private var previousBlock: Label? = null

    /**
     * The current basic block.
     */
    private var currentBlock: Label? = null

    /**
     * The (relative) stack size after the last visited instruction. This size
     * is relative to the beginning of the current basic block, i.e., the true
     * stack size after the last visited instruction is equal to the
     * [beginStackSize][Label.inputStackTop] of the current basic block
     * plus <tt>stackSize</tt>.
     */
    private var stackSize: Int = 0

    /**
     * The (relative) maximum stack size after the last visited instruction.
     * This size is relative to the beginning of the current basic block, i.e.,
     * the true maximum stack size after the last visited instruction is equal
     * to the [beginStackSize][Label.inputStackTop] of the current basic
     * block plus <tt>stackSize</tt>.
     */
    private var maxStackSize: Int = 0

    // ------------------------------------------------------------------------
    // Utility methods: dump bytecode array
    // ------------------------------------------------------------------------

    /**
     * Returns the size of the bytecode of this method.
     *
     * @return the size of the bytecode of this method.
     */
    // replaces the temporary jump opcodes introduced by Label.resolve.
    val size: Int
        get() {
            if (classReaderOffset != 0) {
                return 6 + classReaderLength
            }
            if (resize) {
                if (ClassReader.RESIZE) {
                    resizeInstructions()
                } else {
                    throw RuntimeException("Method code too large!")
                }
            }
            var size = 8
            if (code.length > 0) {
                if (code.length > 65536) {
                    throw RuntimeException("Method code too large!")
                }
                cw.newUTF8("Code")
                size += 18 + code.length + 8 * handlerCount
                if (localVar != null) {
                    cw.newUTF8("LocalVariableTable")
                    size += 8 + localVar!!.length
                }
                if (localVarType != null) {
                    cw.newUTF8("LocalVariableTypeTable")
                    size += 8 + localVarType!!.length
                }
                if (lineNumber != null) {
                    cw.newUTF8("LineNumberTable")
                    size += 8 + lineNumber!!.length
                }
                if (stackMap != null) {
                    val zip = cw.version and 0xFFFF >= Opcodes.V1_6
                    cw.newUTF8(if (zip) "StackMapTable" else "StackMap")
                    size += 8 + stackMap!!.length
                }
                if (ClassReader.ANNOTATIONS && ctanns != null) {
                    cw.newUTF8("RuntimeVisibleTypeAnnotations")
                    size += 8 + ctanns!!.getSize()
                }
                if (ClassReader.ANNOTATIONS && ictanns != null) {
                    cw.newUTF8("RuntimeInvisibleTypeAnnotations")
                    size += 8 + ictanns!!.getSize()
                }
                if (cattrs != null) {
                    size += cattrs!!.getSize(
                        cw, code.data, code.length, maxStack,
                        maxLocals
                    )
                }
            }
            if (exceptionCount > 0) {
                cw.newUTF8("Exceptions")
                size += 8 + 2 * exceptionCount
            }
            if (access and Opcodes.ACC_SYNTHETIC !== 0) {
                if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                    cw.newUTF8("Synthetic")
                    size += 6
                }
            }
            if (access and Opcodes.ACC_DEPRECATED !== 0) {
                cw.newUTF8("Deprecated")
                size += 6
            }
            if (ClassReader.SIGNATURES && signature != null) {
                cw.newUTF8("Signature")
                cw.newUTF8(signature)
                size += 8
            }
            if (methodParameters != null) {
                cw.newUTF8("MethodParameters")
                size += 7 + methodParameters!!.length
            }
            if (ClassReader.ANNOTATIONS && annd != null) {
                cw.newUTF8("AnnotationDefault")
                size += 6 + annd!!.length
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
            if (ClassReader.ANNOTATIONS && panns != null) {
                cw.newUTF8("RuntimeVisibleParameterAnnotations")
                size += 7 + 2 * (panns!!.size - synthetics)
                for (i in panns!!.size - 1 downTo synthetics) {
                    size += if (panns!![i] == null) 0 else panns!![i]!!.getSize()
                }
            }
            if (ClassReader.ANNOTATIONS && ipanns != null) {
                cw.newUTF8("RuntimeInvisibleParameterAnnotations")
                size += 7 + 2 * (ipanns!!.size - synthetics)
                for (i in ipanns!!.size - 1 downTo synthetics) {
                    size += if (ipanns!![i] == null) 0 else ipanns!![i]!!.getSize()
                }
            }
            if (attrs != null) {
                size += attrs!!.getSize(cw, null, 0, -1, -1)
            }
            return size
        }

    init {
        if (cw.firstMethod == null) {
            cw.firstMethod = this
        } else {
            cw.lastMethod!!.mv = this
        }
        cw.lastMethod = this
        this.access = access
        this.name = cw.newUTF8(name)
        this.desc = cw.newUTF8(descriptor)
        if (ClassReader.SIGNATURES) {
            this.signature = signature
        }
        if (exceptions != null && exceptions.size > 0) {
            exceptionCount = exceptions.size
            this.exceptions = IntArray(exceptionCount)
            for (i in 0 until exceptionCount) {
                this.exceptions[i] = cw.newClass(exceptions[i])
            }
        }
        this.compute = if (computeFrames) FRAMES else if (computeMaxs) MAXS else NOTHING
        if (computeMaxs || computeFrames) {
            if (computeFrames && "<init>" == name) {
                this.access = this.access or ACC_CONSTRUCTOR
            }
            // updates maxLocals
            var size = Type.getArgumentsAndReturnSizes(descriptor) shr 2
            if (access and Opcodes.ACC_STATIC !== 0) {
                --size
            }
            maxLocals = size
            currentLocals = size
            // creates and visits the label for the first basic block
            labels = Label()
            labels!!.status = labels!!.status or Label.PUSHED
            visitLabel(labels!!)
        }
    }

    // ------------------------------------------------------------------------
    // Implementation of the MethodVisitor abstract class
    // ------------------------------------------------------------------------

    override fun visitParameter(name: String, access: Int) {
        if (methodParameters == null) {
            methodParameters = ByteVector()
        }
        ++methodParametersCount
        methodParameters!!.putShort(if (name == null) 0 else cw.newUTF8(name))
            .putShort(access)
    }

    override fun visitAnnotationDefault(): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        annd = ByteVector()
        return AnnotationWriter(cw, false, annd!!, null, 0)
    }

    override fun visitAnnotation(
        desc: String,
        visible: Boolean
    ): AnnotationVisitor? {
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

    override fun visitTypeAnnotation(
        typeRef: Int,
        typePath: TypePath, desc: String, visible: Boolean
    ): AnnotationVisitor? {
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

    override fun visitParameterAnnotation(
        parameter: Int,
        desc: String, visible: Boolean
    ): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        if ("Ljava/lang/Synthetic;" == desc) {
            // workaround for a bug in javac with synthetic parameters
            // see ClassReader.readParameterAnnotations
            synthetics = max(synthetics, parameter + 1)
            return AnnotationWriter(cw, false, bv, null, 0)
        }
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(cw, true, bv, bv, 2)
        if (visible) {
            if (panns == null) {
                panns = arrayOfNulls(Type.getArgumentTypes(descriptor).size)
            }
            aw.next = panns!![parameter]
            panns!![parameter] = aw
        } else {
            if (ipanns == null) {
                ipanns = arrayOfNulls(Type.getArgumentTypes(descriptor).size)
            }
            aw.next = ipanns!![parameter]
            ipanns!![parameter] = aw
        }
        return aw
    }

    override fun visitAttribute(attr: Attribute) {
        if (attr.isCodeAttribute) {
            attr.next = cattrs
            cattrs = attr
        } else {
            attr.next = attrs
            attrs = attr
        }
    }

    override fun visitCode() {}

    override fun visitFrame(
        type: Int, nLocal: Int,
        local: Array<Any?>, nStack: Int, stack: Array<Any?>
    ) {
        if (!ClassReader.FRAMES || compute == FRAMES) {
            return
        }

        if (type == Opcodes.F_NEW) {
            currentLocals = nLocal
            startFrame(code.length, nLocal, nStack)
            for (i in 0 until nLocal) {
                if (local[i] is String) {
                    frame!![frameIndex++] = Frame.OBJECT or cw.addType(local[i] as String)
                } else if (local[i] is Int) {
                    frame!![frameIndex++] = (local[i] as Int).toInt()
                } else {
                    frame!![frameIndex++] = Frame.UNINITIALIZED or cw.addUninitializedType(
                        "",
                        (local[i] as Label).position
                    )
                }
            }
            for (i in 0 until nStack) {
                if (stack[i] is String) {
                    frame!![frameIndex++] = Frame.OBJECT or cw.addType(stack[i] as String)
                } else if (stack[i] is Int) {
                    frame!![frameIndex++] = (stack[i] as Int).toInt()
                } else {
                    frame!![frameIndex++] = Frame.UNINITIALIZED or cw.addUninitializedType(
                        "",
                        (stack[i] as Label).position
                    )
                }
            }
            endFrame()
        } else {
            val delta: Int
            if (stackMap == null) {
                stackMap = ByteVector()
                delta = code.length
            } else {
                delta = code.length - previousFrameOffset - 1
                if (delta < 0) {
                    if (type == Opcodes.F_SAME) {
                        return
                    } else {
                        throw IllegalStateException()
                    }
                }
            }

            when (type) {
                Opcodes.F_FULL -> {
                    currentLocals = nLocal
                    stackMap!!.putByte(FULL_FRAME).putShort(delta).putShort(nLocal)
                    for (i in 0 until nLocal) {
                        writeFrameType(local[i]!!)
                    }
                    stackMap!!.putShort(nStack)
                    for (i in 0 until nStack) {
                        writeFrameType(stack[i]!!)
                    }
                }
                Opcodes.F_APPEND -> {
                    currentLocals += nLocal
                    stackMap!!.putByte(SAME_FRAME_EXTENDED + nLocal).putShort(delta)
                    for (i in 0 until nLocal) {
                        writeFrameType(local[i]!!)
                    }
                }
                Opcodes.F_CHOP -> {
                    currentLocals -= nLocal
                    stackMap!!.putByte(SAME_FRAME_EXTENDED - nLocal).putShort(delta)
                }
                Opcodes.F_SAME -> if (delta < 64) {
                    stackMap!!.putByte(delta)
                } else {
                    stackMap!!.putByte(SAME_FRAME_EXTENDED).putShort(delta)
                }
                Opcodes.F_SAME1 -> {
                    if (delta < 64) {
                        stackMap!!.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta)
                    } else {
                        stackMap!!.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                            .putShort(delta)
                    }
                    writeFrameType(stack[0]!!)
                }
            }

            previousFrameOffset = code.length
            ++frameCount
        }

        maxStack = max(maxStack, nStack)
        maxLocals = max(maxLocals, currentLocals)
    }

    override fun visitInsn(opcode: Int) {
        lastCodeOffset = code.length
        // adds the instruction to the bytecode of the method
        code.putByte(opcode)
        // update currentBlock
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, 0, null, null)
            } else {
                // updates current and max stack sizes
                val size = stackSize + Frame.SIZE[opcode]
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
            // if opcode == ATHROW or xRETURN, ends current block (no successor)
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN || opcode == Opcodes.ATHROW) {
                noSuccessor()
            }
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        lastCodeOffset = code.length
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, operand, null, null)
            } else if (opcode != Opcodes.NEWARRAY) {
                // updates current and max stack sizes only for NEWARRAY
                // (stack size variation = 0 for BIPUSH or SIPUSH)
                val size = stackSize + 1
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        if (opcode == Opcodes.SIPUSH) {
            code.put12(opcode, operand)
        } else { // BIPUSH or NEWARRAY
            code.put11(opcode, operand)
        }
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        lastCodeOffset = code.length
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, `var`, null, null)
            } else {
                // updates current and max stack sizes
                if (opcode == Opcodes.RET) {
                    // no stack change, but end of current block (no successor)
                    currentBlock!!.status = currentBlock!!.status or Label.RET
                    // save 'stackSize' here for future use
                    // (see {@link #findSubroutineSuccessors})
                    currentBlock!!.inputStackTop = stackSize
                    noSuccessor()
                } else { // xLOAD or xSTORE
                    val size = stackSize + Frame.SIZE[opcode]
                    if (size > maxStackSize) {
                        maxStackSize = size
                    }
                    stackSize = size
                }
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            val n: Int
            if (opcode == Opcodes.LLOAD || opcode == Opcodes.DLOAD
                || opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE
            ) {
                n = `var` + 2
            } else {
                n = `var` + 1
            }
            if (n > maxLocals) {
                maxLocals = n
            }
        }
        // adds the instruction to the bytecode of the method
        if (`var` < 4 && opcode != Opcodes.RET) {
            val opt: Int
            if (opcode < Opcodes.ISTORE) {
                /* ILOAD_0 */
                opt = 26 + (opcode - Opcodes.ILOAD shl 2) + `var`
            } else {
                /* ISTORE_0 */
                opt = 59 + (opcode - Opcodes.ISTORE shl 2) + `var`
            }
            code.putByte(opt)
        } else if (`var` >= 256) {
            code.putByte(196 /* WIDE */).put12(opcode, `var`)
        } else {
            code.put11(opcode, `var`)
        }
        if (opcode >= Opcodes.ISTORE && compute == FRAMES && handlerCount > 0) {
            visitLabel(Label())
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        lastCodeOffset = code.length
        val i = cw.newClassItem(type)
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, code.length, cw, i)
            } else if (opcode == Opcodes.NEW) {
                // updates current and max stack sizes only if opcode == NEW
                // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF)
                val size = stackSize + 1
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index)
    }

    override fun visitFieldInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        lastCodeOffset = code.length
        val i = cw.newFieldItem(owner, name, desc)
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, 0, cw, i)
            } else {
                val size: Int
                // computes the stack size variation
                val c = desc[0]
                when (opcode) {
                    Opcodes.GETSTATIC -> size = stackSize + (if (c == 'D' || c == 'J') 2 else 1)
                    Opcodes.PUTSTATIC -> size = stackSize + (if (c == 'D' || c == 'J') -2 else -1)
                    Opcodes.GETFIELD -> size = stackSize + (if (c == 'D' || c == 'J') 1 else 0)
                    // case Constants.PUTFIELD:
                    else -> size = stackSize + (if (c == 'D' || c == 'J') -3 else -2)
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(opcode, i.index)
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        lastCodeOffset = code.length
        val itf = opcode == Opcodes.INVOKEINTERFACE
        val i = cw.newMethodItem(owner, name, desc, itf)
        var argSize = i.intVal
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, 0, cw, i)
            } else {
                /*
                 * computes the stack size variation. In order not to recompute
                 * several times this variation for the same Item, we use the
                 * intVal field of this item to store this variation, once it
                 * has been computed. More precisely this intVal field stores
                 * the sizes of the arguments and of the return value
                 * corresponding to desc.
                 */
                if (argSize == 0) {
                    // the above sizes have not been computed yet,
                    // so we compute them...
                    argSize = Type.getArgumentsAndReturnSizes(desc)
                    // ... and we save them in order
                    // not to recompute them in the future
                    i.intVal = argSize
                }
                val size: Int
                if (opcode == Opcodes.INVOKESTATIC) {
                    size = stackSize - (argSize shr 2) + (argSize and 0x03) + 1
                } else {
                    size = stackSize - (argSize shr 2) + (argSize and 0x03)
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        if (itf) {
            if (argSize == 0) {
                argSize = Type.getArgumentsAndReturnSizes(desc)
                i.intVal = argSize
            }
            code.put12(Opcodes.INVOKEINTERFACE, i.index).put11(argSize shr 2, 0)
        } else {
            code.put12(opcode, i.index)
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String, desc: String,
        bsm: Handle, bsmArgs: Array<Any?>
    ) {
        lastCodeOffset = code.length
        val i = cw.newInvokeDynamicItem(name, desc, bsm, bsmArgs)
        var argSize = i.intVal
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(Opcodes.INVOKEDYNAMIC, 0, cw, i)
            } else {
                /*
                 * computes the stack size variation. In order not to recompute
                 * several times this variation for the same Item, we use the
                 * intVal field of this item to store this variation, once it
                 * has been computed. More precisely this intVal field stores
                 * the sizes of the arguments and of the return value
                 * corresponding to desc.
                 */
                if (argSize == 0) {
                    // the above sizes have not been computed yet,
                    // so we compute them...
                    argSize = Type.getArgumentsAndReturnSizes(desc)
                    // ... and we save them in order
                    // not to recompute them in the future
                    i.intVal = argSize
                }
                val size = stackSize - (argSize shr 2) + (argSize and 0x03) + 1

                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(Opcodes.INVOKEDYNAMIC, i.index)
        code.putShort(0)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        lastCodeOffset = code.length
        var nextInsn: Label? = null
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(opcode, 0, null, null)
                // 'label' is the target of a jump instruction
                label.first.status = label.first.status or Label.TARGET
                // adds 'label' as a successor of this basic block
                addSuccessor(Edge.NORMAL, label)
                if (opcode != Opcodes.GOTO) {
                    // creates a Label for the next basic block
                    nextInsn = Label()
                }
            } else {
                if (opcode == Opcodes.JSR) {
                    if (label.status and Label.SUBROUTINE == 0) {
                        label.status = label.status or Label.SUBROUTINE
                        ++subroutines
                    }
                    currentBlock!!.status = currentBlock!!.status or Label.JSR
                    addSuccessor(stackSize + 1, label)
                    // creates a Label for the next basic block
                    nextInsn = Label()
                    /*
                     * note that, by construction in this method, a JSR block
                     * has at least two successors in the control flow graph:
                     * the first one leads the next instruction after the JSR,
                     * while the second one leads to the JSR target.
                     */
                } else {
                    // updates current stack size (max stack size unchanged
                    // because stack size variation always negative in this
                    // case)
                    stackSize += Frame.SIZE[opcode]
                    addSuccessor(stackSize, label)
                }
            }
        }
        // adds the instruction to the bytecode of the method
        if (label.status and Label.RESOLVED != 0 && label.position - code.length < Short.MIN_VALUE) {
            /*
             * case of a backward jump with an offset < -32768. In this case we
             * automatically replace GOTO with GOTO_W, JSR with JSR_W and IFxxx
             * <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the
             * "opposite" opcode of IFxxx (i.e., IFNE for IFEQ) and where <l'>
             * designates the instruction just after the GOTO_W.
             */
            if (opcode == Opcodes.GOTO) {
                code.putByte(200) // GOTO_W
            } else if (opcode == Opcodes.JSR) {
                code.putByte(201) // JSR_W
            } else {
                // if the IF instruction is transformed into IFNOT GOTO_W the
                // next instruction becomes the target of the IFNOT instruction
                if (nextInsn != null) {
                    nextInsn.status = nextInsn.status or Label.TARGET
                }
                code.putByte(
                    if (opcode <= 166)
                        (opcode + 1 xor 1) - 1
                    else
                        opcode xor 1
                )
                code.putShort(8) // jump offset
                code.putByte(200) // GOTO_W
            }
            label.put(this, code, code.length - 1, true)
        } else {
            /*
             * case of a backward jump with an offset >= -32768, or of a forward
             * jump with, of course, an unknown offset. In these cases we store
             * the offset in 2 bytes (which will be increased in
             * resizeInstructions, if needed).
             */
            code.putByte(opcode)
            label.put(this, code, code.length - 1, false)
        }
        if (currentBlock != null) {
            if (nextInsn != null) {
                // if the jump instruction is not a GOTO, the next instruction
                // is also a successor of this instruction. Calling visitLabel
                // adds the label of this next instruction as a successor of the
                // current block, and starts a new basic block
                visitLabel(nextInsn)
            }
            if (opcode == Opcodes.GOTO) {
                noSuccessor()
            }
        }
    }

    override fun visitLabel(label: Label) {
        // resolves previous forward references to label, if any
        resize = resize or label.resolve(this, code.length, code.data)
        // updates currentBlock
        if (label.status and Label.DEBUG != 0) {
            return
        }
        if (compute == FRAMES) {
            if (currentBlock != null) {
                if (label.position == currentBlock!!.position) {
                    // successive labels, do not start a new basic block
                    currentBlock!!.status = currentBlock!!.status or (label.status and Label.TARGET)
                    label.frame = currentBlock!!.frame
                    return
                }
                // ends current block (with one new successor)
                addSuccessor(Edge.NORMAL, label)
            }
            // begins a new current block
            currentBlock = label
            if (label.frame == null) {
                label.frame = Frame()
                label.frame!!.owner = label
            }
            // updates the basic block list
            if (previousBlock != null) {
                if (label.position == previousBlock!!.position) {
                    previousBlock!!.status = previousBlock!!.status or (label.status and Label.TARGET)
                    label.frame = previousBlock!!.frame
                    currentBlock = previousBlock
                    return
                }
                previousBlock!!.successor = label
            }
            previousBlock = label
        } else if (compute == MAXS) {
            if (currentBlock != null) {
                // ends current block (with one new successor)
                currentBlock!!.outputStackMax = maxStackSize
                addSuccessor(stackSize, label)
            }
            // begins a new current block
            currentBlock = label
            // resets the relative current and max stack sizes
            stackSize = 0
            maxStackSize = 0
            // updates the basic block list
            if (previousBlock != null) {
                previousBlock!!.successor = label
            }
            previousBlock = label
        }
    }

    override fun visitLdcInsn(cst: Any) {
        lastCodeOffset = code.length
        val i = cw.newConstItem(cst)
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(Opcodes.LDC, 0, cw, i)
            } else {
                val size: Int
                // computes the stack size variation
                if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
                    size = stackSize + 2
                } else {
                    size = stackSize + 1
                }
                // updates current and max stack sizes
                if (size > maxStackSize) {
                    maxStackSize = size
                }
                stackSize = size
            }
        }
        // adds the instruction to the bytecode of the method
        val index = i.index
        if (i.type == ClassWriter.LONG || i.type == ClassWriter.DOUBLE) {
            code.put12(20 /* LDC2_W */, index)
        } else if (index >= 256) {
            code.put12(19 /* LDC_W */, index)
        } else {
            code.put11(Opcodes.LDC, index)
        }
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        lastCodeOffset = code.length
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(Opcodes.IINC, `var`, null, null)
            }
        }
        if (compute != NOTHING) {
            // updates max locals
            val n = `var` + 1
            if (n > maxLocals) {
                maxLocals = n
            }
        }
        // adds the instruction to the bytecode of the method
        if (`var` > 255 || increment > 127 || increment < -128) {
            code.putByte(196 /* WIDE */).put12(Opcodes.IINC, `var`)
                .putShort(increment)
        } else {
            code.putByte(Opcodes.IINC).put11(`var`, increment)
        }
    }

    override fun visitTableSwitchInsn(
        min: Int, max: Int,
        dflt: Label, labels: Array<Label?>
    ) {
        lastCodeOffset = code.length
        // adds the instruction to the bytecode of the method
        val source = code.length
        code.putByte(Opcodes.TABLESWITCH)
        code.putByteArray(null, 0, (4 - code.length % 4) % 4)
        dflt.put(this, code, source, true)
        code.putInt(min).putInt(max)
        for (i in labels.indices) {
            labels[i]!!.put(this, code, source, true)
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels)
    }

    override fun visitLookupSwitchInsn(
        dflt: Label, keys: IntArray,
        labels: Array<Label?>
    ) {
        lastCodeOffset = code.length
        // adds the instruction to the bytecode of the method
        val source = code.length
        code.putByte(Opcodes.LOOKUPSWITCH)
        code.putByteArray(null, 0, (4 - code.length % 4) % 4)
        dflt.put(this, code, source, true)
        code.putInt(labels.size)
        for (i in labels.indices) {
            code.putInt(keys[i])
            labels[i]!!.put(this, code, source, true)
        }
        // updates currentBlock
        visitSwitchInsn(dflt, labels)
    }

    private fun visitSwitchInsn(dflt: Label, labels: Array<Label?>) {
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(Opcodes.LOOKUPSWITCH, 0, null, null)
                // adds current block successors
                addSuccessor(Edge.NORMAL, dflt)
                dflt.first.status = dflt.first.status or Label.TARGET
                for (i in labels.indices) {
                    addSuccessor(Edge.NORMAL, labels[i]!!)
                    labels[i]!!.first.status = labels[i]!!.first.status or Label.TARGET
                }
            } else {
                // updates current stack size (max stack size unchanged)
                --stackSize
                // adds current block successors
                addSuccessor(stackSize, dflt)
                for (i in labels.indices) {
                    addSuccessor(stackSize, labels[i]!!)
                }
            }
            // ends current block
            noSuccessor()
        }
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        lastCodeOffset = code.length
        val i = cw.newClassItem(desc)
        // Label currentBlock = this.currentBlock;
        if (currentBlock != null) {
            if (compute == FRAMES) {
                currentBlock!!.frame!!.execute(Opcodes.MULTIANEWARRAY, dims, cw, i)
            } else {
                // updates current stack size (max stack size unchanged because
                // stack size variation always negative or null)
                stackSize += 1 - dims
            }
        }
        // adds the instruction to the bytecode of the method
        code.put12(Opcodes.MULTIANEWARRAY, i.index).putByte(dims)
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath, desc: String?, visible: Boolean
    ): AnnotationVisitor? {
        var typeRef = typeRef
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        // write target_type and target_info
        typeRef = typeRef and -0xffff01 or (lastCodeOffset shl 8)
        AnnotationWriter.putTarget(typeRef, typePath, bv)
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(
            cw, true, bv, bv,
            bv.length - 2
        )
        if (visible) {
            aw.next = ctanns
            ctanns = aw
        } else {
            aw.next = ictanns
            ictanns = aw
        }
        return aw
    }

    override fun visitTryCatchBlock(
        start: Label, end: Label,
        handler: Label, type: String?
    ) {
        ++handlerCount
        val h = Handler()
        h.start = start
        h.end = end
        h.handler = handler
        h.desc = type
        h.type = if (type != null) cw.newClass(type) else 0
        if (lastHandler == null) {
            firstHandler = h
        } else {
            lastHandler!!.next = h
        }
        lastHandler = h
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath, desc: String, visible: Boolean
    ): AnnotationVisitor? {
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
            aw.next = ctanns
            ctanns = aw
        } else {
            aw.next = ictanns
            ictanns = aw
        }
        return aw
    }

    override fun visitLocalVariable(
        name: String, desc: String,
        signature: String?, start: Label, end: Label,
        index: Int
    ) {
        if (signature != null) {
            if (localVarType == null) {
                localVarType = ByteVector()
            }
            ++localVarTypeCount
            localVarType!!.putShort(start.position)
                .putShort(end.position - start.position)
                .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature))
                .putShort(index)
        }
        if (localVar == null) {
            localVar = ByteVector()
        }
        ++localVarCount
        localVar!!.putShort(start.position)
            .putShort(end.position - start.position)
            .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
            .putShort(index)
        if (compute != NOTHING) {
            // updates max locals
            val c = desc[0]
            val n = index + if (c == 'J' || c == 'D') 2 else 1
            if (n > maxLocals) {
                maxLocals = n
            }
        }
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath, start: Array<Label?>, end: Array<Label?>, index: IntArray,
        desc: String, visible: Boolean
    ): AnnotationVisitor? {
        if (!ClassReader.ANNOTATIONS) {
            return null
        }
        val bv = ByteVector()
        // write target_type and target_info
        bv.putByte(typeRef.ushr(24)).putShort(start.size)
        for (i in start.indices) {
            bv.putShort(start[i]!!.position)
                .putShort(end[i]!!.position - start[i]!!.position)
                .putShort(index[i])
        }
        if (typePath == null) {
            bv.putByte(0)
        } else {
            val length = typePath.b[typePath.offset] * 2 + 1
            bv.putByteArray(typePath.b, typePath.offset, length)
        }
        // write type, and reserve space for values count
        bv.putShort(cw.newUTF8(desc)).putShort(0)
        val aw = AnnotationWriter(
            cw, true, bv, bv,
            bv.length - 2
        )
        if (visible) {
            aw.next = ctanns
            ctanns = aw
        } else {
            aw.next = ictanns
            ictanns = aw
        }
        return aw
    }

    override fun visitLineNumber(line: Int, start: Label) {
        if (lineNumber == null) {
            lineNumber = ByteVector()
        }
        ++lineNumberCount
        lineNumber!!.putShort(start.position)
        lineNumber!!.putShort(line)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (ClassReader.FRAMES && compute == FRAMES) {
            // completes the control flow graph with exception handler blocks
            var handler = firstHandler
            while (handler != null) {
                var l = handler.start!!.first
                val h = handler.handler!!.first
                val e = handler.end!!.first
                // computes the kind of the edges to 'h'
                val t = if (handler.desc == null)
                    "java/lang/Throwable"
                else
                    handler.desc
                val kind = Frame.OBJECT or cw.addType(t!!)
                // h is an exception handler
                h.status = h.status or Label.TARGET
                // adds 'h' as a successor of labels between 'start' and 'end'
                while (l != e) {
                    // creates an edge to 'h'
                    val b = Edge()
                    b.info = kind
                    b.successor = h
                    // adds it to the successors of 'l'
                    b.next = l!!.successors
                    l!!.successors = b
                    // goes to the next label
                    l = l!!.successor!!
                }
                handler = handler.next
            }

            // creates and visits the first (implicit) frame
            var f = labels!!.frame
            val args = Type.getArgumentTypes(descriptor)
            f!!.initInputFrame(cw, access, args, this.maxLocals)
            visitFrame(f)

            /*
             * fix point algorithm: mark the first basic block as 'changed'
             * (i.e. put it in the 'changed' list) and, while there are changed
             * basic blocks, choose one, mark it as unchanged, and update its
             * successors (which can be changed in the process).
             */
            var max = 0
            var changed = labels
            while (changed != null) {
                // removes a basic block from the list of changed basic blocks
                val l = changed
                changed = changed.next
                l.next = null
                f = l.frame
                // a reachable jump target must be stored in the stack map
                if (l.status and Label.TARGET != 0) {
                    l.status = l.status or Label.STORE
                }
                // all visited labels are reachable, by definition
                l.status = l.status or Label.REACHABLE
                // updates the (absolute) maximum stack size
                val blockMax = f!!.inputStack!!.size + l.outputStackMax
                if (blockMax > max) {
                    max = blockMax
                }
                // updates the successors of the current basic block
                var e: Edge? = l.successors
                while (e != null) {
                    val n = e.successor!!.first
                    val change = f.merge(cw, n.frame!!, e.info)
                    if (change && n.next == null) {
                        // if n has changed and is not already in the 'changed'
                        // list, adds it to this list
                        n.next = changed
                        changed = n
                    }
                    e = e.next
                }
            }

            // visits all the frames that must be stored in the stack map
            var l = labels
            while (l != null) {
                f = l.frame
                if (l.status and Label.STORE != 0) {
                    visitFrame(f!!)
                }
                if (l.status and Label.REACHABLE == 0) {
                    // finds start and end of dead basic block
                    val k = l.successor
                    val start = l.position
                    val end = (k?.position ?: code.length) - 1
                    // if non empty basic block
                    if (end >= start) {
                        max = max(max, 1)
                        // replaces instructions with NOP ... NOP ATHROW
                        for (i in start until end) {
                            code.data[i] = Opcodes.NOP.toByte()
                        }
                        code.data[end] = Opcodes.ATHROW.toByte()
                        // emits a frame for this unreachable block
                        startFrame(start, 0, 1)
                        frame!![frameIndex++] = Frame.OBJECT or cw.addType("java/lang/Throwable")
                        endFrame()
                        // removes the start-end range from the exception
                        // handlers
                        firstHandler = Handler.remove(firstHandler, l, k)
                    }
                }
                l = l.successor
            }

            handler = firstHandler
            handlerCount = 0
            while (handler != null) {
                handlerCount += 1
                handler = handler.next
            }

            this.maxStack = max
        } else if (compute == MAXS) {
            // completes the control flow graph with exception handler blocks
            var handler = firstHandler
            while (handler != null) {
                var l = handler.start
                val h = handler.handler
                val e = handler.end
                // adds 'h' as a successor of labels between 'start' and 'end'
                while (l != e) {
                    // creates an edge to 'h'
                    val b = Edge()
                    b.info = Edge.EXCEPTION
                    b.successor = h
                    // adds it to the successors of 'l'
                    if (l!!.status and Label.JSR == 0) {
                        b.next = l.successors
                        l.successors = b
                    } else {
                        // if l is a JSR block, adds b after the first two edges
                        // to preserve the hypothesis about JSR block successors
                        // order (see {@link #visitJumpInsn})
                        b.next = l.successors?.next!!.next
                        l.successors?.next!!.next = b
                    }
                    // goes to the next label
                    l = l.successor
                }
                handler = handler.next
            }

            if (subroutines > 0) {
                // completes the control flow graph with the RET successors
                /*
                 * first step: finds the subroutines. This step determines, for
                 * each basic block, to which subroutine(s) it belongs.
                 */
                // finds the basic blocks that belong to the "main" subroutine
                var id = 0
                labels!!.visitSubroutine(null, 1, subroutines)
                // finds the basic blocks that belong to the real subroutines
                var l = labels
                while (l != null) {
                    if (l.status and Label.JSR != 0) {
                        // the subroutine is defined by l's TARGET, not by l
                        val subroutine = l.successors?.next!!.successor
                        // if this subroutine has not been visited yet...
                        if (subroutine!!.status and Label.VISITED == 0) {
                            // ...assigns it a new id and finds its basic blocks
                            id += 1
                            subroutine.visitSubroutine(null, id / 32L shl 32 or (1L shl id % 32), subroutines)
                        }
                    }
                    l = l.successor
                }
                // second step: finds the successors of RET blocks
                l = labels
                while (l != null) {
                    if (l.status and Label.JSR != 0) {
                        var L = labels
                        while (L != null) {
                            L.status = L.status and Label.VISITED2.inv()
                            L = L.successor
                        }
                        // the subroutine is defined by l's TARGET, not by l
                        val subroutine = l.successors?.next!!.successor
                        subroutine!!.visitSubroutine(l, 0, subroutines)
                    }
                    l = l.successor
                }
            }

            /*
             * control flow analysis algorithm: while the block stack is not
             * empty, pop a block from this stack, update the max stack size,
             * compute the true (non relative) begin stack size of the
             * successors of this block, and push these successors onto the
             * stack (unless they have already been pushed onto the stack).
             * Note: by hypothesis, the {@link Label#inputStackTop} of the
             * blocks in the block stack are the true (non relative) beginning
             * stack sizes of these blocks.
             */
            var max = 0
            var stack = labels
            while (stack != null) {
                // pops a block from the stack
                var l = stack
                stack = stack.next
                // computes the true (non relative) max stack size of this block
                val start = l.inputStackTop
                val blockMax = start + l.outputStackMax
                // updates the global max stack size
                if (blockMax > max) {
                    max = blockMax
                }
                // analyzes the successors of the block
                var b: Edge? = l.successors
                if (l.status and Label.JSR != 0) {
                    // ignores the first edge of JSR blocks (virtual successor)
                    b = b!!.next
                }
                while (b != null) {
                    l = b.successor
                    // if this successor has not already been pushed...
                    if (l!!.status and Label.PUSHED == 0) {
                        // computes its true beginning stack size...
                        l.inputStackTop = if (b.info == Edge.EXCEPTION)
                            1
                        else
                            start + b.info
                        // ...and pushes it onto the stack
                        l.status = l.status or Label.PUSHED
                        l.next = stack
                        stack = l
                    }
                    b = b.next
                }
            }
            this.maxStack = max(maxStack, max)
        } else {
            this.maxStack = maxStack
            this.maxLocals = maxLocals
        }
    }

    override fun visitEnd() {}

    // ------------------------------------------------------------------------
    // Utility methods: control flow analysis algorithm
    // ------------------------------------------------------------------------

    /**
     * Adds a successor to the [currentBlock][.currentBlock] block.
     *
     * @param info
     * information about the control flow edge to be added.
     * @param successor
     * the successor block to be added to the current block.
     */
    private fun addSuccessor(info: Int, successor: Label) {
        // creates and initializes an Edge object...
        val b = Edge()
        b.info = info
        b.successor = successor
        // ...and adds it to the successor list of the currentBlock block
        b.next = currentBlock!!.successors
        currentBlock!!.successors = b
    }

    /**
     * Ends the current basic block. This method must be used in the case where
     * the current basic block does not have any successor.
     */
    private fun noSuccessor() {
        if (compute == FRAMES) {
            val l = Label()
            l.frame = Frame()
            l.frame!!.owner = l
            l.resolve(this, code.length, code.data)
            previousBlock!!.successor = l
            previousBlock = l
        } else {
            currentBlock!!.outputStackMax = maxStackSize
        }
        currentBlock = null
    }

    // ------------------------------------------------------------------------
    // Utility methods: stack map frames
    // ------------------------------------------------------------------------

    /**
     * Visits a frame that has been computed from scratch.
     *
     * @param f
     * the frame that must be visited.
     */
    private fun visitFrame(f: Frame) {
        var i: Int
        var t: Int
        var nTop = 0
        var nLocal = 0
        var nStack = 0
        val locals = f.inputLocals
        val stacks = f.inputStack
        // computes the number of locals (ignores TOP types that are just after
        // a LONG or a DOUBLE, and all trailing TOP types)
        i = 0
        while (i < locals!!.size) {
            t = locals[i]
            if (t == Frame.TOP) {
                ++nTop
            } else {
                nLocal += nTop + 1
                nTop = 0
            }
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i
            }
            ++i
        }
        // computes the stack size (ignores TOP types that are just after
        // a LONG or a DOUBLE)
        i = 0
        while (i < stacks!!.size) {
            t = stacks[i]
            ++nStack
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i
            }
            ++i
        }
        // visits the frame and its content
        startFrame(f.owner!!.position, nLocal, nStack)
        i = 0
        while (nLocal > 0) {
            t = locals[i]
            frame!![frameIndex++] = t
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i
            }
            ++i
            --nLocal
        }
        i = 0
        while (i < stacks.size) {
            t = stacks[i]
            frame!![frameIndex++] = t
            if (t == Frame.LONG || t == Frame.DOUBLE) {
                ++i
            }
            ++i
        }
        endFrame()
    }

    /**
     * Starts the visit of a stack map frame.
     *
     * @param offset
     * the offset of the instruction to which the frame corresponds.
     * @param nLocal
     * the number of local variables in the frame.
     * @param nStack
     * the number of stack elements in the frame.
     */
    private fun startFrame(offset: Int, nLocal: Int, nStack: Int) {
        val n = 3 + nLocal + nStack
        if (frame == null || frame!!.size < n) {
            frame = IntArray(n)
        }
        frame!![0] = offset
        frame!![1] = nLocal
        frame!![2] = nStack
        frameIndex = 3
    }

    /**
     * Checks if the visit of the current frame [.frame] is finished, and
     * if yes, write it in the StackMapTable attribute.
     */
    private fun endFrame() {
        if (previousFrame != null) { // do not write the first frame
            if (stackMap == null) {
                stackMap = ByteVector()
            }
            writeFrame()
            ++frameCount
        }
        previousFrame = frame
        frame = null
    }

    /**
     * Compress and writes the current frame [.frame] in the StackMapTable
     * attribute.
     */
    private fun writeFrame() {
        val clocalsSize = frame!![1]
        val cstackSize = frame!![2]
        if (cw.version and 0xFFFF < Opcodes.V1_6) {
            stackMap!!.putShort(frame!![0]).putShort(clocalsSize)
            writeFrameTypes(3, 3 + clocalsSize)
            stackMap!!.putShort(cstackSize)
            writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize)
            return
        }
        var localsSize = previousFrame!![1]
        var type = FULL_FRAME
        var k = 0
        val delta: Int
        if (frameCount == 0) {
            delta = frame!![0]
        } else {
            delta = frame!![0] - previousFrame!![0] - 1
        }
        if (cstackSize == 0) {
            k = clocalsSize - localsSize
            when (k) {
                -3, -2, -1 -> {
                    type = CHOP_FRAME
                    localsSize = clocalsSize
                }
                0 -> type = if (delta < 64) SAME_FRAME else SAME_FRAME_EXTENDED
                1, 2, 3 -> type = APPEND_FRAME
            }
        } else if (clocalsSize == localsSize && cstackSize == 1) {
            type = if (delta < 63)
                SAME_LOCALS_1_STACK_ITEM_FRAME
            else
                SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED
        }
        if (type != FULL_FRAME) {
            // verify if locals are the same
            var l = 3
            for (j in 0 until localsSize) {
                if (frame!![l] != previousFrame!![l]) {
                    type = FULL_FRAME
                    break
                }
                l++
            }
        }
        when (type) {
            SAME_FRAME -> stackMap!!.putByte(delta)
            SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                stackMap!!.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME + delta)
                writeFrameTypes(3 + clocalsSize, 4 + clocalsSize)
            }
            SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED -> {
                stackMap!!.putByte(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED).putShort(
                    delta
                )
                writeFrameTypes(3 + clocalsSize, 4 + clocalsSize)
            }
            SAME_FRAME_EXTENDED -> stackMap!!.putByte(SAME_FRAME_EXTENDED).putShort(delta)
            CHOP_FRAME -> stackMap!!.putByte(SAME_FRAME_EXTENDED + k).putShort(delta)
            APPEND_FRAME -> {
                stackMap!!.putByte(SAME_FRAME_EXTENDED + k).putShort(delta)
                writeFrameTypes(3 + localsSize, 3 + clocalsSize)
            }
            // case FULL_FRAME:
            else -> {
                stackMap!!.putByte(FULL_FRAME).putShort(delta).putShort(clocalsSize)
                writeFrameTypes(3, 3 + clocalsSize)
                stackMap!!.putShort(cstackSize)
                writeFrameTypes(3 + clocalsSize, 3 + clocalsSize + cstackSize)
            }
        }
    }

    /**
     * Writes some types of the current frame [.frame] into the
     * StackMapTableAttribute. This method converts types from the format used
     * in [Label] to the format used in StackMapTable attributes. In
     * particular, it converts type table indexes to constant pool indexes.
     *
     * @param start
     * index of the first type in [.frame] to write.
     * @param end
     * index of last type in [.frame] to write (exclusive).
     */
    private fun writeFrameTypes(start: Int, end: Int) {
        for (i in start until end) {
            val t = frame!![i]
            var d = t and Frame.DIM
            if (d == 0) {
                val v = t and Frame.BASE_VALUE
                when (t and Frame.BASE_KIND) {
                    Frame.OBJECT -> stackMap!!.putByte(7).putShort(
                        cw.newClass(cw.typeTable!![v]!!.strVal1)
                    )
                    Frame.UNINITIALIZED -> stackMap!!.putByte(8).putShort(cw.typeTable!![v]!!.intVal)
                    else -> stackMap!!.putByte(v)
                }
            } else {
                val buf = StringBuilder()
                d = d shr 28
                while (d-- > 0) {
                    buf.append('[')
                }
                if (t and Frame.BASE_KIND == Frame.OBJECT) {
                    buf.append('L')
                    buf.append(cw.typeTable!![t and Frame.BASE_VALUE]!!.strVal1)
                    buf.append(';')
                } else {
                    when (t and 0xF) {
                        1 -> buf.append('I')
                        2 -> buf.append('F')
                        3 -> buf.append('D')
                        9 -> buf.append('Z')
                        10 -> buf.append('B')
                        11 -> buf.append('C')
                        12 -> buf.append('S')
                        else -> buf.append('J')
                    }
                }
                stackMap!!.putByte(7).putShort(cw.newClass(buf.toString()))
            }
        }
    }

    private fun writeFrameType(type: Any) {
        if (type is String) {
            stackMap!!.putByte(7).putShort(cw.newClass(type))
        } else if (type is Int) {
            stackMap!!.putByte(type.toInt())
        } else {
            stackMap!!.putByte(8).putShort((type as Label).position)
        }
    }

    /**
     * Puts the bytecode of this method in the given byte vector.
     *
     * @param out
     * the byte vector into which the bytecode of this method must be
     * copied.
     */
    fun put(out: ByteVector) {
        val FACTOR = ClassWriter.TO_ACC_SYNTHETIC
        val mask = (Opcodes.ACC_DEPRECATED or ClassWriter.ACC_SYNTHETIC_ATTRIBUTE
                or (access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE) / FACTOR)
        out.putShort(access and mask.inv()).putShort(name).putShort(desc)
        if (classReaderOffset != 0) {
            out.putByteArray(cw.cr!!.b, classReaderOffset, classReaderLength)
            return
        }
        var attributeCount = 0
        if (code.length > 0) {
            ++attributeCount
        }
        if (exceptionCount > 0) {
            ++attributeCount
        }
        if (access and Opcodes.ACC_SYNTHETIC !== 0) {
            if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                ++attributeCount
            }
        }
        if (access and Opcodes.ACC_DEPRECATED !== 0) {
            ++attributeCount
        }
        if (ClassReader.SIGNATURES && signature != null) {
            ++attributeCount
        }
        if (methodParameters != null) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
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
        if (ClassReader.ANNOTATIONS && panns != null) {
            ++attributeCount
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            ++attributeCount
        }
        if (attrs != null) {
            attributeCount += attrs!!.count
        }
        out.putShort(attributeCount)
        if (code.length > 0) {
            var size = 12 + code.length + 8 * handlerCount
            if (localVar != null) {
                size += 8 + localVar!!.length
            }
            if (localVarType != null) {
                size += 8 + localVarType!!.length
            }
            if (lineNumber != null) {
                size += 8 + lineNumber!!.length
            }
            if (stackMap != null) {
                size += 8 + stackMap!!.length
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                size += 8 + ctanns!!.getSize()
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                size += 8 + ictanns!!.getSize()
            }
            if (cattrs != null) {
                size += cattrs!!.getSize(
                    cw, code.data, code.length, maxStack,
                    maxLocals
                )
            }
            out.putShort(cw.newUTF8("Code")).putInt(size)
            out.putShort(maxStack).putShort(maxLocals)
            out.putInt(code.length).putByteArray(code.data, 0, code.length)
            out.putShort(handlerCount)
            if (handlerCount > 0) {
                var h = firstHandler
                while (h != null) {
                    out.putShort(h.start!!.position).putShort(h.end!!.position)
                        .putShort(h.handler!!.position).putShort(h.type)
                    h = h.next
                }
            }
            attributeCount = 0
            if (localVar != null) {
                ++attributeCount
            }
            if (localVarType != null) {
                ++attributeCount
            }
            if (lineNumber != null) {
                ++attributeCount
            }
            if (stackMap != null) {
                ++attributeCount
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                ++attributeCount
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                ++attributeCount
            }
            if (cattrs != null) {
                attributeCount += cattrs!!.count
            }
            out.putShort(attributeCount)
            if (localVar != null) {
                out.putShort(cw.newUTF8("LocalVariableTable"))
                out.putInt(localVar!!.length + 2).putShort(localVarCount)
                out.putByteArray(localVar!!.data, 0, localVar!!.length)
            }
            if (localVarType != null) {
                out.putShort(cw.newUTF8("LocalVariableTypeTable"))
                out.putInt(localVarType!!.length + 2).putShort(localVarTypeCount)
                out.putByteArray(localVarType!!.data, 0, localVarType!!.length)
            }
            if (lineNumber != null) {
                out.putShort(cw.newUTF8("LineNumberTable"))
                out.putInt(lineNumber!!.length + 2).putShort(lineNumberCount)
                out.putByteArray(lineNumber!!.data, 0, lineNumber!!.length)
            }
            if (stackMap != null) {
                val zip = cw.version and 0xFFFF >= Opcodes.V1_6
                out.putShort(cw.newUTF8(if (zip) "StackMapTable" else "StackMap"))
                out.putInt(stackMap!!.length + 2).putShort(frameCount)
                out.putByteArray(stackMap!!.data, 0, stackMap!!.length)
            }
            if (ClassReader.ANNOTATIONS && ctanns != null) {
                out.putShort(cw.newUTF8("RuntimeVisibleTypeAnnotations"))
                ctanns!!.put(out)
            }
            if (ClassReader.ANNOTATIONS && ictanns != null) {
                out.putShort(cw.newUTF8("RuntimeInvisibleTypeAnnotations"))
                ictanns!!.put(out)
            }
            if (cattrs != null) {
                cattrs!!.put(cw, code.data, code.length, maxLocals, maxStack, out)
            }
        }
        if (exceptionCount > 0) {
            out.putShort(cw.newUTF8("Exceptions")).putInt(
                2 * exceptionCount + 2
            )
            out.putShort(exceptionCount)
            for (i in 0 until exceptionCount) {
                out.putShort(exceptions[i])
            }
        }
        if (access and Opcodes.ACC_SYNTHETIC !== 0) {
            if (cw.version and 0xFFFF < Opcodes.V1_5 || access and ClassWriter.ACC_SYNTHETIC_ATTRIBUTE != 0) {
                out.putShort(cw.newUTF8("Synthetic")).putInt(0)
            }
        }
        if (access and Opcodes.ACC_DEPRECATED !== 0) {
            out.putShort(cw.newUTF8("Deprecated")).putInt(0)
        }
        if (ClassReader.SIGNATURES && signature != null) {
            out.putShort(cw.newUTF8("Signature")).putInt(2)
                .putShort(cw.newUTF8(signature))
        }
        if (methodParameters != null) {
            out.putShort(cw.newUTF8("MethodParameters"))
            out.putInt(methodParameters!!.length + 1).putByte(
                methodParametersCount
            )
            out.putByteArray(methodParameters!!.data, 0, methodParameters!!.length)
        }
        if (ClassReader.ANNOTATIONS && annd != null) {
            out.putShort(cw.newUTF8("AnnotationDefault"))
            out.putInt(annd!!.length)
            out.putByteArray(annd!!.data, 0, annd!!.length)
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
        if (ClassReader.ANNOTATIONS && panns != null) {
            out.putShort(cw.newUTF8("RuntimeVisibleParameterAnnotations"))
            AnnotationWriter.put(panns!!, synthetics, out)
        }
        if (ClassReader.ANNOTATIONS && ipanns != null) {
            out.putShort(cw.newUTF8("RuntimeInvisibleParameterAnnotations"))
            AnnotationWriter.put(ipanns!!, synthetics, out)
        }
        if (attrs != null) {
            attrs!!.put(cw, null, 0, -1, -1, out)
        }
    }

    // ------------------------------------------------------------------------
    // Utility methods: instruction resizing (used to handle GOTO_W and JSR_W)
    // ------------------------------------------------------------------------

    /**
     * Resizes and replaces the temporary instructions inserted by
     * [Label.resolve] for wide forward jumps, while keeping jump offsets
     * and instruction addresses consistent. This may require to resize other
     * existing instructions, or even to introduce new instructions: for
     * example, increasing the size of an instruction by 2 at the middle of a
     * method can increases the offset of an IFEQ instruction from 32766 to
     * 32768, in which case IFEQ 32766 must be replaced with IFNEQ 8 GOTO_W
     * 32765. This, in turn, may require to increase the size of another jump
     * instruction, and so on... All these operations are handled automatically
     * by this method.
     *
     *
     * *This method must be called after all the method that is being built
     * has been visited*. In particular, the [Label] objects used
     * to construct the method are no longer valid after this method has been
     * called.
     */
    private fun resizeInstructions() {
        var b = code.data // bytecode of the method
        var u: Int
        var v: Int
        var label: Int // indexes in b
        var i: Int
        var j: Int // loop indexes
        /*
         * 1st step: As explained above, resizing an instruction may require to
         * resize another one, which may require to resize yet another one, and
         * so on. The first step of the algorithm consists in finding all the
         * instructions that need to be resized, without modifying the code.
         * This is done by the following "fix point" algorithm:
         *
         * Parse the code to find the jump instructions whose offset will need
         * more than 2 bytes to be stored (the future offset is computed from
         * the current offset and from the number of bytes that will be inserted
         * or removed between the source and target instructions). For each such
         * instruction, adds an entry in (a copy of) the indexes and sizes
         * arrays (if this has not already been done in a previous iteration!).
         *
         * If at least one entry has been added during the previous step, go
         * back to the beginning, otherwise stop.
         *
         * In fact the real algorithm is complicated by the fact that the size
         * of TABLESWITCH and LOOKUPSWITCH instructions depends on their
         * position in the bytecode (because of padding). In order to ensure the
         * convergence of the algorithm, the number of bytes to be added or
         * removed from these instructions is over estimated during the previous
         * loop, and computed exactly only after the loop is finished (this
         * requires another pass to parse the bytecode of the method).
         */
        var allIndexes = IntArray(0) // copy of indexes
        var allSizes = IntArray(0) // copy of sizes
        val resize: BooleanArray // instructions to be resized
        var newOffset: Int // future offset of a jump instruction

        resize = BooleanArray(code.length)

        // 3 = loop again, 2 = loop ended, 1 = last pass, 0 = done
        var state = 3
        do {
            if (state == 3) {
                state = 2
            }
            u = 0
            while (u < b.size) {
                var opcode = b[u].toInt() and 0xFF // opcode of current instruction
                var insert = 0 // bytes to be added after this instruction

                when (ClassWriter.TYPE[opcode].toInt()) {
                    ClassWriter.NOARG_INSN, ClassWriter.IMPLVAR_INSN -> u += 1
                    ClassWriter.LABEL_INSN -> {
                        if (opcode > 201) {
                            // converts temporary opcodes 202 to 217, 218 and
                            // 219 to IFEQ ... JSR (inclusive), IFNULL and
                            // IFNONNULL
                            opcode = if (opcode < 218) opcode - 49 else opcode - 20
                            label = u + readUnsignedShort(b, u + 1)
                        } else {
                            label = u + readShort(b, u + 1)
                        }
                        newOffset =
                                getNewOffset(allIndexes, allSizes, u, label)
                        if (newOffset < Short.MIN_VALUE || newOffset >Short.MAX_VALUE) {
                            if (!resize[u]) {
                                if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR) {
                                    // two additional bytes will be required to
                                    // replace this GOTO or JSR instruction with
                                    // a GOTO_W or a JSR_W
                                    insert = 2
                                } else {
                                    // five additional bytes will be required to
                                    // replace this IFxxx <l> instruction with
                                    // IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx
                                    // is the "opposite" opcode of IFxxx (i.e.,
                                    // IFNE for IFEQ) and where <l'> designates
                                    // the instruction just after the GOTO_W.
                                    insert = 5
                                }
                                resize[u] = true
                            }
                        }
                        u += 3
                    }
                    ClassWriter.LABELW_INSN -> u += 5
                    ClassWriter.TABL_INSN -> {
                        if (state == 1) {
                            // true number of bytes to be added (or removed)
                            // from this instruction = (future number of padding
                            // bytes - current number of padding byte) -
                            // previously over estimated variation =
                            // = ((3 - newOffset%4) - (3 - u%4)) - u%4
                            // = (-newOffset%4 + u%4) - u%4
                            // = -(newOffset & 3)
                            newOffset =
                                    getNewOffset(allIndexes, allSizes, 0, u)
                            insert = -(newOffset and 3)
                        } else if (!resize[u]) {
                            // over estimation of the number of bytes to be
                            // added to this instruction = 3 - current number
                            // of padding bytes = 3 - (3 - u%4) = u%4 = u & 3
                            insert = u and 3
                            resize[u] = true
                        }
                        // skips instruction
                        u = u + 4 - (u and 3)
                        u += 4 * (readInt(
                            b,
                            u + 8
                        ) - readInt(b, u + 4) + 1) + 12
                    }
                    ClassWriter.LOOK_INSN -> {
                        if (state == 1) {
                            // like TABL_INSN
                            newOffset =
                                    getNewOffset(allIndexes, allSizes, 0, u)
                            insert = -(newOffset and 3)
                        } else if (!resize[u]) {
                            // like TABL_INSN
                            insert = u and 3
                            resize[u] = true
                        }
                        // skips instruction
                        u = u + 4 - (u and 3)
                        u += 8 * readInt(b, u + 4) + 8
                    }
                    ClassWriter.WIDE_INSN -> {
                        opcode = b[u + 1].toInt() and 0xFF
                        if (opcode == Opcodes.IINC) {
                            u += 6
                        } else {
                            u += 4
                        }
                    }
                    ClassWriter.VAR_INSN, ClassWriter.SBYTE_INSN, ClassWriter.LDC_INSN -> u += 2
                    ClassWriter.SHORT_INSN, ClassWriter.LDCW_INSN, ClassWriter.FIELDORMETH_INSN, ClassWriter.TYPE_INSN, ClassWriter.IINC_INSN -> u += 3
                    ClassWriter.ITFMETH_INSN, ClassWriter.INDYMETH_INSN -> u += 5
                    // case ClassWriter.MANA_INSN:
                    else -> u += 4
                }
                if (insert != 0) {
                    // adds a new (u, insert) entry in the allIndexes and
                    // allSizes arrays
                    val newIndexes = IntArray(allIndexes.size + 1)
                    val newSizes = IntArray(allSizes.size + 1)
                    allIndexes.copyInto(newIndexes,0,0,allIndexes.size)
//                    System.arraycopy(
//                        allIndexes, 0, newIndexes, 0,
//                        allIndexes.size
//                    )
                    allSizes.copyInto(newSizes,0,0,allSizes.size)
//                    System.arraycopy(allSizes, 0, newSizes, 0, allSizes.size)
                    newIndexes[allIndexes.size] = u
                    newSizes[allSizes.size] = insert
                    allIndexes = newIndexes
                    allSizes = newSizes
                    if (insert > 0) {
                        state = 3
                    }
                }
            }
            if (state < 3) {
                --state
            }
        } while (state != 0)

        // 2nd step:
        // copies the bytecode of the method into a new bytevector, updates the
        // offsets, and inserts (or removes) bytes as requested.

        val newCode = ByteVector(code.length)

        u = 0
        while (u < code.length) {
            var opcode = b[u].toInt() and 0xFF
            when (ClassWriter.TYPE[opcode].toInt()) {
                ClassWriter.NOARG_INSN, ClassWriter.IMPLVAR_INSN -> {
                    newCode.putByte(opcode)
                    u += 1
                }
                ClassWriter.LABEL_INSN -> {
                    if (opcode > 201) {
                        // changes temporary opcodes 202 to 217 (inclusive), 218
                        // and 219 to IFEQ ... JSR (inclusive), IFNULL and
                        // IFNONNULL
                        opcode = if (opcode < 218) opcode - 49 else opcode - 20
                        label = u + readUnsignedShort(b, u + 1)
                    } else {
                        label = u + readShort(b, u + 1)
                    }
                    newOffset = getNewOffset(allIndexes, allSizes, u, label)
                    if (resize[u]) {
                        // replaces GOTO with GOTO_W, JSR with JSR_W and IFxxx
                        // <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is
                        // the "opposite" opcode of IFxxx (i.e., IFNE for IFEQ)
                        // and where <l'> designates the instruction just after
                        // the GOTO_W.
                        if (opcode == Opcodes.GOTO) {
                            newCode.putByte(200) // GOTO_W
                        } else if (opcode == Opcodes.JSR) {
                            newCode.putByte(201) // JSR_W
                        } else {
                            newCode.putByte(
                                if (opcode <= 166)
                                    (opcode + 1 xor 1) - 1
                                else
                                    opcode xor 1
                            )
                            newCode.putShort(8) // jump offset
                            newCode.putByte(200) // GOTO_W
                            // newOffset now computed from start of GOTO_W
                            newOffset -= 3
                        }
                        newCode.putInt(newOffset)
                    } else {
                        newCode.putByte(opcode)
                        newCode.putShort(newOffset)
                    }
                    u += 3
                }
                ClassWriter.LABELW_INSN -> {
                    label = u + readInt(b, u + 1)
                    newOffset = getNewOffset(allIndexes, allSizes, u, label)
                    newCode.putByte(opcode)
                    newCode.putInt(newOffset)
                    u += 5
                }
                ClassWriter.TABL_INSN -> {
                    // skips 0 to 3 padding bytes
                    v = u
                    u = u + 4 - (v and 3)
                    // reads and copies instruction
                    newCode.putByte(Opcodes.TABLESWITCH)
                    newCode.putByteArray(null, 0, (4 - newCode.length % 4) % 4)
                    label = v + readInt(b, u)
                    u += 4
                    newOffset = getNewOffset(allIndexes, allSizes, v, label)
                    newCode.putInt(newOffset)
                    j = readInt(b, u)
                    u += 4
                    newCode.putInt(j)
                    j = readInt(b, u) - j + 1
                    u += 4
                    newCode.putInt(readInt(b, u - 4))
                    while (j > 0) {
                        label = v + readInt(b, u)
                        u += 4
                        newOffset =
                                getNewOffset(allIndexes, allSizes, v, label)
                        newCode.putInt(newOffset)
                        --j
                    }
                }
                ClassWriter.LOOK_INSN -> {
                    // skips 0 to 3 padding bytes
                    v = u
                    u = u + 4 - (v and 3)
                    // reads and copies instruction
                    newCode.putByte(Opcodes.LOOKUPSWITCH)
                    newCode.putByteArray(null, 0, (4 - newCode.length % 4) % 4)
                    label = v + readInt(b, u)
                    u += 4
                    newOffset = getNewOffset(allIndexes, allSizes, v, label)
                    newCode.putInt(newOffset)
                    j = readInt(b, u)
                    u += 4
                    newCode.putInt(j)
                    while (j > 0) {
                        newCode.putInt(readInt(b, u))
                        u += 4
                        label = v + readInt(b, u)
                        u += 4
                        newOffset =
                                getNewOffset(allIndexes, allSizes, v, label)
                        newCode.putInt(newOffset)
                        --j
                    }
                }
                ClassWriter.WIDE_INSN -> {
                    opcode = b[u + 1].toInt() and 0xFF
                    if (opcode == Opcodes.IINC) {
                        newCode.putByteArray(b, u, 6)
                        u += 6
                    } else {
                        newCode.putByteArray(b, u, 4)
                        u += 4
                    }
                }
                ClassWriter.VAR_INSN, ClassWriter.SBYTE_INSN, ClassWriter.LDC_INSN -> {
                    newCode.putByteArray(b, u, 2)
                    u += 2
                }
                ClassWriter.SHORT_INSN, ClassWriter.LDCW_INSN, ClassWriter.FIELDORMETH_INSN, ClassWriter.TYPE_INSN, ClassWriter.IINC_INSN -> {
                    newCode.putByteArray(b, u, 3)
                    u += 3
                }
                ClassWriter.ITFMETH_INSN, ClassWriter.INDYMETH_INSN -> {
                    newCode.putByteArray(b, u, 5)
                    u += 5
                }
                // case MANA_INSN:
                else -> {
                    newCode.putByteArray(b, u, 4)
                    u += 4
                }
            }
        }

        // recomputes the stack map frames
        if (frameCount > 0) {
            if (compute == FRAMES) {
                frameCount = 0
                stackMap = null
                previousFrame = null
                frame = null
                val f = Frame()
                f.owner = labels
                val args = Type.getArgumentTypes(descriptor)
                f.initInputFrame(cw, access, args, maxLocals)
                visitFrame(f)
                var l = labels
                while (l != null) {
                    /*
                     * here we need the original label position. getNewOffset
                     * must therefore never have been called for this label.
                     */
                    u = l.position - 3
                    if (l.status and Label.STORE != 0 || u >= 0 && resize[u]) {
                        getNewOffset(allIndexes, allSizes, l)
                        // TODO update offsets in UNINITIALIZED values
                        visitFrame(l.frame!!)
                    }
                    l = l.successor
                }
            } else {
                /*
                 * Resizing an existing stack map frame table is really hard.
                 * Not only the table must be parsed to update the offets, but
                 * new frames may be needed for jump instructions that were
                 * inserted by this method. And updating the offsets or
                 * inserting frames can change the format of the following
                 * frames, in case of packed frames. In practice the whole table
                 * must be recomputed. For this the frames are marked as
                 * potentially invalid. This will cause the whole class to be
                 * reread and rewritten with the COMPUTE_FRAMES option (see the
                 * ClassWriter.toByteArray method). This is not very efficient
                 * but is much easier and requires much less code than any other
                 * method I can think of.
                 */
                cw.invalidFrames = true
            }
        }
        // updates the exception handler block labels
        var h = firstHandler
        while (h != null) {
            getNewOffset(allIndexes, allSizes, h.start!!)
            getNewOffset(allIndexes, allSizes, h.end!!)
            getNewOffset(allIndexes, allSizes, h.handler!!)
            h = h.next
        }
        // updates the instructions addresses in the
        // local var and line number tables
        i = 0
        while (i < 2) {
            val bv = if (i == 0) localVar else localVarType
            if (bv != null) {
                b = bv.data
                u = 0
                while (u < bv.length) {
                    label = readUnsignedShort(b, u)
                    newOffset = getNewOffset(allIndexes, allSizes, 0, label)
                    writeShort(b, u, newOffset)
                    label += readUnsignedShort(b, u + 2)
                    newOffset = getNewOffset(
                        allIndexes,
                        allSizes,
                        0,
                        label
                    ) - newOffset
                    writeShort(b, u + 2, newOffset)
                    u += 10
                }
            }
            ++i
        }
        if (lineNumber != null) {
            b = lineNumber!!.data
            u = 0
            while (u < lineNumber!!.length) {
                writeShort(
                    b,
                    u,
                    getNewOffset(
                        allIndexes, allSizes, 0,
                        readUnsignedShort(b, u)
                    )
                )
                u += 4
            }
        }
        // updates the labels of the other attributes
        var attr = cattrs
        while (attr != null) {
            val labels = attr.labels
            if (labels != null) {
                i = labels.size - 1
                while (i >= 0) {
                    getNewOffset(allIndexes, allSizes, labels[i])
                    --i
                }
            }
            attr = attr.next
        }

        // replaces old bytecodes with new ones
        code = newCode
    }

    companion object {

        /**
         * Pseudo access flag used to denote constructors.
         */
        val ACC_CONSTRUCTOR = 262144

        /**
         * Frame has exactly the same locals as the previous stack map frame and
         * number of stack items is zero.
         */
        val SAME_FRAME = 0 // to 63 (0-3f)

        /**
         * Frame has exactly the same locals as the previous stack map frame and
         * number of stack items is 1
         */
        val SAME_LOCALS_1_STACK_ITEM_FRAME = 64 // to 127 (40-7f)

        /**
         * Reserved for future use
         */
        val RESERVED = 128

        /**
         * Frame has exactly the same locals as the previous stack map frame and
         * number of stack items is 1. Offset is bigger then 63;
         */
        val SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247 // f7

        /**
         * Frame where current locals are the same as the locals in the previous
         * frame, except that the k last locals are absent. The value of k is given
         * by the formula 251-frame_type.
         */
        val CHOP_FRAME = 248 // to 250 (f8-fA)

        /**
         * Frame has exactly the same locals as the previous stack map frame and
         * number of stack items is zero. Offset is bigger then 63;
         */
        val SAME_FRAME_EXTENDED = 251 // fb

        /**
         * Frame where current locals are the same as the locals in the previous
         * frame, except that k additional locals are defined. The value of k is
         * given by the formula frame_type-251.
         */
        val APPEND_FRAME = 252 // to 254 // fc-fe

        /**
         * Full frame
         */
        val FULL_FRAME = 255 // ff

        /**
         * Indicates that the stack map frames must be recomputed from scratch. In
         * this case the maximum stack size and number of local variables is also
         * recomputed from scratch.
         *
         * @see .compute
         */
        private val FRAMES = 0

        /**
         * Indicates that the maximum stack size and number of local variables must
         * be automatically computed.
         *
         * @see .compute
         */
        private val MAXS = 1

        /**
         * Indicates that nothing must be automatically computed.
         *
         * @see .compute
         */
        private val NOTHING = 2

        /**
         * Reads an unsigned short value in the given byte array.
         *
         * @param b
         * a byte array.
         * @param index
         * the start index of the value to be read.
         * @return the read value.
         */
        fun readUnsignedShort(b: ByteArray, index: Int): Int {
            return b[index].toInt() and 0xFF shl 8 or (b[index + 1].toInt() and 0xFF)
        }

        /**
         * Reads a signed short value in the given byte array.
         *
         * @param b
         * a byte array.
         * @param index
         * the start index of the value to be read.
         * @return the read value.
         */
        fun readShort(b: ByteArray, index: Int): Short {
            return (b[index].toInt() and 0xFF shl 8 or (b[index + 1].toInt() and 0xFF)).toShort()
        }

        /**
         * Reads a signed int value in the given byte array.
         *
         * @param b
         * a byte array.
         * @param index
         * the start index of the value to be read.
         * @return the read value.
         */
        fun readInt(b: ByteArray, index: Int): Int {
            return (b[index].toInt() and 0xFF shl 24 or (b[index + 1].toInt() and 0xFF shl 16)
                    or (b[index + 2].toInt() and 0xFF shl 8) or (b[index + 3].toInt() and 0xFF))
        }

        /**
         * Writes a short value in the given byte array.
         *
         * @param b
         * a byte array.
         * @param index
         * where the first byte of the short value must be written.
         * @param s
         * the value to be written in the given byte array.
         */
        fun writeShort(b: ByteArray, index: Int, s: Int) {
            b[index] = s.ushr(8).toByte()
            b[index + 1] = s.toByte()
        }

        /**
         * Computes the future value of a bytecode offset.
         *
         *
         * Note: it is possible to have several entries for the same instruction in
         * the <tt>indexes</tt> and <tt>sizes</tt>: two entries (index=a,size=b) and
         * (index=a,size=b') are equivalent to a single entry (index=a,size=b+b').
         *
         * @param indexes
         * current positions of the instructions to be resized. Each
         * instruction must be designated by the index of its *last*
         * byte, plus one (or, in other words, by the index of the
         * *first* byte of the *next* instruction).
         * @param sizes
         * the number of bytes to be *added* to the above
         * instructions. More precisely, for each i < <tt>len</tt>,
         * <tt>sizes</tt>[i] bytes will be added at the end of the
         * instruction designated by <tt>indexes</tt>[i] or, if
         * <tt>sizes</tt>[i] is negative, the *last* |
         * <tt>sizes[i]</tt>| bytes of the instruction will be removed
         * (the instruction size *must not* become negative or
         * null).
         * @param begin
         * index of the first byte of the source instruction.
         * @param end
         * index of the first byte of the target instruction.
         * @return the future value of the given bytecode offset.
         */
        fun getNewOffset(
            indexes: IntArray, sizes: IntArray,
            begin: Int, end: Int
        ): Int {
            var offset = end - begin
            for (i in indexes.indices) {
                if (begin < indexes[i] && indexes[i] <= end) {
                    // forward jump
                    offset += sizes[i]
                } else if (end < indexes[i] && indexes[i] <= begin) {
                    // backward jump
                    offset -= sizes[i]
                }
            }
            return offset
        }

        /**
         * Updates the offset of the given label.
         *
         * @param indexes
         * current positions of the instructions to be resized. Each
         * instruction must be designated by the index of its *last*
         * byte, plus one (or, in other words, by the index of the
         * *first* byte of the *next* instruction).
         * @param sizes
         * the number of bytes to be *added* to the above
         * instructions. More precisely, for each i < <tt>len</tt>,
         * <tt>sizes</tt>[i] bytes will be added at the end of the
         * instruction designated by <tt>indexes</tt>[i] or, if
         * <tt>sizes</tt>[i] is negative, the *last* |
         * <tt>sizes[i]</tt>| bytes of the instruction will be removed
         * (the instruction size *must not* become negative or
         * null).
         * @param label
         * the label whose offset must be updated.
         */
        fun getNewOffset(
            indexes: IntArray, sizes: IntArray,
            label: Label
        ) {
            if (label.status and Label.RESIZED == 0) {
                label.position =
                        getNewOffset(indexes, sizes, 0, label.position)
                label.status = label.status or Label.RESIZED
            }
        }
    }
}
