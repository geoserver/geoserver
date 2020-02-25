/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.geotools.util.DateRange;

/**
 * Formats date/times into ISO8601
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ISO8601Formatter {

    private final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    private void pad(StringBuilder buf, int value, int amt) {
        if (amt == 2 && value < 10) {
            buf.append('0');
        } else if (amt == 4 && value < 1000) {
            if (value >= 100) {
                buf.append("0");
            } else if (value >= 10) {
                buf.append("00");
            } else {
                buf.append("000");
            }
        } else if (amt == 3 && value < 100) {
            if (value >= 10) {
                buf.append('0');
            } else {
                buf.append("00");
            }
        }
        buf.append(value);
    }

    /**
     * Formats the specified object either as a single time, if it's a Date, or as a continuous
     * interval, if it's a DateRange (and will throw an {@link IllegalArgumentException} otherwise)
     */
    public String format(Object date) {
        if (date instanceof Date) {
            return format((Date) date);
        } else if (date instanceof DateRange) {
            DateRange range = (DateRange) date;
            StringBuilder sb = new StringBuilder();
            format(range.getMinValue(), sb);
            sb.append("/");
            format(range.getMaxValue(), sb);
            sb.append("/PT1S");
            return sb.toString();
        } else {
            throw new IllegalArgumentException(
                    "Date argument should be either a Date or a "
                            + "DateRange, however this one is neither: "
                            + date);
        }
    }

    /** Formats the specified Date in ISO8601 format */
    public String format(Date date) {
        return format(date, new StringBuilder()).toString();
    }

    public StringBuilder format(Date date, StringBuilder buf) {
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            if (year > 1) {
                buf.append('-');
            }
            year = year - 1;
        }
        pad(buf, year, 4);
        buf.append('-');
        pad(buf, cal.get(Calendar.MONTH) + 1, 2);
        buf.append('-');
        pad(buf, cal.get(Calendar.DAY_OF_MONTH), 2);
        buf.append('T');
        pad(buf, cal.get(Calendar.HOUR_OF_DAY), 2);
        buf.append(':');
        pad(buf, cal.get(Calendar.MINUTE), 2);
        buf.append(':');
        pad(buf, cal.get(Calendar.SECOND), 2);
        buf.append('.');
        pad(buf, cal.get(Calendar.MILLISECOND), 3);
        buf.append('Z');

        return buf;
    }
}
