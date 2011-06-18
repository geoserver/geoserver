/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DNSNameAttribute;
import com.sun.xacml.attr.IPAddressAttribute;
import com.sun.xacml.attr.RFC822NameAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.X500NameAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;
import com.sun.xacml.cond.FunctionBase;

/**
 * XACML Function implementing wildcard matching
 * 
 * @author Christian Mueller
 * 
 */
public class WildCardFunction extends FunctionBase {

    /**
     * geotools identifier for the string-wildcard-match function. NOTE: this is a geotools specific
     * extension
     */
    public static final String GEOTOOLS_FUNCTION_NS = "org:geotools:function:";

    public static final String NAME_STRING_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "string-wildcard-match";

    /**
     * Geotools identifier for the anyURI-wildcard-match function.
     */
    public static final String NAME_ANYURI_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "anyURI-wildcard-match";

    /**
     * Geotools identifier for the ipAddress-wildcard-match function.
     */
    public static final String NAME_IPADDRESS_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "ipAddress-wildcard-match";

    /**
     * Geotools identifier for the dnsName-wildcard-match function.
     */
    public static final String NAME_DNSNAME_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "dnsName-wildcard-match";

    /**
     * Geotools identifier for the rfc822Name-wildcard-match function.
     */
    public static final String NAME_RFC822NAME_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "rfc822Name-wildcard-match";

    /**
     * Geotools identifier for the x500Name-wildcard-match function.
     */
    public static final String NAME_X500NAME_WILDCARD_MATCH = GEOTOOLS_FUNCTION_NS
            + "x500Name-wildcard-match";

    // private identifiers for the supported functions
    private static final int ID_STRING_WILDCARD_MATCH = 0;

    private static final int ID_ANYURI_WILDCARD_MATCH = 1;

    private static final int ID_IPADDRESS_WILDCARD_MATCH = 2;

    private static final int ID_DNSNAME_WILDCARD_MATCH = 3;

    private static final int ID_RFC822NAME_WILDCARD_MATCH = 4;

    private static final int ID_X500NAME_WILDCARD_MATCH = 5;

    // private mappings for the input arguments
    private static final String stringWildcardParams[] = { StringAttribute.identifier,
            StringAttribute.identifier };

    private static final String anyURIWildcardParams[] = { StringAttribute.identifier,
            AnyURIAttribute.identifier };

    private static final String ipAddressWildcardParams[] = { StringAttribute.identifier,
            IPAddressAttribute.identifier };

    private static final String dnsNameWildcardParams[] = { StringAttribute.identifier,
            DNSNameAttribute.identifier };

    private static final String rfc822NameWildcardParams[] = { StringAttribute.identifier,
            RFC822NameAttribute.identifier };

    private static final String x500NameWildcardParams[] = { StringAttribute.identifier,
            X500NameAttribute.identifier };

    // private mapping for bag input options
    private static final boolean bagParams[] = { false, false };

    /**
     * Creates a new <code>WildCardFunction</code> based on the given name.
     * 
     * @param functionName
     *            the name of the standard match function, including the complete namespace
     * 
     * @throws IllegalArgumentException
     *             if the function is unknown
     */
    public WildCardFunction(String functionName) {
        super(functionName, getId(functionName), getArgumentTypes(functionName), bagParams,
                BooleanAttribute.identifier, false);
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        if (functionName.equals(NAME_STRING_WILDCARD_MATCH))
            return ID_STRING_WILDCARD_MATCH;
        else if (functionName.equals(NAME_ANYURI_WILDCARD_MATCH))
            return ID_ANYURI_WILDCARD_MATCH;
        else if (functionName.equals(NAME_IPADDRESS_WILDCARD_MATCH))
            return ID_IPADDRESS_WILDCARD_MATCH;
        else if (functionName.equals(NAME_DNSNAME_WILDCARD_MATCH))
            return ID_DNSNAME_WILDCARD_MATCH;
        else if (functionName.equals(NAME_RFC822NAME_WILDCARD_MATCH))
            return ID_RFC822NAME_WILDCARD_MATCH;
        else if (functionName.equals(NAME_X500NAME_WILDCARD_MATCH))
            return ID_X500NAME_WILDCARD_MATCH;

        throw new IllegalArgumentException("unknown match function: " + functionName);
    }

    /**
     * Private helper that returns the types used for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String[] getArgumentTypes(String functionName) {
        if (functionName.equals(NAME_STRING_WILDCARD_MATCH))
            return stringWildcardParams;
        else if (functionName.equals(NAME_ANYURI_WILDCARD_MATCH))
            return anyURIWildcardParams;
        else if (functionName.equals(NAME_IPADDRESS_WILDCARD_MATCH))
            return ipAddressWildcardParams;
        else if (functionName.equals(NAME_DNSNAME_WILDCARD_MATCH))
            return dnsNameWildcardParams;
        else if (functionName.equals(NAME_RFC822NAME_WILDCARD_MATCH))
            return rfc822NameWildcardParams;
        else if (functionName.equals(NAME_X500NAME_WILDCARD_MATCH))
            return x500NameWildcardParams;

        return null;
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        Set<String> set = new HashSet<String>();

        set.add(NAME_STRING_WILDCARD_MATCH);
        set.add(NAME_ANYURI_WILDCARD_MATCH);
        set.add(NAME_IPADDRESS_WILDCARD_MATCH);
        set.add(NAME_DNSNAME_WILDCARD_MATCH);
        set.add(NAME_RFC822NAME_WILDCARD_MATCH);
        set.add(NAME_X500NAME_WILDCARD_MATCH);

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

        case ID_STRING_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        case ID_ANYURI_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((AnyURIAttribute) (argValues[1])).encode();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        case ID_IPADDRESS_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((IPAddressAttribute) (argValues[1])).encode();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        case ID_DNSNAME_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((DNSNameAttribute) (argValues[1])).encode();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        case ID_RFC822NAME_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((RFC822NameAttribute) (argValues[1])).encode();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        case ID_X500NAME_WILDCARD_MATCH: {
            // arg0 is a regular expression; arg1 is a general string
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((X500NameAttribute) (argValues[1])).encode();

            boolResult = wildCardMatch(arg0, arg1);

            break;
        }

        }

        // Return the result as a BooleanAttribute.
        return EvaluationResult.getInstance(boolResult);
    }

    private boolean wildCardMatch(String wildcard, String testString) {
        String regexp = wildcardToRegex(wildcard);
        return Pattern.matches(regexp, testString);
    }

    private String wildcardToRegex(String wildcard) {
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
            case '*':
                s.append(".*");
                break;
            case '?':
                s.append(".");
                break;
            // escape special regexp-characters
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
            case '.':
            case '{':
            case '}':
            case '|':
            case '\\':
                s.append("\\");
                s.append(c);
                break;
            default:
                s.append(c);
                break;
            }
        }
        s.append('$');
        return (s.toString());
    }

}
