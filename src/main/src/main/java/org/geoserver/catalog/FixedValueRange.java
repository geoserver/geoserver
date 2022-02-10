/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import org.geoserver.ows.kvp.TimeParser;

public class FixedValueRange {
    /**
     * Parses the fixed value range
     *
     * @param spec The specification from the UI
     * @param dataType The target data type (e.g. {@link Date}
     * @return An {@link FixedValueRange} object, or null if the spec was null or empty
     */
    static TimeParser parser = new TimeParser();

    public FixedValueRange(String spec, Class<?> dataType) {}

    public FixedValueRange() {}

    public Object parse(String value) throws ParseException {
        TimeParser parser = getTimeParser();
        return parser.parse(value);
    }

    /** Allows subclasses to customize the {@link TimeParser} used in {@link #parse(String)} */
    protected static TimeParser getTimeParser() {
        return parser;
    }

    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static FixedValueRange checkFixedValueRange(String spec, Class<?> dataType)
            throws ParseException, IOException {
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }
        List<String> fixedValueList = Arrays.asList(spec.split(","));
        if (!fixedValueList.isEmpty()) {
            for (String fixedValue : fixedValueList) {
                if (Number.class.isAssignableFrom(dataType)) {
                    boolean checkresult = isDouble(fixedValue);
                    if (!checkresult) {
                        throw new IllegalArgumentException(
                                "Invalid Fixed Value range specification");
                    }
                } else if (Date.class.isAssignableFrom(dataType)) {
                    TimeParser parser = getTimeParser();
                    parser.parse(fixedValue);
                } else {
                    throw new IllegalArgumentException("Invalid Fixed Value range specification");
                }
            }
        }

        return new FixedValueRange(spec, dataType);
    }

    public static TreeSet<Object> getFixedValueRange(String spec) throws ParseException {
        TreeSet<Object> result = new TreeSet<>();
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }
        List<String> fixedValueList = Arrays.asList(spec.split(","));
        if (!fixedValueList.isEmpty()) {
            for (String fixedValue : fixedValueList) {
                result.add(fixedValue);
            }
        }

        return result;
    }

    public static TreeSet<Comparable> getFixedValueRangeComp(String spec) throws ParseException {
        TreeSet<Comparable> result = new TreeSet<>();
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }
        List<String> fixedValueList = Arrays.asList(spec.split(","));
        if (!fixedValueList.isEmpty()) {
            for (String fixedValue : fixedValueList) {
                result.add(fixedValue);
            }
        }

        return result;
    }

    public static TreeSet<Double> getFixedValueRangeElevation(String spec) throws ParseException {
        TreeSet<Double> result = new TreeSet<>();
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }
        List<String> fixedValueList = Arrays.asList(spec.split(","));
        if (!fixedValueList.isEmpty()) {
            for (String fixedValue : fixedValueList) {
                result.add(Double.parseDouble(fixedValue));
            }
        }

        return result;
    }

    public static TreeSet<Date> getFixedValueRangeTimes(String spec) throws ParseException {
        TreeSet<Date> result = new TreeSet<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }
        List<String> fixedValueList = Arrays.asList(spec.split(","));
        if (!fixedValueList.isEmpty()) {
            for (String fixedValue : fixedValueList) {
                String reformattedStr = outputDateFormat.format(inputDateFormat.parse(fixedValue));
                Date date = outputDateFormat.parse(reformattedStr);
                result.add(date);
            }
        }

        return result;
    }
}
