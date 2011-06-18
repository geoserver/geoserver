/*
 * @(#)SetFunction.java
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
 * Represents all of the Set functions, though the actual implementations are in two sub-classes
 * specific to the condition and general set functions.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public abstract class SetFunction extends FunctionBase {

    /**
     * Base name for the type-intersection funtions. To get the standard identifier for a given
     * type, use <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g.,
     * <code>string</code>) + </code>NAME_BASE_INTERSECTION</code>.
     */
    public static final String NAME_BASE_INTERSECTION = "-intersection";

    /**
     * Base name for the type-at-least-one-member-of funtions. To get the standard identifier for a
     * given type, use <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g.,
     * <code>string</code>) + </code>NAME_BASE_AT_LEAST_ONE_MEMBER_OF</code>.
     */
    public static final String NAME_BASE_AT_LEAST_ONE_MEMBER_OF = "-at-least-one-member-of";

    /**
     * Base name for the type-union funtions. To get the standard identifier for a given type, use
     * <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g., <code>string</code>)
     * + </code>NAME_BASE_UNION</code>.
     */
    public static final String NAME_BASE_UNION = "-union";

    /**
     * Base name for the type-subset funtions. To get the standard identifier for a given type, use
     * <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g., <code>string</code>)
     * + </code>NAME_BASE_SUBSET</code>.
     */
    public static final String NAME_BASE_SUBSET = "-subset";

    /**
     * Base name for the type-set-equals funtions. To get the standard identifier for a given type,
     * use <code>FunctionBase.FUNCTION_NS</code> + the datatype's base name (e.g.,
     * <code>string</code>) + </code>NAME_BASE_SET_EQUALS</code>.
     */
    public static final String NAME_BASE_SET_EQUALS = "-set-equals";

    /**
     * A complete list of all the XACML datatypes supported by the Set functions in XACML 1.x
     */
    protected static String baseTypes[] = { StringAttribute.identifier,
            BooleanAttribute.identifier, IntegerAttribute.identifier, DoubleAttribute.identifier,
            DateAttribute.identifier, DateTimeAttribute.identifier, TimeAttribute.identifier,
            AnyURIAttribute.identifier, HexBinaryAttribute.identifier,
            Base64BinaryAttribute.identifier, DayTimeDurationAttribute.identifier,
            YearMonthDurationAttribute.identifier, X500NameAttribute.identifier,
            RFC822NameAttribute.identifier };

    /**
     * A complete list of all the XACML datatypes newly supported by the Set functions in XACML 2.0
     */
    protected static String baseTypes2[] = { IPAddressAttribute.identifier,
            DNSNameAttribute.identifier };

    /**
     * A complete list of all the XACML datatypes supported by the Set functions in XACML 1.x, using
     * the "simple" form of the names (eg, string instead of
     * http://www.w3.org/2001/XMLSchema#string)
     */
    protected static String simpleTypes[] = { "string", "boolean", "integer", "double", "date",
            "dateTime", "time", "anyURI", "hexBinary", "base64Binary", "dayTimeDuration",
            "yearMonthDuration", "x500Name", "rfc822Name" };

    /**
     * A complete list of all the XACML datatypes newly supported by the Set functions in XACML 2.0,
     * using the "simple" form of the names (eg, string instead of
     * http://www.w3.org/2001/XMLSchema#string)
     */
    protected static String simpleTypes2[] = { "ipAddress", "dnsName" };

    /**
     * Creates a new instance of the intersection set function. This should be used to create
     * support for any new attribute types and then the new <code>SetFunction</code> object should
     * be added to the factory (all set functions for the base types are already installed in the
     * factory).
     * 
     * @param functionName
     *            the name of the function
     * @param argumentType
     *            the attribute type this function will work with
     * 
     * @return a new <code>SetFunction</code> for the given type
     */
    public static SetFunction getIntersectionInstance(String functionName, String argumentType) {
        return new GeneralSetFunction(functionName, argumentType, NAME_BASE_INTERSECTION);
    }

    /**
     * Creates a new instance of the at-least-one-member-of set function. This should be used to
     * create support for any new attribute types and then the new <code>SetFunction</code> object
     * should be added to the factory (all set functions for the base types are already installed in
     * the factory).
     * 
     * @param functionName
     *            the name of the function
     * @param argumentType
     *            the attribute type this function will work with
     * 
     * @return a new <code>SetFunction</code> for the given type
     */
    public static SetFunction getAtLeastOneInstance(String functionName, String argumentType) {
        return new ConditionSetFunction(functionName, argumentType,
                NAME_BASE_AT_LEAST_ONE_MEMBER_OF);
    }

    /**
     * Creates a new instance of the union set function. This should be used to create support for
     * any new attribute types and then the new <code>SetFunction</code> object should be added to
     * the factory (all set functions for the base types are already installed in the factory).
     * 
     * @param functionName
     *            the name of the function
     * @param argumentType
     *            the attribute type this function will work with
     * 
     * @return a new <code>SetFunction</code> for the given type
     */
    public static SetFunction getUnionInstance(String functionName, String argumentType) {
        return new GeneralSetFunction(functionName, argumentType, NAME_BASE_UNION);
    }

    /**
     * Creates a new instance of the subset set function. This should be used to create support for
     * any new attribute types and then the new <code>SetFunction</code> object should be added to
     * the factory (all set functions for the base types are already installed in the factory).
     * 
     * @param functionName
     *            the name of the function
     * @param argumentType
     *            the attribute type this function will work with
     * 
     * @return a new <code>SetFunction</code> for the given type
     */
    public static SetFunction getSubsetInstance(String functionName, String argumentType) {
        return new ConditionSetFunction(functionName, argumentType, NAME_BASE_SUBSET);
    }

    /**
     * Creates a new instance of the equals set function. This should be used to create support for
     * any new attribute types and then the new <code>SetFunction</code> object should be added to
     * the factory (all set functions for the base types are already installed in the factory).
     * 
     * @param functionName
     *            the name of the function
     * @param argumentType
     *            the attribute type this function will work with
     * 
     * @return a new <code>SetFunction</code> for the given type
     */
    public static SetFunction getSetEqualsInstance(String functionName, String argumentType) {
        return new ConditionSetFunction(functionName, argumentType, NAME_BASE_SET_EQUALS);
    }

    /**
     * Protected constuctor used by the general and condition subclasses. If you need to create a
     * new <code>SetFunction</code> instance you should either use one of the
     * <code>getInstance</code> methods or construct one of the sub-classes directly.
     * 
     * @param functionName
     *            the identitifer for the function
     * @param functionId
     *            an optional, internal numeric identifier
     * @param argumentType
     *            the datatype this function accepts
     * @param returnType
     *            the datatype this function returns
     * @param returnsBag
     *            whether this function returns bags
     */
    protected SetFunction(String functionName, int functionId, String argumentType,
            String returnType, boolean returnsBag) {
        super(functionName, functionId, argumentType, true, 2, returnType, returnsBag);
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.addAll(ConditionSetFunction.getSupportedIdentifiers());
        set.addAll(GeneralSetFunction.getSupportedIdentifiers());

        return set;
    }

}
