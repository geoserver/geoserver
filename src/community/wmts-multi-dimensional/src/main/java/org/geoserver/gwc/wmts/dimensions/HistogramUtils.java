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
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.gce.imagemosaic.properties.time.TimeParser;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;

/**
 * Utilities method to produce histogram from dimension domains values. Two types of histograms are
 * supported numerical, times and enumerated values.
 */
final class HistogramUtils {

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
        resolution = resolution != null ? resolution : NUMERICAL_DEFAULT_RESOLUTION;
        double finalResolution = Double.parseDouble(resolution);
        double min = minMax.first;
        double max = Math.max(minMax.second, finalResolution);
        int i = 0;
        while ((max - min) / finalResolution >= HISTOGRAM_MAX_THRESHOLD && i < MAX_ITERATIONS) {
            finalResolution += 10;
            i++;
        }
        String domainString = min + "/" + max + "/" + finalResolution;
        if ((max - min) / finalResolution == 1) {
            return Tuple.tuple(
                    domainString, Collections.singletonList(NumberRange.create(min, max)));
        }
        List<Range> buckets = new ArrayList<>();
        for (double step = min; step < max; step += finalResolution) {
            double limit = step + finalResolution;
            if (limit > max) {
                buckets.add(NumberRange.create(step, max));
                break;
            }
            buckets.add(NumberRange.create(step, limit));
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
        resolution = resolution != null ? resolution : TIME_DEFAULT_RESOLUTION;
        Tuple<String, List<Date>> intervals = getDateIntervals(minMax, resolution);
        int i = 0;
        while (intervals.second.size() >= HISTOGRAM_MAX_THRESHOLD && i < MAX_ITERATIONS) {
            i++;
            resolution = "PT" + i + "M";
            intervals = getDateIntervals(minMax, resolution);
        }
        if (intervals.second.size() == 1) {
            return Tuple.tuple(
                    intervals.first,
                    Collections.singletonList(new DateRange(minMax.first, minMax.second)));
        }
        List<Range> buckets = new ArrayList<>();
        Date previous = intervals.second.get(0);
        for (int step = 1; step < intervals.second.size(); step++) {
            buckets.add(new DateRange(previous, intervals.second.get(step)));
            previous = intervals.second.get(step);
        }
        return Tuple.tuple(intervals.first, buckets);
    }

    /** Helper method that computes the time intervals for a certain resolution. */
    private static Tuple<String, List<Date>> getDateIntervals(
            Tuple<Date, Date> minMax, String resolution) {
        ISO8601Formatter dateFormatter = new ISO8601Formatter();
        String domainString = dateFormatter.format(minMax.first);
        domainString += "/" + dateFormatter.format(minMax.second) + "/" + resolution;
        TimeParser timeParser = new TimeParser();
        try {
            return Tuple.tuple(domainString, timeParser.parse(domainString));
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
