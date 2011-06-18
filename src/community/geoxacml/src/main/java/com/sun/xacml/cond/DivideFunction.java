/*
 * @(#)DivideFunction.java
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
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DoubleAttribute;
import com.sun.xacml.attr.IntegerAttribute;

/**
 * A class that implements all the *-divide functions. It takes two operands of the appropriate type
 * and returns the quotient of the operands. If either of the operands is indeterminate, an
 * indeterminate result is returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class DivideFunction extends FunctionBase {

    /**
     * Standard identifier for the integer-divide function.
     */
    public static final String NAME_INTEGER_DIVIDE = FUNCTION_NS + "integer-divide";

    /**
     * Standard identifier for the double-divide function.
     */
    public static final String NAME_DOUBLE_DIVIDE = FUNCTION_NS + "double-divide";

    // inernal identifiers for each of the supported functions
    private static final int ID_INTEGER_DIVIDE = 0;

    private static final int ID_DOUBLE_DIVIDE = 1;

    /**
     * Creates a new <code>DivideFunction</code> object.
     * 
     * @param functionName
     *            the standard XACML name of the function to be handled by this object, including
     *            the full namespace
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public DivideFunction(String functionName) {
        super(functionName, getId(functionName), getArgumentType(functionName), false, 2,
                getArgumentType(functionName), false);
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        if (functionName.equals(NAME_INTEGER_DIVIDE))
            return ID_INTEGER_DIVIDE;
        else if (functionName.equals(NAME_DOUBLE_DIVIDE))
            return ID_DOUBLE_DIVIDE;
        else
            throw new IllegalArgumentException("unknown divide function " + functionName);
    }

    /**
     * Private helper that returns the type used for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String getArgumentType(String functionName) {
        if (functionName.equals(NAME_INTEGER_DIVIDE))
            return IntegerAttribute.identifier;
        else
            return DoubleAttribute.identifier;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_INTEGER_DIVIDE);
        set.add(NAME_DOUBLE_DIVIDE);

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

        // Evaluate the arguments
        AttributeValue[] argValues = new AttributeValue[inputs.size()];
        EvaluationResult result = evalArgs(inputs, context, argValues);
        if (result != null)
            return result;

        // Now that we have real values, perform the divide operation
        // in the manner appropriate for the type of the arguments.
        switch (getFunctionId()) {
        case ID_INTEGER_DIVIDE: {
            long dividend = ((IntegerAttribute) argValues[0]).getValue();
            long divisor = ((IntegerAttribute) argValues[1]).getValue();

            if (divisor == 0) {
                result = makeProcessingError("divide by zero");
                break;
            }

            long quotient = dividend / divisor;

            result = new EvaluationResult(new IntegerAttribute(quotient));
            break;
        }
        case ID_DOUBLE_DIVIDE: {
            double dividend = ((DoubleAttribute) argValues[0]).getValue();
            double divisor = ((DoubleAttribute) argValues[1]).getValue();

            if (divisor == 0) {
                result = makeProcessingError("divide by zero");
                break;
            }

            double quotient = dividend / divisor;

            result = new EvaluationResult(new DoubleAttribute(quotient));
            break;
        }
        }

        return result;
    }
}
