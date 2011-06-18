/*
 * @(#)DateAttribute.java
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

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.w3c.dom.Node;

/**
 * Representation of an xs:date value. This class supports parsing xs:date values. All objects of
 * this class are immutable and thread-safe. The <code>Date</code> objects returned are not, but
 * these objects are cloned before being returned.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 * @author Steve Hanna
 */
public class DateAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#date";

    /**
     * URI version of name for this type
     * <p>
     * This object is used for synchronization whenever we need protection across this whole class.
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Parser for dates with no time zones
     * <p>
     * This field is only initialized if needed (by initParsers()).
     * <p>
     * NOTE: This object should only be accessed from code that has synchronized on it, since
     * SimpleDateFormat objects are not thread-safe. If this is causing performance problems, we
     * could easily make this a method variable in methods that use it instead of a class field. But
     * that would mean we'd need to spend a lot more time creating these objects.
     */
    private static DateFormat simpleParser;

    /**
     * Parser for dates with RFC 822 time zones (like +0300)
     * <p>
     * This field is only initialized if needed (by initParsers()).
     * <p>
     * NOTE: This object should only be accessed from code that has a lock on it, since
     * SimpleDateFormat objects are not thread-safe.
     */
    private static DateFormat zoneParser;

    /**
     * Calendar for GMT
     * <p>
     * NOTE: This object should only be accessed from code that has a lock on it, since Calendar
     * objects are not generally thread-safe.
     */
    private static Calendar gmtCalendar;

    /**
     * Number of nanoseconds per millisecond (shared by other classes in this package)
     */
    static final int NANOS_PER_MILLI = 1000000;

    /**
     * Number of milliseconds per second (shared by other classes in this package)
     */
    static final int MILLIS_PER_SECOND = 1000;

    /**
     * Number of seconds in a minute (shared by other classes in this package)
     */
    static final int SECONDS_PER_MINUTE = 60;

    /**
     * Number of minutes in an hour (shared by other classes in this package)
     */
    static final int MINUTES_PER_HOUR = 60;

    /**
     * Number of hours in a day (shared by other classes in this package)
     */
    static final int HOURS_PER_DAY = 24;

    /**
     * Number of nanoseconds per second (shared by other classes in this package)
     */
    static final int NANOS_PER_SECOND = NANOS_PER_MILLI * MILLIS_PER_SECOND;

    /**
     * Number of milliseconds in a minute (shared by other classes in this package)
     */
    static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;

    /**
     * Number of milliseconds in an hour (shared by other classes in this package)
     */
    static final int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * MINUTES_PER_HOUR;

    /**
     * Number of milliseconds in a day (shared by other classes in this package)
     */
    static final long MILLIS_PER_DAY = MILLIS_PER_HOUR * HOURS_PER_DAY;

    /**
     * Time zone value that indicates that the time zone was not specified.
     */
    public static final int TZ_UNSPECIFIED = -1000000;

    /**
     * The instant (in GMT) at which the specified date began (midnight) in the specified time zone.
     * If no time zone was specified, the local time zone is used.
     */
    private Date value;

    /**
     * The time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The offset to
     * GMT, in minutes.
     */
    private int timeZone;

    /**
     * The time zone actually used for this object (if it was originally unspecified, the default
     * time zone used). The offset to GMT, in minutes.
     */
    private int defaultedTimeZone;

    /**
     * Cached encoded value (null if not cached yet).
     */
    private String encodedValue = null;

    /**
     * Creates a new <code>TimeAttribute</code> that represents the current date in the default time
     * zone.
     */
    public DateAttribute() {
        this(new Date());
    }

    /**
     * Creates a new <code>TimeAttribute</code> that represents the given date with default timezone
     * values.
     * 
     * @param date
     *            a <code>Date</code> object representing the instant at which the specified date
     *            began (midnight) in the specified time zone (the actual time value will be forced
     *            to midnight)
     */
    public DateAttribute(Date date) {
        super(identifierURI);

        // Get the current time and GMT offset
        int currOffset = DateTimeAttribute.getDefaultTZOffset(date);
        long millis = date.getTime();

        // Now find out the last time it was midnight local time
        // (actually the last time it was midnight with the current
        // GMT offset, but that's good enough).

        // Skip back by time zone offset.
        millis += currOffset * MILLIS_PER_MINUTE;
        // Reset to last GMT midnight
        millis -= millis % MILLIS_PER_DAY;
        // Skip forward by time zone offset.
        millis -= currOffset * MILLIS_PER_MINUTE;
        date.setTime(millis);
        init(date, currOffset, currOffset);
    }

    /**
     * Creates a new <code>DateAttribute</code> that represents the date supplied.
     * 
     * @param date
     *            a <code>Date</code> object representing the instant at which the specified date
     *            began (midnight) in the specified time zone
     * @param timeZone
     *            the time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The
     *            offset to GMT, in minutes.
     * @param defaultedTimeZone
     *            the time zone actually used for this object (if it was originally unspecified, the
     *            default time zone used). The offset to GMT, in minutes.
     */
    public DateAttribute(Date date, int timeZone, int defaultedTimeZone) {
        super(identifierURI);

        init(date, timeZone, defaultedTimeZone);
    }

    /**
     * Initialization code shared by constructors.
     * 
     * @param date
     *            a <code>Date</code> object representing the instant at which the specified date
     *            began (midnight) in the specified time zone.
     * @param timeZone
     *            the time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The
     *            offset to GMT, in minutes.
     * @param defaultedTimeZone
     *            the time zone actually used for this object (if it was originally unspecified, the
     *            default time zone used). The offset to GMT, in minutes.
     */
    private void init(Date date, int timeZone, int defaultedTimeZone) {

        this.value = (Date) date.clone();
        this.timeZone = timeZone;
        this.defaultedTimeZone = defaultedTimeZone;
    }

    /**
     * Returns a new <code>DateAttribute</code> that represents the xs:date at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>DateAttribute</code> representing the appropriate value (null if there is
     *         a parsing error)
     */
    public static DateAttribute getInstance(Node root) throws ParseException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>DateAttribute</code> that represents the xs:date value indicated by the
     * string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>DateAttribute</code> representing the desired value (null if there is a
     *         parsing error)
     */
    public static DateAttribute getInstance(String value) throws ParseException {
        Date dateValue = null;
        int timeZone;
        int defaultedTimeZone;

        if (simpleParser == null)
            initParsers();

        // If string ends with Z, it's in GMT. Chop off the Z and
        // add +0000 to make the time zone explicit, then parse it
        // with the timezone parser.
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1) + "+0000";
            dateValue = strictParse(zoneParser, value);
            timeZone = 0;
            defaultedTimeZone = 0;
        } else {
            // If string ends with :XX, it must have a time zone
            // or be invalid. Strip off the possible time zone and
            // make sure what's left is a valid simple date. If so,
            // reformat the time zone by stripping out the colon
            // and parse the whole thing with the timezone parser.
            int len = value.length();

            if ((len > 6) && (value.charAt(len - 3) == ':')) {
                Date gmtValue = strictParse(zoneParser, value.substring(0, len - 6) + "+0000");
                value = value.substring(0, len - 3) + value.substring(len - 2, len);
                dateValue = strictParse(zoneParser, value);
                timeZone = (int) (gmtValue.getTime() - dateValue.getTime());
                timeZone = timeZone / 60000;
                defaultedTimeZone = timeZone;
            } else {
                // No funny business. This must be a simple date.
                dateValue = strictParse(simpleParser, value);
                timeZone = TZ_UNSPECIFIED;
                Date gmtValue = strictParse(zoneParser, value + "+0000");
                defaultedTimeZone = (int) (gmtValue.getTime() - dateValue.getTime());
                defaultedTimeZone = defaultedTimeZone / 60000;
            }
        }

        // If parsing went OK, create a new DateAttribute object and
        // return it.
        DateAttribute attr = new DateAttribute(dateValue, timeZone, defaultedTimeZone);
        return attr;
    }

    /**
     * Parse a String using a DateFormat parser, requiring that the entire String be consumed by the
     * parser. On success, return a Date. On failure, throw a ParseException.
     * <p>
     * Synchronize on the parser object when using it, since we assume they're the shared static
     * objects in this class.
     */
    private static Date strictParse(DateFormat parser, String str) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        Date ret;
        synchronized (parser) {
            ret = parser.parse(str, pos);
        }
        if (pos.getIndex() != str.length())
            throw new ParseException("", 0);
        return ret;
    }

    /**
     * Initialize the parser objects.
     */
    private static void initParsers() {
        // If simpleParser is already set, we're done.
        if (simpleParser != null)
            return;

        // Synchronize on identifierURI while initializing parsers
        // so we don't end up using a half-way initialized parser
        synchronized (identifierURI) {
            // This simple parser has no time zone
            simpleParser = new SimpleDateFormat("yyyy-MM-dd");
            simpleParser.setLenient(false);

            // This parser has a four digit offset to GMT with sign
            zoneParser = new SimpleDateFormat("yyyy-MM-ddZ");
            zoneParser.setLenient(false);
        }
    }

    /**
     * Gets the date represented by this object. The return value is a <code>Date</code> object
     * representing the instant at which the specified date began (midnight) in the time zone.
     * <p>
     * <b>NOTE:</b> The <code>Date</code> object is cloned before it is returned to avoid
     * unauthorized changes.
     * 
     * @return a <code>Date</code> object representing the instant at which the date began
     */
    public Date getValue() {
        return (Date) value.clone();
    }

    /**
     * Gets the specified time zone of this object (or TZ_UNSPECIFIED if unspecified).
     * 
     * @return the offset to GMT in minutes (positive or negative)
     */
    public int getTimeZone() {
        return timeZone;
    }

    /**
     * Gets the time zone actually used for this object (if it was originally unspecified, the
     * default time zone used).
     * 
     * @return the offset to GMT in minutes (positive or negative)
     */
    public int getDefaultedTimeZone() {
        return defaultedTimeZone;
    }

    /**
     * Returns true if the input is an instance of this class and if its value equals the value
     * contained in this class.
     * <p>
     * Two <code>DateAttribute</code>s are equal if and only if the instant on which the date began
     * is equal. This means that they must have the same time zone.
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof DateAttribute))
            return false;

        DateAttribute other = (DateAttribute) o;

        return value.equals(other.value);
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        // Only the value field is considered by the equals method, so only
        // that field should be considered by this method.
        return value.hashCode();
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("DateAttribute: [\n");
        sb.append("  Date: " + value + " local time");
        sb.append("  TimeZone: " + timeZone);
        sb.append("  Defaulted TimeZone: " + defaultedTimeZone);
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

        if (timeZone == TZ_UNSPECIFIED) {
            // If no time zone was specified, format Date value in
            // local time with no time zone string.
            initParsers();
            synchronized (simpleParser) {
                encodedValue = simpleParser.format(value);
            }
        } else {
            // If a time zone was specified, don't use SimpleParser
            // because it can only format dates in the local (default)
            // time zone. And the offset between that time zone and the
            // time zone we need to display can vary in complicated ways.

            // Instead, do it ourselves using our formatDateWithTZ method.
            encodedValue = formatDateWithTZ();
        }
        return encodedValue;
    }

    /**
     * Encodes the value of this object as an xsi:date. Only for use when the time zone is
     * specified.
     * 
     * @return a <code>String</code> form of the value
     */
    private String formatDateWithTZ() {
        if (gmtCalendar == null) {
            TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

            // Locale doesn't make much difference here. We don't use
            // any of the strings in the Locale and we don't do anything
            // that depends on week count conventions. We use the US
            // locale because it's always around and it ensures that we
            // will always get a Gregorian calendar, which is necessary
            // for compliance with ISO 8501.
            gmtCalendar = Calendar.getInstance(gmtTimeZone, Locale.US);
        }

        // "YYYY-MM-DD+hh:mm".length() = 16
        // Length may be longer if years < -999 or > 9999
        StringBuffer buf = new StringBuffer(16);

        synchronized (gmtCalendar) {
            // Start with the GMT instant when the date started in the
            // specified time zone (would be 7:00 PM the preceding day
            // if the specified time zone was +0500).
            gmtCalendar.setTime(value);
            // Bump by the timeZone (so we get the right date/time that
            // that we want to format)
            gmtCalendar.add(Calendar.MINUTE, timeZone);

            // Now, assemble the string
            int year = gmtCalendar.get(Calendar.YEAR);
            buf.append(zeroPadInt(year, 4));
            buf.append('-');
            // JANUARY is 0
            int month = gmtCalendar.get(Calendar.MONTH) + 1;
            buf.append(zeroPadInt(month, 2));
            buf.append('-');
            int dom = gmtCalendar.get(Calendar.DAY_OF_MONTH);
            buf.append(zeroPadInt(dom, 2));
        }

        int tzNoSign = timeZone;
        if (timeZone < 0) {
            tzNoSign = -tzNoSign;
            buf.append('-');
        } else
            buf.append('+');
        int tzHours = tzNoSign / 60;
        buf.append(zeroPadInt(tzHours, 2));
        buf.append(':');
        int tzMinutes = tzNoSign % 60;
        buf.append(zeroPadInt(tzMinutes, 2));

        return buf.toString();
    }

    /**
     * Takes a String representation of an integer (an optional sign followed by digits) and pads it
     * with zeros on the left until it has at least the specified number of digits. Note that this
     * function will work for an integer of any size: int, long, etc.
     * 
     * @param unpadded
     *            the unpadded <code>String</code> (must have length of at least one)
     * @param minDigits
     *            the minimum number of digits desired
     * @return the padded <code>String</code>
     */
    static String zeroPadIntString(String unpadded, int minDigits) {
        int len = unpadded.length();

        // Get the sign character (or 0 if none)
        char sign = unpadded.charAt(0);
        if ((sign != '-') && (sign != '+'))
            sign = 0;

        // The number of characters required is the number of digits,
        // plus one for the sign if present.
        int minChars = minDigits;
        if (sign != 0)
            minChars++;

        // If we already have that many characters, we're done.
        if (len >= minChars)
            return unpadded;

        // Otherwise, create the buffer
        StringBuffer buf = new StringBuffer();

        // Copy in the sign first, if present
        if (sign != 0) {
            buf.append(sign);
        }

        // Add the zeros
        int zerosNeeded = minChars - len;
        while (zerosNeeded-- != 0)
            buf.append('0');

        // Copy the rest of the unpadded string
        if (sign != 0) {
            // Skip sign
            buf.append(unpadded.substring(1, len));
        } else {
            buf.append(unpadded);
        }

        return buf.toString();
    }

    /**
     * Converts an integer to a base 10 string and pads it with zeros on the left until it has at
     * least the specified number of digits. Note that the length of the resulting string will be
     * greater than minDigits if the number is negative since the string will start with a minus
     * sign.
     * 
     * @param intValue
     *            the integer to convert
     * @param minDigits
     *            the minimum number of digits desired
     * @return the padded <code>String</code>
     */
    static String zeroPadInt(int intValue, int minDigits) {
        return zeroPadIntString(Integer.toString(intValue), minDigits);
    }
}
