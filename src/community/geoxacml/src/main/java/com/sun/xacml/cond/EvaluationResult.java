/*
 * @(#)EvaluationResult.java
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

package com.sun.xacml.cond;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.ctx.Status;

/**
 * This is used in cases where a normal result is some AttributeValue, but if an attribute couldn't
 * be resolved (or some other problem occurred), then a Status object needs to be returned instead.
 * This is used instead of throwing an exception for performance, but mainly because failure to
 * resolve an attribute is not an error case for the code, merely for the evaluation, and represents
 * normal operation. Separate exception types will be added later to represent errors in pdp
 * operation.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class EvaluationResult {

    //
    private boolean wasInd;

    private AttributeValue value;

    private Status status;

    /**
     * Single instances of EvaluationResults with false and true BooleanAttributes in them. This
     * avoids the need to create new objects when performing boolean operations, which we do a lot
     * of.
     */
    private static EvaluationResult falseBooleanResult;

    private static EvaluationResult trueBooleanResult;

    /**
     * Constructor that creates an <code>EvaluationResult</code> containing a single
     * <code>AttributeValue</code>
     * 
     * @param value
     *            the attribute value
     */
    public EvaluationResult(AttributeValue value) {
        wasInd = false;
        this.value = value;
        this.status = null;
    }

    /**
     * Constructor that creates an <code>EvaluationResult</code> of Indeterminate, including Status
     * data.
     * 
     * @param status
     *            the error information
     */
    public EvaluationResult(Status status) {
        wasInd = true;
        this.value = null;
        this.status = status;
    }

    /**
     * Returns true if the result was indeterminate
     * 
     * @return true if there was an error
     */
    public boolean indeterminate() {
        return wasInd;
    }

    /**
     * Returns the attribute value, or null if there was an error
     * 
     * @return the attribute value or null
     */
    public AttributeValue getAttributeValue() {
        return value;
    }

    /**
     * Returns the status if there was an error, or null it no error occurred
     * 
     * @return the status or null
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns an <code>EvaluationResult</code> that represents the boolean value provided.
     * 
     * @param value
     *            a boolean representing the desired value
     * @return an <code>EvaluationResult</code> representing the appropriate value
     */
    public static EvaluationResult getInstance(boolean value) {
        if (value)
            return getTrueInstance();
        else
            return getFalseInstance();
    }

    /**
     * Returns an <code>EvaluationResult</code> that represents a false value.
     * 
     * @return an <code>EvaluationResult</code> representing a false value
     */
    public static EvaluationResult getFalseInstance() {
        if (falseBooleanResult == null) {
            falseBooleanResult = new EvaluationResult(BooleanAttribute.getFalseInstance());
        }
        return falseBooleanResult;
    }

    /**
     * Returns an <code>EvaluationResult</code> that represents a true value.
     * 
     * @return an <code>EvaluationResult</code> representing a true value
     */
    public static EvaluationResult getTrueInstance() {
        if (trueBooleanResult == null) {
            trueBooleanResult = new EvaluationResult(BooleanAttribute.getTrueInstance());
        }
        return trueBooleanResult;
    }
}
