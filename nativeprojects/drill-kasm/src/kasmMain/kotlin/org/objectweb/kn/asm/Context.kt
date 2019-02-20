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
 * Information about a class being parsed in a [ClassReader].
 *
 * @author Eric Bruneton
 */
internal class Context {

    /**
     * Prototypes of the attributes that must be parsed for this class.
     */
    var attrs: Array<Attribute?>? = null

    /**
     * The [ClassReader] option flags for the parsing of this class.
     */
    var flags: Int = 0

    /**
     * The buffer used to read strings.
     */
    var buffer: CharArray? = null

    /**
     * The start index of each bootstrap method.
     */
    var bootstrapMethods: IntArray? = null

    /**
     * The access flags of the method currently being parsed.
     */
    var access: Int = 0

    /**
     * The name of the method currently being parsed.
     */
    var name: String? = null

    /**
     * The descriptor of the method currently being parsed.
     */
    var desc: String? = null

    /**
     * The label objects, indexed by bytecode offset, of the method currently
     * being parsed (only bytecode offsets for which a label is needed have a
     * non null associated Label object).
     */
    var labels: Array<Label?>? = null

    /**
     * The target of the type annotation currently being parsed.
     */
    var typeRef: Int = 0

    /**
     * The path of the type annotation currently being parsed.
     */
    var typePath: TypePath? = null

    /**
     * The offset of the latest stack map frame that has been parsed.
     */
    var offset: Int = 0

    /**
     * The labels corresponding to the start of the local variable ranges in the
     * local variable type annotation currently being parsed.
     */
    var start: Array<Label?>? = null

    /**
     * The labels corresponding to the end of the local variable ranges in the
     * local variable type annotation currently being parsed.
     */
    var end: Array<Label?>? = null

    /**
     * The local variable indices for each local variable range in the local
     * variable type annotation currently being parsed.
     */
    var index: IntArray? = null

    /**
     * The encoding of the latest stack map frame that has been parsed.
     */
    var mode: Int = 0

    /**
     * The number of locals in the latest stack map frame that has been parsed.
     */
    var localCount: Int = 0

    /**
     * The number locals in the latest stack map frame that has been parsed,
     * minus the number of locals in the previous frame.
     */
    var localDiff: Int = 0

    /**
     * The local values of the latest stack map frame that has been parsed.
     */
    var local: Array<Any?>? = null

    /**
     * The stack size of the latest stack map frame that has been parsed.
     */
    var stackCount: Int = 0

    /**
     * The stack values of the latest stack map frame that has been parsed.
     */
    var stack: Array<Any?>? = null
}