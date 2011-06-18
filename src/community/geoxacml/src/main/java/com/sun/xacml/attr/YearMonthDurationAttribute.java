/*
 * @(#)YearMonthDurationAttribute.java
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

package com.sun.xacml.attr;

import java.math.BigInteger;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;

/**
 * Representation of an xf:yearMonthDuration value. This class supports parsing xd:yearMonthDuration
 * values. All objects of this class are immutable and thread-safe. The <code>Date</code> objects
 * returned are not, but these objects are cloned before being returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 */
public class YearMonthDurationAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/TR/2002/WD-xquery-operators-20020816#"
            + "yearMonthDuration";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Regular expression for yearMonthDuration (a la java.util.regex)
     */
    private static final String patternString = "(\\-)?P((\\d+)?Y)?((\\d+)?M)?";

    /**
     * The index of the capturing group for the negative sign.
     */
    private static final int GROUP_SIGN = 1;

    /**
     * The index of the capturing group for the number of years.
     */
    private static final int GROUP_YEARS = 3;

    /**
     * The index of the capturing group for the number of months.
     */
    private static final int GROUP_MONTHS = 5;

    /**
     * Static BigInteger values. We only use these if one of the components is bigger than
     * Integer.MAX_LONG and we want to detect overflow, so we don't initialize these until they're
     * needed.
     */
    private static BigInteger big12;

    private static BigInteger bigMaxLong;

    /**
     * A shared Pattern object, only initialized if needed
     */
    private static Pattern pattern;

    /**
     * Negative flag. true if duration is negative, false otherwise
     */
    private boolean negative;

    /**
     * Number of years
     */
    private long years;

    /**
     * Number of months
     */
    private long months;

    /**
     * Total number of months (used for equals)
     */
    private long totalMonths;

    /**
     * Cached encoded value (null if not cached yet).
     */
    private String encodedValue = null;

    /**
     * Creates a new <code>YearMonthDurationAttribute</code> that represents the duration supplied.
     * 
     * @param negative
     *            true if the duration is negative, false otherwise
     * @param years
     *            the number of years in the duration (must be positive)
     * @param months
     *            the number of months in the duration (must be positive)
     * @throws IllegalArgumentException
     *             if the total number of months exceeds Long.MAX_LONG or the number of months or
     *             years is negative
     */
    public YearMonthDurationAttribute(boolean negative, long years, long months)
            throws IllegalArgumentException {
        super(identifierURI);
        this.negative = negative;
        this.years = years;
        this.months = months;

        // Convert all the components except nanoseconds to milliseconds

        // If any of the components is big (too big to be an int),
        // use the BigInteger class to do the math so we can detect
        // overflow.
        if ((years > Integer.MAX_VALUE) || (months > Integer.MAX_VALUE)) {
            if (big12 == null) {
                big12 = BigInteger.valueOf(12);
                bigMaxLong = BigInteger.valueOf(Long.MAX_VALUE);
            }
            BigInteger bigMonths = BigInteger.valueOf(months);
            BigInteger bigYears = BigInteger.valueOf(years);

            BigInteger bigTotal = bigYears.multiply(big12).add(bigMonths);

            // If the result is bigger than Long.MAX_VALUE, we have an
            // overflow. Indicate an error (should be a processing error,
            // since it can be argued that we should handle gigantic
            // values for this).
            if (bigTotal.compareTo(bigMaxLong) == 1)
                throw new IllegalArgumentException("total number of " + "months "
                        + "exceeds Long.MAX_VALUE");
            // If no overflow, convert to a long.
            totalMonths = bigTotal.longValue();
            if (negative)
                totalMonths = -totalMonths;
        } else {
            // The numbers are small, so do it the fast way.
            totalMonths = ((years * 12) + months) * (negative ? -1 : 1);
        }
    }

    /**
     * Returns a new <code>YearMonthDurationAttribute</code> that represents the
     * xf:yearMonthDuration at a particular DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>YearMonthDurationAttribute</code> representing the appropriate value
     * @throws ParsingException
     *             if any problems occurred while parsing
     */
    public static YearMonthDurationAttribute getInstance(Node root) throws ParsingException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns the long value for the capturing group groupNumber. This method takes a Matcher that
     * has been used to match a Pattern against a String, fetches the value for the specified
     * capturing group, converts that value to an long, and returns the value. If that group did not
     * match, 0 is returned. If the matched value is not a valid long, NumberFormatException is
     * thrown.
     * 
     * @param matcher
     *            the Matcher from which to fetch the group
     * @param groupNumber
     *            the group number to fetch
     * @return the long value for that groupNumber
     * @throws NumberFormatException
     *             if the string value for that groupNumber is not a valid long
     */
    private static long parseGroup(Matcher matcher, int groupNumber) throws NumberFormatException {
        long groupLong = 0;

        if (matcher.start(groupNumber) != -1) {
            String groupString = matcher.group(groupNumber);
            groupLong = Long.parseLong(groupString);
        }
        return groupLong;
    }

    /**
     * Returns a new <code>YearMonthDurationAttribute</code> that represents the
     * xf:yearMonthDuration value indicated by the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * 
     * @return a new <code>YearMonthDurationAttribute</code> representing the desired value
     * 
     * @throws ParsingException
     *             if any problems occurred while parsing
     */
    public static YearMonthDurationAttribute getInstance(String value) throws ParsingException {
        boolean negative = false;
        long years = 0;
        long months = 0;

        // Compile the pattern, if not already done.
        if (pattern == null) {
            try {
                pattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException e) {
                // This should never happen
                throw new ParsingException("unexpected pattern syntax error");
            }
        }

        // See if the value matches the pattern.
        Matcher matcher = pattern.matcher(value);
        boolean matches = matcher.matches();

        // If not, syntax error!
        if (!matches) {
            throw new ParsingException("Syntax error in yearMonthDuration");
        }

        // If the negative group matched, the value is negative.
        if (matcher.start(GROUP_SIGN) != -1)
            negative = true;

        try {
            // If the years group matched, parse that value.
            years = parseGroup(matcher, GROUP_YEARS);

            // If the months group matched, parse that value.
            months = parseGroup(matcher, GROUP_MONTHS);
        } catch (NumberFormatException e) {
            // If we run into a number that's too big to be a long
            // that's an error. Really, it's a processing error,
            // since one can argue that we should handle that.
            throw new ParsingException("Unable to handle number size");
        }

        // If parsing went OK, create a new YearMonthDurationAttribute
        // object and return it.
        return new YearMonthDurationAttribute(negative, years, months);
    }

    /**
     * Returns true if the duration is negative.
     * 
     * @return true if the duration is negative, false otherwise
     */
    public boolean isNegative() {
        return negative;
    }

    /**
     * Gets the number of years.
     * 
     * @return the number of years
     */
    public long getYears() {
        return years;
    }

    /**
     * Gets the number of months.
     * 
     * @return the number of months
     */
    public long getMonths() {
        return months;
    }

    /**
     * Returns true if the input is an instance of this class and if its value equals the value
     * contained in this class.
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof YearMonthDurationAttribute))
            return false;

        YearMonthDurationAttribute other = (YearMonthDurationAttribute) o;

        return (totalMonths == other.totalMonths);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        return (int) totalMonths ^ (int) (totalMonths >> 32);
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("YearMonthDurationAttribute: [\n");
        sb.append("  Negative: " + negative);
        sb.append("  Years: " + years);
        sb.append("  Months: " + months);
        sb.append("]");

        return sb.toString();
    }

    /**
     * Encodes the value in a form suitable for including in XML data like a request or an
     * obligation. This must return a value that could in turn be used by the factory to create a
     * new instance with the same value.
     * 
     * @return a <code>String</code> form of the value
     */
    public String encode() {
        if (encodedValue != null)
            return encodedValue;

        // Length is variable
        StringBuffer buf = new StringBuffer(10);

        if (negative)
            buf.append('-');
        buf.append('P');
        if ((years != 0) || (months == 0)) {
            buf.append(Long.toString(years));
            buf.append('Y');
        }
        if (months != 0) {
            buf.append(Long.toString(months));
            buf.append('M');
        }

        encodedValue = buf.toString();

        return encodedValue;
    }
}
