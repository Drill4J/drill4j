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
 * Information about the input and output stack map frames of a basic block.
 *
 * @author Eric Bruneton
 */
internal class Frame {

    /**
     * The label (i.e. basic block) to which these input and output stack map
     * frames correspond.
     */
    var owner: Label? = null

    /**
     * The input stack map frame locals.
     */
    var inputLocals: IntArray? = null

    /**
     * The input stack map frame stack.
     */
    var inputStack: IntArray? = null

    /**
     * The output stack map frame locals.
     */
    private var outputLocals: IntArray? = null

    /**
     * The output stack map frame stack.
     */
    private var outputStack: IntArray? = null

    /**
     * Relative size of the output stack. The exact semantics of this field
     * depends on the algorithm that is used.
     *
     * When only the maximum stack size is computed, this field is the size of
     * the output stack relatively to the top of the input stack.
     *
     * When the stack map frames are completely computed, this field is the
     * actual number of types in [.outputStack].
     */
    private var outputStackTop: Int = 0

    /**
     * Number of types that are initialized in the basic block.
     *
     * @see .initializations
     */
    private var initializationCount: Int = 0

    /**
     * The types that are initialized in the basic block. A constructor
     * invocation on an UNINITIALIZED or UNINITIALIZED_THIS type must replace
     * *every occurence* of this type in the local variables and in the
     * operand stack. This cannot be done during the first phase of the
     * algorithm since, during this phase, the local variables and the operand
     * stack are not completely computed. It is therefore necessary to store the
     * types on which constructors are invoked in the basic block, in order to
     * do this replacement during the second phase of the algorithm, where the
     * frames are fully computed. Note that this array can contain types that
     * are relative to input locals or to the input stack (see below for the
     * description of the algorithm).
     */
    private var initializations: IntArray? = null

    /**
     * Returns the output frame local variable type at the given index.
     *
     * @param local
     * the index of the local that must be returned.
     * @return the output frame local variable type at the given index.
     */
    private operator fun get(local: Int): Int {
        if (outputLocals == null || local >= outputLocals!!.size) {
            // this local has never been assigned in this basic block,
            // so it is still equal to its value in the input frame
            return LOCAL or local
        } else {
            var type = outputLocals!![local]
            if (type == 0) {
                // this local has never been assigned in this basic block,
                // so it is still equal to its value in the input frame
                outputLocals!![local] = LOCAL or local
                type = outputLocals!![local]
            }
            return type
        }
    }

    /**
     * Sets the output frame local variable type at the given index.
     *
     * @param local
     * the index of the local that must be set.
     * @param type
     * the value of the local that must be set.
     */
    private operator fun set(local: Int, type: Int) {
        // creates and/or resizes the output local variables array if necessary
        if (outputLocals == null) {
            outputLocals = IntArray(10)
        }
        val n = outputLocals!!.size
        if (local >= n) {
            val t = IntArray(max(local + 1, 2 * n))
            outputLocals!!.copyInto(t,0,0,n)
            outputLocals = t
        }
        // sets the local variable
        outputLocals!![local] = type
    }

    /**
     * Pushes a new type onto the output frame stack.
     *
     * @param type
     * the type that must be pushed.
     */
    private fun push(type: Int) {
        // creates and/or resizes the output stack array if necessary
        if (outputStack == null) {
            outputStack = IntArray(10)
        }
        val n = outputStack!!.size
        if (outputStackTop >= n) {
            val t = IntArray(max(outputStackTop + 1, 2 * n))
            outputStack!!.copyInto(t,0,0,n)
//            System.arraycopy(outputStack!!, 0, t, 0, n)
            outputStack = t
        }
        // pushes the type on the output stack
        outputStack!![outputStackTop++] = type
        // updates the maximun height reached by the output stack, if needed
        val top = owner!!.inputStackTop + outputStackTop
        if (top > owner!!.outputStackMax) {
            owner!!.outputStackMax = top
        }
    }

    /**
     * Pushes a new type onto the output frame stack.
     *
     * @param cw
     * the ClassWriter to which this label belongs.
     * @param desc
     * the descriptor of the type to be pushed. Can also be a method
     * descriptor (in this case this method pushes its return type
     * onto the output frame stack).
     */
    private fun push(cw: ClassWriter, desc: String?) {
        val type = type(cw, desc!!)
        if (type != 0) {
            push(type)
            if (type == LONG || type == DOUBLE) {
                push(TOP)
            }
        }
    }

    /**
     * Pops a type from the output frame stack and returns its value.
     *
     * @return the type that has been popped from the output frame stack.
     */
    private fun pop(): Int {
        return if (outputStackTop > 0) {
            outputStack!![--outputStackTop]
        } else {
            // if the output frame stack is empty, pops from the input stack
            owner!!.inputStackTop = ( owner!!.inputStackTop - 1)
            STACK or -owner!!.inputStackTop
        }
    }

    /**
     * Pops the given number of types from the output frame stack.
     *
     * @param elements
     * the number of types that must be popped.
     */
    private fun pop(elements: Int) {
        if (outputStackTop >= elements) {
            outputStackTop -= elements
        } else {
            // if the number of elements to be popped is greater than the number
            // of elements in the output stack, clear it, and pops the remaining
            // elements from the input stack.
            owner!!.inputStackTop=(owner!!.inputStackTop - (elements - outputStackTop))
            outputStackTop = 0
        }
    }

    /**
     * Pops a type from the output frame stack.
     *
     * @param desc
     * the descriptor of the type to be popped. Can also be a method
     * descriptor (in this case this method pops the types
     * corresponding to the method arguments).
     */
    private fun pop(desc: String) {
        val c = desc[0]
        if (c == '(') {
            pop((Type.getArgumentsAndReturnSizes(desc) shr 2) - 1)
        } else if (c == 'J' || c == 'D') {
            pop(2)
        } else {
            pop(1)
        }
    }

    /**
     * Adds a new type to the list of types on which a constructor is invoked in
     * the basic block.
     *
     * @param var
     * a type on a which a constructor is invoked.
     */
    private fun init(`var`: Int) {
        // creates and/or resizes the initializations array if necessary
        if (initializations == null) {
            initializations = IntArray(2)
        }
        val n = initializations!!.size
        if (initializationCount >= n) {
            val t = IntArray(max(initializationCount + 1, 2 * n))
            initializations!!.copyInto(t,0,0,n)
//            System.arraycopy(initializations!!, 0, t, 0, n)
            initializations = t
        }
        // stores the type to be initialized
        initializations!![initializationCount++] = `var`
    }

    /**
     * Replaces the given type with the appropriate type if it is one of the
     * types on which a constructor is invoked in the basic block.
     *
     * @param cw
     * the ClassWriter to which this label belongs.
     * @param t
     * a type
     * @return t or, if t is one of the types on which a constructor is invoked
     * in the basic block, the type corresponding to this constructor.
     */
    private fun init(cw: ClassWriter, t: Int): Int {
        val s: Int
        if (t == UNINITIALIZED_THIS) {
            s = OBJECT or cw.addType(cw.thisName)
        } else if (t and (DIM or BASE_KIND) == UNINITIALIZED) {
            val type = cw.typeTable!![t and BASE_VALUE]?.strVal1
            s = OBJECT or cw.addType(type)
        } else {
            return t
        }
        for (j in 0 until initializationCount) {
            var u = initializations!![j]
            val dim = u and DIM
            val kind = u and KIND
            if (kind == LOCAL) {
                u = dim + inputLocals!![u and VALUE]
            } else if (kind == STACK) {
                u = dim + inputStack!![inputStack!!.size - (u and VALUE)]
            }
            if (t == u) {
                return s
            }
        }
        return t
    }

    /**
     * Initializes the input frame of the first basic block from the method
     * descriptor.
     *
     * @param cw
     * the ClassWriter to which this label belongs.
     * @param access
     * the access flags of the method to which this label belongs.
     * @param args
     * the formal parameter types of this method.
     * @param maxLocals
     * the maximum number of local variables of this method.
     */
    fun initInputFrame(cw: ClassWriter, access: Int,
                       args: Array<Type>, maxLocals: Int) {
        inputLocals = IntArray(maxLocals)
        inputStack = IntArray(0)
        var i = 0
        if (access and Opcodes.ACC_STATIC == 0) {
            if (access and MethodWriter.ACC_CONSTRUCTOR == 0) {
                inputLocals!![i++] = OBJECT or cw.addType(cw.thisName)
            } else {
                inputLocals!![i++] = UNINITIALIZED_THIS
            }
        }
        for (j in args.indices) {
            val t = type(cw, args[j].descriptor)
            inputLocals!![i++] = t
            if (t == LONG || t == DOUBLE) {
                inputLocals!![i++] = TOP
            }
        }
        while (i < maxLocals) {
            inputLocals!![i++] = TOP
        }
    }

    /**
     * Simulates the action of the given instruction on the output stack frame.
     *
     * @param opcode
     * the opcode of the instruction.
     * @param arg
     * the operand of the instruction, if any.
     * @param cw
     * the class writer to which this label belongs.
     * @param item
     * the operand of the instructions, if any.
     */
    fun execute(opcode: Int, arg: Int, cw: ClassWriter?,
                item: Item?) {
        val t1: Int
        val t2: Int
        val t3: Int
        val t4: Int
        when (opcode) {
            Opcodes.NOP, Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S, Opcodes.GOTO, Opcodes.RETURN -> {
            }
            Opcodes.ACONST_NULL -> push(NULL)
            Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.ILOAD -> push(
                INTEGER
            )
            Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.LLOAD -> {
                push(LONG)
                push(TOP)
            }
            Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.FLOAD -> push(
                FLOAT
            )
            Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.DLOAD -> {
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.LDC -> when (item?.type) {
                ClassWriter.INT -> push(INTEGER)
                ClassWriter.LONG -> {
                    push(LONG)
                    push(TOP)
                }
                ClassWriter.FLOAT -> push(FLOAT)
                ClassWriter.DOUBLE -> {
                    push(DOUBLE)
                    push(TOP)
                }
                ClassWriter.CLASS -> push(OBJECT or cw?.addType("java/lang/Class")!!)
                ClassWriter.STR -> push(OBJECT or cw?.addType("java/lang/String")!!)
                ClassWriter.MTYPE -> push(OBJECT or cw?.addType("java/lang/invoke/MethodType")!!)
                // case ClassWriter.HANDLE_BASE + [1..9]:
                else -> push(OBJECT or cw?.addType("java/lang/invoke/MethodHandle")!!)
            }
            Opcodes.ALOAD -> push(get(arg))
            Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> {
                pop(2)
                push(INTEGER)
            }
            Opcodes.LALOAD, Opcodes.D2L -> {
                pop(2)
                push(LONG)
                push(TOP)
            }
            Opcodes.FALOAD -> {
                pop(2)
                push(FLOAT)
            }
            Opcodes.DALOAD, Opcodes.L2D -> {
                pop(2)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.AALOAD -> {
                pop(1)
                t1 = pop()
                push(ELEMENT_OF + t1)
            }
            Opcodes.ISTORE, Opcodes.FSTORE, Opcodes.ASTORE -> {
                t1 = pop()
                set(arg, t1)
                if (arg > 0) {
                    t2 = get(arg - 1)
                    // if t2 is of kind STACK or LOCAL we cannot know its size!
                    if (t2 == LONG || t2 == DOUBLE) {
                        set(arg - 1, TOP)
                    } else if (t2 and KIND != BASE) {
                        set(arg - 1, t2 or TOP_IF_LONG_OR_DOUBLE)
                    }
                }
            }
            Opcodes.LSTORE, Opcodes.DSTORE -> {
                pop(1)
                t1 = pop()
                set(arg, t1)
                set(arg + 1, TOP)
                if (arg > 0) {
                    t2 = get(arg - 1)
                    // if t2 is of kind STACK or LOCAL we cannot know its size!
                    if (t2 == LONG || t2 == DOUBLE) {
                        set(arg - 1, TOP)
                    } else if (t2 and KIND != BASE) {
                        set(arg - 1, t2 or TOP_IF_LONG_OR_DOUBLE)
                    }
                }
            }
            Opcodes.IASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.FASTORE, Opcodes.AASTORE -> pop(3)
            Opcodes.LASTORE, Opcodes.DASTORE -> pop(4)
            Opcodes.POP, Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IRETURN, Opcodes.FRETURN, Opcodes.ARETURN, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH, Opcodes.ATHROW, Opcodes.MONITORENTER, Opcodes.MONITOREXIT, Opcodes.IFNULL, Opcodes.IFNONNULL -> pop(1)
            Opcodes.POP2, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.LRETURN, Opcodes.DRETURN -> pop(2)
            Opcodes.DUP -> {
                t1 = pop()
                push(t1)
                push(t1)
            }
            Opcodes.DUP_X1 -> {
                t1 = pop()
                t2 = pop()
                push(t1)
                push(t2)
                push(t1)
            }
            Opcodes.DUP_X2 -> {
                t1 = pop()
                t2 = pop()
                t3 = pop()
                push(t1)
                push(t3)
                push(t2)
                push(t1)
            }
            Opcodes.DUP2 -> {
                t1 = pop()
                t2 = pop()
                push(t2)
                push(t1)
                push(t2)
                push(t1)
            }
            Opcodes.DUP2_X1 -> {
                t1 = pop()
                t2 = pop()
                t3 = pop()
                push(t2)
                push(t1)
                push(t3)
                push(t2)
                push(t1)
            }
            Opcodes.DUP2_X2 -> {
                t1 = pop()
                t2 = pop()
                t3 = pop()
                t4 = pop()
                push(t2)
                push(t1)
                push(t4)
                push(t3)
                push(t2)
                push(t1)
            }
            Opcodes.SWAP -> {
                t1 = pop()
                t2 = pop()
                push(t1)
                push(t2)
            }
            Opcodes.IADD, Opcodes.ISUB, Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR, Opcodes.L2I, Opcodes.D2I, Opcodes.FCMPL, Opcodes.FCMPG -> {
                pop(2)
                push(INTEGER)
            }
            Opcodes.LADD, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR -> {
                pop(4)
                push(LONG)
                push(TOP)
            }
            Opcodes.FADD, Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.L2F, Opcodes.D2F -> {
                pop(2)
                push(FLOAT)
            }
            Opcodes.DADD, Opcodes.DSUB, Opcodes.DMUL, Opcodes.DDIV, Opcodes.DREM -> {
                pop(4)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR -> {
                pop(3)
                push(LONG)
                push(TOP)
            }
            Opcodes.IINC -> set(arg, INTEGER)
            Opcodes.I2L, Opcodes.F2L -> {
                pop(1)
                push(LONG)
                push(TOP)
            }
            Opcodes.I2F -> {
                pop(1)
                push(FLOAT)
            }
            Opcodes.I2D, Opcodes.F2D -> {
                pop(1)
                push(DOUBLE)
                push(TOP)
            }
            Opcodes.F2I, Opcodes.ARRAYLENGTH, Opcodes.INSTANCEOF -> {
                pop(1)
                push(INTEGER)
            }
            Opcodes.LCMP, Opcodes.DCMPL, Opcodes.DCMPG -> {
                pop(4)
                push(INTEGER)
            }
            Opcodes.JSR, Opcodes.RET -> throw RuntimeException(
                    "JSR/RET are not supported with computeFrames option")
            Opcodes.GETSTATIC -> push(cw!!, item!!.strVal3)
            Opcodes.PUTSTATIC -> pop(item!!.strVal3!!)
            Opcodes.GETFIELD -> {
                pop(1)
                push(cw!!, item!!.strVal3)
            }
            Opcodes.PUTFIELD -> {
                pop(item!!.strVal3!!)
                pop()
            }
            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                pop(item!!.strVal3!!)
                if (opcode != Opcodes.INVOKESTATIC) {
                    t1 = pop()
                    if (opcode == Opcodes.INVOKESPECIAL && item.strVal2!![0] == '<') {
                        init(t1)
                    }
                }
                push(cw!!, item.strVal3)
            }
            Opcodes.INVOKEDYNAMIC -> {
                pop(item!!.strVal2!!)
                push(cw!!, item!!.strVal2)
            }
            Opcodes.NEW -> push(UNINITIALIZED or cw!!.addUninitializedType(item!!.strVal1, arg))
            Opcodes.NEWARRAY -> {
                pop()
                when (arg) {
                    Opcodes.T_BOOLEAN -> push(ARRAY_OF or BOOLEAN)
                    Opcodes.T_CHAR -> push(ARRAY_OF or CHAR)
                    Opcodes.T_BYTE -> push(ARRAY_OF or BYTE)
                    Opcodes.T_SHORT -> push(ARRAY_OF or SHORT)
                    Opcodes.T_INT -> push(ARRAY_OF or INTEGER)
                    Opcodes.T_FLOAT -> push(ARRAY_OF or FLOAT)
                    Opcodes.T_DOUBLE -> push(ARRAY_OF or DOUBLE)
                    // case Opcodes.T_LONG:
                    else -> push(ARRAY_OF or LONG)
                }
            }
            Opcodes.ANEWARRAY -> {
                var s = item!!.strVal1
                pop()
                if (s!![0] == '[') {
                    push(cw!!, "[$s")
                } else {
                    push(ARRAY_OF or OBJECT or cw!!.addType(s))
                }
            }
            Opcodes.CHECKCAST -> {
                val s = item!!.strVal1
                pop()
                if (s!!.get(0) == '[') {
                    push(cw!!, s)
                } else {
                    push(OBJECT or cw!!.addType(s))
                }
            }
            // case Opcodes.MULTIANEWARRAY:
            else -> {
                pop(arg)
                push(cw!!, item!!.strVal1)
            }
        }
    }

    /**
     * Merges the input frame of the given basic block with the input and output
     * frames of this basic block. Returns <tt>true</tt> if the input frame of
     * the given label has been changed by this operation.
     *
     * @param cw
     * the ClassWriter to which this label belongs.
     * @param frame
     * the basic block whose input frame must be updated.
     * @param edge
     * the kind of the [Edge] between this label and 'label'.
     * See [Edge.info].
     * @return <tt>true</tt> if the input frame of the given label has been
     * changed by this operation.
     */
    fun merge(cw: ClassWriter, frame: Frame, edge: Int): Boolean {
        var changed = false
        var i: Int
        var s: Int
        var dim: Int
        var kind: Int
        var t: Int

        val nLocal = inputLocals!!.size
        val nStack = inputStack!!.size
        if (frame.inputLocals == null) {
            frame.inputLocals = IntArray(nLocal)
            changed = true
        }

        i = 0
        while (i < nLocal) {
            if (outputLocals != null && i < outputLocals!!.size) {
                s = outputLocals!![i]
                if (s == 0) {
                    t = inputLocals!![i]
                } else {
                    dim = s and DIM
                    kind = s and KIND
                    if (kind == BASE) {
                        t = s
                    } else {
                        if (kind == LOCAL) {
                            t = dim + inputLocals!![s and VALUE]
                        } else {
                            t = dim + inputStack!![nStack - (s and VALUE)]
                        }
                        if (s and TOP_IF_LONG_OR_DOUBLE != 0 && (t == LONG || t == DOUBLE)) {
                            t = TOP
                        }
                    }
                }
            } else {
                t = inputLocals!![i]
            }
            if (initializations != null) {
                t = init(cw, t)
            }
            changed = changed or merge(cw, t, frame.inputLocals!!, i)
            ++i
        }

        if (edge > 0) {
            i = 0
            while (i < nLocal) {
                t = inputLocals!![i]
                changed = changed or merge(cw, t, frame.inputLocals!!, i)
                ++i
            }
            if (frame.inputStack == null) {
                frame.inputStack = IntArray(1)
                changed = true
            }
            changed = changed or merge(cw, edge, frame.inputStack!!, 0)
            return changed
        }

        val nInputStack = inputStack!!.size + owner!!.inputStackTop
        if (frame.inputStack == null) {
            frame.inputStack = IntArray(nInputStack + outputStackTop)
            changed = true
        }

        i = 0
        while (i < nInputStack) {
            t = inputStack!![i]
            if (initializations != null) {
                t = init(cw, t)
            }
            changed = changed or merge(cw, t, frame.inputStack!!, i)
            ++i
        }
        i = 0
        while (i < outputStackTop) {
            s = outputStack!![i]
            dim = s and DIM
            kind = s and KIND
            if (kind == BASE) {
                t = s
            } else {
                if (kind == LOCAL) {
                    t = dim + inputLocals!![s and VALUE]
                } else {
                    t = dim + inputStack!![nStack - (s and VALUE)]
                }
                if (s and TOP_IF_LONG_OR_DOUBLE != 0 && (t == LONG || t == DOUBLE)) {
                    t = TOP
                }
            }
            if (initializations != null) {
                t = init(cw, t)
            }
            changed = changed or merge(cw, t, frame.inputStack!!, nInputStack + i)
            ++i
        }
        return changed
    }

    companion object {

        /*
     * Frames are computed in a two steps process: during the visit of each
     * instruction, the state of the frame at the end of current basic block is
     * updated by simulating the action of the instruction on the previous state
     * of this so called "output frame". In visitMaxs, a fix point algorithm is
     * used to compute the "input frame" of each basic block, i.e. the stack map
     * frame at the beginning of the basic block, starting from the input frame
     * of the first basic block (which is computed from the method descriptor),
     * and by using the previously computed output frames to compute the input
     * state of the other blocks.
     *
     * All output and input frames are stored as arrays of integers. Reference
     * and array types are represented by an index into a type table (which is
     * not the same as the constant pool of the class, in order to avoid adding
     * unnecessary constants in the pool - not all computed frames will end up
     * being stored in the stack map table). This allows very fast type
     * comparisons.
     *
     * Output stack map frames are computed relatively to the input frame of the
     * basic block, which is not yet known when output frames are computed. It
     * is therefore necessary to be able to represent abstract types such as
     * "the type at position x in the input frame locals" or "the type at
     * position x from the top of the input frame stack" or even "the type at
     * position x in the input frame, with y more (or less) array dimensions".
     * This explains the rather complicated type format used in output frames.
     *
     * This format is the following: DIM KIND VALUE (4, 4 and 24 bits). DIM is a
     * signed number of array dimensions (from -8 to 7). KIND is either BASE,
     * LOCAL or STACK. BASE is used for types that are not relative to the input
     * frame. LOCAL is used for types that are relative to the input local
     * variable types. STACK is used for types that are relative to the input
     * stack types. VALUE depends on KIND. For LOCAL types, it is an index in
     * the input local variable types. For STACK types, it is a position
     * relatively to the top of input frame stack. For BASE types, it is either
     * one of the constants defined in FrameVisitor, or for OBJECT and
     * UNINITIALIZED types, a tag and an index in the type table.
     *
     * Output frames can contain types of any kind and with a positive or
     * negative dimension (and even unassigned types, represented by 0 - which
     * does not correspond to any valid type value). Input frames can only
     * contain BASE types of positive or null dimension. In all cases the type
     * table contains only internal type names (array type descriptors are
     * forbidden - dimensions must be represented through the DIM field).
     *
     * The LONG and DOUBLE types are always represented by using two slots (LONG
     * + TOP or DOUBLE + TOP), for local variable types as well as in the
     * operand stack. This is necessary to be able to simulate DUPx_y
     * instructions, whose effect would be dependent on the actual type values
     * if types were always represented by a single slot in the stack (and this
     * is not possible, since actual type values are not always known - cf LOCAL
     * and STACK type kinds).
     */

        /**
         * Mask to get the dimension of a frame type. This dimension is a signed
         * integer between -8 and 7.
         */
        val DIM = -0x10000000

        /**
         * Constant to be added to a type to get a type with one more dimension.
         */
        val ARRAY_OF = 0x10000000

        /**
         * Constant to be added to a type to get a type with one less dimension.
         */
        val ELEMENT_OF = -0x10000000

        /**
         * Mask to get the kind of a frame type.
         *
         * @see .BASE
         *
         * @see .LOCAL
         *
         * @see .STACK
         */
        val KIND = 0xF000000

        /**
         * Flag used for LOCAL and STACK types. Indicates that if this type happens
         * to be a long or double type (during the computations of input frames),
         * then it must be set to TOP because the second word of this value has been
         * reused to store other data in the basic block. Hence the first word no
         * longer stores a valid long or double value.
         */
        val TOP_IF_LONG_OR_DOUBLE = 0x800000

        /**
         * Mask to get the value of a frame type.
         */
        val VALUE = 0x7FFFFF

        /**
         * Mask to get the kind of base types.
         */
        val BASE_KIND = 0xFF00000

        /**
         * Mask to get the value of base types.
         */
        val BASE_VALUE = 0xFFFFF

        /**
         * Kind of the types that are not relative to an input stack map frame.
         */
        val BASE = 0x1000000

        /**
         * Base kind of the base reference types. The BASE_VALUE of such types is an
         * index into the type table.
         */
        val OBJECT = BASE or 0x700000

        /**
         * Base kind of the uninitialized base types. The BASE_VALUE of such types
         * in an index into the type table (the Item at that index contains both an
         * instruction offset and an internal class name).
         */
        val UNINITIALIZED = BASE or 0x800000

        /**
         * Kind of the types that are relative to the local variable types of an
         * input stack map frame. The value of such types is a local variable index.
         */
        private val LOCAL = 0x2000000

        /**
         * Kind of the the types that are relative to the stack of an input stack
         * map frame. The value of such types is a position relatively to the top of
         * this stack.
         */
        private val STACK = 0x3000000

        /**
         * The TOP type. This is a BASE type.
         */
        val TOP = BASE or 0

        /**
         * The BOOLEAN type. This is a BASE type mainly used for array types.
         */
        val BOOLEAN = BASE or 9

        /**
         * The BYTE type. This is a BASE type mainly used for array types.
         */
        val BYTE = BASE or 10

        /**
         * The CHAR type. This is a BASE type mainly used for array types.
         */
        val CHAR = BASE or 11

        /**
         * The SHORT type. This is a BASE type mainly used for array types.
         */
        val SHORT = BASE or 12

        /**
         * The INTEGER type. This is a BASE type.
         */
        val INTEGER = BASE or 1

        /**
         * The FLOAT type. This is a BASE type.
         */
        val FLOAT = BASE or 2

        /**
         * The DOUBLE type. This is a BASE type.
         */
        val DOUBLE = BASE or 3

        /**
         * The LONG type. This is a BASE type.
         */
        val LONG = BASE or 4

        /**
         * The NULL type. This is a BASE type.
         */
        val NULL = BASE or 5

        /**
         * The UNINITIALIZED_THIS type. This is a BASE type.
         */
        val UNINITIALIZED_THIS = BASE or 6

        /**
         * The stack size variation corresponding to each JVM instruction. This
         * stack variation is equal to the size of the values produced by an
         * instruction, minus the size of the values consumed by this instruction.
         */
        val SIZE: IntArray

        /**
         * Computes the stack size variation corresponding to each JVM instruction.
         */
        init {
            var i: Int
            val b = IntArray(202)
            val s = ("EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD"
                    + "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD"
                    + "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED"
                    + "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE")
            i = 0
            while (i < b.size) {
                b[i] = s[i] - 'E'
                ++i
            }
            SIZE = b

            // code to generate the above string
            //
            // int NA = 0; // not applicable (unused opcode or variable size opcode)
            //
            // b = new int[] {
            // 0, //NOP, // visitInsn
            // 1, //ACONST_NULL, // -
            // 1, //ICONST_M1, // -
            // 1, //ICONST_0, // -
            // 1, //ICONST_1, // -
            // 1, //ICONST_2, // -
            // 1, //ICONST_3, // -
            // 1, //ICONST_4, // -
            // 1, //ICONST_5, // -
            // 2, //LCONST_0, // -
            // 2, //LCONST_1, // -
            // 1, //FCONST_0, // -
            // 1, //FCONST_1, // -
            // 1, //FCONST_2, // -
            // 2, //DCONST_0, // -
            // 2, //DCONST_1, // -
            // 1, //BIPUSH, // visitIntInsn
            // 1, //SIPUSH, // -
            // 1, //LDC, // visitLdcInsn
            // NA, //LDC_W, // -
            // NA, //LDC2_W, // -
            // 1, //ILOAD, // visitVarInsn
            // 2, //LLOAD, // -
            // 1, //FLOAD, // -
            // 2, //DLOAD, // -
            // 1, //ALOAD, // -
            // NA, //ILOAD_0, // -
            // NA, //ILOAD_1, // -
            // NA, //ILOAD_2, // -
            // NA, //ILOAD_3, // -
            // NA, //LLOAD_0, // -
            // NA, //LLOAD_1, // -
            // NA, //LLOAD_2, // -
            // NA, //LLOAD_3, // -
            // NA, //FLOAD_0, // -
            // NA, //FLOAD_1, // -
            // NA, //FLOAD_2, // -
            // NA, //FLOAD_3, // -
            // NA, //DLOAD_0, // -
            // NA, //DLOAD_1, // -
            // NA, //DLOAD_2, // -
            // NA, //DLOAD_3, // -
            // NA, //ALOAD_0, // -
            // NA, //ALOAD_1, // -
            // NA, //ALOAD_2, // -
            // NA, //ALOAD_3, // -
            // -1, //IALOAD, // visitInsn
            // 0, //LALOAD, // -
            // -1, //FALOAD, // -
            // 0, //DALOAD, // -
            // -1, //AALOAD, // -
            // -1, //BALOAD, // -
            // -1, //CALOAD, // -
            // -1, //SALOAD, // -
            // -1, //ISTORE, // visitVarInsn
            // -2, //LSTORE, // -
            // -1, //FSTORE, // -
            // -2, //DSTORE, // -
            // -1, //ASTORE, // -
            // NA, //ISTORE_0, // -
            // NA, //ISTORE_1, // -
            // NA, //ISTORE_2, // -
            // NA, //ISTORE_3, // -
            // NA, //LSTORE_0, // -
            // NA, //LSTORE_1, // -
            // NA, //LSTORE_2, // -
            // NA, //LSTORE_3, // -
            // NA, //FSTORE_0, // -
            // NA, //FSTORE_1, // -
            // NA, //FSTORE_2, // -
            // NA, //FSTORE_3, // -
            // NA, //DSTORE_0, // -
            // NA, //DSTORE_1, // -
            // NA, //DSTORE_2, // -
            // NA, //DSTORE_3, // -
            // NA, //ASTORE_0, // -
            // NA, //ASTORE_1, // -
            // NA, //ASTORE_2, // -
            // NA, //ASTORE_3, // -
            // -3, //IASTORE, // visitInsn
            // -4, //LASTORE, // -
            // -3, //FASTORE, // -
            // -4, //DASTORE, // -
            // -3, //AASTORE, // -
            // -3, //BASTORE, // -
            // -3, //CASTORE, // -
            // -3, //SASTORE, // -
            // -1, //POP, // -
            // -2, //POP2, // -
            // 1, //DUP, // -
            // 1, //DUP_X1, // -
            // 1, //DUP_X2, // -
            // 2, //DUP2, // -
            // 2, //DUP2_X1, // -
            // 2, //DUP2_X2, // -
            // 0, //SWAP, // -
            // -1, //IADD, // -
            // -2, //LADD, // -
            // -1, //FADD, // -
            // -2, //DADD, // -
            // -1, //ISUB, // -
            // -2, //LSUB, // -
            // -1, //FSUB, // -
            // -2, //DSUB, // -
            // -1, //IMUL, // -
            // -2, //LMUL, // -
            // -1, //FMUL, // -
            // -2, //DMUL, // -
            // -1, //IDIV, // -
            // -2, //LDIV, // -
            // -1, //FDIV, // -
            // -2, //DDIV, // -
            // -1, //IREM, // -
            // -2, //LREM, // -
            // -1, //FREM, // -
            // -2, //DREM, // -
            // 0, //INEG, // -
            // 0, //LNEG, // -
            // 0, //FNEG, // -
            // 0, //DNEG, // -
            // -1, //ISHL, // -
            // -1, //LSHL, // -
            // -1, //ISHR, // -
            // -1, //LSHR, // -
            // -1, //IUSHR, // -
            // -1, //LUSHR, // -
            // -1, //IAND, // -
            // -2, //LAND, // -
            // -1, //IOR, // -
            // -2, //LOR, // -
            // -1, //IXOR, // -
            // -2, //LXOR, // -
            // 0, //IINC, // visitIincInsn
            // 1, //I2L, // visitInsn
            // 0, //I2F, // -
            // 1, //I2D, // -
            // -1, //L2I, // -
            // -1, //L2F, // -
            // 0, //L2D, // -
            // 0, //F2I, // -
            // 1, //F2L, // -
            // 1, //F2D, // -
            // -1, //D2I, // -
            // 0, //D2L, // -
            // -1, //D2F, // -
            // 0, //I2B, // -
            // 0, //I2C, // -
            // 0, //I2S, // -
            // -3, //LCMP, // -
            // -1, //FCMPL, // -
            // -1, //FCMPG, // -
            // -3, //DCMPL, // -
            // -3, //DCMPG, // -
            // -1, //IFEQ, // visitJumpInsn
            // -1, //IFNE, // -
            // -1, //IFLT, // -
            // -1, //IFGE, // -
            // -1, //IFGT, // -
            // -1, //IFLE, // -
            // -2, //IF_ICMPEQ, // -
            // -2, //IF_ICMPNE, // -
            // -2, //IF_ICMPLT, // -
            // -2, //IF_ICMPGE, // -
            // -2, //IF_ICMPGT, // -
            // -2, //IF_ICMPLE, // -
            // -2, //IF_ACMPEQ, // -
            // -2, //IF_ACMPNE, // -
            // 0, //GOTO, // -
            // 1, //JSR, // -
            // 0, //RET, // visitVarInsn
            // -1, //TABLESWITCH, // visiTableSwitchInsn
            // -1, //LOOKUPSWITCH, // visitLookupSwitch
            // -1, //IRETURN, // visitInsn
            // -2, //LRETURN, // -
            // -1, //FRETURN, // -
            // -2, //DRETURN, // -
            // -1, //ARETURN, // -
            // 0, //RETURN, // -
            // NA, //GETSTATIC, // visitFieldInsn
            // NA, //PUTSTATIC, // -
            // NA, //GETFIELD, // -
            // NA, //PUTFIELD, // -
            // NA, //INVOKEVIRTUAL, // visitMethodInsn
            // NA, //INVOKESPECIAL, // -
            // NA, //INVOKESTATIC, // -
            // NA, //INVOKEINTERFACE, // -
            // NA, //INVOKEDYNAMIC, // visitInvokeDynamicInsn
            // 1, //NEW, // visitTypeInsn
            // 0, //NEWARRAY, // visitIntInsn
            // 0, //ANEWARRAY, // visitTypeInsn
            // 0, //ARRAYLENGTH, // visitInsn
            // NA, //ATHROW, // -
            // 0, //CHECKCAST, // visitTypeInsn
            // 0, //INSTANCEOF, // -
            // -1, //MONITORENTER, // visitInsn
            // -1, //MONITOREXIT, // -
            // NA, //WIDE, // NOT VISITED
            // NA, //MULTIANEWARRAY, // visitMultiANewArrayInsn
            // -1, //IFNULL, // visitJumpInsn
            // -1, //IFNONNULL, // -
            // NA, //GOTO_W, // -
            // NA, //JSR_W, // -
            // };
            // for (i = 0; i < b.length; ++i) {
            // System.err.print((char)('E' + b[i]));
            // }
            // System.err.
        }

        /**
         * Returns the int encoding of the given type.
         *
         * @param cw
         * the ClassWriter to which this label belongs.
         * @param desc
         * a type descriptor.
         * @return the int encoding of the given type.
         */
        private fun type(cw: ClassWriter, desc: String): Int {
            val t: String
            val index = if (desc[0] == '(') desc.indexOf(')') + 1 else 0
            when (desc[index]) {
                'V' -> return 0
                'Z', 'C', 'B', 'S', 'I' -> return INTEGER
                'F' -> return FLOAT
                'J' -> return LONG
                'D' -> return DOUBLE
                'L' -> {
                    // stores the internal name, not the descriptor!
                    t = desc.substring(index + 1, desc.length - 1)
                    return OBJECT or cw.addType(t)
                }
                // case '[':
                else -> {
                    // extracts the dimensions and the element type
                    val data: Int
                    var dims = index + 1
                    while (desc[dims] == '[') {
                        ++dims
                    }
                    when (desc[dims]) {
                        'Z' -> data = BOOLEAN
                        'C' -> data = CHAR
                        'B' -> data = BYTE
                        'S' -> data = SHORT
                        'I' -> data = INTEGER
                        'F' -> data = FLOAT
                        'J' -> data = LONG
                        'D' -> data = DOUBLE
                        // case 'L':
                        else -> {
                            // stores the internal name, not the descriptor
                            t = desc.substring(dims + 1, desc.length - 1)
                            data = OBJECT or cw.addType(t)
                        }
                    }
                    return dims - index shl 28 or data
                }
            }
        }

        /**
         * Merges the type at the given index in the given type array with the given
         * type. Returns <tt>true</tt> if the type array has been modified by this
         * operation.
         *
         * @param cw
         * the ClassWriter to which this label belongs.
         * @param t
         * the type with which the type array element must be merged.
         * @param types
         * an array of types.
         * @param index
         * the index of the type that must be merged in 'types'.
         * @return <tt>true</tt> if the type array has been modified by this
         * operation.
         */
        private fun merge(cw: ClassWriter, t: Int,
                          types: IntArray, index: Int): Boolean {
            var t = t
            val u = types[index]
            if (u == t) {
                // if the types are equal, merge(u,t)=u, so there is no change
                return false
            }
            if (t and DIM.inv() == NULL) {
                if (u == NULL) {
                    return false
                }
                t = NULL
            }
            if (u == 0) {
                // if types[index] has never been assigned, merge(u,t)=t
                types[index] = t
                return true
            }
            val v: Int
            if (u and BASE_KIND == OBJECT || u and DIM != 0) {
                // if u is a reference type of any dimension
                if (t == NULL) {
                    // if t is the NULL type, merge(u,t)=u, so there is no change
                    return false
                } else if (t and (DIM or BASE_KIND) == u and (DIM or BASE_KIND)) {
                    if (u and BASE_KIND == OBJECT) {
                        // if t is also a reference type, and if u and t have the
                        // same dimension merge(u,t) = dim(t) | common parent of the
                        // element types of u and t
                        v = (t and DIM or OBJECT
                                or cw.getMergedType(t and BASE_VALUE, u and BASE_VALUE))
                    } else {
                        // if u and t are array types, but not with the same element
                        // type, merge(u,t)=java/lang/Object
                        v = OBJECT or cw.addType("java/lang/Object")
                    }
                } else if (t and BASE_KIND == OBJECT || t and DIM != 0) {
                    // if t is any other reference or array type,
                    // merge(u,t)=java/lang/Object
                    v = OBJECT or cw.addType("java/lang/Object")
                } else {
                    // if t is any other type, merge(u,t)=TOP
                    v = TOP
                }
            } else if (u == NULL) {
                // if u is the NULL type, merge(u,t)=t,
                // or TOP if t is not a reference type
                v = if (t and BASE_KIND == OBJECT || t and DIM != 0) t else TOP
            } else {
                // if u is any other type, merge(u,t)=TOP whatever t
                v = TOP
            }
            if (u != v) {
                types[index] = v
                return true
            }
            return false
        }
    }
}
