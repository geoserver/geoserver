/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.xs;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link DateBinding}
 * 
 * @author awaterme
 */
public class DateBindingTest {

    private DateBinding sut = new DateBinding();
    private TimeZone systemTimeZone;

    /**
     * Save & restore system time zone, so later tests are not affected.
     */
    @Before
    public void saveSystemTimeZone() {
        systemTimeZone = TimeZone.getDefault();
    }

    @After
    public void restoreSystemTimeZone() {
        TimeZone.setDefault(systemTimeZone);
    }

    /**
     * {@link DateBinding#encode(Object, String)} test, simulates behavior of
     * most common case: Date with default time zone, as coming from DB, XML,
     * date format parser.
     * 
     * @throws Exception
     */
    @Test
    public void testEncodeWithDefaultTimezone() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = format.parse("2015-09-02");
        assertEquals("2015-09-02Z", sut.encode(date, null));
    }

    /**
     * {@link DateBinding#encode(Object, String)} test, simulates behavior for
     * zone-offset == 0 for certain hours.
     * 
     * @throws Exception
     */
    @Test
    public void testEncodeWithTimezoneUTC() throws Exception {
        testEncodeWithTimezone("UTC");
        testEncodeWithTimezone("GMT");
    }

    /**
     * {@link DateBinding#encode(Object, String)} test, simulates behavior for
     * zone-offset greater than 0 for certain hours.
     * 
     * @throws Exception
     */
    @Test
    public void testEncodeWithTimezoneCET() throws Exception {
        testEncodeWithTimezone("CET");
    }

    /**
     * {@link DateBinding#encode(Object, String)} test, simulates behavior for
     * zone-offset less than 0 for certain hours.
     * 
     * @throws Exception
     */
    @Test
    public void testEncodeWithTimezoneEST() throws Exception {
        testEncodeWithTimezone("EST");
    }

    private void testEncodeWithTimezone(String timezoneId) throws Exception {
        TimeZone timeZone = TimeZone.getTimeZone(timezoneId);
        TimeZone.setDefault(timeZone);
        int[] hours = new int[] { 0, 1, 12, 23 };
        for (int hour : hours) {
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(Calendar.YEAR, 2015);
            calendar.set(Calendar.DAY_OF_MONTH, 2);
            calendar.set(Calendar.MONTH, 8);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            Date date = calendar.getTime();
            assertEquals("2015-09-02Z", sut.encode(date, null));
        }
    }

}
