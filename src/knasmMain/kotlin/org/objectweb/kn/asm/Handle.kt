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
 * A reference to a field or a method.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
class Handle
/**
 * Constructs a new field or method handle.
 *
 * @param tag
 * the kind of field or method designated by this Handle. Must be
 * [Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC],
 * [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC],
 * [Opcodes.H_INVOKEVIRTUAL],
 * [Opcodes.H_INVOKESTATIC],
 * [Opcodes.H_INVOKESPECIAL],
 * [Opcodes.H_NEWINVOKESPECIAL] or
 * [Opcodes.H_INVOKEINTERFACE].
 * @param owner
 * the internal name of the field or method designed by this
 * handle.
 * @param name
 * the name of the field or method designated by this handle.
 * @param desc
 * the descriptor of the field or method designated by this
 * handle.
 */
(
        /**
         * The kind of field or method designated by this Handle. Should be
         * [Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC],
         * [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC],
         * [Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC],
         * [Opcodes.H_INVOKESPECIAL], [Opcodes.H_NEWINVOKESPECIAL] or
         * [Opcodes.H_INVOKEINTERFACE].
         */
        /**
         * Returns the kind of field or method designated by this handle.
         *
         * @return [Opcodes.H_GETFIELD], [Opcodes.H_GETSTATIC],
         * [Opcodes.H_PUTFIELD], [Opcodes.H_PUTSTATIC],
         * [Opcodes.H_INVOKEVIRTUAL], [Opcodes.H_INVOKESTATIC],
         * [Opcodes.H_INVOKESPECIAL],
         * [Opcodes.H_NEWINVOKESPECIAL] or
         * [Opcodes.H_INVOKEINTERFACE].
         */
        val tag: Int,
        /**
         * The internal name of the field or method designed by this handle.
         */
        /**
         * Returns the internal name of the field or method designed by this handle.
         *
         * @return the internal name of the field or method designed by this handle.
         */
        val owner: String,
        /**
         * The name of the field or method designated by this handle.
         */
        /**
         * Returns the name of the field or method designated by this handle.
         *
         * @return the name of the field or method designated by this handle.
         */
        val name: String,
        /**
         * The descriptor of the field or method designated by this handle.
         */
        /**
         * Returns the descriptor of the field or method designated by this handle.
         *
         * @return the descriptor of the field or method designated by this handle.
         */
        val desc: String) {

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is Handle) {
            return false
        }
        val h = obj as Handle?
        return (tag == h!!.tag && owner == h.owner && name == h.name
                && desc == h.desc)
    }

    override fun hashCode(): Int {
        return tag + owner.hashCode() * name.hashCode() * desc.hashCode()
    }

    /**
     * Returns the textual representation of this handle. The textual
     * representation is:
     *
     * <pre>
     * owner '.' name desc ' ' '(' tag ')'
    </pre> *
     *
     * . As this format is unambiguous, it can be parsed if necessary.
     */
    override fun toString(): String {
        return owner + '.'.toString() + name + desc + " (" + tag + ')'.toString()
    }
}
