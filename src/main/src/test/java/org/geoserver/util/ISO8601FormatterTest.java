/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.geotools.util.DateRange;

import org.junit.Test;

public class ISO8601FormatterTest {

    @Test
    public void testDate() throws Exception {
        Date date = generateDate();
        ISO8601Formatter formatter = new ISO8601Formatter();
        String formattedDate = formatter.format(date);
        assertEquals("2015-03-28T23:03:18.000Z", formattedDate);
    }

    @Test
    public void testDateObject() throws Exception {
        Date date = generateDate();
        Object dateObj = date;
        ISO8601Formatter formatter = new ISO8601Formatter();
        String formattedDate = formatter.format(dateObj);
        assertEquals("2015-03-28T23:03:18.000Z", formattedDate);
    }

    @Test 
    public void testUnsupportedObject() throws Exception {
        try {
            ISO8601Formatter formatter = new ISO8601Formatter();
            formatter.format(Locale.US);
            fail("formatter should have thrown IllegalArgumentException");
        }
        catch(IllegalArgumentException e) {
            assertEquals("Date argument should be either a Date or a DateRange, however this one is neither: en_US", e.getMessage());
        }
    }

    @Test
    public void testDateRange() throws Exception {
        DateRange dateRange = new DateRange(generateDate(), generateDate(5));
        ISO8601Formatter formatter = new ISO8601Formatter();
        String formattedDateRange = formatter.format(dateRange);
        assertEquals("2015-03-28T23:03:18.000Z/2015-03-28T23:03:23.000Z/PT1S", formattedDateRange);
    }

    @Test
    public void testDateRangeZeroLength() throws Exception {
        DateRange dateRange = new DateRange(generateDate(1), generateDate(1));
        ISO8601Formatter formatter = new ISO8601Formatter();
        String formattedDateRange = formatter.format(dateRange);
        assertEquals("2015-03-28T23:03:19.000Z", formattedDateRange);
    }

    private Date generateDate() {
        return generateDate(0);
    }

    private Date generateDate(int offset) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), new Locale("en", "aus"));
        cal.clear();
        cal.set(2015, Calendar.MARCH, 28, 23, 3, 18 + offset);
        return cal.getTime();
    }
    
    
    
    
    
    
}
