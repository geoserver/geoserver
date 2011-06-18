/*
 * @(#)ComparisonFunction.java
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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BooleanAttribute;
import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.DoubleAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.TimeAttribute;

/**
 * A class that implements all of the standard comparison functions.
 * 
 * @since 1.0
 * @author Steve Hanna
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ComparisonFunction extends FunctionBase {

    /**
     * Standard identifier for the integer-greater-than function.
     */
    public static final String NAME_INTEGER_GREATER_THAN = FUNCTION_NS + "integer-greater-than";

    /**
     * Standard identifier for the integer-greater-than-or-equal function.
     */
    public static final String NAME_INTEGER_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "integer-greater-than-or-equal";

    /**
     * Standard identifier for the integer-less-than function.
     */
    public static final String NAME_INTEGER_LESS_THAN = FUNCTION_NS + "integer-less-than";

    /**
     * Standard identifier for the integer-less-than-or-equal function.
     */
    public static final String NAME_INTEGER_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "integer-less-than-or-equal";

    /**
     * Standard identifier for the double-greater-than function.
     */
    public static final String NAME_DOUBLE_GREATER_THAN = FUNCTION_NS + "double-greater-than";

    /**
     * Standard identifier for the double-greater-than-or-equal function.
     */
    public static final String NAME_DOUBLE_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "double-greater-than-or-equal";

    /**
     * Standard identifier for the double-less-than function.
     */
    public static final String NAME_DOUBLE_LESS_THAN = FUNCTION_NS + "double-less-than";

    /**
     * Standard identifier for the double-less-than-or-equal function.
     */
    public static final String NAME_DOUBLE_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "double-less-than-or-equal";

    /**
     * Standard identifier for the string-greater-than function.
     */
    public static final String NAME_STRING_GREATER_THAN = FUNCTION_NS + "string-greater-than";

    /**
     * Standard identifier for the string-greater-than-or-equal function.
     */
    public static final String NAME_STRING_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "string-greater-than-or-equal";

    /**
     * Standard identifier for the string-less-than function.
     */
    public static final String NAME_STRING_LESS_THAN = FUNCTION_NS + "string-less-than";

    /**
     * Standard identifier for the string-less-than-or-equal function.
     */
    public static final String NAME_STRING_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "string-less-than-or-equal";

    /**
     * Standard identifier for the time-greater-than function.
     */
    public static final String NAME_TIME_GREATER_THAN = FUNCTION_NS + "time-greater-than";

    /**
     * Standard identifier for the time-greater-than-or-equal function.
     */
    public static final String NAME_TIME_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "time-greater-than-or-equal";

    /**
     * Standard identifier for the time-less-than function.
     */
    public static final String NAME_TIME_LESS_THAN = FUNCTION_NS + "time-less-than";

    /**
     * Standard identifier for the time-less-than-or-equal function.
     */
    public static final String NAME_TIME_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "time-less-than-or-equal";

    /**
     * Standard identifier for the dateTime-greater-than function.
     */
    public static final String NAME_DATETIME_GREATER_THAN = FUNCTION_NS + "dateTime-greater-than";

    /**
     * Standard identifier for the dateTime-greater-than-or-equal function.
     */
    public static final String NAME_DATETIME_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "dateTime-greater-than-or-equal";

    /**
     * Standard identifier for the dateTime-less-than function.
     */
    public static final String NAME_DATETIME_LESS_THAN = FUNCTION_NS + "dateTime-less-than";

    /**
     * Standard identifier for the dateTime-less-than-or-equal function.
     */
    public static final String NAME_DATETIME_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "dateTime-less-than-or-equal";

    /**
     * Standard identifier for the date-greater-than function.
     */
    public static final String NAME_DATE_GREATER_THAN = FUNCTION_NS + "date-greater-than";

    /**
     * Standard identifier for the date-greater-than-or-equal function.
     */
    public static final String NAME_DATE_GREATER_THAN_OR_EQUAL = FUNCTION_NS
            + "date-greater-than-or-equal";

    /**
     * Standard identifier for the date-less-than function.
     */
    public static final String NAME_DATE_LESS_THAN = FUNCTION_NS + "date-less-than";

    /**
     * Standard identifier for the date-less-than-or-equal function.
     */
    public static final String NAME_DATE_LESS_THAN_OR_EQUAL = FUNCTION_NS
            + "date-less-than-or-equal";

    // private identifiers for the supported functions
    private static final int ID_INTEGER_GREATER_THAN = 0;

    private static final int ID_INTEGER_GREATER_THAN_OR_EQUAL = 1;

    private static final int ID_INTEGER_LESS_THAN = 2;

    private static final int ID_INTEGER_LESS_THAN_OR_EQUAL = 3;

    private static final int ID_DOUBLE_GREATER_THAN = 4;

    private static final int ID_DOUBLE_GREATER_THAN_OR_EQUAL = 5;

    private static final int ID_DOUBLE_LESS_THAN = 6;

    private static final int ID_DOUBLE_LESS_THAN_OR_EQUAL = 7;

    private static final int ID_STRING_GREATER_THAN = 8;

    private static final int ID_STRING_GREATER_THAN_OR_EQUAL = 9;

    private static final int ID_STRING_LESS_THAN = 10;

    private static final int ID_STRING_LESS_THAN_OR_EQUAL = 11;

    private static final int ID_TIME_GREATER_THAN = 12;

    private static final int ID_TIME_GREATER_THAN_OR_EQUAL = 13;

    private static final int ID_TIME_LESS_THAN = 14;

    private static final int ID_TIME_LESS_THAN_OR_EQUAL = 15;

    private static final int ID_DATE_GREATER_THAN = 16;

    private static final int ID_DATE_GREATER_THAN_OR_EQUAL = 17;

    private static final int ID_DATE_LESS_THAN = 18;

    private static final int ID_DATE_LESS_THAN_OR_EQUAL = 19;

    private static final int ID_DATETIME_GREATER_THAN = 20;

    private static final int ID_DATETIME_GREATER_THAN_OR_EQUAL = 21;

    private static final int ID_DATETIME_LESS_THAN = 22;

    private static final int ID_DATETIME_LESS_THAN_OR_EQUAL = 23;

    // mappings from name to private identifier and argument datatype
    private static HashMap<String, Integer> idMap;

    private static HashMap<String, String> typeMap;

    /**
     * Static initializer to setup the two maps.
     */
    static {
        idMap = new HashMap<String, Integer>();

        idMap.put(NAME_INTEGER_GREATER_THAN, new Integer(ID_INTEGER_GREATER_THAN));
        idMap
                .put(NAME_INTEGER_GREATER_THAN_OR_EQUAL, new Integer(
                        ID_INTEGER_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_INTEGER_LESS_THAN, new Integer(ID_INTEGER_LESS_THAN));
        idMap.put(NAME_INTEGER_LESS_THAN_OR_EQUAL, new Integer(ID_INTEGER_LESS_THAN_OR_EQUAL));
        idMap.put(NAME_DOUBLE_GREATER_THAN, new Integer(ID_DOUBLE_GREATER_THAN));
        idMap.put(NAME_DOUBLE_GREATER_THAN_OR_EQUAL, new Integer(ID_DOUBLE_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_DOUBLE_LESS_THAN, new Integer(ID_DOUBLE_LESS_THAN));
        idMap.put(NAME_DOUBLE_LESS_THAN_OR_EQUAL, new Integer(ID_DOUBLE_LESS_THAN_OR_EQUAL));
        idMap.put(NAME_STRING_GREATER_THAN, new Integer(ID_STRING_GREATER_THAN));
        idMap.put(NAME_STRING_GREATER_THAN_OR_EQUAL, new Integer(ID_STRING_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_STRING_LESS_THAN, new Integer(ID_STRING_LESS_THAN));
        idMap.put(NAME_STRING_LESS_THAN_OR_EQUAL, new Integer(ID_STRING_LESS_THAN_OR_EQUAL));
        idMap.put(NAME_TIME_GREATER_THAN, new Integer(ID_TIME_GREATER_THAN));
        idMap.put(NAME_TIME_GREATER_THAN_OR_EQUAL, new Integer(ID_TIME_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_TIME_LESS_THAN, new Integer(ID_TIME_LESS_THAN));
        idMap.put(NAME_TIME_LESS_THAN_OR_EQUAL, new Integer(ID_TIME_LESS_THAN_OR_EQUAL));
        idMap.put(NAME_DATE_GREATER_THAN, new Integer(ID_DATE_GREATER_THAN));
        idMap.put(NAME_DATE_GREATER_THAN_OR_EQUAL, new Integer(ID_DATE_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_DATE_LESS_THAN, new Integer(ID_DATE_LESS_THAN));
        idMap.put(NAME_DATE_LESS_THAN_OR_EQUAL, new Integer(ID_DATE_LESS_THAN_OR_EQUAL));
        idMap.put(NAME_DATETIME_GREATER_THAN, new Integer(ID_DATETIME_GREATER_THAN));
        idMap.put(NAME_DATETIME_GREATER_THAN_OR_EQUAL, new Integer(
                ID_DATETIME_GREATER_THAN_OR_EQUAL));
        idMap.put(NAME_DATETIME_LESS_THAN, new Integer(ID_DATETIME_LESS_THAN));
        idMap.put(NAME_DATETIME_LESS_THAN_OR_EQUAL, new Integer(ID_DATETIME_LESS_THAN_OR_EQUAL));

        typeMap = new HashMap<String, String>();

        typeMap.put(NAME_INTEGER_GREATER_THAN, IntegerAttribute.identifier);
        typeMap.put(NAME_INTEGER_GREATER_THAN_OR_EQUAL, IntegerAttribute.identifier);
        typeMap.put(NAME_INTEGER_LESS_THAN, IntegerAttribute.identifier);
        typeMap.put(NAME_INTEGER_LESS_THAN_OR_EQUAL, IntegerAttribute.identifier);
        typeMap.put(NAME_DOUBLE_GREATER_THAN, DoubleAttribute.identifier);
        typeMap.put(NAME_DOUBLE_GREATER_THAN_OR_EQUAL, DoubleAttribute.identifier);
        typeMap.put(NAME_DOUBLE_LESS_THAN, DoubleAttribute.identifier);
        typeMap.put(NAME_DOUBLE_LESS_THAN_OR_EQUAL, DoubleAttribute.identifier);
        typeMap.put(NAME_STRING_GREATER_THAN, StringAttribute.identifier);
        typeMap.put(NAME_STRING_GREATER_THAN_OR_EQUAL, StringAttribute.identifier);
        typeMap.put(NAME_STRING_LESS_THAN, StringAttribute.identifier);
        typeMap.put(NAME_STRING_LESS_THAN_OR_EQUAL, StringAttribute.identifier);
        typeMap.put(NAME_TIME_GREATER_THAN, TimeAttribute.identifier);
        typeMap.put(NAME_TIME_GREATER_THAN_OR_EQUAL, TimeAttribute.identifier);
        typeMap.put(NAME_TIME_LESS_THAN, TimeAttribute.identifier);
        typeMap.put(NAME_TIME_LESS_THAN_OR_EQUAL, TimeAttribute.identifier);
        typeMap.put(NAME_DATETIME_GREATER_THAN, DateTimeAttribute.identifier);
        typeMap.put(NAME_DATETIME_GREATER_THAN_OR_EQUAL, DateTimeAttribute.identifier);
        typeMap.put(NAME_DATETIME_LESS_THAN, DateTimeAttribute.identifier);
        typeMap.put(NAME_DATETIME_LESS_THAN_OR_EQUAL, DateTimeAttribute.identifier);
        typeMap.put(NAME_DATE_GREATER_THAN, DateAttribute.identifier);
        typeMap.put(NAME_DATE_GREATER_THAN_OR_EQUAL, DateAttribute.identifier);
        typeMap.put(NAME_DATE_LESS_THAN, DateAttribute.identifier);
        typeMap.put(NAME_DATE_LESS_THAN_OR_EQUAL, DateAttribute.identifier);
    };

    /**
     * Creates a new <code>ComparisonFunction</code> object.
     * 
     * @param functionName
     *            the standard XACML name of the function to be handled by this object, including
     *            the full namespace
     * 
     * @throws IllegalArgumentException
     *             if the function isn't known
     */
    public ComparisonFunction(String functionName) {
        super(functionName, getId(functionName), getArgumentType(functionName), false, 2,
                BooleanAttribute.identifier, false);
    }

    /**
     * Private helper that returns the internal identifier used for the given standard function.
     */
    private static int getId(String functionName) {
        Integer i = (Integer) (idMap.get(functionName));

        if (i == null)
            throw new IllegalArgumentException("unknown comparison function " + functionName);

        return i.intValue();
    }

    /**
     * Private helper that returns the type used for the given standard function. Note that this
     * doesn't check on the return value since the method always is called after getId, so we assume
     * that the function is present.
     */
    private static String getArgumentType(String functionName) {
        return (String) (typeMap.get(functionName));
    }

    /**
     * Returns a <code>Set</code> containing all the function identifiers supported by this class.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public static Set<String> getSupportedIdentifiers() {
        return Collections.unmodifiableSet(idMap.keySet());
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

        // Now that we have real values, perform the comparison operation

        boolean boolResult = false;

        switch (getFunctionId()) {

        case ID_INTEGER_GREATER_THAN: {
            long arg0 = ((IntegerAttribute) (argValues[0])).getValue();
            long arg1 = ((IntegerAttribute) (argValues[1])).getValue();

            boolResult = (arg0 > arg1);

            break;
        }

        case ID_INTEGER_GREATER_THAN_OR_EQUAL: {
            long arg0 = ((IntegerAttribute) (argValues[0])).getValue();
            long arg1 = ((IntegerAttribute) (argValues[1])).getValue();

            boolResult = (arg0 >= arg1);

            break;
        }

        case ID_INTEGER_LESS_THAN: {
            long arg0 = ((IntegerAttribute) (argValues[0])).getValue();
            long arg1 = ((IntegerAttribute) (argValues[1])).getValue();

            boolResult = (arg0 < arg1);

            break;
        }

        case ID_INTEGER_LESS_THAN_OR_EQUAL: {
            long arg0 = ((IntegerAttribute) (argValues[0])).getValue();
            long arg1 = ((IntegerAttribute) (argValues[1])).getValue();

            boolResult = (arg0 <= arg1);

            break;
        }

        case ID_DOUBLE_GREATER_THAN: {
            double arg0 = ((DoubleAttribute) (argValues[0])).getValue();
            double arg1 = ((DoubleAttribute) (argValues[1])).getValue();

            boolResult = (doubleCompare(arg0, arg1) > 0);

            break;
        }

        case ID_DOUBLE_GREATER_THAN_OR_EQUAL: {
            double arg0 = ((DoubleAttribute) (argValues[0])).getValue();
            double arg1 = ((DoubleAttribute) (argValues[1])).getValue();

            boolResult = (doubleCompare(arg0, arg1) >= 0);

            break;
        }

        case ID_DOUBLE_LESS_THAN: {
            double arg0 = ((DoubleAttribute) (argValues[0])).getValue();
            double arg1 = ((DoubleAttribute) (argValues[1])).getValue();

            boolResult = (doubleCompare(arg0, arg1) < 0);

            break;
        }

        case ID_DOUBLE_LESS_THAN_OR_EQUAL: {
            double arg0 = ((DoubleAttribute) (argValues[0])).getValue();
            double arg1 = ((DoubleAttribute) (argValues[1])).getValue();

            boolResult = (doubleCompare(arg0, arg1) <= 0);

            break;
        }

        case ID_STRING_GREATER_THAN: {
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) > 0);

            break;
        }

        case ID_STRING_GREATER_THAN_OR_EQUAL: {
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) >= 0);

            break;
        }

        case ID_STRING_LESS_THAN: {
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) < 0);

            break;
        }

        case ID_STRING_LESS_THAN_OR_EQUAL: {
            String arg0 = ((StringAttribute) (argValues[0])).getValue();
            String arg1 = ((StringAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) <= 0);

            break;
        }

        case ID_TIME_GREATER_THAN: {
            TimeAttribute arg0 = (TimeAttribute) (argValues[0]);
            TimeAttribute arg1 = (TimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) > 0);

            break;
        }

        case ID_TIME_GREATER_THAN_OR_EQUAL: {
            TimeAttribute arg0 = (TimeAttribute) (argValues[0]);
            TimeAttribute arg1 = (TimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) >= 0);

            break;
        }

        case ID_TIME_LESS_THAN: {
            TimeAttribute arg0 = (TimeAttribute) (argValues[0]);
            TimeAttribute arg1 = (TimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) < 0);

            break;
        }

        case ID_TIME_LESS_THAN_OR_EQUAL: {
            TimeAttribute arg0 = (TimeAttribute) (argValues[0]);
            TimeAttribute arg1 = (TimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) <= 0);

            break;
        }

        case ID_DATETIME_GREATER_THAN: {
            DateTimeAttribute arg0 = (DateTimeAttribute) (argValues[0]);
            DateTimeAttribute arg1 = (DateTimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) > 0);

            break;
        }

        case ID_DATETIME_GREATER_THAN_OR_EQUAL: {
            DateTimeAttribute arg0 = (DateTimeAttribute) (argValues[0]);
            DateTimeAttribute arg1 = (DateTimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) >= 0);

            break;
        }

        case ID_DATETIME_LESS_THAN: {
            DateTimeAttribute arg0 = (DateTimeAttribute) (argValues[0]);
            DateTimeAttribute arg1 = (DateTimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) < 0);

            break;
        }

        case ID_DATETIME_LESS_THAN_OR_EQUAL: {
            DateTimeAttribute arg0 = (DateTimeAttribute) (argValues[0]);
            DateTimeAttribute arg1 = (DateTimeAttribute) (argValues[1]);

            boolResult = (dateCompare(arg0.getValue(), arg0.getNanoseconds(), arg1.getValue(), arg1
                    .getNanoseconds()) <= 0);

            break;
        }

        case ID_DATE_GREATER_THAN: {
            Date arg0 = ((DateAttribute) (argValues[0])).getValue();
            Date arg1 = ((DateAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) > 0);

            break;
        }

        case ID_DATE_GREATER_THAN_OR_EQUAL: {
            Date arg0 = ((DateAttribute) (argValues[0])).getValue();
            Date arg1 = ((DateAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) >= 0);

            break;
        }

        case ID_DATE_LESS_THAN: {
            Date arg0 = ((DateAttribute) (argValues[0])).getValue();
            Date arg1 = ((DateAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) < 0);

            break;
        }

        case ID_DATE_LESS_THAN_OR_EQUAL: {
            Date arg0 = ((DateAttribute) (argValues[0])).getValue();
            Date arg1 = ((DateAttribute) (argValues[1])).getValue();

            boolResult = (arg0.compareTo(arg1) <= 0);

            break;
        }

        }

        // Return the result as a BooleanAttribute.
        return EvaluationResult.getInstance(boolResult);
    }

    /**
     * Helper function that does a comparison of the two doubles using the rules of XMLSchema. Like
     * all compare methods, this returns 0 if they're equal, a positive value if d1 > d2, and a
     * negative value if d1 < d2.
     */
    private int doubleCompare(double d1, double d2) {
        // see if the numbers equal each other
        if (d1 == d2) {
            // these are not NaNs, and therefore we just need to check that
            // that they're not zeros, which may have different signs
            if (d1 != 0)
                return 0;

            // they're both zeros, so we compare strings to figure out
            // the significance of any signs
            return Double.toString(d1).compareTo(Double.toString(d2));
        }

        // see if d1 is NaN
        if (Double.isNaN(d1)) {
            // d1 is NaN, so see if d2 is as well
            if (Double.isNaN(d2)) {
                // they're both NaNs, so they're equal
                return 0;
            } else {
                // d1 is always bigger than d2 since it's a NaN
                return 1;
            }
        }

        // see if d2 is NaN
        if (Double.isNaN(d2)) {
            // d2 is a NaN, though d1 isn't, so d2 is always bigger
            return -1;
        }

        // if we got here then neither is a NaN, and the numbers aren't
        // equal...given those facts, basic comparison works the same in
        // java as it's defined in XMLSchema, so now we can do the simple
        // comparison and return whatever we find
        return ((d1 > d2) ? 1 : -1);
    }

    /**
     * Helper function to compare two Date objects and their associated nanosecond values. Like all
     * compare methods, this returns 0 if they're equal, a positive value if d1 > d2, and a negative
     * value if d1 < d2.
     */
    private int dateCompare(Date d1, int n1, Date d2, int n2) {
        int compareResult = d1.compareTo(d2);

        // we only worry about the nanosecond values if the Dates are equal
        if (compareResult != 0)
            return compareResult;

        // see if there's any difference
        if (n1 == n2)
            return 0;

        // there is some difference in the nanoseconds, and that's how
        // we'll determine the comparison
        return ((n1 > n2) ? 1 : -1);
    }

}
