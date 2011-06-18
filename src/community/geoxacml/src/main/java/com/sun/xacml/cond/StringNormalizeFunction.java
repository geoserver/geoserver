/*
 * @(#)StringNormalizeFunction.java
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
import com.sun.xacml.attr.StringAttribute;

/**
 * A class that implements all the string conversion functions (string-normalize-space and
 * string-normalize-to-lower-case). It takes string argument, normalizes that value, and returns the
 * result. If the argument is indeterminate, an indeterminate result is returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class StringNormalizeFunction extends FunctionBase {

    /**
     * Standard identifier for the string-normalize-space function.
     */
    public static final String NAME_STRING_NORMALIZE_SPACE = FUNCTION_NS + "string-normalize-space";

    /**
     * Standard identifier for the string-normalize-to-lower-case function.
     */
    public static final String NAME_STRING_NORMALIZE_TO_LOWER_CASE = FUNCTION_NS
            + "string-normalize-to-lower-case";

    // private identifiers for the supported functions
    private static final int ID_STRING_NORMALIZE_SPACE = 0;

    private static final int ID_STRING_NORMALIZE_TO_LOWER_CASE = 1;

    /**
     * Creates a new <code>StringNormalizeFunction</code> object.
     * 
     * @param functionName
     *            the standard XACML name of the function to be handled by this object, including
     *            the full namespace
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public StringNormalizeFunction(String functionName) {
        super(functionName, getId(functionName), StringAttribute.identifier, false, 1,
                StringAttribute.identifier, false);
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        if (functionName.equals(NAME_STRING_NORMALIZE_SPACE))
            return ID_STRING_NORMALIZE_SPACE;
        else if (functionName.equals(NAME_STRING_NORMALIZE_TO_LOWER_CASE))
            return ID_STRING_NORMALIZE_TO_LOWER_CASE;
        else
            throw new IllegalArgumentException("unknown normalize function " + functionName);
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_STRING_NORMALIZE_SPACE);
        set.add(NAME_STRING_NORMALIZE_TO_LOWER_CASE);

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

        // Now that we have real values, perform the numeric conversion
        // operation in the manner appropriate for this function.
        switch (getFunctionId()) {
        case ID_STRING_NORMALIZE_SPACE: {
            String str = ((StringAttribute) argValues[0]).getValue();

            // Trim whitespace from start and end of string
            int startIndex = 0;
            int endIndex = str.length() - 1;
            while ((startIndex <= endIndex) && Character.isWhitespace(str.charAt(startIndex)))
                startIndex++;
            while ((startIndex <= endIndex) && Character.isWhitespace(str.charAt(endIndex)))
                endIndex--;
            String strResult = str.substring(startIndex, endIndex + 1);

            result = new EvaluationResult(new StringAttribute(strResult));
            break;
        }
        case ID_STRING_NORMALIZE_TO_LOWER_CASE: {
            String str = ((StringAttribute) argValues[0]).getValue();

            // Convert string to lower case
            String strResult = str.toLowerCase();

            result = new EvaluationResult(new StringAttribute(strResult));
            break;
        }
        }

        return result;
    }
}
