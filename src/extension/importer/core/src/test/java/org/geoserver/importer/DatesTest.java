/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static java.util.Calendar.*;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.Test;

public class DatesTest {

    @Test
    public void testParse() {
        doTestParse(date(2012, FEBRUARY, 6, 13, 12, 59, 123), "2012-02-06T13:12:59.123Z");
        doTestParse(date(2012, FEBRUARY, 6, 13, 12, 59, 0), "2012-02-06T13:12:59Z");
        doTestParse(date(2012, FEBRUARY, 6, 13, 12, 123, 0), "2012-02-06T13:12:123Z");
        doTestParse(date(2012, FEBRUARY, 6, 13, 12, 0, 0), "2012-02-06T13:12Z");
        doTestParse(date(2012, FEBRUARY, 6, 13, 0, 0, 0), "2012-02-06T13Z");
        doTestParse(date(2012, FEBRUARY, 6, 0, 0, 0, 0), "2012-02-06");
        doTestParse(date(2012, FEBRUARY, 1, 0, 0, 0, 0), "2012-02");
        doTestParse(date(2012, JANUARY, 1, 0, 0, 0, 0), "2012");
    }

    void doTestParse(Date expected, String str) {
        // test straight up
        assertEquals(expected, Dates.parse(str));

        // padd string
        assertEquals(expected, Dates.matchAndParse("foo_" + str + ".bar"));
    }

    Date date(
            int year,
            int month,
            int dayOfMonth,
            int hourOfDay,
            int minute,
            int second,
            int millisecond) {
        return date(
                year,
                month,
                dayOfMonth,
                hourOfDay,
                minute,
                second,
                millisecond,
                TimeZone.getTimeZone("GMT"));
    }

    Date date(
            int year,
            int month,
            int dayOfMonth,
            int hourOfDay,
            int minute,
            int second,
            int millisecond,
            TimeZone tz) {
        Calendar c = Calendar.getInstance();
        c.set(YEAR, year);
        c.set(MONTH, month);
        c.set(DAY_OF_MONTH, dayOfMonth);
        c.set(HOUR_OF_DAY, hourOfDay);
        c.set(MINUTE, minute);
        c.set(SECOND, second);
        c.set(MILLISECOND, millisecond);
        c.setTimeZone(tz);
        return c.getTime();
    }
}
