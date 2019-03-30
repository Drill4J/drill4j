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


import org.objectweb.kn.asm.Label
import org.objectweb.kn.asm.MethodVisitor
import org.objectweb.kn.asm.Opcodes
import org.objectweb.kn.asm.Type


/**
 * A [org.objectweb.asm.MethodVisitor] to insert before, after and around
 * advices in methods and constructors.
 *
 *
 * The behavior for constructors is like this:
 *
 *
 *  1. as long as the INVOKESPECIAL for the object initialization has not been
 * reached, every bytecode instruction is dispatched in the ctor code visitor
 *
 *  1. when this one is reached, it is only added in the ctor code visitor and a
 * JP invoke is added
 *
 *  1. after that, only the other code visitor receives the instructions
 *
 *
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
abstract class AdviceAdapter
/**
 * Creates a new [AdviceAdapter].
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
protected constructor(
    api: Int, mv: MethodVisitor,
    protected var methodAccess: Int, name: String, protected var methodDesc: String
) : GeneratorAdapter(api, mv, methodAccess, name, methodDesc), Opcodes {

    private var constructor: Boolean = false

    private var superInitialized: Boolean = false

    private var stackFrame: MutableList<Any>? = null

    private var branches: MutableMap<Label, List<Any>>? = null

    init {
        constructor = "<init>" == name
    }

    override fun visitCode() {
        mv!!.visitCode()
        if (constructor) {
            stackFrame = ArrayList()
            branches = HashMap()
        } else {
            superInitialized = true
            onMethodEnter()
        }
    }

    override fun visitLabel(label: Label) {
        mv!!.visitLabel(label)
        if (constructor && branches != null) {
            val frame = branches!![label]
            if (frame != null) {
                stackFrame = frame.toMutableList()
                branches!!.remove(label)
            }
        }
    }

    override fun visitInsn(opcode: Int) {
        if (constructor) {
            val s: Int
            when (opcode) {
                Opcodes.RETURN // empty stack
                -> onMethodExit(opcode)
                Opcodes.IRETURN // 1 before n/a after
                    , Opcodes.FRETURN // 1 before n/a after
                    , Opcodes.ARETURN // 1 before n/a after
                    , Opcodes.ATHROW // 1 before n/a after
                -> {
                    popValue()
                    onMethodExit(opcode)
                }
                Opcodes.LRETURN // 2 before n/a after
                    , Opcodes.DRETURN // 2 before n/a after
                -> {
                    popValue()
                    popValue()
                    onMethodExit(opcode)
                }
                Opcodes.NOP, Opcodes.LALOAD // remove 2 add 2
                    , Opcodes.DALOAD // remove 2 add 2
                    , Opcodes.LNEG, Opcodes.DNEG, Opcodes.FNEG, Opcodes.INEG, Opcodes.L2D, Opcodes.D2L, Opcodes.F2I, Opcodes.I2B, Opcodes.I2C, Opcodes.I2S, Opcodes.I2F, Opcodes.ARRAYLENGTH -> {
                }
                Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.F2L // 1 before 2 after
                    , Opcodes.F2D, Opcodes.I2L, Opcodes.I2D -> pushValue(OTHER)
                Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.DCONST_0, Opcodes.DCONST_1 -> {
                    pushValue(OTHER)
                    pushValue(OTHER)
                }
                Opcodes.IALOAD // remove 2 add 1
                    , Opcodes.FALOAD // remove 2 add 1
                    , Opcodes.AALOAD // remove 2 add 1
                    , Opcodes.BALOAD // remove 2 add 1
                    , Opcodes.CALOAD // remove 2 add 1
                    , Opcodes.SALOAD // remove 2 add 1
                    , Opcodes.POP, Opcodes.IADD, Opcodes.FADD, Opcodes.ISUB, Opcodes.LSHL // 3 before 2 after
                    , Opcodes.LSHR // 3 before 2 after
                    , Opcodes.LUSHR // 3 before 2 after
                    , Opcodes.L2I // 2 before 1 after
                    , Opcodes.L2F // 2 before 1 after
                    , Opcodes.D2I // 2 before 1 after
                    , Opcodes.D2F // 2 before 1 after
                    , Opcodes.FSUB, Opcodes.FMUL, Opcodes.FDIV, Opcodes.FREM, Opcodes.FCMPL // 2 before 1 after
                    , Opcodes.FCMPG // 2 before 1 after
                    , Opcodes.IMUL, Opcodes.IDIV, Opcodes.IREM, Opcodes.ISHL, Opcodes.ISHR, Opcodes.IUSHR, Opcodes.IAND, Opcodes.IOR, Opcodes.IXOR, Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> popValue()
                Opcodes.POP2, Opcodes.LSUB, Opcodes.LMUL, Opcodes.LDIV, Opcodes.LREM, Opcodes.LADD, Opcodes.LAND, Opcodes.LOR, Opcodes.LXOR, Opcodes.DADD, Opcodes.DMUL, Opcodes.DSUB, Opcodes.DDIV, Opcodes.DREM -> {
                    popValue()
                    popValue()
                }
                Opcodes.IASTORE, Opcodes.FASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.LCMP // 4 before 1 after
                    , Opcodes.DCMPL, Opcodes.DCMPG -> {
                    popValue()
                    popValue()
                    popValue()
                }
                Opcodes.LASTORE, Opcodes.DASTORE -> {
                    popValue()
                    popValue()
                    popValue()
                    popValue()
                }
                Opcodes.DUP -> pushValue(peekValue())
                Opcodes.DUP_X1 -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 2, stackFrame!![s - 1])
                }
                Opcodes.DUP_X2 -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 3, stackFrame!![s - 1])
                }
                Opcodes.DUP2 -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 2, stackFrame!![s - 1])
                    stackFrame!!.add(s - 2, stackFrame!![s - 1])
                }
                Opcodes.DUP2_X1 -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 3, stackFrame!![s - 1])
                    stackFrame!!.add(s - 3, stackFrame!![s - 1])
                }
                Opcodes.DUP2_X2 -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 4, stackFrame!![s - 1])
                    stackFrame!!.add(s - 4, stackFrame!![s - 1])
                }
                Opcodes.SWAP -> {
                    s = stackFrame!!.size
                    stackFrame!!.add(s - 2, stackFrame!![s - 1])
                    stackFrame!!.removeAt(s)
                }
            }
        } else {
            when (opcode) {
                Opcodes.RETURN, Opcodes.IRETURN, Opcodes.FRETURN, Opcodes.ARETURN, Opcodes.LRETURN, Opcodes.DRETURN, Opcodes.ATHROW -> onMethodExit(
                    opcode
                )
            }
        }
        mv!!.visitInsn(opcode)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        super.visitVarInsn(opcode, `var`)
        if (constructor) {
            when (opcode) {
                Opcodes.ILOAD, Opcodes.FLOAD -> pushValue(OTHER)
                Opcodes.LLOAD, Opcodes.DLOAD -> {
                    pushValue(OTHER)
                    pushValue(OTHER)
                }
                Opcodes.ALOAD -> pushValue(if (`var` == 0) THIS else OTHER)
                Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE -> popValue()
                Opcodes.LSTORE, Opcodes.DSTORE -> {
                    popValue()
                    popValue()
                }
            }
        }
    }

    override fun visitFieldInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        mv!!.visitFieldInsn(opcode, owner, name, desc)
        if (constructor) {
            val c = desc[0]
            val longOrDouble = c == 'J' || c == 'D'
            when (opcode) {
                Opcodes.GETSTATIC -> {
                    pushValue(OTHER)
                    if (longOrDouble) {
                        pushValue(OTHER)
                    }
                }
                Opcodes.PUTSTATIC -> {
                    popValue()
                    if (longOrDouble) {
                        popValue()
                    }
                }
                Opcodes.PUTFIELD -> {
                    popValue()
                    if (longOrDouble) {
                        popValue()
                        popValue()
                    }
                }
                // case GETFIELD:
                else -> if (longOrDouble) {
                    pushValue(OTHER)
                }
            }
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        mv!!.visitIntInsn(opcode, operand)
        if (constructor && opcode != Opcodes.NEWARRAY) {
            pushValue(OTHER)
        }
    }

    override fun visitLdcInsn(cst: Any) {
        mv!!.visitLdcInsn(cst)
        if (constructor) {
            pushValue(OTHER)
            if (cst is Double || cst is Long) {
                pushValue(OTHER)
            }
        }
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        mv!!.visitMultiANewArrayInsn(desc, dims)
        if (constructor) {
            for (i in 0 until dims) {
                popValue()
            }
            pushValue(OTHER)
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        mv!!.visitTypeInsn(opcode, type)
        // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack
        if (constructor && opcode == Opcodes.NEW) {
            pushValue(OTHER)
        }
    }

    override fun visitMethodInsn(
        opcode: Int, owner: String,
        name: String, desc: String
    ) {
        mv!!.visitMethodInsn(opcode, owner, name, desc)
        if (constructor) {
            val types = Type.getArgumentTypes(desc)
            for (i in types.indices) {
                popValue()
                if (types[i].size == 2) {
                    popValue()
                }
            }
            when (opcode) {
                // case INVOKESTATIC:
                // break;
                Opcodes.INVOKEINTERFACE, Opcodes.INVOKEVIRTUAL -> popValue() // objectref
                Opcodes.INVOKESPECIAL -> {
                    val type = popValue() // objectref
                    if (type === THIS && !superInitialized) {
                        onMethodEnter()
                        superInitialized = true
                        // once super has been initialized it is no longer
                        // necessary to keep track of stack state
                        constructor = false
                    }
                }
            }

            val returnType = Type.getReturnType(desc)
            if (returnType !== Type.VOID_TYPE) {
                pushValue(OTHER)
                if (returnType.size == 2) {
                    pushValue(OTHER)
                }
            }
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        mv!!.visitJumpInsn(opcode, label)
        if (constructor) {
            when (opcode) {
                Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IFNULL, Opcodes.IFNONNULL -> popValue()
                Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> {
                    popValue()
                    popValue()
                }
                Opcodes.JSR -> pushValue(OTHER)
            }
            addBranch(label)
        }
    }

    override fun visitLookupSwitchInsn(
        dflt: Label, keys: IntArray,
        labels: Array<Label?>
    ) {
        mv!!.visitLookupSwitchInsn(dflt, keys, labels)
        if (constructor) {
            popValue()
            addBranches(dflt, labels)
        }
    }


    override fun visitTryCatchBlock(
        start: Label, end: Label, handler: Label,
        type: String?
    ) {
        super.visitTryCatchBlock(start, end, handler, type)
        if (constructor && !branches!!.containsKey(handler)) {
            val stackFrame = ArrayList<Any>()
            stackFrame.add(OTHER)
            branches!![handler] = stackFrame
        }
    }

    private fun addBranches(dflt: Label, labels: Array<Label?>) {
        addBranch(dflt)
        for (i in labels.indices) {
            addBranch(labels[i]!!)
        }
    }

    private fun addBranch(label: Label) {
        if (branches!!.containsKey(label)) {
            return
        }
        branches!![label] = ArrayList(stackFrame!!)
    }

    private fun popValue(): Any {
        return stackFrame!!.removeAt(stackFrame!!.size - 1)
    }

    private fun peekValue(): Any {
        return stackFrame!![stackFrame!!.size - 1]
    }

    private fun pushValue(o: Any) {
        stackFrame!!.add(o)
    }

    /**
     * Called at the beginning of the method or after super class class call in
     * the constructor. <br></br>
     * <br></br>
     *
     * *Custom code can use or change all the local variables, but should not
     * change state of the stack.*
     */
    protected open fun onMethodEnter() {}

    /**
     * Called before explicit exit from the method using either return or throw.
     * Top element on the stack contains the return value or exception instance.
     * For example:
     *
     * <pre>
     * public void onMethodExit(int opcode) {
     * if(opcode==RETURN) {
     * visitInsn(ACONST_NULL);
     * } else if(opcode==ARETURN || opcode==ATHROW) {
     * dup();
     * } else {
     * if(opcode==LRETURN || opcode==DRETURN) {
     * dup2();
     * } else {
     * dup();
     * }
     * box(Type.getReturnType(this.methodDesc));
     * }
     * visitIntInsn(SIPUSH, opcode);
     * visitMethodInsn(INVOKESTATIC, owner, "onExit", "(Ljava/lang/Object;I)V");
     * }
     *
     * // an actual call back method
     * public static void onExit(Object param, int opcode) {
     * ...
    </pre> *
     *
     * <br></br>
     * <br></br>
     *
     * *Custom code can use or change all the local variables, but should not
     * change state of the stack.*
     *
     * @param opcode
     * one of the RETURN, IRETURN, FRETURN, ARETURN, LRETURN, DRETURN
     * or ATHROW
     */
    protected open fun onMethodExit(opcode: Int) {}

    companion object {

        private val THIS = Any()

        private val OTHER = Any()
    }

    // TODO onException, onMethodCall
}
