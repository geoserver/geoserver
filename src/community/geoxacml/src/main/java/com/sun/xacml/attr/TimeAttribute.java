/*
 * @(#)TimeAttribute.java
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
import java.text.ParseException;
import java.util.Date;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.ProcessingException;

/**
 * Representation of an xs:time value. This class supports parsing xs:time values. All objects of
 * this class are immutable and thread-safe. The <code>Date</code> objects returned are not, but
 * these objects are cloned before being returned.
 * 
 * @since 1.0
 * @author Steve Hanna
 * @author Seth Proctor
 */
public class TimeAttribute extends AttributeValue {
    /**
     * Official name of this type
     */
    public static final String identifier = "http://www.w3.org/2001/XMLSchema#time";

    /**
     * URI version of name for this type
     * <p>
     * This object is used for synchronization whenever we need protection across this whole class.
     */
    private static final URI identifierURI = URI.create(identifier);

    /**
     * Time zone value that indicates that the time zone was not specified.
     */
    public static final int TZ_UNSPECIFIED = -1000000;

    /**
     * The time that this object represents in second resolution, in milliseconds GMT, with zero
     * being midnight. If no time zone was specified, the local time zone is used to convert to
     * milliseconds relative to GMT.
     */
    private long timeGMT;

    /**
     * The number of nanoseconds beyond the time given by the timeGMT field. The XML Query document
     * says that fractional seconds must be supported down to at least 100 nanosecond resolution.
     * The Date class only supports milliseconds, so we include here support for nanosecond
     * resolution.
     */
    private int nanoseconds;

    // NOTE: now that we're not using a Date object, the above two variables
    // could be condensed, and the interface could be changed so we don't
    // need to worry about tracking the time values separately

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
     * Creates a new <code>TimeAttribute</code> that represents the current time in the current time
     * zone.
     */
    public TimeAttribute() {
        this(new Date());
    }

    /**
     * Creates a new <code>TimeAttribute</code> that represents the given time but uses the default
     * timezone and offset values.
     * 
     * @param time
     *            a <code>Date</code> object representing the specified time down to second
     *            resolution. This date should have a date of 01/01/1970. If it does not, such a
     *            date will be forced. If this object has non-zero milliseconds, they are combined
     *            with the nanoseconds parameter.
     */
    public TimeAttribute(Date time) {
        super(identifierURI);

        int currOffset = DateTimeAttribute.getDefaultTZOffset(time);
        init(time, 0, currOffset, currOffset);
    }

    /**
     * Creates a new <code>TimeAttribute</code> that represents the time supplied.
     * 
     * @param time
     *            a <code>Date</code> object representing the specified time down to second
     *            resolution. This date should have a date of 01/01/1970. If it does not, such a
     *            date will be forced. If this object has non-zero milliseconds, they are combined
     *            with the nanoseconds parameter.
     * @param nanoseconds
     *            the number of nanoseconds beyond the Date specified in the date parameter
     * @param timeZone
     *            the time zone specified for this object (or TZ_UNSPECIFIED if unspecified). The
     *            offset to GMT, in minutes.
     * @param defaultedTimeZone
     *            the time zone actually used for this object, which must be specified. The offset
     *            to GMT, in minutes.
     */
    public TimeAttribute(Date time, int nanoseconds, int timeZone, int defaultedTimeZone) {
        super(identifierURI);

        // if the timezone is unspecified, it's illegal for the defaulted
        // timezone to also be unspecified
        if ((timeZone == TZ_UNSPECIFIED) && (defaultedTimeZone == TZ_UNSPECIFIED))
            throw new ProcessingException("default timezone must be specified"
                    + "when a timezone is provided");

        init(time, nanoseconds, timeZone, defaultedTimeZone);
    }

    /**
     * Initialization code shared by constructors.
     * 
     * @param date
     *            a <code>Date</code> object representing the specified time down to second
     *            resolution. This date should have a date of 01/01/1970. If it does not, such a
     *            date will be forced. If this object has non-zero milliseconds, they are combined
     *            with the nanoseconds parameter.
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

        // get a temporary copy of the date
        Date tmpDate = (Date) (date.clone());

        // Combine the nanoseconds so they are between 0 and 999,999,999
        this.nanoseconds = DateTimeAttribute.combineNanos(tmpDate, nanoseconds);

        // now that the date has been (potentially) updated, store the time
        this.timeGMT = tmpDate.getTime();

        // keep track of the timezone values
        this.timeZone = timeZone;
        this.defaultedTimeZone = defaultedTimeZone;

        // Check that the date is normalized to 1/1/70
        if ((timeGMT >= DateAttribute.MILLIS_PER_DAY) || (timeGMT < 0)) {
            timeGMT = timeGMT % DateAttribute.MILLIS_PER_DAY;

            // if we had a negative value then we need to shift by a day
            if (timeGMT < 0)
                timeGMT += DateAttribute.MILLIS_PER_DAY;
        }
    }

    /**
     * Returns a new <code>TimeAttribute</code> that represents the xs:time at a particular DOM
     * node.
     * 
     * @param root
     *            the <code>Node</code> that contains the desired value
     * @return a new <code>TimeAttribute</code> representing the appropriate value (null if there is
     *         a parsing error)
     */
    public static TimeAttribute getInstance(Node root) throws ParsingException,
            NumberFormatException, ParseException {
        return getInstance(root.getFirstChild().getNodeValue());
    }

    /**
     * Returns a new <code>TimeAttribute</code> that represents the xs:time value indicated by the
     * string provided.
     * 
     * @param value
     *            a string representing the desired value
     * @return a new <code>TimeAttribute</code> representing the desired value (null if there is a
     *         parsing error)
     * @throws ParsingException
     *             if any problems occurred while parsing
     */
    public static TimeAttribute getInstance(String value) throws ParsingException,
            NumberFormatException, ParseException {
        // Prepend date string for Jan 1 1970 and use the
        // DateTimeAttribute parsing code.

        value = "1970-01-01T" + value;

        DateTimeAttribute dateTime = DateTimeAttribute.getInstance(value);

        // if there was no explicit TZ provided, then we want to make sure
        // the that the defaulting is done correctly, especially since 1/1/70
        // is always out of daylight savings time

        Date dateValue = dateTime.getValue();
        int defaultedTimeZone = dateTime.getDefaultedTimeZone();
        if (dateTime.getTimeZone() == TZ_UNSPECIFIED) {
            // TimeZone localTZ = TimeZone.getDefault();
            int newDefTimeZone = DateTimeAttribute.getDefaultTZOffset(new Date());
            dateValue = new Date(dateValue.getTime() - (newDefTimeZone - defaultedTimeZone)
                    * DateAttribute.MILLIS_PER_MINUTE);
            defaultedTimeZone = newDefTimeZone;
        }

        return new TimeAttribute(dateValue, dateTime.getNanoseconds(), dateTime.getTimeZone(),
                defaultedTimeZone);
    }

    /**
     * Gets the time represented by this object. The return value is a <code>Date</code> object
     * representing the specified time down to second resolution with a date of January 1, 1970.
     * Subsecond values are handled by the {@link #getNanoseconds getNanoseconds} method.
     * 
     * @return a <code>Date</code> object representing the time represented by this object
     */
    public Date getValue() {
        return new Date(timeGMT);
    }

    /**
     * Gets the number of milliseconds since midnight GMT that this attribute value represents. This
     * is the same time returned by <code>getValue</code>, and likewise the milliseconds are
     * provided with second resolution.
     * 
     * @return milliseconds since midnight GMT
     */
    public long getMilliseconds() {
        return timeGMT;
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
     * 
     * @param o
     *            the object to compare
     * 
     * @return true if this object and the input represent the same value
     */
    public boolean equals(Object o) {
        if (!(o instanceof TimeAttribute))
            return false;

        TimeAttribute other = (TimeAttribute) o;

        return (timeGMT == other.timeGMT && (nanoseconds == other.nanoseconds));
    }

    /**
     * Returns the hashcode value used to index and compare this object with others of the same
     * type. Typically this is the hashcode of the backing data object.
     * 
     * @return the object's hashcode value
     */
    public int hashCode() {
        // the standard Date hashcode is used here...
        int hashCode = (int) (timeGMT ^ (timeGMT >>> 32));

        // ...but both the timeGMT and the nanoseconds fields are considered
        // by the equals method, so it's best if the hashCode is derived
        // from both of those fields.
        hashCode = (31 * hashCode) + nanoseconds;

        return hashCode;
    }

    /**
     * Converts to a String representation.
     * 
     * @return the String representation
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("TimeAttribute: [\n");

        // calculate the GMT value of this time
        long secsGMT = timeGMT / 1000;
        long minsGMT = secsGMT / 60;
        secsGMT = secsGMT % 60;
        long hoursGMT = minsGMT / 60;
        minsGMT = minsGMT % 60;

        // put the right number of zeros in place
        String hoursStr = (hoursGMT < 10) ? "0" + hoursGMT : "" + hoursGMT;
        String minsStr = (minsGMT < 10) ? "0" + minsGMT : "" + minsGMT;
        String secsStr = (secsGMT < 10) ? "0" + secsGMT : "" + secsGMT;

        sb.append("  Time GMT: " + hoursStr + ":" + minsStr + ":" + secsStr);
        sb.append("  Nanoseconds: " + nanoseconds);
        sb.append("  TimeZone: " + timeZone);
        sb.append("  Defaulted TimeZone: " + defaultedTimeZone);
        sb.append("]");

        return sb.toString();
    }

    /**
     * Encodes the value in a form suitable for including in XML data like a request or an
     * obligation. This returns a time value that could in turn be used by the factory to create a
     * new instance with the same value.
     * 
     * @return a <code>String</code> form of the value
     */
    public String encode() {
        if (encodedValue != null)
            return encodedValue;

        // "hh:mm:ss.sssssssss+hh:mm".length() = 27
        StringBuffer buf = new StringBuffer(27);

        // get the correct time for the timezone being used
        int millis = (int) timeGMT;
        if (timeZone == TZ_UNSPECIFIED)
            millis += (defaultedTimeZone * DateAttribute.MILLIS_PER_MINUTE);
        else
            millis += (timeZone * DateAttribute.MILLIS_PER_MINUTE);

        if (millis < 0) {
            millis += DateAttribute.MILLIS_PER_DAY;
        } else if (millis >= DateAttribute.MILLIS_PER_DAY) {
            millis -= DateAttribute.MILLIS_PER_DAY;
        }

        // now generate the time string
        int hour = millis / DateAttribute.MILLIS_PER_HOUR;
        millis = millis % DateAttribute.MILLIS_PER_HOUR;
        buf.append(DateAttribute.zeroPadInt(hour, 2));
        buf.append(':');
        int minute = millis / DateAttribute.MILLIS_PER_MINUTE;
        millis = millis % DateAttribute.MILLIS_PER_MINUTE;
        buf.append(DateAttribute.zeroPadInt(minute, 2));
        buf.append(':');
        int second = millis / DateAttribute.MILLIS_PER_SECOND;
        buf.append(DateAttribute.zeroPadInt(second, 2));

        // add any nanoseconds
        if (nanoseconds != 0) {
            buf.append('.');
            buf.append(DateAttribute.zeroPadInt(nanoseconds, 9));
        }

        // if there is a specified timezone, then include that in the encoding
        if (timeZone != TZ_UNSPECIFIED) {
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
        }

        // remember the encoding for later
        encodedValue = buf.toString();

        return encodedValue;
    }

}
