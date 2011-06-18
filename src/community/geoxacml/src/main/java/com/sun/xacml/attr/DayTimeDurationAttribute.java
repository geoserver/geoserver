/*
 * @(#)DayTimeDurationAttribute.java
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
 * Representation of an xf:dayTimeDuration value. This class supports parsing xd:dayTimeDuration
 * values. All objects of this class are immutable and thread-safe. The <code>Date</code> objects
 * returned are not, but these objects are cloned before being returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 */
public class DayTimeDurationAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/TR/2002/WD-xquery-operators-20020816#"
            + "dayTimeDuration";

    /**
     * URI version of name for this type
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Regular expression for dayTimeDuration (a la java.util.regex)
     */
    private static final String patternString = "(\\-)?P((\\d+)?D)?(T((\\d+)?H)?((\\d+)?M)?((\\d+)?(.(\\d+)?)?S)?)?";

    /**
     * The index of the capturing group for the negative sign.
     */
    private static final int GROUP_SIGN = 1;

    /**
     * The index of the capturing group for the number of days.
     */
    private static final int GROUP_DAYS = 3;

    /**
     * The index of the capturing group for the number of hours.
     */
    private static final int GROUP_HOURS = 6;

    /**
     * The index of the capturing group for the number of minutes.
     */
    private static final int GROUP_MINUTES = 8;

    /**
     * The index of the capturing group for the number of seconds.
     */
    private static final int GROUP_SECONDS = 10;

    /**
     * The index of the capturing group for the number of nanoseconds.
     */
    private static final int GROUP_NANOSECONDS = 12;

    /**
     * Static BigInteger values. We only use these if one of the components is bigger than
     * Integer.MAX_LONG and we want to detect overflow, so we don't initialize these until they're
     * needed.
     */
    private static BigInteger big24;

    private static BigInteger big60;

    private static BigInteger big1000;

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
     * Number of days
     */
    private long days;

    /**
     * Number of hours
     */
    private long hours;

    /**
     * Number of minutes
     */
    private long minutes;

    /**
     * Number of seconds
     */
    private long seconds;

    /**
     * Number of nanoseconds
     */
    private int nanoseconds;

    /**
     * Total number of round seconds (in milliseconds)
     */
    private long totalMillis;

    /**
     * Cached encoded value (null if not cached yet).
     */
    private String encodedValue = null;

    /**
     * Creates a new <code>DayTimeDurationAttribute</code> that represents the duration supplied.
     * 
     * @param negative
     *            true if the duration is negative, false otherwise
     * @param days
     *            the number of days in the duration
     * @param hours
     *            the number of hours in the duration
     * @param minutes
     *            the number of minutes in the duration
     * @param seconds
     *            the number of seconds in the duration
     * @param nanoseconds
     *            the number of nanoseconds in the duration
     * @throws IllegalArgumentException
     *             if the total number of milliseconds exceeds Long.MAX_LONG
     */
    public DayTimeDurationAttribute(boolean negative, long days, long hours, long minutes,
            long seconds, int nanoseconds) throws IllegalArgumentException {
        super(identifierURI);

        this.negative = negative;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.nanoseconds = nanoseconds;

        // Convert all the components except nanoseconds to milliseconds

        // If any of the components is big (too big to be an int),
        // use the BigInteger class to do the math so we can detect
        // overflow.
        if ((days > Integer.MAX_VALUE) || (hours > Integer.MAX_VALUE)
                || (minutes > Integer.MAX_VALUE) || (seconds > Integer.MAX_VALUE)) {
            if (big24 == null) {
                big24 = BigInteger.valueOf(24);
                big60 = BigInteger.valueOf(60);
                big1000 = BigInteger.valueOf(1000);
                bigMaxLong = BigInteger.valueOf(Long.MAX_VALUE);
            }
            BigInteger bigDays = BigInteger.valueOf(days);
            BigInteger bigHours = BigInteger.valueOf(hours);
            BigInteger bigMinutes = BigInteger.valueOf(minutes);
            BigInteger bigSeconds = BigInteger.valueOf(seconds);

            BigInteger bigTotal = bigDays.multiply(big24).add(bigHours).multiply(big60).add(
                    bigMinutes).multiply(big60).add(bigSeconds).multiply(big1000);

            // If the result is bigger than Long.MAX_VALUE, we have an
            // overflow. Indicate an error (should be a processing error,
            // since it can be argued that we should handle gigantic
            // values for this).
            if (bigTotal.compareTo(bigMaxLong) == 1)
                throw new IllegalArgumentException("total number of " + "milliseconds "
                        + "exceeds Long.MAX_VALUE");
            // If no overflow, convert to a long.
            totalMillis = bigTotal.longValue();
        } else {
            // The numbers are small, so do it the fast way.
            totalMillis = ((((((days * 24) + hours) * 60) + minutes) * 60) + seconds) * 1000;
        }
    }

    /**
     * Returns a new <code>DayTimeDurationAttribute</code> that represents the xf:dayTimeDuration at
     * a particular DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>DayTimeDurationAttribute</code> representing the appropriate value (null
     *         if there is a parsing error)
     */
    public static DayTimeDurationAttribute getInstance(Node root) throws ParsingException,
            NumberFormatException {
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
     * Returns a new <code>DayTimeDurationAttribute</code> that represents the xf:dayTimeDuration
     * value indicated by the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>DayTimeDurationAttribute</code> representing the desired value (null if
     *         there is a parsing error)
     */
    public static DayTimeDurationAttribute getInstance(String value) throws ParsingException,
            NumberFormatException {
        boolean negative = false;
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        int nanoseconds = 0;

        // Compile the pattern, if not already done.
        // No thread-safety problem here. The worst that can
        // happen is that we initialize pattern several times.
        if (pattern == null) {
            try {
                pattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException e) {
                // This should never happen
                throw new ParsingException("unexpected pattern match error");
            }
        }

        // See if the value matches the pattern.
        Matcher matcher = pattern.matcher(value);
        boolean matches = matcher.matches();

        // If not, syntax error!
        if (!matches) {
            throw new ParsingException("Syntax error in dayTimeDuration");
        }

        // If the negative group matched, the value is negative.
        if (matcher.start(GROUP_SIGN) != -1)
            negative = true;

        try {
            // If the days group matched, parse that value.
            days = parseGroup(matcher, GROUP_DAYS);

            // If the hours group matched, parse that value.
            hours = parseGroup(matcher, GROUP_HOURS);

            // If the minutes group matched, parse that value.
            minutes = parseGroup(matcher, GROUP_MINUTES);

            // If the seconds group matched, parse that value.
            seconds = parseGroup(matcher, GROUP_SECONDS);

            // Special handling for fractional seconds, since
            // they can have any resolution.
            if (matcher.start(GROUP_NANOSECONDS) != -1) {
                String nanosecondString = matcher.group(GROUP_NANOSECONDS);

                // If there are less than 9 digits in the fractional seconds,
                // pad with zeros on the right so it's nanoseconds.
                while (nanosecondString.length() < 9)
                    nanosecondString += "0";

                // If there are more than 9 digits in the fractional seconds,
                // drop the least significant digits.
                if (nanosecondString.length() > 9) {
                    nanosecondString = nanosecondString.substring(0, 9);
                }

                nanoseconds = Integer.parseInt(nanosecondString);
            }
        } catch (NumberFormatException e) {
            // If we run into a number that's too big to be a long
            // that's an error. Really, it's a processing error,
            // since one can argue that we should handle that.
            throw e;
        }

        // Here's a requirement that's not checked for in the pattern.
        // The designator 'T' must be absent if all the time
        // items are absent. So the string can't end in 'T'.
        // Note that we don't have to worry about a zero length
        // string, since the pattern won't allow that.
        if (value.charAt(value.length() - 1) == 'T')
            throw new ParsingException("'T' must be absent if all" + "time items are absent");

        // If parsing went OK, create a new DayTimeDurationAttribute object and
        // return it.
        return new DayTimeDurationAttribute(negative, days, hours, minutes, seconds, nanoseconds);
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
     * Gets the number of days.
     * 
     * @return the number of days
     */
    public long getDays() {
        return days;
    }

    /**
     * Gets the number of hours.
     * 
     * @return the number of hours
     */
    public long getHours() {
        return hours;
    }

    /**
     * Gets the number of minutes.
     * 
     * @return the number of minutes
     */
    public long getMinutes() {
        return minutes;
    }

    /**
     * Gets the number of seconds.
     * 
     * @return the number of seconds
     */
    public long getSeconds() {
        return seconds;
    }

    /**
     * Gets the number of nanoseconds.
     * 
     * @return the number of nanoseconds
     */
    public int getNanoseconds() {
        return nanoseconds;
    }

    /**
     * Gets the total number of round seconds (in milliseconds).
     * 
     * @return the total number of seconds (in milliseconds)
     */
    public long getTotalSeconds() {
        return totalMillis;
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
        if (!(o instanceof DayTimeDurationAttribute))
            return false;

        DayTimeDurationAttribute other = (DayTimeDurationAttribute) o;

        return ((totalMillis == other.totalMillis) && (nanoseconds == other.nanoseconds) && (negative == other.negative));
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        // The totalMillis, nanoseconds, and negative fields are all considered
        // by the equals method, so it's best if the hashCode is derived
        // from all of those fields.
        int hashCode = (int) totalMillis ^ (int) (totalMillis >> 32);
        hashCode = 31 * hashCode + nanoseconds;
        if (negative)
            hashCode = -hashCode;
        return hashCode;
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("DayTimeDurationAttribute: [\n");
        sb.append("  Negative: " + negative);
        sb.append("  Days: " + days);
        sb.append("  Hours: " + hours);
        sb.append("  Minutes: " + minutes);
        sb.append("  Seconds: " + seconds);
        sb.append("  Nanoseconds: " + nanoseconds);
        sb.append("  TotalSeconds: " + totalMillis);
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

        // Length is quite variable
        StringBuffer buf = new StringBuffer(10);

        if (negative)
            buf.append('-');
        buf.append('P');
        if (days != 0) {
            buf.append(Long.toString(days));
            buf.append('D');
        }
        if ((hours != 0) || (minutes != 0) || (seconds != 0) || (nanoseconds != 0)) {
            // Only include the T if there are some time fields
            buf.append('T');
        } else {
            // Make sure that there's always at least one field specified
            if (days == 0)
                buf.append("0D");
        }
        if (hours != 0) {
            buf.append(Long.toString(hours));
            buf.append('H');
        }
        if (minutes != 0) {
            buf.append(Long.toString(minutes));
            buf.append('M');
        }
        if ((seconds != 0) || (nanoseconds != 0)) {
            buf.append(Long.toString(seconds));
            if (nanoseconds != 0) {
                buf.append('.');
                buf.append(DateAttribute.zeroPadInt(nanoseconds, 9));
            }
            buf.append('S');
        }

        encodedValue = buf.toString();

        return encodedValue;
    }
}
