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


import org.objectweb.kn.asm.ClassVisitor
import org.objectweb.kn.asm.Handle
import org.objectweb.kn.asm.Label
import org.objectweb.kn.asm.MethodVisitor
import org.objectweb.kn.asm.Opcodes
import org.objectweb.kn.asm.Type

/**
 * A [org.objectweb.asm.MethodVisitor] with convenient methods to generate
 * code. For example, using this adapter, the class below
 *
 * <pre>
 * public class Example {
 * public static void main(String[] args) {
 * System.out.println(&quot;Hello world!&quot;);
 * }
 * }
</pre> *
 *
 * can be generated as follows:
 *
 * <pre>
 * ClassWriter cw = new ClassWriter(true);
 * cw.visit(V1_1, ACC_PUBLIC, &quot;Example&quot;, null, &quot;java/lang/Object&quot;, null);
 *
 * Method m = Method.getMethod(&quot;void &lt;init&gt; ()&quot;);
 * GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
 * mg.loadThis();
 * mg.invokeConstructor(Type.getType(Object.class), m);
 * mg.returnValue();
 * mg.endMethod();
 *
 * m = Method.getMethod(&quot;void main (String[])&quot;);
 * mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
 * mg.getStatic(Type.getType(System.class), &quot;out&quot;, Type.getType(PrintStream.class));
 * mg.push(&quot;Hello world!&quot;);
 * mg.invokeVirtual(Type.getType(PrintStream.class),
 * Method.getMethod(&quot;void println (String)&quot;));
 * mg.returnValue();
 * mg.endMethod();
 *
 * cw.visitEnd();
</pre> *
 *
 * @author Juozas Baliuka
 * @author Chris Nokleberg
 * @author Eric Bruneton
 * @author Prashant Deva
 */
open class GeneratorAdapter
/**
 * Creates a new [GeneratorAdapter].
 *
 * @param api
 * the ASM API version implemented by this visitor. Must be one
 * of [Opcodes.ASM4] or [Opcodes.ASM5].
 * @param mv
 * the method visitor to which this adapter delegates calls.
 * @param access
 * the method's access flags (see [Opcodes]).
 * @param name
 * the method's name.
 * @param desc
 * the method's descriptor (see [Type]).
 */
protected constructor(api: Int, mv: MethodVisitor?,
                      /**
                       * Access flags of the method visited by this adapter.
                       */
                      private val access: Int, name: String?, desc: String) : LocalVariablesSorter(api, access, desc, mv!!) {

    /**
     * Return type of the method visited by this adapter.
     */
    private val returnType: Type

    /**
     * Argument types of the method visited by this adapter.
     */
    private val argumentTypes: Array<Type>

    /**
     * Types of the local variables of the method visited by this adapter.
     */
    private val localTypes = ArrayList<Type?>()

    /**
     * Creates a new [GeneratorAdapter]. *Subclasses must not use this
     * constructor*. Instead, they must use the
     * [.GeneratorAdapter]
     * version.
     *
     * @param mv
     * the method visitor to which this adapter delegates calls.
     * @param access
     * the method's access flags (see [Opcodes]).
     * @param name
     * the method's name.
     * @param desc
     * the method's descriptor (see [Type]).
     */
    constructor(mv: MethodVisitor?, access: Int,
                name: String?, desc: String) : this(Opcodes.ASM5, mv, access, name, desc) {
    }

    init {
        this.returnType = Type.getReturnType(desc)
        this.argumentTypes = Type.getArgumentTypes(desc)
    }

    /**
     * Creates a new [GeneratorAdapter]. *Subclasses must not use this
     * constructor*. Instead, they must use the
     * [.GeneratorAdapter]
     * version.
     *
     * @param access
     * access flags of the adapted method.
     * @param method
     * the adapted method.
     * @param mv
     * the method visitor to which this adapter delegates calls.
     */
    constructor(access: Int, method: Method,
                mv: MethodVisitor?) : this(mv, access, null, method.descriptor) {
    }

    /**
     * Creates a new [GeneratorAdapter]. *Subclasses must not use this
     * constructor*. Instead, they must use the
     * [.GeneratorAdapter]
     * version.
     *
     * @param access
     * access flags of the adapted method.
     * @param method
     * the adapted method.
     * @param signature
     * the signature of the adapted method (may be <tt>null</tt>).
     * @param exceptions
     * the exceptions thrown by the adapted method (may be
     * <tt>null</tt>).
     * @param cv
     * the class visitor to which this adapter delegates calls.
     */
    constructor(access: Int, method: Method,
                signature: String, exceptions: Array<Type>,
                cv: ClassVisitor) : this(access, method, cv
            .visitMethod(access, method.name, method.descriptor,
                    signature, getInternalNames(exceptions))) {
    }

    // ------------------------------------------------------------------------
    // Instructions to push constants on the stack
    // ------------------------------------------------------------------------

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */
    fun push(value: Boolean) {
        push(if (value) 1 else 0)
    }

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */
    fun push(value: Int) {
        if (value >= -1 && value <= 5) {
            mv?.visitInsn(Opcodes.ICONST_0 + value)
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv?.visitIntInsn(Opcodes.BIPUSH, value)
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv?.visitIntInsn(Opcodes.SIPUSH, value)
        } else {
            mv?.visitLdcInsn(value)
        }
    }

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */
    fun push(value: Long) {
        if (value == 0L || value == 1L) {
            mv!!.visitInsn(Opcodes.LCONST_0 + value.toInt())
        } else {
            mv!!.visitLdcInsn(value)
        }
    }

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack. May be <tt>null</tt>.
     */
    fun push(value: String?) {
        if (value == null) {
            mv!!.visitInsn(Opcodes.ACONST_NULL)
        } else {
            mv!!.visitLdcInsn(value)
        }
    }

    /**
     * Generates the instruction to push the given value on the stack.
     *
     * @param value
     * the value to be pushed on the stack.
     */
    fun push(value: Type?) {
        if (value == null) {
            mv!!.visitInsn(Opcodes.ACONST_NULL)
        } else {
            when (value.sort) {
                Type.BOOLEAN -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean",
                        "TYPE", CLDESC)
                Type.CHAR -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Character",
                        "TYPE", CLDESC)
                Type.BYTE -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE",
                        CLDESC)
                Type.SHORT -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Short", "TYPE",
                        CLDESC)
                Type.INT -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Integer",
                        "TYPE", CLDESC)
                Type.FLOAT -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Float", "TYPE",
                        CLDESC)
                Type.LONG -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "TYPE",
                        CLDESC)
                Type.DOUBLE -> mv!!.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Double",
                        "TYPE", CLDESC)
                else -> mv!!.visitLdcInsn(value)
            }
        }
    }

    /**
     * Generates the instruction to push a handle on the stack.
     *
     * @param handle
     * the handle to be pushed on the stack.
     */
    fun push(handle: Handle) {
        mv!!.visitLdcInsn(handle)
    }

    // ------------------------------------------------------------------------
    // Instructions to load and store method arguments
    // ------------------------------------------------------------------------

    /**
     * Returns the index of the given method argument in the frame's local
     * variables array.
     *
     * @param arg
     * the index of a method argument.
     * @return the index of the given method argument in the frame's local
     * variables array.
     */
    private fun getArgIndex(arg: Int): Int {
        var index = if (access and Opcodes.ACC_STATIC == 0) 1 else 0
        for (i in 0 until arg) {
            index += argumentTypes[i].size
        }
        return index
    }

    /**
     * Generates the instruction to push a local variable on the stack.
     *
     * @param type
     * the type of the local variable to be loaded.
     * @param index
     * an index in the frame's local variables array.
     */
    private fun loadInsn(type: Type, index: Int) {
        mv!!.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index)
    }

    /**
     * Generates the instruction to store the top stack value in a local
     * variable.
     *
     * @param type
     * the type of the local variable to be stored.
     * @param index
     * an index in the frame's local variables array.
     */
    private fun storeInsn(type: Type, index: Int) {
        mv!!.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index)
    }

    /**
     * Generates the instruction to load 'this' on the stack.
     */
    fun loadThis() {
        if (access and Opcodes.ACC_STATIC != 0) {
            throw IllegalStateException(
                    "no 'this' pointer within static method")
        }
        mv!!.visitVarInsn(Opcodes.ALOAD, 0)
    }

    /**
     * Generates the instruction to load the given method argument on the stack.
     *
     * @param arg
     * the index of a method argument.
     */
    fun loadArg(arg: Int) {
        loadInsn(argumentTypes[arg], getArgIndex(arg))
    }

    /**
     * Generates the instructions to load the given method arguments on the
     * stack.
     *
     * @param arg
     * the index of the first method argument to be loaded.
     * @param count
     * the number of method arguments to be loaded.
     */
    fun loadArgs(arg: Int = 0, count: Int = argumentTypes.size) {
        var index = getArgIndex(arg)
        for (i in 0 until count) {
            val t = argumentTypes[arg + i]
            loadInsn(t, index)
            index += t.size
        }
    }

    /**
     * Generates the instructions to load all the method arguments on the stack,
     * as a single object array.
     */
    fun loadArgArray() {
        push(argumentTypes.size)
        newArray(OBJECT_TYPE)
        for (i in argumentTypes.indices) {
            dup()
            push(i)
            loadArg(i)
            box(argumentTypes[i])
            arrayStore(OBJECT_TYPE)
        }
    }

    /**
     * Generates the instruction to store the top stack value in the given
     * method argument.
     *
     * @param arg
     * the index of a method argument.
     */
    fun storeArg(arg: Int) {
        storeInsn(argumentTypes[arg], getArgIndex(arg))
    }

    // ------------------------------------------------------------------------
    // Instructions to load and store local variables
    // ------------------------------------------------------------------------

    /**
     * Returns the type of the given local variable.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     * @return the type of the given local variable.
     */
    fun getLocalType(local: Int): Type {
        return localTypes[local - firstLocal]!!
    }

    override fun setLocalType(local: Int, type: Type) {
        val index = local - firstLocal
        while (localTypes.size < index + 1) {
            localTypes.add(null)
        }
        localTypes[index] = type
    }

    /**
     * Generates the instruction to load the given local variable on the stack.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     */
    fun loadLocal(local: Int) {
        loadInsn(getLocalType(local), local)
    }

    /**
     * Generates the instruction to load the given local variable on the stack.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     * @param type
     * the type of this local variable.
     */
    fun loadLocal(local: Int, type: Type) {
        setLocalType(local, type)
        loadInsn(type, local)
    }

    /**
     * Generates the instruction to store the top stack value in the given local
     * variable.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     */
    fun storeLocal(local: Int) {
        storeInsn(getLocalType(local), local)
    }

    /**
     * Generates the instruction to store the top stack value in the given local
     * variable.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     * @param type
     * the type of this local variable.
     */
    fun storeLocal(local: Int, type: Type) {
        setLocalType(local, type)
        storeInsn(type, local)
    }

    /**
     * Generates the instruction to load an element from an array.
     *
     * @param type
     * the type of the array element to be loaded.
     */
    fun arrayLoad(type: Type) {
        mv!!.visitInsn(type.getOpcode(Opcodes.IALOAD))
    }

    /**
     * Generates the instruction to store an element in an array.
     *
     * @param type
     * the type of the array element to be stored.
     */
    fun arrayStore(type: Type) {
        mv!!.visitInsn(type.getOpcode(Opcodes.IASTORE))
    }

    // ------------------------------------------------------------------------
    // Instructions to manage the stack
    // ------------------------------------------------------------------------

    /**
     * Generates a POP instruction.
     */
    fun pop() {
        mv!!.visitInsn(Opcodes.POP)
    }

    /**
     * Generates a POP2 instruction.
     */
    fun pop2() {
        mv!!.visitInsn(Opcodes.POP2)
    }

    /**
     * Generates a DUP instruction.
     */
    fun dup() {
        mv!!.visitInsn(Opcodes.DUP)
    }

    /**
     * Generates a DUP2 instruction.
     */
    fun dup2() {
        mv!!.visitInsn(Opcodes.DUP2)
    }

    /**
     * Generates a DUP_X1 instruction.
     */
    fun dupX1() {
        mv!!.visitInsn(Opcodes.DUP_X1)
    }

    /**
     * Generates a DUP_X2 instruction.
     */
    fun dupX2() {
        mv!!.visitInsn(Opcodes.DUP_X2)
    }

    /**
     * Generates a DUP2_X1 instruction.
     */
    fun dup2X1() {
        mv!!.visitInsn(Opcodes.DUP2_X1)
    }

    /**
     * Generates a DUP2_X2 instruction.
     */
    fun dup2X2() {
        mv!!.visitInsn(Opcodes.DUP2_X2)
    }

    /**
     * Generates a SWAP instruction.
     */
    fun swap() {
        mv!!.visitInsn(Opcodes.SWAP)
    }

    /**
     * Generates the instructions to swap the top two stack values.
     *
     * @param prev
     * type of the top - 1 stack value.
     * @param type
     * type of the top stack value.
     */
    fun swap(prev: Type, type: Type) {
        if (type.size == 1) {
            if (prev.size == 1) {
                swap() // same as dupX1(), pop();
            } else {
                dupX2()
                pop()
            }
        } else {
            if (prev.size == 1) {
                dup2X1()
                pop2()
            } else {
                dup2X2()
                pop2()
            }
        }
    }

    // ------------------------------------------------------------------------
    // Instructions to do mathematical and logical operations
    // ------------------------------------------------------------------------

    /**
     * Generates the instruction to do the specified mathematical or logical
     * operation.
     *
     * @param op
     * a mathematical or logical operation. Must be one of ADD, SUB,
     * MUL, DIV, REM, NEG, SHL, SHR, USHR, AND, OR, XOR.
     * @param type
     * the type of the operand(s) for this operation.
     */
    fun math(op: Int, type: Type) {
        mv!!.visitInsn(type.getOpcode(op))
    }

    /**
     * Generates the instructions to compute the bitwise negation of the top
     * stack value.
     */
    operator fun not() {
        mv!!.visitInsn(Opcodes.ICONST_1)
        mv!!.visitInsn(Opcodes.IXOR)
    }

    /**
     * Generates the instruction to increment the given local variable.
     *
     * @param local
     * the local variable to be incremented.
     * @param amount
     * the amount by which the local variable must be incremented.
     */
    fun iinc(local: Int, amount: Int) {
        mv!!.visitIincInsn(local, amount)
    }

    /**
     * Generates the instructions to cast a numerical value from one type to
     * another.
     *
     * @param from
     * the type of the top stack value
     * @param to
     * the type into which this value must be cast.
     */
    fun cast(from: Type, to: Type) {
        if (from !== to) {
            if (from === Type.DOUBLE_TYPE) {
                if (to === Type.FLOAT_TYPE) {
                    mv!!.visitInsn(Opcodes.D2F)
                } else if (to === Type.LONG_TYPE) {
                    mv!!.visitInsn(Opcodes.D2L)
                } else {
                    mv!!.visitInsn(Opcodes.D2I)
                    cast(Type.INT_TYPE, to)
                }
            } else if (from === Type.FLOAT_TYPE) {
                if (to === Type.DOUBLE_TYPE) {
                    mv!!.visitInsn(Opcodes.F2D)
                } else if (to === Type.LONG_TYPE) {
                    mv!!.visitInsn(Opcodes.F2L)
                } else {
                    mv!!.visitInsn(Opcodes.F2I)
                    cast(Type.INT_TYPE, to)
                }
            } else if (from === Type.LONG_TYPE) {
                if (to === Type.DOUBLE_TYPE) {
                    mv!!.visitInsn(Opcodes.L2D)
                } else if (to === Type.FLOAT_TYPE) {
                    mv!!.visitInsn(Opcodes.L2F)
                } else {
                    mv!!.visitInsn(Opcodes.L2I)
                    cast(Type.INT_TYPE, to)
                }
            } else {
                if (to === Type.BYTE_TYPE) {
                    mv!!.visitInsn(Opcodes.I2B)
                } else if (to === Type.CHAR_TYPE) {
                    mv!!.visitInsn(Opcodes.I2C)
                } else if (to === Type.DOUBLE_TYPE) {
                    mv!!.visitInsn(Opcodes.I2D)
                } else if (to === Type.FLOAT_TYPE) {
                    mv!!.visitInsn(Opcodes.I2F)
                } else if (to === Type.LONG_TYPE) {
                    mv!!.visitInsn(Opcodes.I2L)
                } else if (to === Type.SHORT_TYPE) {
                    mv!!.visitInsn(Opcodes.I2S)
                }
            }
        }
    }

    /**
     * Generates the instructions to box the top stack value. This value is
     * replaced by its boxed equivalent on top of the stack.
     *
     * @param type
     * the type of the top stack value.
     */
    fun box(type: Type) {
        if (type.sort == Type.OBJECT || type.sort == Type.ARRAY) {
            return
        }
        if (type === Type.VOID_TYPE) {
            push(null as String?)
        } else {
            val boxed = getBoxedType(type)
            newInstance(boxed)
            if (type.size == 2) {
                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                dupX2()
                dupX2()
                pop()
            } else {
                // p -> po -> opo -> oop -> o
                dupX1()
                swap()
            }
            invokeConstructor(boxed, Method("<init>", Type.VOID_TYPE,
                    arrayOf(type)))
        }
    }

    /**
     * Generates the instructions to box the top stack value using Java 5's
     * valueOf() method. This value is replaced by its boxed equivalent on top
     * of the stack.
     *
     * @param type
     * the type of the top stack value.
     */
    fun valueOf(type: Type) {
        if (type.sort == Type.OBJECT || type.sort == Type.ARRAY) {
            return
        }
        if (type === Type.VOID_TYPE) {
            push(null as String?)
        } else {
            val boxed = getBoxedType(type)
            invokeStatic(boxed, Method("valueOf", boxed,
                    arrayOf(type)))
        }
    }

    /**
     * Generates the instructions to unbox the top stack value. This value is
     * replaced by its unboxed equivalent on top of the stack.
     *
     * @param type
     * the type of the top stack value.
     */
    fun unbox(type: Type) {
        var t = NUMBER_TYPE
        var sig: Method? = null
        when (type.sort) {
            Type.VOID -> return
            Type.CHAR -> {
                t = CHARACTER_TYPE
                sig = CHAR_VALUE
            }
            Type.BOOLEAN -> {
                t = BOOLEAN_TYPE
                sig = BOOLEAN_VALUE
            }
            Type.DOUBLE -> sig = DOUBLE_VALUE
            Type.FLOAT -> sig = FLOAT_VALUE
            Type.LONG -> sig = LONG_VALUE
            Type.INT, Type.SHORT, Type.BYTE -> sig = INT_VALUE
        }
        if (sig == null) {
            checkCast(type)
        } else {
            checkCast(t)
            invokeVirtual(t, sig)
        }
    }

    // ------------------------------------------------------------------------
    // Instructions to jump to other instructions
    // ------------------------------------------------------------------------

    /**
     * Creates a new [Label].
     *
     * @return a new [Label].
     */
    fun newLabel(): Label {
        return Label()
    }

    /**
     * Marks the current code position with the given label.
     *
     * @param label
     * a label.
     */
    fun mark(label: Label) {
        mv!!.visitLabel(label)
    }

    /**
     * Marks the current code position with a new label.
     *
     * @return the label that was created to mark the current code position.
     */
    fun mark(): Label {
        val label = Label()
        mv!!.visitLabel(label)
        return label
    }

    /**
     * Generates the instructions to jump to a label based on the comparison of
     * the top two stack values.
     *
     * @param type
     * the type of the top two stack values.
     * @param mode
     * how these values must be compared. One of EQ, NE, LT, GE, GT,
     * LE.
     * @param label
     * where to jump if the comparison result is <tt>true</tt>.
     */
    fun ifCmp(type: Type, mode: Int, label: Label) {
        when (type.sort) {
            Type.LONG -> mv!!.visitInsn(Opcodes.LCMP)
            Type.DOUBLE -> mv!!.visitInsn(if (mode == GE || mode == GT)
                Opcodes.DCMPL
            else
                Opcodes.DCMPG)
            Type.FLOAT -> mv!!.visitInsn(if (mode == GE || mode == GT)
                Opcodes.FCMPL
            else
                Opcodes.FCMPG)
            Type.ARRAY, Type.OBJECT -> {
                when (mode) {
                    EQ -> {
                        mv!!.visitJumpInsn(Opcodes.IF_ACMPEQ, label)
                        return
                    }
                    NE -> {
                        mv!!.visitJumpInsn(Opcodes.IF_ACMPNE, label)
                        return
                    }
                }
                throw IllegalArgumentException("Bad comparison for type $type")
            }
            else -> {
                var intOp = -1
                when (mode) {
                    EQ -> intOp = Opcodes.IF_ICMPEQ
                    NE -> intOp = Opcodes.IF_ICMPNE
                    GE -> intOp = Opcodes.IF_ICMPGE
                    LT -> intOp = Opcodes.IF_ICMPLT
                    LE -> intOp = Opcodes.IF_ICMPLE
                    GT -> intOp = Opcodes.IF_ICMPGT
                }
                mv!!.visitJumpInsn(intOp, label)
                return
            }
        }
        mv!!.visitJumpInsn(mode, label)
    }

    /**
     * Generates the instructions to jump to a label based on the comparison of
     * the top two integer stack values.
     *
     * @param mode
     * how these values must be compared. One of EQ, NE, LT, GE, GT,
     * LE.
     * @param label
     * where to jump if the comparison result is <tt>true</tt>.
     */
    fun ifICmp(mode: Int, label: Label) {
        ifCmp(Type.INT_TYPE, mode, label)
    }

    /**
     * Generates the instructions to jump to a label based on the comparison of
     * the top integer stack value with zero.
     *
     * @param mode
     * how these values must be compared. One of EQ, NE, LT, GE, GT,
     * LE.
     * @param label
     * where to jump if the comparison result is <tt>true</tt>.
     */
    fun ifZCmp(mode: Int, label: Label) {
        mv!!.visitJumpInsn(mode, label)
    }

    /**
     * Generates the instruction to jump to the given label if the top stack
     * value is null.
     *
     * @param label
     * where to jump if the condition is <tt>true</tt>.
     */
    fun ifNull(label: Label) {
        mv!!.visitJumpInsn(Opcodes.IFNULL, label)
    }

    /**
     * Generates the instruction to jump to the given label if the top stack
     * value is not null.
     *
     * @param label
     * where to jump if the condition is <tt>true</tt>.
     */
    fun ifNonNull(label: Label) {
        mv!!.visitJumpInsn(Opcodes.IFNONNULL, label)
    }

    /**
     * Generates the instruction to jump to the given label.
     *
     * @param label
     * where to jump if the condition is <tt>true</tt>.
     */
    fun goTo(label: Label) {
        mv!!.visitJumpInsn(Opcodes.GOTO, label)
    }

    /**
     * Generates a RET instruction.
     *
     * @param local
     * a local variable identifier, as returned by
     * [newLocal()][LocalVariablesSorter.newLocal].
     */
    fun ret(local: Int) {
        mv!!.visitVarInsn(Opcodes.RET, local)
    }

    /**
     * Generates the instructions for a switch statement.
     *
     * @param keys
     * the switch case keys.
     * @param generator
     * a generator to generate the code for the switch cases.
     */
    /**
     * Generates the instructions for a switch statement.
     *
     * @param keys
     * the switch case keys.
     * @param generator
     * a generator to generate the code for the switch cases.
     * @param useTable
     * <tt>true</tt> to use a TABLESWITCH instruction, or
     * <tt>false</tt> to use a LOOKUPSWITCH instruction.
     */
    /**
     * Generates the instruction to return the top stack value to the caller.
     */
    fun returnValue() {
        mv!!.visitInsn(returnType.getOpcode(Opcodes.IRETURN))
    }

    // ------------------------------------------------------------------------
    // Instructions to load and store fields
    // ------------------------------------------------------------------------

    /**
     * Generates a get field or set field instruction.
     *
     * @param opcode
     * the instruction's opcode.
     * @param ownerType
     * the class in which the field is defined.
     * @param name
     * the name of the field.
     * @param fieldType
     * the type of the field.
     */
    private fun fieldInsn(opcode: Int, ownerType: Type,
                          name: String, fieldType: Type) {
        mv!!.visitFieldInsn(opcode, ownerType.internalName, name,
                fieldType.descriptor)
    }

    /**
     * Generates the instruction to push the value of a static field on the
     * stack.
     *
     * @param owner
     * the class in which the field is defined.
     * @param name
     * the name of the field.
     * @param type
     * the type of the field.
     */
    fun getStatic(owner: Type, name: String, type: Type) {
        fieldInsn(Opcodes.GETSTATIC, owner, name, type)
    }

    /**
     * Generates the instruction to store the top stack value in a static field.
     *
     * @param owner
     * the class in which the field is defined.
     * @param name
     * the name of the field.
     * @param type
     * the type of the field.
     */
    fun putStatic(owner: Type, name: String, type: Type) {
        fieldInsn(Opcodes.PUTSTATIC, owner, name, type)
    }

    /**
     * Generates the instruction to push the value of a non static field on the
     * stack.
     *
     * @param owner
     * the class in which the field is defined.
     * @param name
     * the name of the field.
     * @param type
     * the type of the field.
     */
    fun getField(owner: Type, name: String, type: Type) {
        fieldInsn(Opcodes.GETFIELD, owner, name, type)
    }

    /**
     * Generates the instruction to store the top stack value in a non static
     * field.
     *
     * @param owner
     * the class in which the field is defined.
     * @param name
     * the name of the field.
     * @param type
     * the type of the field.
     */
    fun putField(owner: Type, name: String, type: Type) {
        fieldInsn(Opcodes.PUTFIELD, owner, name, type)
    }

    // ------------------------------------------------------------------------
    // Instructions to invoke methods
    // ------------------------------------------------------------------------

    /**
     * Generates an invoke method instruction.
     *
     * @param opcode
     * the instruction's opcode.
     * @param type
     * the class in which the method is defined.
     * @param method
     * the method to be invoked.
     */
    private fun invokeInsn(opcode: Int, type: Type,
                           method: Method) {
        val owner = if (type.sort == Type.ARRAY)
            type.descriptor
        else
            type.internalName
        mv!!.visitMethodInsn(opcode, owner, method.name,
                method.descriptor)
    }

    /**
     * Generates the instruction to invoke a normal method.
     *
     * @param owner
     * the class in which the method is defined.
     * @param method
     * the method to be invoked.
     */
    fun invokeVirtual(owner: Type, method: Method) {
        invokeInsn(Opcodes.INVOKEVIRTUAL, owner, method)
    }

    /**
     * Generates the instruction to invoke a constructor.
     *
     * @param type
     * the class in which the constructor is defined.
     * @param method
     * the constructor to be invoked.
     */
    fun invokeConstructor(type: Type, method: Method) {
        invokeInsn(Opcodes.INVOKESPECIAL, type, method)
    }

    /**
     * Generates the instruction to invoke a static method.
     *
     * @param owner
     * the class in which the method is defined.
     * @param method
     * the method to be invoked.
     */
    fun invokeStatic(owner: Type, method: Method) {
        invokeInsn(Opcodes.INVOKESTATIC, owner, method)
    }

    /**
     * Generates the instruction to invoke an interface method.
     *
     * @param owner
     * the class in which the method is defined.
     * @param method
     * the method to be invoked.
     */
    fun invokeInterface(owner: Type, method: Method) {
        invokeInsn(Opcodes.INVOKEINTERFACE, owner, method)
    }

    /**
     * Generates an invokedynamic instruction.
     *
     * @param name
     * the method's name.
     * @param desc
     * the method's descriptor (see [Type]).
     * @param bsm
     * the bootstrap method.
     * @param bsmArgs
     * the bootstrap method constant arguments. Each argument must be
     * an [Integer], [Float], [Long],
     * [Double], [String], [Type] or [Handle]
     * value. This method is allowed to modify the content of the
     * array so a caller should expect that this array may change.
     */
    fun invokeDynamic(name: String, desc: String, bsm: Handle,
                      vararg bsmArgs: Any) {
        val bsmArgs1: Array<Any?> = arrayOf(bsmArgs)
        mv!!.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs1)
    }

    // ------------------------------------------------------------------------
    // Instructions to create objects and arrays
    // ------------------------------------------------------------------------

    /**
     * Generates a type dependent instruction.
     *
     * @param opcode
     * the instruction's opcode.
     * @param type
     * the instruction's operand.
     */
    private fun typeInsn(opcode: Int, type: Type) {
        mv!!.visitTypeInsn(opcode, type.internalName)
    }

    /**
     * Generates the instruction to create a new object.
     *
     * @param type
     * the class of the object to be created.
     */
    fun newInstance(type: Type) {
        typeInsn(Opcodes.NEW, type)
    }

    /**
     * Generates the instruction to create a new array.
     *
     * @param type
     * the type of the array elements.
     */
    fun newArray(type: Type) {
        val typ: Int
        when (type.sort) {
            Type.BOOLEAN -> typ = Opcodes.T_BOOLEAN
            Type.CHAR -> typ = Opcodes.T_CHAR
            Type.BYTE -> typ = Opcodes.T_BYTE
            Type.SHORT -> typ = Opcodes.T_SHORT
            Type.INT -> typ = Opcodes.T_INT
            Type.FLOAT -> typ = Opcodes.T_FLOAT
            Type.LONG -> typ = Opcodes.T_LONG
            Type.DOUBLE -> typ = Opcodes.T_DOUBLE
            else -> {
                typeInsn(Opcodes.ANEWARRAY, type)
                return
            }
        }
        mv!!.visitIntInsn(Opcodes.NEWARRAY, typ)
    }

    // ------------------------------------------------------------------------
    // Miscelaneous instructions
    // ------------------------------------------------------------------------

    /**
     * Generates the instruction to compute the length of an array.
     */
    fun arrayLength() {
        mv!!.visitInsn(Opcodes.ARRAYLENGTH)
    }

    /**
     * Generates the instruction to throw an exception.
     */
    fun throwException() {
        mv!!.visitInsn(Opcodes.ATHROW)
    }

    /**
     * Generates the instructions to create and throw an exception. The
     * exception class must have a constructor with a single String argument.
     *
     * @param type
     * the class of the exception to be thrown.
     * @param msg
     * the detailed message of the exception.
     */
    fun throwException(type: Type, msg: String) {
        newInstance(type)
        dup()
        push(msg)
        invokeConstructor(type, Method.getMethod("void <init> (String)"))
        throwException()
    }

    /**
     * Generates the instruction to check that the top stack value is of the
     * given type.
     *
     * @param type
     * a class or interface type.
     */
    fun checkCast(type: Type) {
        if (type != OBJECT_TYPE) {
            typeInsn(Opcodes.CHECKCAST, type)
        }
    }

    /**
     * Generates the instruction to test if the top stack value is of the given
     * type.
     *
     * @param type
     * a class or interface type.
     */
    fun instanceOf(type: Type) {
        typeInsn(Opcodes.INSTANCEOF, type)
    }

    /**
     * Generates the instruction to get the monitor of the top stack value.
     */
    fun monitorEnter() {
        mv!!.visitInsn(Opcodes.MONITORENTER)
    }

    /**
     * Generates the instruction to release the monitor of the top stack value.
     */
    fun monitorExit() {
        mv!!.visitInsn(Opcodes.MONITOREXIT)
    }

    // ------------------------------------------------------------------------
    // Non instructions
    // ------------------------------------------------------------------------

    /**
     * Marks the end of the visited method.
     */
    fun endMethod() {
        if (access and Opcodes.ACC_ABSTRACT == 0) {
            mv!!.visitMaxs(0, 0)
        }
        mv!!.visitEnd()
    }

    /**
     * Marks the start of an exception handler.
     *
     * @param start
     * beginning of the exception handler's scope (inclusive).
     * @param end
     * end of the exception handler's scope (exclusive).
     * @param exception
     * internal name of the type of exceptions handled by the
     * handler.
     */
    fun catchException(start: Label, end: Label,
                       exception: Type?) {
        if (exception == null) {
            mv!!.visitTryCatchBlock(start, end, mark(), null)
        } else {
            mv!!.visitTryCatchBlock(start, end, mark(),
                    exception.internalName)
        }
    }

    companion object {

        private val CLDESC = "Ljava/lang/Class;"

        private val BYTE_TYPE = Type.getObjectType("java/lang/Byte")

        private val BOOLEAN_TYPE = Type
                .getObjectType("java/lang/Boolean")

        private val SHORT_TYPE = Type
                .getObjectType("java/lang/Short")

        private val CHARACTER_TYPE = Type
                .getObjectType("java/lang/Character")

        private val INTEGER_TYPE = Type
                .getObjectType("java/lang/Integer")

        private val FLOAT_TYPE = Type
                .getObjectType("java/lang/Float")

        private val LONG_TYPE = Type.getObjectType("java/lang/Long")

        private val DOUBLE_TYPE = Type
                .getObjectType("java/lang/Double")

        private val NUMBER_TYPE = Type
                .getObjectType("java/lang/Number")

        private val OBJECT_TYPE = Type
                .getObjectType("java/lang/Object")

        private val BOOLEAN_VALUE = Method
                .getMethod("boolean booleanValue()")

        private val CHAR_VALUE = Method
                .getMethod("char charValue()")

        private val INT_VALUE = Method.getMethod("int intValue()")

        private val FLOAT_VALUE = Method
                .getMethod("float floatValue()")

        private val LONG_VALUE = Method
                .getMethod("long longValue()")

        private val DOUBLE_VALUE = Method
                .getMethod("double doubleValue()")

        /**
         * Constant for the [math][.math] method.
         */
        val ADD = Opcodes.IADD

        /**
         * Constant for the [math][.math] method.
         */
        val SUB = Opcodes.ISUB

        /**
         * Constant for the [math][.math] method.
         */
        val MUL = Opcodes.IMUL

        /**
         * Constant for the [math][.math] method.
         */
        val DIV = Opcodes.IDIV

        /**
         * Constant for the [math][.math] method.
         */
        val REM = Opcodes.IREM

        /**
         * Constant for the [math][.math] method.
         */
        val NEG = Opcodes.INEG

        /**
         * Constant for the [math][.math] method.
         */
        val SHL = Opcodes.ISHL

        /**
         * Constant for the [math][.math] method.
         */
        val SHR = Opcodes.ISHR

        /**
         * Constant for the [math][.math] method.
         */
        val USHR = Opcodes.IUSHR

        /**
         * Constant for the [math][.math] method.
         */
        val AND = Opcodes.IAND

        /**
         * Constant for the [math][.math] method.
         */
        val OR = Opcodes.IOR

        /**
         * Constant for the [math][.math] method.
         */
        val XOR = Opcodes.IXOR

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val EQ = Opcodes.IFEQ

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val NE = Opcodes.IFNE

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val LT = Opcodes.IFLT

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val GE = Opcodes.IFGE

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val GT = Opcodes.IFGT

        /**
         * Constant for the [ifCmp][.ifCmp] method.
         */
        val LE = Opcodes.IFLE

        /**
         * Returns the internal names of the given types.
         *
         * @param types
         * a set of types.
         * @return the internal names of the given types.
         */
        private fun getInternalNames(types: Array<Type>?): Array<String?>? {
            if (types == null) {
                return null
            }
            val names = arrayOfNulls<String>(types.size)
            for (i in names.indices) {
                names[i] = types[i].internalName
            }
            return names
        }

        // ------------------------------------------------------------------------
        // Instructions to do boxing and unboxing operations
        // ------------------------------------------------------------------------

        private fun getBoxedType(type: Type): Type {
            when (type.sort) {
                Type.BYTE -> return BYTE_TYPE
                Type.BOOLEAN -> return BOOLEAN_TYPE
                Type.SHORT -> return SHORT_TYPE
                Type.CHAR -> return CHARACTER_TYPE
                Type.INT -> return INTEGER_TYPE
                Type.FLOAT -> return FLOAT_TYPE
                Type.LONG -> return LONG_TYPE
                Type.DOUBLE -> return DOUBLE_TYPE
            }
            return type
        }
    }
}
/**
 * Generates the instructions to load all the method arguments on the stack.
 */
