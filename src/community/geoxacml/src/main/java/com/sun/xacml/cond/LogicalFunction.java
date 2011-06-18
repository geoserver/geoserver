/*
 * @(#)LogicalFunction.java
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;

/**
 * A class that implements the logical functions "or" and "and". These functions take any number of
 * boolean arguments and evaluate them one at a time, starting with the first argument. As soon as
 * the result of the function can be determined, evaluation stops and that result is returned.
 * During this process, if any argument evaluates to indeterminate, an indeterminate result is
 * returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class LogicalFunction extends FunctionBase {

    /**
     * Standard identifier for the or function.
     */
    public static final String NAME_OR = FUNCTION_NS + "or";

    /**
     * Standard identifier for the and function.
     */
    public static final String NAME_AND = FUNCTION_NS + "and";

    // internal identifiers for each of the supported functions
    private static final int ID_OR = 0;

    private static final int ID_AND = 1;

    /**
     * Creates a new <code>LogicalFunction</code> object.
     * 
     * @param functionName
     *            the standard XACML name of the function to be handled by this object, including
     *            the full namespace
     * 
     * @throws IllegalArgumentException
     *             if the functionName is unknown
     */
    public LogicalFunction(String functionName) {
        super(functionName, getId(functionName), BooleanAttribute.identifier, false, -1,
                BooleanAttribute.identifier, false);
    }

    /**
     * Private helper that looks up the private id based on the function name.
     */
    private static int getId(String functionName) {
        if (functionName.equals(NAME_OR))
            return ID_OR;
        else if (functionName.equals(NAME_AND))
            return ID_AND;
        else
            throw new IllegalArgumentException("unknown logical function: " + functionName);
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_OR);
        set.add(NAME_AND);

        return set;
    }

    /**
     * Evaluate the function, using the specified parameters.
     * 
     * @param inputs
     *            a <code>List</code> of <code>Evaluatable</code> objects representing the arguments
     *            passed to the function
     * @param context
     *            an <code>EvaluationCtx</code> so that the <code>Evaluatable</code> objects can be
     *            evaluated
     * @return an <code>EvaluationResult</code> representing the function's result
     */
    public EvaluationResult evaluate(List<? extends Expression> inputs, EvaluationCtx context) {

        // Evaluate the arguments one by one. As soon as we can
        // return a result, do so. Return Indeterminate if any argument
        // evaluated is indeterminate.
        Iterator<? extends Expression> it = inputs.iterator();
        while (it.hasNext()) {
            Evaluatable eval = (Evaluatable) (it.next());

            // Evaluate the argument
            EvaluationResult result = eval.evaluate(context);
            if (result.indeterminate())
                return result;

            AttributeValue value = result.getAttributeValue();
            boolean argBooleanValue = ((BooleanAttribute) value).getValue();

            switch (getFunctionId()) {
            case ID_OR:
                if (argBooleanValue)
                    return EvaluationResult.getTrueInstance();
                break;
            case ID_AND:
                if (!argBooleanValue)
                    return EvaluationResult.getFalseInstance();
                break;
            }
        }

        if (getFunctionId() == ID_OR)
            return EvaluationResult.getFalseInstance();
        else
            return EvaluationResult.getTrueInstance();
    }
}
