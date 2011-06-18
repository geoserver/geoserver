/*
 * @(#)GeneralBagFunction.java
 *
 * Copyright 2004-2006 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.IntegerAttribute;

/**
 * Specific <code>BagFunction</code> class that supports all of the general-purpose bag functions:
 * type-one-and-only, type-bag-size, and type-bag.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class GeneralBagFunction extends BagFunction {

    // private identifiers for the supported functions
    private static final int ID_BASE_ONE_AND_ONLY = 0;

    private static final int ID_BASE_BAG_SIZE = 1;

    private static final int ID_BASE_BAG = 2;

    // mapping of function name to its associated parameters
    private static HashMap<String, BagParameters> paramMap;

    private static Set<String> supportedIds;

    /**
     * Static initializer that sets up the paramater info for all the supported functions.
     */
    static {
        paramMap = new HashMap<String, BagParameters>();

        for (int i = 0; i < baseTypes.length; i++) {
            String baseType = baseTypes[i];
            String functionBaseName = FUNCTION_NS + simpleTypes[i];

            paramMap.put(functionBaseName + NAME_BASE_ONE_AND_ONLY, new BagParameters(
                    ID_BASE_ONE_AND_ONLY, baseType, true, 1, baseType, false));

            paramMap.put(functionBaseName + NAME_BASE_BAG_SIZE, new BagParameters(ID_BASE_BAG_SIZE,
                    baseType, true, 1, IntegerAttribute.identifier, false));

            paramMap.put(functionBaseName + NAME_BASE_BAG, new BagParameters(ID_BASE_BAG, baseType,
                    false, -1, baseType, true));
        }

        for (int i = 0; i < baseTypes2.length; i++) {
            String baseType = baseTypes2[i];
            String functionBaseName = FUNCTION_NS_2 + simpleTypes2[i];

            paramMap.put(functionBaseName + NAME_BASE_ONE_AND_ONLY, new BagParameters(
                    ID_BASE_ONE_AND_ONLY, baseType, true, 1, baseType, false));

            paramMap.put(functionBaseName + NAME_BASE_BAG_SIZE, new BagParameters(ID_BASE_BAG_SIZE,
                    baseType, true, 1, IntegerAttribute.identifier, false));

            paramMap.put(functionBaseName + NAME_BASE_BAG, new BagParameters(ID_BASE_BAG, baseType,
                    false, -1, baseType, true));
        }

        supportedIds = Collections.unmodifiableSet(new HashSet<String>(paramMap.keySet()));

        paramMap.put(NAME_BASE_ONE_AND_ONLY, new BagParameters(ID_BASE_ONE_AND_ONLY, null, true, 1,
                null, false));
        paramMap.put(NAME_BASE_BAG_SIZE, new BagParameters(ID_BASE_BAG_SIZE, null, true, 1,
                IntegerAttribute.identifier, false));
        paramMap.put(NAME_BASE_BAG, new BagParameters(ID_BASE_BAG, null, false, -1, null, true));

    };

    /**
     * Constructor that is used to create one of the general-purpose standard bag functions. The
     * name supplied must be one of the standard XACML functions supported by this class, including
     * the full namespace, otherwise an exception is thrown. Look in <code>BagFunction</code> for
     * details about the supported names.
     * 
     * @param functionName
     *            the name of the function to create
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public GeneralBagFunction(String functionName) {
        super(functionName, getId(functionName), getArgumentType(functionName),
                getIsBag(functionName), getNumArgs(functionName), getReturnType(functionName),
                getReturnsBag(functionName));
    }

    /**
     * Constructor that is used to create instances of general-purpose bag functions for new
     * (non-standard) datatypes. This is equivalent to using the <code>getInstance</code> methods in
     * <code>BagFunction</code> and is generally only used by the run-time configuration code.
     * 
     * @param functionName
     *            the name of the new function
     * @param datatype
     *            the full identifier for the supported datatype
     * @param functionType
     *            which kind of Bag function, based on the <code>NAME_BASE_*</code> fields
     */
    public GeneralBagFunction(String functionName, String datatype, String functionType) {
        super(functionName, getId(functionType), datatype, getIsBag(functionType),
                getNumArgs(functionType), getCustomReturnType(functionType, datatype),
                getReturnsBag(functionType));
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        BagParameters params = (BagParameters) (paramMap.get(functionName));

        if (params == null)
            throw new IllegalArgumentException("unknown bag function: " + functionName);

        return params.id;
    }

    /**
     * Private helper that returns the argument type for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String getArgumentType(String functionName) {
        return ((BagParameters) (paramMap.get(functionName))).arg;
    }

    /**
     * Private helper that returns if the given standard function takes a bag. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static boolean getIsBag(String functionName) {
        return ((BagParameters) (paramMap.get(functionName))).argIsBag;
    }

    /**
     * Private helper that returns the argument count for the given standard function. Note that
     * this doesn't check on the return value since the method always is called after getId, so we
     * assume that the function is present.
     */
    private static int getNumArgs(String functionName) {
        return ((BagParameters) (paramMap.get(functionName))).params;
    }

    /**
     * Private helper that returns the return type for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String getReturnType(String functionName) {
        return ((BagParameters) (paramMap.get(functionName))).returnType;
    }

    /**
     * Private helper that returns if the return type is a bag for the given standard function. Note
     * that this doesn't check on the return value since the method always is called after getId, so
     * we assume that the function is present.
     */
    private static boolean getReturnsBag(String functionName) {
        return ((BagParameters) (paramMap.get(functionName))).returnsBag;
    }

    /**
     * Private helper used by the custom datatype constructor to figure out what the return type is.
     * Note that this doesn't check on the return value since the method always is called after
     * getId, so we assume that the function is present.
     */
    private static String getCustomReturnType(String functionType, String datatype) {
        String ret = ((BagParameters) (paramMap.get(functionType))).returnType;

        if (ret == null)
            return datatype;
        else
            return ret;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        return supportedIds;
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

        // Now that we have real values, perform the requested operation.
        AttributeValue attrResult = null;

        switch (getFunctionId()) {

        // *-one-and-only takes a single bag and returns a
        // single value of baseType
        case ID_BASE_ONE_AND_ONLY: {
            BagAttribute bag = (BagAttribute) (argValues[0]);

            if (bag.size() != 1)
                return makeProcessingError(getFunctionName() + " expects "
                        + "a bag that contains a single " + "element, got a bag with " + bag.size()
                        + " elements");

            attrResult = (AttributeValue) (bag.iterator().next());
            break;
        }

            // *-size takes a single bag and returns an integer
        case ID_BASE_BAG_SIZE: {
            BagAttribute bag = (BagAttribute) (argValues[0]);

            attrResult = new IntegerAttribute(bag.size());
            break;
        }

            // *-bag takes any number of elements of baseType and
            // returns a bag containing those elements
        case ID_BASE_BAG: {
            List<AttributeValue> argsList = Arrays.asList(argValues);

            attrResult = new BagAttribute(getReturnType(), argsList);
            break;
        }
        }

        return new EvaluationResult(attrResult);
    }

    /**
     * Private class that is used for mapping each function to it set of parameters.
     */
    private static class BagParameters {
        public int id;

        public String arg;

        public boolean argIsBag;

        public int params;

        public String returnType;

        public boolean returnsBag;

        public BagParameters(int id, String arg, boolean argIsBag, int params, String returnType,
                boolean returnsBag) {
            this.id = id;
            this.arg = arg;
            this.argIsBag = argIsBag;
            this.params = params;
            this.returnType = returnType;
            this.returnsBag = returnsBag;
        }
    }

}
