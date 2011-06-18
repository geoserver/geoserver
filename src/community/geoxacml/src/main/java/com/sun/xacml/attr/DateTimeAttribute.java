/*
 * @(#)DateTimeAttribute.java
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

import com.sun.xacml.ParsingException;

/**
 * Representation of an xs:dateTime value. This class supports parsing xs:dateTime values. All
 * objects of this class are immutable and thread-safe. The <code>Date</code> objects returned are
 * not, but these objects are cloned before being returned.
 * 
 * @since 1.0
 * @author Marco Barreno
 * @author Seth Proctor
 * @author Steve Hanna
 */
public class DateTimeAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#dateTime";

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
     * NOTE: This object should only be accessed from code that has synchronized on it, since
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
     * Time zone value that indicates that the time zone was not specified.
     */
    public static final int TZ_UNSPECIFIED = -1000000;

    /**
     * The actual date and time that this object represents (in GMT, as with all Date objects). If
     * no time zone was specified, the local time zone is used to convert to GMT.
     * <p>
     * This Date does not include fractions of a second. Those are handled by the separate
     * nanoseconds field, since Date only provides millisecond accuracy and the XML Query spec
     * requires at least 100 nanosecond accuracy.
     */
    private Date value;

    /**
     * The number of nanoseconds beyond the Date given by the value field. The XML Query document
     * says that fractional seconds must be supported down to at least 100 nanosecond resolution.
     * The Date class only supports milliseconds, so we include here support for nanosecond
     * resolution.
     */
    private int nanoseconds;

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
     * Creates a new <code>DateTimeAttribute</code> that represents the current date in the default
     * time zone.
     */
    public DateTimeAttribute() {
        this(new Date());
    }

    /**
     * Creates a new <code>DateTimeAttribute</code> that represents the supplied date but uses
     * default timezone and offset values.
     * 
     * @param dateTime
     *            a <code>Date</code> object representing the specified date and time down to second
     *            resolution. If this object has non-zero milliseconds, they are combined with the
     *            nanoseconds parameter.
     */
    public DateTimeAttribute(Date dateTime) {
        super(identifierURI);

        int currOffset = getDefaultTZOffset(dateTime);
        init(dateTime, 0, currOffset, currOffset);
    }

    /**
     * Creates a new <code>DateTimeAttribute</code> that represents the date supplied.
     * 
     * @param dateTime
     *            a <code>Date</code> object representing the specified date and time down to second
     *            resolution. If this object has non-zero milliseconds, they are combined with the
     *            nanoseconds parameter.
     * @param nanoseconds
     *            the number of nanoseconds beyond the Date specified in the date parameter
     * @param timeZone
     *            the time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The
     *            offset to GMT, in minutes.
     * @param defaultedTimeZone
     *            the time zone actually used for this object (if it was originally unspecified, the
     *            default time zone used). The offset to GMT, in minutes.
     */
    public DateTimeAttribute(Date dateTime, int nanoseconds, int timeZone, int defaultedTimeZone) {
        super(identifierURI);

        init(dateTime, nanoseconds, timeZone, defaultedTimeZone);
    }

    /**
     * Initialization code shared by constructors.
     * 
     * @param date
     *            a <code>Date</code> object representing the specified date and time down to second
     *            resolution. If this object has non-zero milliseconds, they are combined with the
     *            nanoseconds parameter.
     * @param nanoseconds
     *            the number of nanoseconds beyond the Date specified in the date parameter
     * @param timeZone
     *            the time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The
     *            offset to GMT, in minutes.
     * @param defaultedTimeZone
     *            the time zone actually used for this object (if it was originally unspecified, the
     *            default time zone used). The offset to GMT, in minutes.
     */
    private void init(Date date, int nanoseconds, int timeZone, int defaultedTimeZone) {

        // Make a new Date object
        this.value = (Date) date.clone();
        // Combine the nanoseconds so they are between 0 and 999,999,999
        this.nanoseconds = combineNanos(this.value, nanoseconds);
        this.timeZone = timeZone;
        this.defaultedTimeZone = defaultedTimeZone;
    }

    /**
     * Returns a new <code>DateTimeAttribute</code> that represents the xs:dateTime at a particular
     * DOM node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>DateTimeAttribute</code> representing the appropriate value
     * @throws ParsingException
     *             if any problems occurred while parsing
     */
    public static DateTimeAttribute getInstance(Node root) throws ParsingException,
            NumberFormatException, ParseException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>DateTimeAttribute</code> that represents the xs:dateTime value indicated
     * by the string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>DateTimeAttribute</code> representing the desired value
     * @throws ParsingException
     *             if the text is formatted incorrectly
     * @throws NumberFormatException
     *             if the nanosecond format is incorrect
     * @throws ParseException
     */
    public static DateTimeAttribute getInstance(String value) throws ParsingException,
            NumberFormatException, ParseException {
        Date dateValue = null;
        int nanoseconds = 0;
        int timeZone;
        int defaultedTimeZone;

        initParsers();

        // If string ends with Z, it's in GMT. Chop off the Z and
        // add +00:00 to make the time zone explicit.
        if (value.endsWith("Z"))
            value = value.substring(0, value.length() - 1) + "+00:00";

        // Figure out if the string has a time zone.
        // If string ends with +XX:XX or -XX:XX, it must have
        // a time zone or be invalid.
        int len = value.length(); // This variable is often not up-to-date
        boolean hasTimeZone = ((value.charAt(len - 3) == ':') && ((value.charAt(len - 6) == '-') || (value
                .charAt(len - 6) == '+')));

        // If string contains a period, it must have fractional
        // seconds (or be invalid). Strip them out and put the
        // value in nanoseconds.
        int dotIndex = value.indexOf('.');
        if (dotIndex != -1) {
            // Decide where fractional seconds end.
            int secondsEnd = value.length();
            if (hasTimeZone)
                secondsEnd -= 6;
            // Copy the fractional seconds out of the string.
            String nanoString = value.substring(dotIndex + 1, secondsEnd);
            // Check that all those characters are ASCII digits.
            for (int i = nanoString.length() - 1; i >= 0; i--) {
                char c = nanoString.charAt(i);
                if ((c < '0') || (c > '9'))
                    throw new ParsingException("non-ascii digit found");
            }
            // If there are less than 9 digits in the fractional seconds,
            // pad with zeros on the right so it's nanoseconds.
            while (nanoString.length() < 9)
                nanoString += "0";
            // If there are more than 9 digits in the fractional seconds,
            // drop the least significant digits.
            if (nanoString.length() > 9) {
                nanoString = nanoString.substring(0, 9);
            }
            // Parse the fractional seconds.
            nanoseconds = Integer.parseInt(nanoString);

            // Remove the fractional seconds from the string.
            value = value.substring(0, dotIndex) + value.substring(secondsEnd, value.length());
        }

        // this is the code that may trow a ParseException
        if (hasTimeZone) {
            // Strip off the purported time zone and make sure what's
            // left is a valid unzoned date and time (by parsing in GMT).
            // If so, reformat the time zone by stripping out the colon
            // and parse the revised string with the timezone parser.

            len = value.length();

            Date gmtValue = strictParse(zoneParser, value.substring(0, len - 6) + "+0000");
            value = value.substring(0, len - 3) + value.substring(len - 2, len);
            dateValue = strictParse(zoneParser, value);
            timeZone = (int) (gmtValue.getTime() - dateValue.getTime());
            timeZone = timeZone / 60000;
            defaultedTimeZone = timeZone;
        } else {
            // No funny business. This must be a simple date and time.
            dateValue = strictParse(simpleParser, value);
            timeZone = TZ_UNSPECIFIED;
            // Figure out what time zone was used.
            Date gmtValue = strictParse(zoneParser, value + "+0000");
            defaultedTimeZone = (int) (gmtValue.getTime() - dateValue.getTime());
            defaultedTimeZone = defaultedTimeZone / 60000;
        }

        // If parsing went OK, create a new DateTimeAttribute object and
        // return it.

        DateTimeAttribute attr = new DateTimeAttribute(dateValue, nanoseconds, timeZone,
                defaultedTimeZone);
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
            simpleParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            simpleParser.setLenient(false);

            // This parser has a four digit offset to GMT with sign
            zoneParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            zoneParser.setLenient(false);
        }
    }

    /**
     * Gets the date and time represented by this object. The return value is a <code>Date</code>
     * object representing the specified date and time down to second resolution. Subsecond values
     * are handled by the {@link #getNanoseconds getNanoseconds} method.
     * <p>
     * <b>NOTE:</b> The <code>Date</code> object is cloned before it is returned to avoid
     * unauthorized changes.
     * 
     * @return a <code>Date</code> object representing the date and time represented by this object
     */
    public Date getValue() {
        return (Date) value.clone();
    }

    /**
     * Gets the nanoseconds of this object.
     * 
     * @return the number of nanoseconds
     */
    public int getNanoseconds() {
        return nanoseconds;
    }

    /**
     * Gets the time zone of this object (or TZ_UNSPECIFIED if unspecified).
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
     * Two <code>DateTimeAttribute</code>s are equal if and only if the dates and times represented
     * are identical (down to the nanosecond).
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof DateTimeAttribute))
            return false;

        DateTimeAttribute other = (DateTimeAttribute) o;

        // Since the value field is normalized into GMT, this is a
        // good way to compare.
        return (value.equals(other.value) && (nanoseconds == other.nanoseconds));
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        // Both the value field and the nanoseconds field are considered
        // by the equals method, so it's best if the hashCode is derived
        // from both of those fields.
        int hashCode = value.hashCode();
        hashCode = 31 * hashCode + nanoseconds;
        return hashCode;
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("DateTimeAttribute: [\n");
        sb.append("  Date: " + value + " local time");
        sb.append("  Nanoseconds: " + nanoseconds);
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
            if (nanoseconds != 0) {
                encodedValue = encodedValue + "." + DateAttribute.zeroPadInt(nanoseconds, 9);
            }
        } else {
            // If a time zone was specified, don't use SimpleParser
            // because it can only format dates in the local (default)
            // time zone. And the offset between that time zone and the
            // time zone we need to display can vary in complicated ways.

            // Instead, do it ourselves using our formatDateWithTZ method.
            encodedValue = formatDateTimeWithTZ();
        }
        return encodedValue;
    }

    /**
     * Encodes the value of this object as an xsi:dateTime. Only for use when the time zone is
     * specified.
     * 
     * @return a <code>String</code> form of the value
     */
    private String formatDateTimeWithTZ() {
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

        // "YYYY-MM-DDThh:mm:ss.sssssssss+hh:mm".length() = 35
        // Length may be longer if years < -999 or > 9999
        StringBuffer buf = new StringBuffer(35);

        synchronized (gmtCalendar) {
            // Start with the proper time in GMT.
            gmtCalendar.setTime(value);
            // Bump by the timeZone, since we're going to be extracting
            // the value in GMT
            gmtCalendar.add(Calendar.MINUTE, timeZone);

            // Now, assemble the string
            int year = gmtCalendar.get(Calendar.YEAR);
            buf.append(DateAttribute.zeroPadInt(year, 4));
            buf.append('-');
            // JANUARY is 0
            int month = gmtCalendar.get(Calendar.MONTH) + 1;
            buf.append(DateAttribute.zeroPadInt(month, 2));
            buf.append('-');
            int dom = gmtCalendar.get(Calendar.DAY_OF_MONTH);
            buf.append(DateAttribute.zeroPadInt(dom, 2));
            buf.append('T');
            int hour = gmtCalendar.get(Calendar.HOUR_OF_DAY);
            buf.append(DateAttribute.zeroPadInt(hour, 2));
            buf.append(':');
            int minute = gmtCalendar.get(Calendar.MINUTE);
            buf.append(DateAttribute.zeroPadInt(minute, 2));
            buf.append(':');
            int second = gmtCalendar.get(Calendar.SECOND);
            buf.append(DateAttribute.zeroPadInt(second, 2));
        }

        if (nanoseconds != 0) {
            buf.append('.');
            buf.append(DateAttribute.zeroPadInt(nanoseconds, 9));
        }

        int tzNoSign = timeZone;
        if (timeZone < 0) {
            tzNoSign = -tzNoSign;
            buf.append('-');
        } else
            buf.append('+');
        int tzHours = tzNoSign / 60;
        buf.append(DateAttribute.zeroPadInt(tzHours, 2));
        buf.append(':');
        int tzMinutes = tzNoSign % 60;
        buf.append(DateAttribute.zeroPadInt(tzMinutes, 2));

        return buf.toString();
    }

    /**
     * Gets the offset in minutes between the default time zone and UTC for the specified date.
     * 
     * @param the
     *            <code>Date</code> whose offset is desired
     * @return the offset in minutes
     */
    static int getDefaultTZOffset(Date date) {
        int offset = TimeZone.getDefault().getOffset(date.getTime());
        offset = offset / DateAttribute.MILLIS_PER_MINUTE;
        return offset;
    }

    /**
     * Combines a number of nanoseconds with a <code>Date</code> so that the Date has no fractional
     * seconds and the number of nanoseconds is non-negative and less than a second.
     * <p>
     * <b>WARNING</b>: This function changes the value stored in the date parameter!
     * 
     * @param date
     *            the <code>Date</code> to be combined (<b>value may be modified!</b>)
     * @param nanos
     *            the nanoseconds to be combined
     * @return the resulting number of nanoseconds
     */
    static int combineNanos(Date date, int nanoseconds) {
        long millis = date.getTime();
        int milliCarry = (int) (millis % DateAttribute.MILLIS_PER_SECOND);

        // If nothing needs fixing, get out quick
        if ((milliCarry == 0) && (nanoseconds > 0)
                && (nanoseconds < DateAttribute.NANOS_PER_SECOND))
            return nanoseconds;

        // Remove any non-zero milliseconds from the date.
        millis -= milliCarry;
        // Add them into the nanoseconds.
        long nanoTemp = nanoseconds;
        nanoTemp += milliCarry * DateAttribute.NANOS_PER_MILLI;
        // Get the nanoseconds that represent fractional seconds.
        // This we'll return.
        int nanoResult = (int) (nanoTemp % DateAttribute.NANOS_PER_SECOND);
        // Get nanoseconds that represent whole seconds.
        nanoTemp -= nanoResult;
        // Convert that to milliseconds and add it back to the date.
        millis += nanoTemp / DateAttribute.NANOS_PER_MILLI;
        date.setTime(millis);

        return nanoResult;
    }
}
