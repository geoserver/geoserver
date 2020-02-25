/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.geoserver.importer.DatePattern;
import org.geoserver.importer.Dates;
import org.geoserver.importer.transform.DateFormatTransform;
import org.junit.Test;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class DateFormatTransformTest extends TransformTestSupport {

    public DateFormatTransformTest() {}

    @Test
    public void testExtents() throws Exception {
        // this is mostly a verification of the extents of the builtin date parsing
        DateFormatTransform transform = new DateFormatTransform("not used", null);

        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        int minYear = -292269052; // this is the text value
        Date parsed = transform.parseDate("" + minYear);
        cal.setTime(parsed);

        // the real value is the minYear - 1 since 0BC == 1AD
        assertEquals(minYear - 1, -cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));

        cal.setTimeInMillis(Long.MAX_VALUE);
        int maxYear = cal.get(Calendar.YEAR);
        parsed = transform.parseDate("" + maxYear);
        cal.setTime(parsed);
        assertEquals(maxYear, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.AD, cal.get(Calendar.ERA));
    }

    @Test
    public void testTransformSuccess() throws ParseException {
        DateFormatTransform transform = new DateFormatTransform("not used", null);

        Date now = new Date();

        // make a big shuffled list of patterns to ensure caching of last pattern
        // doesn't cause any problems
        List<String> patterns = new ArrayList<String>();
        patterns.addAll(
                Collections2.transform(
                        Dates.patterns(false),
                        new Function<DatePattern, String>() {

                            @Override
                            public String apply(DatePattern input) {
                                return input.dateFormat().toPattern();
                            }
                        }));

        Collections.shuffle(patterns);

        for (String f : patterns) {
            SimpleDateFormat fmt = new SimpleDateFormat(f);
            fmt.setTimeZone(Dates.UTC_TZ);
            Date expected = fmt.parse(fmt.format(now));
            Date parsed = transform.parseDate(fmt.format(now));
            assertEquals(expected, parsed);
        }
    }

    @Test
    public void testTransformSuccessCustomFormat() throws ParseException {
        String customFormat = "yyyy-MM-dd'X'00";
        DateFormatTransform transform = new DateFormatTransform("not used", customFormat);

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat(customFormat);
        fmt.setTimeZone(Dates.UTC_TZ);
        Date expected = fmt.parse(fmt.format(now));
        Date parsed = transform.parseDate(fmt.format(now));
        assertEquals(expected, parsed);
    }

    @Test
    public void testJSON() throws Exception {
        doJSONTest(new DateFormatTransform("foo", null));
        doJSONTest(new DateFormatTransform("foo", "yyyy-MM-dd"));
        doJSONTest(new DateFormatTransform("foo", null, null, "LIST"));
        doJSONTest(new DateFormatTransform("foo", null, null, "DISCRETE_INTERVAL"));
        doJSONTest(new DateFormatTransform("foo", null, null, "CONTINUOUS_INTERVAL"));
        doJSONTest(new DateFormatTransform("foo", null, "enddate", null));
    }
}
