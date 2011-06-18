/*
 * @(#)PolicyTreeElement.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml;

import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import com.sun.xacml.ctx.Result;

/**
 * This represents a single node in a policy tree. A node is either a policy set, a policy, or a
 * rule. This interface is used to interact with these node types in a general way. Note that rules
 * are leaf nodes in a policy tree as they never contain children.
 * 
 * @since 1.1
 * @author seth proctor
 */
public interface PolicyTreeElement {

    /**
     * Returns the <code>List</code> of <code>PolicyTreeElement</code> objects that are the children
     * of this node. If this node has no children then this list is empty. The children are returned
     * as a <code>List</code> instead of some unordered collection because in cases like combining
     * or evaluation the order is often important.
     * 
     * @return the non-null <code>List</code> of children of this node
     */
    public List<PolicyTreeElement> getChildren();

    /**
     * Returns the given description of this element or null if there is no description
     * 
     * @return the description or null
     */
    public String getDescription();

    /**
     * Returns the id of this element
     * 
     * @return the element's identifier
     */
    public URI getId();

    /**
     * Returns the target for this element or null if there is no target
     * 
     * @return the element's target
     */
    public Target getTarget();

    /**
     * Given the input context sees whether or not the request matches this element's target. The
     * rules for matching are different depending on the type of element being matched.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of trying to match this element and the request
     */
    public MatchResult match(EvaluationCtx context);

    /**
     * Evaluates this element in the policy tree, and therefore all elements underneath this
     * element. The rules for evaluation are different depending on the type of element being
     * evaluated.
     * 
     * @param context
     *            the representation of the request we're evaluating
     * 
     * @return the result of the evaluation
     */
    public Result evaluate(EvaluationCtx context);

    /**
     * Encodes this element into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output);

    /**
     * Encodes this element into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter);

}
