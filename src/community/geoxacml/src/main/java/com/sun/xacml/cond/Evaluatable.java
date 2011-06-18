/*
 * @(#)Evaluatable.java
 *
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.cond;

import java.util.List;

import com.sun.xacml.EvaluationCtx;

/**
 * Generic interface that is implemented by all objects that can be evaluated directly (
 * <code>AttributeDesignator</code>, <code>Apply</code>, <code>AttributeValue</code>, etc.). As of
 * version 2.0 several methods were extracted to the new <code>Expression</code> super-interface.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public interface Evaluatable extends Expression {

    /**
     * Evaluates the object using the given context, and either returns an error or a resulting
     * value.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of evaluation
     */
    public EvaluationResult evaluate(EvaluationCtx context);

    /**
     * Tells whether evaluation will return a bag or a single value.
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @return true if evaluation will return a bag, false otherwise
     */
    public boolean evaluatesToBag();

    /**
     * Returns all children, in order, of this element in the Condition tree, or en empty set if
     * this element has no children. In XACML 1.x, only the ApplyType ever has children.
     * 
     * @return a <code>List</code> of <code>Evaluatable</code>s
     */
    public List<Expression> getChildren();

}
