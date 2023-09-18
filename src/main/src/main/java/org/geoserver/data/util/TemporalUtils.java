/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.geotools.util.factory.Hints;
import org.geotools.xml.impl.DatatypeConverterImpl;

/**
 * Date and Time related util functions.
 *
 * @author Fernando Mino - Geosolutions
 */
public final class TemporalUtils {

    private TemporalUtils() {}

    /**
     * Returns a print ready string representation for a Date value, handling timezone
     * configurations and date/datetime difference.
     */
    public static String printDate(Date date) {
        if (date == null) return "null";
        Calendar calendar = toCalendar(date);
        // if it's only a date, no time involved
        if (date instanceof java.sql.Date) {
            return DatatypeConverterImpl.getInstance().printDate(calendar);
        } else {
            // timestamp handling
            return DatatypeConverterImpl.getInstance().printDateTime(calendar);
        }
    }

    private static Calendar toCalendar(Date date) {
        Object hint = Hints.getSystemDefault(Hints.LOCAL_DATE_TIME_HANDLING);
        Calendar calendar;
        if (Boolean.TRUE.equals(hint)) {
            calendar = Calendar.getInstance();
        } else {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        }
        calendar.clear();
        calendar.setTimeInMillis(date.getTime());
        return calendar;
    }

    /** Return true if DateTime format configuration is enabled. */
    public static boolean isDateTimeFormatEnabled() {
        Object hint = Hints.getSystemDefault(Hints.DATE_TIME_FORMAT_HANDLING);
        return !Boolean.FALSE.equals(hint);
    }

    /**
     * Serialize date to format specified
     *
     * @param date date to be converted
     * @param dateFormatPattern Pattern of dateFormat
     * @return converted dateTime text
     * @throws RuntimeException on conversion error
     */
    public static String serializeDateTime(Date date, String dateFormatPattern) {
        try {
            Object hint = Hints.getSystemDefault(Hints.LOCAL_DATE_TIME_HANDLING);

            Calendar cal = toCalendar(date);
            // if it's only a date, no time involved
            if (date instanceof java.sql.Date) {
                SimpleDateFormat formatter = new SimpleDateFormat(dateFormatPattern);
                return formatter.format(date);
            } else {
                // timestamp handling
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatPattern);
                ZonedDateTime zonedDateTime;
                if (Boolean.TRUE.equals(hint)) {
                    zonedDateTime = cal.toInstant().atZone(ZoneId.systemDefault());
                } else {
                    zonedDateTime = cal.toInstant().atZone(TimeZone.getTimeZone("UTC").toZoneId());
                }
                return zonedDateTime.format(formatter);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error formatting date" + ex);
        }
    }
}
