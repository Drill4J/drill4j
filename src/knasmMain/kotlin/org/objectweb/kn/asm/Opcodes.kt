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
 * Defines the JVM opcodes, access flags and array type codes. This interface
 * does not define all the JVM opcodes because some opcodes are automatically
 * handled. For example, the xLOAD and xSTORE opcodes are automatically replaced
 * by xLOAD_n and xSTORE_n opcodes when possible. The xLOAD_n and xSTORE_n
 * opcodes are therefore not defined in this interface. Likewise for LDC,
 * automatically replaced by LDC_W or LDC2_W when necessary, WIDE, GOTO_W and
 * JSR_W.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
interface Opcodes {
    companion object {

        // ASM API versions

        val ASM4 = 4 shl 16 or (0 shl 8) or 0
        val ASM5 = 5 shl 16 or (0 shl 8) or 0

        // versions

        val V1_1 = 3 shl 16 or 45
        val V1_2 = 0 shl 16 or 46
        val V1_3 = 0 shl 16 or 47
        val V1_4 = 0 shl 16 or 48
        val V1_5 = 0 shl 16 or 49
        val V1_6 = 0 shl 16 or 50
        val V1_7 = 0 shl 16 or 51
        val V1_8 = 0 shl 16 or 52

        // access flags

        val ACC_PUBLIC = 0x0001 // class, field, method
        val ACC_PRIVATE = 0x0002 // class, field, method
        val ACC_PROTECTED = 0x0004 // class, field, method
        val ACC_STATIC = 0x0008 // field, method
        val ACC_FINAL = 0x0010 // class, field, method, parameter
        val ACC_SUPER = 0x0020 // class
        val ACC_SYNCHRONIZED = 0x0020 // method
        val ACC_VOLATILE = 0x0040 // field
        val ACC_BRIDGE = 0x0040 // method
        val ACC_VARARGS = 0x0080 // method
        val ACC_TRANSIENT = 0x0080 // field
        val ACC_NATIVE = 0x0100 // method
        val ACC_INTERFACE = 0x0200 // class
        val ACC_ABSTRACT = 0x0400 // class, method
        val ACC_STRICT = 0x0800 // method
        val ACC_SYNTHETIC = 0x1000 // class, field, method, parameter
        val ACC_ANNOTATION = 0x2000 // class
        val ACC_ENUM = 0x4000 // class(?) field inner
        val ACC_MANDATED = 0x8000 // parameter

        // ASM specific pseudo access flags

        val ACC_DEPRECATED = 0x20000 // class, field, method

        // types for NEWARRAY

        val T_BOOLEAN = 4
        val T_CHAR = 5
        val T_FLOAT = 6
        val T_DOUBLE = 7
        val T_BYTE = 8
        val T_SHORT = 9
        val T_INT = 10
        val T_LONG = 11

        // tags for Handle

        val H_GETFIELD = 1
        val H_GETSTATIC = 2
        val H_PUTFIELD = 3
        val H_PUTSTATIC = 4
        val H_INVOKEVIRTUAL = 5
        val H_INVOKESTATIC = 6
        val H_INVOKESPECIAL = 7
        val H_NEWINVOKESPECIAL = 8
        val H_INVOKEINTERFACE = 9

        // stack map frame types

        /**
         * Represents an expanded frame. See [ClassReader.EXPAND_FRAMES].
         */
        val F_NEW = -1

        /**
         * Represents a compressed frame with complete frame data.
         */
        val F_FULL = 0

        /**
         * Represents a compressed frame where locals are the same as the locals in
         * the previous frame, except that additional 1-3 locals are defined, and
         * with an empty stack.
         */
        val F_APPEND = 1

        /**
         * Represents a compressed frame where locals are the same as the locals in
         * the previous frame, except that the last 1-3 locals are absent and with
         * an empty stack.
         */
        val F_CHOP = 2

        /**
         * Represents a compressed frame with exactly the same locals as the
         * previous frame and with an empty stack.
         */
        val F_SAME = 3

        /**
         * Represents a compressed frame with exactly the same locals as the
         * previous frame and with a single value on the stack.
         */
        val F_SAME1 = 4

        val TOP = 0
        val INTEGER = 1
        val FLOAT = 2
        val DOUBLE = 3
        val LONG = 4
        val NULL = 5
        val UNINITIALIZED_THIS = 6

        // opcodes // visit method (- = idem)

        val NOP = 0 // visitInsn
        val ACONST_NULL = 1 // -
        val ICONST_M1 = 2 // -
        val ICONST_0 = 3 // -
        val ICONST_1 = 4 // -
        val ICONST_2 = 5 // -
        val ICONST_3 = 6 // -
        val ICONST_4 = 7 // -
        val ICONST_5 = 8 // -
        val LCONST_0 = 9 // -
        val LCONST_1 = 10 // -
        val FCONST_0 = 11 // -
        val FCONST_1 = 12 // -
        val FCONST_2 = 13 // -
        val DCONST_0 = 14 // -
        val DCONST_1 = 15 // -
        val BIPUSH = 16 // visitIntInsn
        val SIPUSH = 17 // -
        val LDC = 18 // visitLdcInsn
        // int LDC_W = 19; // -
        // int LDC2_W = 20; // -
        val ILOAD = 21 // visitVarInsn
        val LLOAD = 22 // -
        val FLOAD = 23 // -
        val DLOAD = 24 // -
        val ALOAD = 25 // -
        // int ILOAD_0 = 26; // -
        // int ILOAD_1 = 27; // -
        // int ILOAD_2 = 28; // -
        // int ILOAD_3 = 29; // -
        // int LLOAD_0 = 30; // -
        // int LLOAD_1 = 31; // -
        // int LLOAD_2 = 32; // -
        // int LLOAD_3 = 33; // -
        // int FLOAD_0 = 34; // -
        // int FLOAD_1 = 35; // -
        // int FLOAD_2 = 36; // -
        // int FLOAD_3 = 37; // -
        // int DLOAD_0 = 38; // -
        // int DLOAD_1 = 39; // -
        // int DLOAD_2 = 40; // -
        // int DLOAD_3 = 41; // -
        // int ALOAD_0 = 42; // -
        // int ALOAD_1 = 43; // -
        // int ALOAD_2 = 44; // -
        // int ALOAD_3 = 45; // -
        val IALOAD = 46 // visitInsn
        val LALOAD = 47 // -
        val FALOAD = 48 // -
        val DALOAD = 49 // -
        val AALOAD = 50 // -
        val BALOAD = 51 // -
        val CALOAD = 52 // -
        val SALOAD = 53 // -
        val ISTORE = 54 // visitVarInsn
        val LSTORE = 55 // -
        val FSTORE = 56 // -
        val DSTORE = 57 // -
        val ASTORE = 58 // -
        // int ISTORE_0 = 59; // -
        // int ISTORE_1 = 60; // -
        // int ISTORE_2 = 61; // -
        // int ISTORE_3 = 62; // -
        // int LSTORE_0 = 63; // -
        // int LSTORE_1 = 64; // -
        // int LSTORE_2 = 65; // -
        // int LSTORE_3 = 66; // -
        // int FSTORE_0 = 67; // -
        // int FSTORE_1 = 68; // -
        // int FSTORE_2 = 69; // -
        // int FSTORE_3 = 70; // -
        // int DSTORE_0 = 71; // -
        // int DSTORE_1 = 72; // -
        // int DSTORE_2 = 73; // -
        // int DSTORE_3 = 74; // -
        // int ASTORE_0 = 75; // -
        // int ASTORE_1 = 76; // -
        // int ASTORE_2 = 77; // -
        // int ASTORE_3 = 78; // -
        val IASTORE = 79 // visitInsn
        val LASTORE = 80 // -
        val FASTORE = 81 // -
        val DASTORE = 82 // -
        val AASTORE = 83 // -
        val BASTORE = 84 // -
        val CASTORE = 85 // -
        val SASTORE = 86 // -
        val POP = 87 // -
        val POP2 = 88 // -
        val DUP = 89 // -
        val DUP_X1 = 90 // -
        val DUP_X2 = 91 // -
        val DUP2 = 92 // -
        val DUP2_X1 = 93 // -
        val DUP2_X2 = 94 // -
        val SWAP = 95 // -
        val IADD = 96 // -
        val LADD = 97 // -
        val FADD = 98 // -
        val DADD = 99 // -
        val ISUB = 100 // -
        val LSUB = 101 // -
        val FSUB = 102 // -
        val DSUB = 103 // -
        val IMUL = 104 // -
        val LMUL = 105 // -
        val FMUL = 106 // -
        val DMUL = 107 // -
        val IDIV = 108 // -
        val LDIV = 109 // -
        val FDIV = 110 // -
        val DDIV = 111 // -
        val IREM = 112 // -
        val LREM = 113 // -
        val FREM = 114 // -
        val DREM = 115 // -
        val INEG = 116 // -
        val LNEG = 117 // -
        val FNEG = 118 // -
        val DNEG = 119 // -
        val ISHL = 120 // -
        val LSHL = 121 // -
        val ISHR = 122 // -
        val LSHR = 123 // -
        val IUSHR = 124 // -
        val LUSHR = 125 // -
        val IAND = 126 // -
        val LAND = 127 // -
        val IOR = 128 // -
        val LOR = 129 // -
        val IXOR = 130 // -
        val LXOR = 131 // -
        val IINC = 132 // visitIincInsn
        val I2L = 133 // visitInsn
        val I2F = 134 // -
        val I2D = 135 // -
        val L2I = 136 // -
        val L2F = 137 // -
        val L2D = 138 // -
        val F2I = 139 // -
        val F2L = 140 // -
        val F2D = 141 // -
        val D2I = 142 // -
        val D2L = 143 // -
        val D2F = 144 // -
        val I2B = 145 // -
        val I2C = 146 // -
        val I2S = 147 // -
        val LCMP = 148 // -
        val FCMPL = 149 // -
        val FCMPG = 150 // -
        val DCMPL = 151 // -
        val DCMPG = 152 // -
        val IFEQ = 153 // visitJumpInsn
        val IFNE = 154 // -
        val IFLT = 155 // -
        val IFGE = 156 // -
        val IFGT = 157 // -
        val IFLE = 158 // -
        val IF_ICMPEQ = 159 // -
        val IF_ICMPNE = 160 // -
        val IF_ICMPLT = 161 // -
        val IF_ICMPGE = 162 // -
        val IF_ICMPGT = 163 // -
        val IF_ICMPLE = 164 // -
        val IF_ACMPEQ = 165 // -
        val IF_ACMPNE = 166 // -
        val GOTO = 167 // -
        val JSR = 168 // -
        val RET = 169 // visitVarInsn
        val TABLESWITCH = 170 // visiTableSwitchInsn
        val LOOKUPSWITCH = 171 // visitLookupSwitch
        val IRETURN = 172 // visitInsn
        val LRETURN = 173 // -
        val FRETURN = 174 // -
        val DRETURN = 175 // -
        val ARETURN = 176 // -
        val RETURN = 177 // -
        val GETSTATIC = 178 // visitFieldInsn
        val PUTSTATIC = 179 // -
        val GETFIELD = 180 // -
        val PUTFIELD = 181 // -
        val INVOKEVIRTUAL = 182 // visitMethodInsn
        val INVOKESPECIAL = 183 // -
        val INVOKESTATIC = 184 // -
        val INVOKEINTERFACE = 185 // -
        val INVOKEDYNAMIC = 186 // visitInvokeDynamicInsn
        val NEW = 187 // visitTypeInsn
        val NEWARRAY = 188 // visitIntInsn
        val ANEWARRAY = 189 // visitTypeInsn
        val ARRAYLENGTH = 190 // visitInsn
        val ATHROW = 191 // -
        val CHECKCAST = 192 // visitTypeInsn
        val INSTANCEOF = 193 // -
        val MONITORENTER = 194 // visitInsn
        val MONITOREXIT = 195 // -
        // int WIDE = 196; // NOT VISITED
        val MULTIANEWARRAY = 197 // visitMultiANewArrayInsn
        val IFNULL = 198 // visitJumpInsn
        val IFNONNULL = 199 // -
    }
    // int GOTO_W = 200; // -
    // int JSR_W = 201; // -
}
