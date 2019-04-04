/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.gce.imagemosaic.properties.time.TimeParser;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.logging.Logging;

/**
 * Utilities method to produce histogram from dimension domains values. Two types of histograms are
 * supported numerical, times and enumerated values.
 */
final class HistogramUtils {

    private static final Logger LOGGER = Logging.getLogger(HistogramUtils.class);

    private static final String HISTOGRAM_MAX_THRESHOLD_VARIABLE = "HISTORGRAM_MAX_THRESHOLD";
    private static final long HISTOGRAM_MAX_THRESHOLD_DEFAULT = 10000L;
    private static final long HISTOGRAM_MAX_THRESHOLD = getHistogramMaxThreshold();

    private static final String NUMERICAL_DEFAULT_RESOLUTION = "100";
    private static final String TIME_DEFAULT_RESOLUTION = "PT1H";

    private static final long MAX_ITERATIONS = 10000;

    private enum HistogramType {
        NUMERIC,
        TIME,
        ENUMERATED
    }

    private HistogramUtils() {}

    /**
     * Helper method that get the threshold value that will be used to check if the resolution is to
     * high.
     */
    private static long getHistogramMaxThreshold() {
        String value = System.getProperty(HISTOGRAM_MAX_THRESHOLD_VARIABLE);
        if (value == null) {
            // no user provided value, so let's return the default value
            return HISTOGRAM_MAX_THRESHOLD_DEFAULT;
        }
        // using the user provided value
        return Long.parseLong(value);
    }

    /**
     * Builds an histogram for the provided domain values. The returned tuple will contain the
     * domain representation and the histogram values. The domain values should be numbers, dates or
     * strings. Ranges are also supported, the min value will be used to discover the domain values
     * type.
     */
    static Tuple<String, List<Integer>> buildHistogram(
            List<Object> domainValues, String resolution) {
        if (domainValues.isEmpty()) {
            // FIXME: How to represent a domain with no values ?
            return Tuple.tuple("", Collections.emptyList());
        }
        Tuple<String, List<Range>> buckets = computeBuckets(domainValues, resolution);
        ArrayList<Integer> histogramValues = new ArrayList<>(buckets.second.size());
        for (int i = 0; i < buckets.second.size(); i++) {
            histogramValues.add(0);
        }
        for (Object value : domainValues) {
            int index = getBucketIndex(buckets.second, (Comparable) value);
            if (index >= 0) {
                histogramValues.set(index, histogramValues.get(index) + 1);
            } else if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Bucket not found for value: " + value);
            }
        }

        return Tuple.tuple(buckets.first, histogramValues);
    }

    /** Compute the buckets for the given domain values and resolution. */
    private static Tuple<String, List<Range>> computeBuckets(
            List<Object> domainValues, String resolution) {
        switch (findHistogramType(domainValues)) {
            case NUMERIC:
                return getNumericBuckets(domainValues, resolution);
            case TIME:
                return getTimeBuckets(domainValues, resolution);
            default:
                return getEnumeratedBuckets(domainValues);
        }
    }

    /** Helper method that just founds the histogram type based on domains values. */
    private static HistogramType findHistogramType(List<Object> domainValues) {
        Object value = domainValues.get(domainValues.size() - 1);
        if (value instanceof Range) {
            // this is a range so lets use the min value
            value = ((Range) value).getMinValue();
        }
        // let's try to find this histogram type
        if (value instanceof Number) {
            return HistogramType.NUMERIC;
        }
        if (value instanceof Date) {
            return HistogramType.TIME;
        }
        // well by default we consider the histogram to be of type enumerated
        return HistogramType.ENUMERATED;
    }

    /**
     * Helper method that creates buckets for a numeric domain based on the provided resolution. The
     * returned tuple will contain the domain representation and the domain buckets.
     */
    private static Tuple<String, List<Range>> getNumericBuckets(
            List<Object> domainValues, String resolution) {
        Tuple<Double, Double> minMax = DimensionsUtils.getMinMax(domainValues, Double.class);
        return getNumericBuckets(minMax.first, minMax.second, resolution);
    }

    /**
     * Helper method that creates buckets for a numeric domain based on the provided resolution. The
     * returned tuple will contain the domain representation and the domain buckets.
     */
    public static Tuple<String, List<Range>> getNumericBuckets(
            double min, double max, String resolution) {
        resolution = resolution != null ? resolution : NUMERICAL_DEFAULT_RESOLUTION;
        double finalResolution = Double.parseDouble(resolution);
        int i = 0;
        while ((max - min) / finalResolution >= HISTOGRAM_MAX_THRESHOLD && i < MAX_ITERATIONS) {
            finalResolution += 10;
            i++;
        }
        // if the max value is at the very edge of the last interval, then add one more (we are
        // going to
        // use intervals that contain first but not last to avoid overlaps
        if ((max - min) % finalResolution == 0) {
            max += finalResolution;
        }
        String domainString = min + "/" + max + "/" + finalResolution;
        if ((max - min) / finalResolution == 1) {
            // one bucket catches all
            boolean includeLast = (max - min) < finalResolution;
            return Tuple.tuple(
                    domainString,
                    Collections.singletonList(NumberRange.create(min, true, max, includeLast)));
        }
        List<Range> buckets = new ArrayList<>();
        for (double step = min; step < max; step += finalResolution) {
            // single buckets in a list don't include last to avoid overlap
            double limit = step + finalResolution;
            if (limit > max) {
                buckets.add(NumberRange.create(step, true, max, true));
                break;
            }
            buckets.add(NumberRange.create(step, true, limit, false));
        }
        return Tuple.tuple(domainString, buckets);
    }

    /**
     * Helper method that creates buckets for a time domain based on the provided resolution. The
     * returned tuple will contain the domain representation and the domain buckets.
     */
    private static Tuple<String, List<Range>> getTimeBuckets(
            List<Object> domainValues, String resolution) {
        Tuple<Date, Date> minMax = DimensionsUtils.getMinMax(domainValues, Date.class);
        return getTimeBuckets(minMax.first, minMax.second, resolution);
    }

    /**
     * Helper method that creates buckets for a time domain based on the provided resolution. The
     * returned tuple will contain the domain representation and the domain buckets.
     */
    public static Tuple<String, List<Range>> getTimeBuckets(Date min, Date max, String resolution) {
        // if the max value is at the very edge of the last interval, then add one more (we are
        // going to
        // use intervals that contain first but not last to avoid overlaps
        resolution = resolution != null ? resolution : TIME_DEFAULT_RESOLUTION;
        long difference = max.getTime() - min.getTime();
        long resolutionInMs;
        try {
            resolutionInMs = org.geoserver.ows.kvp.TimeParser.parsePeriod(resolution);
            if (difference % resolutionInMs == 0) {
                // if the max value is at the very edge of the last interval, then add one more (we
                // are going to
                // use intervals that contain first but not last to avoid overlaps
                max = new Date(max.getTime() + resolutionInMs);
            }
        } catch (ParseException e) {
            throw new RuntimeException(
                    String.format("Error parsing time resolution '%s'.", resolution), e);
        }
        Tuple<Date, Date> minMax = Tuple.tuple(min, max);
        Tuple<String, List<Date>> intervalsAndSpec = getDateIntervals(minMax, resolution);
        int i = 0;
        while (intervalsAndSpec.second.size() >= HISTOGRAM_MAX_THRESHOLD && i < MAX_ITERATIONS) {
            i++;
            resolution = "PT" + i + "M";
            resolutionInMs = i * 30L * 24 * 60 * 60 * 1000;
            intervalsAndSpec = getDateIntervals(minMax, resolution);
        }
        List<Date> intervals = intervalsAndSpec.second;
        if (intervals.size() == 1) {
            boolean includeLast = difference < resolutionInMs;
            return Tuple.tuple(
                    intervalsAndSpec.first,
                    Collections.singletonList(new DateRange(min, true, max, includeLast)));
        }
        List<Range> buckets = new ArrayList<>();
        Date previous = intervals.get(0);
        for (int step = 1; step < intervals.size(); step++) {
            buckets.add(new DateRange(previous, true, intervals.get(step), false));
            previous = intervals.get(step);
        }
        return Tuple.tuple(intervalsAndSpec.first, buckets);
    }

    /** Helper method that computes the time intervals for a certain resolution. */
    private static Tuple<String, List<Date>> getDateIntervals(
            Tuple<Date, Date> minMax, String resolution) {
        ISO8601Formatter dateFormatter = new ISO8601Formatter();
        String domainString = dateFormatter.format(minMax.first);
        domainString += "/" + dateFormatter.format(minMax.second) + "/" + resolution;
        TimeParser timeParser = new TimeParser();
        try {
            List<Date> intervals = timeParser.parse(domainString);
            Date last = intervals.get(intervals.size() - 1);
            long resolutionInMs = org.geoserver.ows.kvp.TimeParser.parsePeriod(resolution);
            if (last.getTime() < minMax.second.getTime()) {
                intervals.add(new Date(last.getTime() + resolutionInMs));
            }
            return Tuple.tuple(domainString, intervals);
        } catch (ParseException exception) {
            throw new RuntimeException(
                    String.format("Error parsing time resolution '%s'.", resolution), exception);
        }
    }

    /**
     * Helper method that creates buckets for an enumerated domain. The returned tuple will contain
     * the domain representation and the domain buckets. Note that in this case the resolution will
     * be ignored.
     */
    private static Tuple<String, List<Range>> getEnumeratedBuckets(List<Object> domainValues) {
        StringBuilder domain = new StringBuilder();
        List<Range> buckets = new ArrayList<>();
        for (Object value : domainValues) {
            String stringValue = value.toString();
            domain.append(stringValue).append(',');
            buckets.add(new EnumeratedRange(stringValue));
        }
        domain.delete(domain.length() - 1, domain.length());
        // FIXME: we don't really have a domain range we simply enumerate all the values.
        return Tuple.tuple(domain.toString(), buckets);
    }

    /** Simple helper method that founds the bucket for a value or returns -1 otherwise. */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable> int getBucketIndex(List<Range> buckets, T value) {
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).contains(value)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Range class used to represent enumerated values. The contains operation will return true if
     * the provided values is equal to the enumerated value.
     */
    private static final class EnumeratedRange extends Range<String> {

        public EnumeratedRange(String value) {
            super(String.class, value, true, value, true);
        }
    }
}
