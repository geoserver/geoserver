/*
 * @(#)MatchFunction.java
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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DNSNameAttribute;
import com.sun.xacml.attr.IPAddressAttribute;
import com.sun.xacml.attr.RFC822NameAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.X500NameAttribute;

/**
 * Implements the standard matching and regular expression functions.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Yassir Elley
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class MatchFunction extends FunctionBase {

    /**
     * Standard identifier for the regexp-string-match function.
     */
    public static final String NAME_REGEXP_STRING_MATCH = FUNCTION_NS + "regexp-string-match";

    /**
     * Standard identifier for the x500Name-match function.
     */
    public static final String NAME_X500NAME_MATCH = FUNCTION_NS + "x500Name-match";

    /**
     * Standard identifier for the rfc822Name-match function.
     */
    public static final String NAME_RFC822NAME_MATCH = FUNCTION_NS + "rfc822Name-match";

    /**
     * Standard identifier for the string-regexp-match function. NOTE: this in the 1.0 namespace
     * right now because of a bug in the XACML 2.0 specification, but this will be changed to the
     * 2.0 namespace as soon as the errata is recognized.
     */
    public static final String NAME_STRING_REGEXP_MATCH = FUNCTION_NS + "string-regexp-match";

    /**
     * Standard identifier for the anyURI-regexp-match function.
     */
    public static final String NAME_ANYURI_REGEXP_MATCH = FUNCTION_NS_2 + "anyURI-regexp-match";

    /**
     * Standard identifier for the ipAddress-regexp-match function.
     */
    public static final String NAME_IPADDRESS_REGEXP_MATCH = FUNCTION_NS_2
            + "ipAddress-regexp-match";

    /**
     * Standard identifier for the dnsName-regexp-match function.
     */
    public static final String NAME_DNSNAME_REGEXP_MATCH = FUNCTION_NS_2 + "dnsName-regexp-match";

    /**
     * Standard identifier for the rfc822Name-regexp-match function.
     */
    public static final String NAME_RFC822NAME_REGEXP_MATCH = FUNCTION_NS_2
            + "rfc822Name-regexp-match";

    /**
     * Standard identifier for the x500Name-regexp-match function.
     */
    public static final String NAME_X500NAME_REGEXP_MATCH = FUNCTION_NS_2 + "x500Name-regexp-match";

    // private identifiers for the supported functions
    private static final int ID_REGEXP_STRING_MATCH = 0;

    private static final int ID_X500NAME_MATCH = 1;

    private static final int ID_RFC822NAME_MATCH = 2;

    private static final int ID_STRING_REGEXP_MATCH = 3;

    private static final int ID_ANYURI_REGEXP_MATCH = 4;

    private static final int ID_IPADDRESS_REGEXP_MATCH = 5;

    private static final int ID_DNSNAME_REGEXP_MATCH = 6;

    private static final int ID_RFC822NAME_REGEXP_MATCH = 7;

    private static final int ID_X500NAME_REGEXP_MATCH = 8;

    // private mappings for the input arguments
    private static final String regexpParams[] = { StringAttribute.identifier,
            StringAttribute.identifier };

    private static final String x500Params[] = { X500NameAttribute.identifier,
            X500NameAttribute.identifier };

    private static final String rfc822Params[] = { StringAttribute.identifier,
            RFC822NameAttribute.identifier };

    private static final String stringRegexpParams[] = { StringAttribute.identifier,
            StringAttribute.identifier };

    private static final String anyURIRegexpParams[] = { StringAttribute.identifier,
            AnyURIAttribute.identifier };

    private static final String ipAddressRegexpParams[] = { StringAttribute.identifier,
            IPAddressAttribute.identifier };

    private static final String dnsNameRegexpParams[] = { StringAttribute.identifier,
            DNSNameAttribute.identifier };

    private static final String rfc822NameRegexpParams[] = { StringAttribute.identifier,
            RFC822NameAttribute.identifier };

    private static final String x500NameRegexpParams[] = { StringAttribute.identifier,
            X500NameAttribute.identifier };

    // private mapping for bag input options
    private static final boolean bagParams[] = { false, false };

    /**
     * Creates a new <code>MatchFunction</code> based on the given name.
     * 
     * @param functionName
     *            the name of the standard match function, including the complete namespace
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public MatchFunction(String functionName) {
        super(functionName, getId(functionName), getArgumentTypes(functionName), bagParams,
                BooleanAttribute.identifier, false);
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        if (functionName.equals(NAME_REGEXP_STRING_MATCH))
            return ID_REGEXP_STRING_MATCH;
        else if (functionName.equals(NAME_X500NAME_MATCH))
            return ID_X500NAME_MATCH;
        else if (functionName.equals(NAME_RFC822NAME_MATCH))
            return ID_RFC822NAME_MATCH;
        else if (functionName.equals(NAME_STRING_REGEXP_MATCH))
            return ID_STRING_REGEXP_MATCH;
        else if (functionName.equals(NAME_ANYURI_REGEXP_MATCH))
            return ID_ANYURI_REGEXP_MATCH;
        else if (functionName.equals(NAME_IPADDRESS_REGEXP_MATCH))
            return ID_IPADDRESS_REGEXP_MATCH;
        else if (functionName.equals(NAME_DNSNAME_REGEXP_MATCH))
            return ID_DNSNAME_REGEXP_MATCH;
        else if (functionName.equals(NAME_RFC822NAME_REGEXP_MATCH))
            return ID_RFC822NAME_REGEXP_MATCH;
        else if (functionName.equals(NAME_X500NAME_REGEXP_MATCH))
            return ID_X500NAME_REGEXP_MATCH;

        throw new IllegalArgumentException("unknown match function: " + functionName);
    }

    /**
     * Private helper that returns the types used for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String[] getArgumentTypes(String functionName) {
        if (functionName.equals(NAME_REGEXP_STRING_MATCH))
            return regexpParams;
        else if (functionName.equals(NAME_X500NAME_MATCH))
            return x500Params;
        else if (functionName.equals(NAME_RFC822NAME_MATCH))
            return rfc822Params;
        else if (functionName.equals(NAME_STRING_REGEXP_MATCH))
            return stringRegexpParams;
        else if (functionName.equals(NAME_ANYURI_REGEXP_MATCH))
            return anyURIRegexpParams;
        else if (functionName.equals(NAME_IPADDRESS_REGEXP_MATCH))
            return ipAddressRegexpParams;
        else if (functionName.equals(NAME_DNSNAME_REGEXP_MATCH))
            return dnsNameRegexpParams;
        else if (functionName.equals(NAME_RFC822NAME_REGEXP_MATCH))
            return rfc822NameRegexpParams;
        else if (functionName.equals(NAME_X500NAME_REGEXP_MATCH))
            return x500NameRegexpParams;

        return null;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_REGEXP_STRING_MATCH);
        set.add(NAME_X500NAME_MATCH);
        set.add(NAME_RFC822NAME_MATCH);
        set.add(NAME_STRING_REGEXP_MATCH);
        set.add(NAME_ANYURI_REGEXP_MATCH);
        set.add(NAME_IPADDRESS_REGEXP_MATCH);
        set.add(NAME_DNSNAME_REGEXP_MATCH);
        set.add(NAME_RFC822NAME_REGEXP_MATCH);
        set.add(NAME_X500NAME_REGEXP_MATCH);

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

        // make sure we didn't get an error in processing the args
        if (result != null)
            return result;

        // now that we're setup, we can do the matching operations

        boolean boolResult = false;

        switch (getFunctionId()) {

        case ID_REGEXP_STRING_MATCH:
        case ID_STRING_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        case ID_X500NAME_MATCH: {
            X500Principal arg0 = ((X500NameAttribute) (argValues[0])).getValue();
            X500Principal arg1 = ((X500NameAttribute) (argValues[1])).getValue();

            boolResult = arg1.getName(X500Principal.CANONICAL).endsWith(
                    arg0.getName(X500Principal.CANONICAL));

            break;
        }

        case ID_RFC822NAME_MATCH: {
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((RFC822NameAttribute) (argValues[1])).getValue();

            if (arg0.indexOf('@') != -1) {
                // this is case #1 : a whole address
                String normalized = (new RFC822NameAttribute(arg0)).getValue();
                boolResult = normalized.equals(arg1);
            } else if (arg0.charAt(0) == '.') {
                // this is case #3 : a sub-domain
                boolResult = arg1.endsWith(arg0.toLowerCase());
            } else {
                // this is case #2 : any mailbox at a specific domain
                String mailDomain = arg1.substring(arg1.indexOf('@') + 1);
                boolResult = arg0.toLowerCase().equals(mailDomain);
            }

            break;
        }

        case ID_ANYURI_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((AnyURIAttribute) (argValues[1])).encode();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        case ID_IPADDRESS_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((IPAddressAttribute) (argValues[1])).encode();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        case ID_DNSNAME_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((DNSNameAttribute) (argValues[1])).encode();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        case ID_RFC822NAME_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((RFC822NameAttribute) (argValues[1])).encode();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        case ID_X500NAME_REGEXP_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((X500NameAttribute) (argValues[1])).encode();

            boolResult = regexpHelper(arg0, arg1);

            break;
        }

        }

        // Return the result as a BooleanAttribute.
        return EvaluationResult.getInstance(boolResult);
    }

    /**
     *
     */
    private boolean regexpHelper(String xpr, String str) {
        // the regular expression syntax required by XACML differs
        // from the syntax supported by java.util.regex.Pattern
        // in several ways; the next several code blocks transform
        // the XACML syntax into a semantically equivalent Pattern syntax

        StringBuffer buf = new StringBuffer(xpr);

        // in order to handle the requirement that the string is
        // considered to match the pattern if any substring matches
        // the pattern, we prepend ".*" and append ".*" to the reg exp,
        // but only if there isn't an anchor (^ or $) in place

        if (xpr.charAt(0) != '^')
            buf = buf.insert(0, ".*");

        if (xpr.charAt(xpr.length() - 1) != '$')
            buf = buf.insert(buf.length(), ".*");

        // in order to handle Unicode blocks, we replace all
        // instances of "\p{Is" with "\p{In" in the reg exp

        int idx = -1;
        idx = buf.indexOf("\\p{Is", 0);
        while (idx != -1) {
            buf = buf.replace(idx, idx + 5, "\\p{In");
            idx = buf.indexOf("\\p{Is", idx);
        }

        // in order to handle Unicode blocks, we replace all instances
        // of "\P{Is" with "\P{In" in the reg exp

        idx = -1;
        idx = buf.indexOf("\\P{Is", 0);
        while (idx != -1) {
            buf = buf.replace(idx, idx + 5, "\\P{In");
            idx = buf.indexOf("\\P{Is", idx);
        }

        // in order to handle character class subtraction, we
        // replace all instances of "-[" with "&&[^" in the reg exp

        idx = -1;
        idx = buf.indexOf("-[", 0);
        while (idx != -1) {
            buf = buf.replace(idx, idx + 2, "&&[^");
            idx = buf.indexOf("-[", idx);
        }

        return Pattern.matches(buf.toString(), str);
    }

}
