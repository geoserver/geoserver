/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * (c) 2002-2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;
import org.geotools.util.factory.Hints;
import org.jaitools.numeric.Range;

/**
 * ConverterFactory for trading between strings and JAITools ranges
 *
 * @author Andrea Aime - GeoSolutions
 * @source $URL$
 */
public class JAIToolsRangeConverterFactory implements ConverterFactory {

    private static final String RE_OPEN = "(\\(|\\[)"; // char for opening a range
    private static final String RE_CLOSE = "(\\)|\\])"; // char for closing range
    private static final String RE_NUM = "(\\-?\\d+(?:\\.\\d*)?)?"; // a nullable general number

    private static final String RANGE_REGEX =
            RE_OPEN + RE_NUM + ";" + RE_NUM + RE_CLOSE; // + "\\z";
    private static final Pattern RANGE_PATTERN = Pattern.compile(RANGE_REGEX);

    private static final String RANGELIST_REGEX =
            "(" + RE_OPEN + RE_NUM + ";" + RE_NUM + RE_CLOSE + ")+"; // "\\z";
    private static final Pattern RANGELIST_PATTERN = Pattern.compile(RANGELIST_REGEX);

    public Converter createConverter(Class source, Class target, Hints hints) {
        if (target.equals(Range.class) && source.equals(String.class)) {
            return new Converter() {

                public <T> T convert(Object source, Class<T> target) throws Exception {
                    String sRange = (String) source;
                    Matcher m = RANGE_PATTERN.matcher(sRange);

                    if (!m.matches()) return null;

                    return (T) parseRangeInternal(m, sRange);
                }
            };
        }

        return null;
    }

    /** Return the parsed Range. */
    static Range<Double> parseRangeInternal(Matcher m, String sRange) {
        Double min = null;
        Double max = null;

        if (m.groupCount() != 4) {
            throw new IllegalStateException(
                    "Range returned wrong group count (" + sRange + ") : " + m.groupCount());
        }

        if (m.group(2) != null) {
            min = Double.valueOf(m.group(2));
        }
        if (m.group(3) != null) {
            max = Double.valueOf(m.group(3));
        }

        boolean inclmin;
        if (m.group(1).equals("(")) inclmin = false;
        else if (m.group(1).equals("[")) inclmin = true;
        else throw new IllegalArgumentException("Bad min delimiter (" + sRange + ")");

        boolean inclmax;
        if (m.group(4).equals(")")) inclmax = false;
        else if (m.group(4).equals("]")) inclmax = true;
        else throw new IllegalArgumentException("Bad max delimiter (" + sRange + ")");

        if (min != null && max != null && min > max)
            throw new IllegalArgumentException("Bad min/max relation (" + sRange + ")");

        return new Range<Double>(min, inclmin, max, inclmax);
    }

    /** Parses a list of ranges from a string */
    public static List<Range<Double>> parseRanges(String sRangeList) {
        // check that the whole input string is a list of ranges
        Matcher m = RANGELIST_PATTERN.matcher(sRangeList);
        if (!m.matches())
            throw new IllegalArgumentException("Bad range definition '" + sRangeList + "'");

        // fetch every single range
        m = RANGE_PATTERN.matcher(sRangeList);

        List<Range<Double>> ret = new ArrayList<Range<Double>>();
        while (m.find()) {
            Range<Double> range = parseRangeInternal(m, sRangeList);
            ret.add(range);
        }

        return ret;
    }
}
