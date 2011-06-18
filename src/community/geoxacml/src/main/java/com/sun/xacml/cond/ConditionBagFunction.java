/*
 * @(#)ConditionBagFunction.java
 *
 * Copyright 2004-206 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.BooleanAttribute;

/**
 * Specific <code>BagFunction</code> class that supports the single condition bag function:
 * type-is-in.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ConditionBagFunction extends BagFunction {

    // mapping of function name to its associated argument type
    private static HashMap<String, String[]> argMap;

    /**
     * Static initializer that sets up the argument info for all the supported functions.
     */
    static {
        argMap = new HashMap<String, String[]>();

        for (int i = 0; i < baseTypes.length; i++) {
            String[] args = { baseTypes[i], baseTypes[i] };

            argMap.put(FUNCTION_NS + simpleTypes[i] + NAME_BASE_IS_IN, args);
        }

        for (int i = 0; i < baseTypes2.length; i++) {
            String[] args = { baseTypes2[i], baseTypes2[i] };

            argMap.put(FUNCTION_NS_2 + simpleTypes2[i] + NAME_BASE_IS_IN, args);
        }
    }

    /**
     * Constructor that is used to create one of the condition standard bag functions. The name
     * supplied must be one of the standard XACML functions supported by this class, including the
     * full namespace, otherwise an exception is thrown. Look in <code>BagFunction</code> for
     * details about the supported names.
     * 
     * @param functionName
     *            the name of the function to create
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public ConditionBagFunction(String functionName) {
        super(functionName, 0, getArguments(functionName));
    }

    /**
     * Constructor that is used to create instances of condition bag functions for new
     * (non-standard) datatypes. This is equivalent to using the <code>getInstance</code> methods in
     * <code>BagFunction</code> and is generally only used by the run-time configuration code.
     * 
     * @param functionName
     *            the name of the new function
     * @param datatype
     *            the full identifier for the supported datatype
     */
    public ConditionBagFunction(String functionName, String datatype) {
        super(functionName, 0, new String[] { datatype, datatype });
    }

    /**
     * Private helper that returns the argument types for the given standard function.
     */
    private static String[] getArguments(String functionName) {
        String[] args = (String[]) (argMap.get(functionName));

        if (args == null)
            throw new IllegalArgumentException("unknown bag function: " + functionName);

        return args;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        return Collections.unmodifiableSet(argMap.keySet());
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

        // *-is-in takes a bag and an element of baseType and
        // returns a single boolean value
        AttributeValue item = (AttributeValue) (argValues[0]);
        BagAttribute bag = (BagAttribute) (argValues[1]);

        return new EvaluationResult(BooleanAttribute.getInstance(bag.contains(item)));
    }

}
