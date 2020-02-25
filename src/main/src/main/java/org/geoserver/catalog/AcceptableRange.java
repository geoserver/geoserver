/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import org.geoserver.ows.kvp.TimeParser;
import org.geotools.util.DateRange;
import org.geotools.util.Range;

/**
 * Represents the parsed acceptable range. For elevation it's simple numbers, for dates it's a
 * number of milliseconds.
 */
public class AcceptableRange {

    /**
     * Parses the acceptable range
     *
     * @param spec The specification from the UI
     * @param dataType The target data type (e.g. {@link Date}
     * @return An {@link AcceptableRange} object, or null if the spec was null or empty
     */
    public static AcceptableRange getAcceptableRange(String spec, Class dataType)
            throws ParseException {
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }

        String[] split = spec.split("/");
        if (split.length > 2) {
            throw new IllegalArgumentException(
                    "Invalid acceptable range specification, must be either a single "
                            + "value, or two values split by a forward slash");
        }
        Number before = parseValue(split[0], dataType);
        Number after = before;
        if (split.length == 2) {
            after = parseValue(split[1], dataType);
        }
        // avoid complications in case the search range is empty
        if (before.doubleValue() == 0 && after.doubleValue() == 0) {
            return null;
        }
        return new AcceptableRange(before, after, dataType);
    }

    private static Number parseValue(String s, Class dataType) throws ParseException {
        if (Date.class.isAssignableFrom(dataType)) {
            return TimeParser.parsePeriod(s);
        }
        // TODO: add support for Number, e.g., elevation
        throw new IllegalArgumentException("Unsupported value type " + dataType);
    }

    private Number before;
    private Number after;
    private Class dataType;

    public AcceptableRange(Number before, Number after, Class dataType) {
        this.before = before;
        this.after = after;
        this.dataType = dataType;
    }

    public Range getSearchRange(Object value) {
        if (value instanceof Range) {
            Range range = (Range) value;
            Range before = getSearchRangeOnSingleValue(range.getMinValue());
            Range after = getSearchRangeOnSingleValue(range.getMaxValue());
            return before.union(after);
        } else {
            return getSearchRangeOnSingleValue(value);
        }
    }

    public Range getSearchRangeOnSingleValue(Object value) {
        if (Date.class.isAssignableFrom(dataType)) {
            Date center = (Date) value;
            Calendar cal = Calendar.getInstance();
            cal.setTime(center);
            cal.setTimeInMillis(cal.getTimeInMillis() - before.longValue());
            Date min = cal.getTime();
            cal.setTime(center);
            cal.setTimeInMillis(cal.getTimeInMillis() + after.longValue());
            Date max = cal.getTime();
            return new DateRange(min, max);
        }
        // TODO: add support for Number, e.g., elevation
        throw new IllegalArgumentException("Unsupported value type " + dataType);
    }

    /** Before offset */
    public Number getBefore() {
        return before;
    }

    /** After offset */
    public Number getAfter() {
        return after;
    }

    /** The range data type */
    public Class getDataType() {
        return dataType;
    }
}
