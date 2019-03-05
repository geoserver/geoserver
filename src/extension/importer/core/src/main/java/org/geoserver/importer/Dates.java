/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import com.google.common.collect.Collections2;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing/encoding dates.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Dates {

    static List<DatePattern> PATTERNS =
            Arrays.asList(
                    dp(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}Z"),
                    dp(
                            "yyyy-MM-dd'T'HH:mm:sss'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,3}Z"),
                    dp(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}Z"),
                    dp("yyyy-MM-dd'T'HH:mm'Z'", "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}Z"),
                    dp("yyyy-MM-dd'T'HH'Z'", "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}Z"),
                    dp("yyyy-MM-dd", "\\d{4}-\\d{1,2}-\\d{1,2}"),
                    dp("yyyy-MM", "\\d{4}-\\d{1,2}"),
                    dp("yyyyMMdd", "\\d{6,8}", true, true),
                    dp("yyyyMM", "\\d{5,6}", true, true),
                    dp("yyyy", "\\d{4}"));

    /**
     * Returns list of all patterns, optionally filtering out ones that require a strict match.
     *
     * @param strict when <tt>false</tt> those patterns that require a strict match (ie. a pattern
     *     match and a date parse) are filtered out.
     */
    public static Collection<DatePattern> patterns(boolean strict) {
        Collection<DatePattern> patterns = PATTERNS;
        if (!strict) {
            patterns = Collections2.filter(patterns, input -> input != null && !input.isStrict());
        }
        return patterns;
    }

    public static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    static DatePattern dp(String format, String regex) {
        return new DatePattern(format, regex);
    }

    static DatePattern dp(String format, String regex, boolean forceGmt, boolean strict) {
        return new DatePattern(format, regex, forceGmt, strict);
    }

    public static Date matchAndParse(String str) {
        return parse(str, true);
    }

    public static Date parse(String str) {
        return parse(str, false);
    }

    static Date parse(String str, boolean match) {
        Collection<DatePattern> patterns = patterns(match);

        for (DatePattern dp : patterns) {
            Date parsed = match ? dp.matchAndParse(str) : dp.parse(str);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    static Date parseDate(DatePattern dp, String str) {
        Pattern p = dp.pattern();
        Matcher m = p.matcher(str);
        if (m.matches()) {
            String match = m.group(1);
            try {
                Date parsed = dp.dateFormat().parse(match);
                if (parsed != null) {
                    return parsed;
                }
            } catch (ParseException e) {
                // ignore
            }
        }
        return null;
    }
}
