/*
 * @(#)BagFunction.java
 *
 * Copyright 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
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
import java.util.Set;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.Base64BinaryAttribute;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DNSNameAttribute;
import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.DayTimeDurationAttribute;
import com.sun.xacml.attr.DoubleAttribute;
import com.sun.xacml.attr.HexBinaryAttribute;
import com.sun.xacml.attr.IPAddressAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.RFC822NameAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.TimeAttribute;
import com.sun.xacml.attr.X500NameAttribute;
import com.sun.xacml.attr.YearMonthDurationAttribute;

/**
 * Represents all of the Bag functions, though the actual implementations are in two sub-classes
 * specific to the condition and general bag functions.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public abstract class BagFunction extends FunctionBase {

    /**
     * Base name for the type-one-and-only funtions. To get the standard identifier for a given
     * type, use <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g.,
     * <code>string</code>) + </code>NAME_BASE_ONE_AND_ONLY</code>.
     */
    public static final String NAME_BASE_ONE_AND_ONLY = "-one-and-only";

    /**
     * Base name for the type-bag-size funtions. To get the standard identifier for a given type,
     * use <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g.,
     * <code>string</code>) + </code>NAME_BASE_BAG_SIZE</code>.
     */
    public static final String NAME_BASE_BAG_SIZE = "-bag-size";

    /**
     * Base name for the type-is-in. To get the standard identifier for a given type, use
     * <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g., <code>string</code>)
     * + </code>NAME_BASE_IS_IN</code>.
     */
    public static final String NAME_BASE_IS_IN = "-is-in";

    /**
     * Base name for the type-bag funtions. To get the standard identifier for a given type, use
     * <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g., <code>string</code>)
     * + </code>NAME_BASE_BAG</code>.
     */
    public static final String NAME_BASE_BAG = "-bag";

    // bag parameter info for the functions that accept multiple args
    private static final boolean bagParams[] = { false, true };

    /**
     * A complete list of all the XACML 1.x datatypes supported by the Bag functions
     */
    protected static String baseTypes[] = { StringAttribute.identifier,
            BooleanAttribute.identifier, IntegerAttribute.identifier, DoubleAttribute.identifier,
            DateAttribute.identifier, DateTimeAttribute.identifier, TimeAttribute.identifier,
            AnyURIAttribute.identifier, HexBinaryAttribute.identifier,
            Base64BinaryAttribute.identifier, DayTimeDurationAttribute.identifier,
            YearMonthDurationAttribute.identifier, X500NameAttribute.identifier,
            RFC822NameAttribute.identifier };

    /**
     * A complete list of all the XACML 2.0 datatypes newly supported by the Bag functions
     */
    protected static String baseTypes2[] = { IPAddressAttribute.identifier,
            DNSNameAttribute.identifier };

    /**
     * A complete list of all the 1.x XACML datatypes supported by the Bag functions, using the
     * "simple" form of the names (eg, string instead of http://www.w3.org/2001/XMLSchema#string)
     */
    protected static String simpleTypes[] = { "string", "boolean", "integer", "double", "date",
            "dateTime", "time", "anyURI", "hexBinary", "base64Binary", "dayTimeDuration",
            "yearMonthDuration", "x500Name", "rfc822Name" };

    /**
     * A complete list of all the 2.0 XACML datatypes newly supported by the Bag functions, using
     * the "simple" form of the names (eg, string instead of
     * http://www.w3.org/2001/XMLSchema#string)
     */
    protected static String simpleTypes2[] = { "ipAddress", "dnsName" };

    /**
     * Returns a new <code>BagFunction</code> that provides the type-one-and-only functionality over
     * the given attribute type. This should be used to create new function instances for any new
     * attribute types, and the resulting object should be put into the <code>FunctionFactory</code>
     * (instances already exist in the factory for the standard attribute types).
     * 
     * @param functionName
     *            the name to use for the function
     * @param argumentType
     *            the type to operate on
     * 
     * @return a new <code>BagFunction</code>
     */
    public static BagFunction getOneAndOnlyInstance(String functionName, String argumentType) {
        return new GeneralBagFunction(functionName, argumentType, NAME_BASE_ONE_AND_ONLY);
    }

    /**
     * Returns a new <code>BagFunction</code> that provides the type-bag-size functionality over the
     * given attribute type. This should be used to create new function instances for any new
     * attribute types, and the resulting object should be put into the <code>FunctionFactory</code>
     * (instances already exist in the factory for the standard attribute types).
     * 
     * @param functionName
     *            the name to use for the function
     * @param argumentType
     *            the type to operate on
     * 
     * @return a new <code>BagFunction</code>
     */
    public static BagFunction getBagSizeInstance(String functionName, String argumentType) {
        return new GeneralBagFunction(functionName, argumentType, NAME_BASE_BAG_SIZE);
    }

    /**
     * Returns a new <code>BagFunction</code> that provides the type-is-in functionality over the
     * given attribute type. This should be used to create new function instances for any new
     * attribute types, and the resulting object should be put into the <code>FunctionFactory</code>
     * (instances already exist in the factory for the standard attribute types).
     * 
     * @param functionName
     *            the name to use for the function
     * @param argumentType
     *            the type to operate on
     * 
     * @return a new <code>BagFunction</code>
     */
    public static BagFunction getIsInInstance(String functionName, String argumentType) {
        return new ConditionBagFunction(functionName, argumentType);
    }

    /**
     * Returns a new <code>BagFunction</code> that provides the type-bag functionality over the
     * given attribute type. This should be used to create new function instances for any new
     * attribute types, and the resulting object should be put into the <code>FunctionFactory</code>
     * (instances already exist in the factory for the standard attribute types).
     * 
     * @param functionName
     *            the name to use for the function
     * @param argumentType
     *            the type to operate on
     * 
     * @return a new <code>BagFunction</code>
     */
    public static BagFunction getBagInstance(String functionName, String argumentType) {
        return new GeneralBagFunction(functionName, argumentType, NAME_BASE_BAG);
    }

    /**
     * Protected constuctor used by the general and condition subclasses to create a non-boolean
     * function with parameters of the same datatype. If you need to create a new
     * <code>BagFunction</code> instance you should either use one of the <code>getInstance</code>
     * methods or construct one of the sub-classes directly.
     * 
     * @param functionName
     *            the identitifer for the function
     * @param functionId
     *            an optional, internal numeric identifier
     * @param paramType
     *            the datatype this function accepts
     * @param paramIsBag
     *            whether the parameters are bags
     * @param numParams
     *            number of parameters allowed or -1 for any number
     * @param returnType
     *            the datatype this function returns
     * @param returnsBag
     *            whether this function returns bags
     */
    protected BagFunction(String functionName, int functionId, String paramType,
            boolean paramIsBag, int numParams, String returnType, boolean returnsBag) {
        super(functionName, functionId, paramType, paramIsBag, numParams, returnType, returnsBag);
    }

    /**
     * Protected constuctor used by the general and condition subclasses to create a boolean
     * function with parameters of different datatypes. If you need to create a new
     * <code>BagFunction</code> instance you should either use one of the <code>getInstance</code>
     * methods or construct one of the sub-classes directly.
     * 
     * @param functionName
     *            the identitifer for the function
     * @param functionId
     *            an optional, internal numeric identifier
     * @param paramTypes
     *            the datatype of each parameter
     */
    protected BagFunction(String functionName, int functionId, String[] paramTypes) {
        super(functionName, functionId, paramTypes, bagParams, BooleanAttribute.identifier, false);
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.addAll(ConditionBagFunction.getSupportedIdentifiers());
        set.addAll(GeneralBagFunction.getSupportedIdentifiers());

        return set;
    }

}
