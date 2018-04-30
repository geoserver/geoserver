/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.geotools.util.DateRange;

        
/**
 * Test for the time parameter in a WMS request.
 * 
 * @author Cedric Briancon
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 */
public class TimeKvpParserTest extends TestCase {
    /**
     * A time period for testing.
     */
    private final static String PERIOD = "2007-01-01T12Z/2007-01-31T12Z/P1DT12H";
    
    private final static String CONTINUOUS_PERIOD = "2007-01-01T12Z/2007-01-31T12Z";
    
    private final static String CONTINUOUS_PERIOD_TIME_DURATION = "2007-01-01T12Z/P1DT1H";
    
    private final static String CONTINUOUS_PERIOD_INVALID_DURATION = "P1D/P1DT1H";
    
    private final static String CONTINUOUS_RELATIVE_PERIOD_H = "PT2H/PRESENT";
    private final static String CONTINUOUS_RELATIVE_PERIOD_D = "P10D/PRESENT";
    private final static String CONTINUOUS_RELATIVE_PERIOD_W = "P2W/PRESENT";
    
    /**
     * Format of dates.
     */
    private final static DateFormat format;
    static {
    	 format = new SimpleDateFormat("yyyy-MM-dd'T'HH'Z'");
    	 format.setTimeZone(TimeParser.UTC_TZ);
    }
    
    public void testReducedAccuracyYear() throws Exception {
    	Calendar c = new GregorianCalendar();
    	c.setTimeZone(TimeParser.UTC_TZ);
    	
    	DateRange year = (DateRange) TimeParser.getFuzzyDate("2000");
    	c.clear();
    	c.set(Calendar.YEAR, 2000);
    	assertRangeStarts(year, c.getTime());
    	c.set(Calendar.YEAR, 2001);
    	c.add(Calendar.MILLISECOND, -1);
    	assertRangeEnds(year, c.getTime());
    	
    	year = (DateRange) TimeParser.getFuzzyDate("2001");
    	c.clear();
    	c.set(Calendar.YEAR, 2001);
    	assertRangeStarts(year, c.getTime());
    	c.set(Calendar.YEAR, 2002);
    	c.add(Calendar.MILLISECOND, -1);
    	assertRangeEnds(year, c.getTime());
    	
    	year = (DateRange) TimeParser.getFuzzyDate("-6052");
    	c.clear();
    	c.set(Calendar.ERA, GregorianCalendar.BC);
    	c.set(Calendar.YEAR, 6053);
    	assertRangeStarts(year, c.getTime());
    	c.set(Calendar.YEAR, 6052);
    	c.add(Calendar.MILLISECOND, -1);
    	assertRangeEnds(year, c.getTime());
    }
    
    public void testReducedAccuracyHour() throws Exception {
        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeParser.UTC_TZ);
        c.clear();
        
        DateRange hour = (DateRange) TimeParser.getFuzzyDate("2000-04-04T12Z");
        c.set(Calendar.YEAR, 2000);
        c.set(Calendar.MONTH, 3); // 0-indexed
        c.set(Calendar.DAY_OF_MONTH, 4);
        c.set(Calendar.HOUR_OF_DAY, 12);
        assertRangeStarts(hour, c.getTime());
        c.add(Calendar.HOUR_OF_DAY, 1);
        c.add(Calendar.MILLISECOND, -1);
        assertRangeEnds(hour, c.getTime());
        
        hour = (DateRange) TimeParser.getFuzzyDate("2005-12-31T23Z"); // selected due to leapsecond at 23:59:60 UTC
        c.clear();
        c.set(Calendar.YEAR, 2005);
        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DAY_OF_MONTH, 31);
        c.set(Calendar.HOUR_OF_DAY, 23);
        assertRangeStarts(hour, c.getTime());
        c.add(Calendar.HOUR_OF_DAY, 1);
        c.add(Calendar.MILLISECOND, -1);
        assertRangeEnds(hour, c.getTime());
        
        hour = (DateRange) TimeParser.getFuzzyDate("-25-06-08T17Z");
        c.clear();
        c.set(Calendar.ERA, GregorianCalendar.BC);
        c.set(Calendar.YEAR, 26);
        c.set(Calendar.MONTH, 5);
        c.set(Calendar.DAY_OF_MONTH, 8);
        c.set(Calendar.HOUR_OF_DAY, 17);
        assertRangeStarts(hour, c.getTime());
        c.add(Calendar.HOUR_OF_DAY, 1);
        c.add(Calendar.MILLISECOND, -1);
        assertRangeEnds(hour, c.getTime());
    }
    
    public void testReducedAccuracyMilliseconds() throws Exception {
        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeParser.UTC_TZ);
        c.clear();
        
        Date instant = (Date) TimeParser.getFuzzyDate("2000-04-04T12:00:00.000Z");
        c.set(Calendar.YEAR, 2000);
        c.set(Calendar.MONTH, 3); // 0-indexed
        c.set(Calendar.DAY_OF_MONTH, 4);
        c.set(Calendar.HOUR_OF_DAY, 12);
        assertEquals(instant, c.getTime());
        
        instant = (Date) TimeParser.getFuzzyDate("2005-12-31T23:59:60.000Z"); // selected due to leapsecond at 23:59:60 UTC
        c.clear();
        c.set(Calendar.YEAR, 2005);
        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DAY_OF_MONTH, 31);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 60);
        assertEquals(instant, c.getTime());
        
        instant = (Date) TimeParser.getFuzzyDate("-25-06-08T17:15:00.123Z");
        c.clear();
        c.set(Calendar.ERA, GregorianCalendar.BC);
        c.set(Calendar.YEAR, 26);
        c.set(Calendar.MONTH, 5);
        c.set(Calendar.DAY_OF_MONTH, 8);
        c.set(Calendar.HOUR_OF_DAY, 17);
        c.set(Calendar.MINUTE, 15);
        c.set(Calendar.MILLISECOND, 123);
        assertEquals(instant, c.getTime());
    }
    
    /**
     * Tests only the increment part of the time parameter.
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testPeriod() throws ParseException {
        final long millisInDay = TimeParser.MILLIS_IN_DAY;
        assertEquals(               millisInDay,  TimeParser.parsePeriod("P1D"));
        assertEquals(             3*millisInDay,  TimeParser.parsePeriod("P3D"));
        assertEquals(            14*millisInDay,  TimeParser.parsePeriod("P2W"));
        assertEquals(             8*millisInDay,  TimeParser.parsePeriod("P1W1D"));
        assertEquals(               millisInDay,  TimeParser.parsePeriod("PT24H"));
        assertEquals(Math.round(1.5*millisInDay), TimeParser.parsePeriod("P1.5D"));
    }

    /**
     * Compares the dates obtained by parsing the time parameter with the expected values.
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testInterval() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection) timeKvpParser.parse(PERIOD));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertInstant(format.parse("2007-01-01T12Z"), l.get(0));
        assertInstant(format.parse("2007-01-03T00Z"), l.get(1));
        assertInstant(format.parse("2007-01-04T12Z"), l.get(2));
        assertInstant(format.parse("2007-01-06T00Z"), l.get(3));
        assertInstant(format.parse("2007-01-07T12Z"), l.get(4));
        assertInstant(format.parse("2007-01-09T00Z"), l.get(5));
        assertInstant(format.parse("2007-01-10T12Z"), l.get(6));
        assertInstant(format.parse("2007-01-12T00Z"), l.get(7));
        
        l = new ArrayList((Collection) timeKvpParser.parse("2007-01-01T12Z/2007-01-01T13Z/PT10M"));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertEquals(12, l.size());
        assertInstant(format.parse("2007-01-01T12Z"), l.get(0));
    }
    
    public void testContinuousInterval() throws ParseException {

        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection) timeKvpParser.parse(CONTINUOUS_PERIOD));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        final DateRange range = (DateRange) l.get(0);
        assertEquals(format.parse("2007-01-01T12Z"), range.getMinValue());
        Date end = format.parse("2007-01-31T13Z");
        end.setTime(end.getTime() - 1);
        assertEquals(end, range.getMaxValue());
    }

    public void testContinuousIntervalDuration() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection) timeKvpParser.parse(CONTINUOUS_PERIOD_TIME_DURATION));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        final DateRange range = (DateRange) l.get(0);
        assertEquals(format.parse("2007-01-01T12Z"), range.getMinValue());
        Date end = format.parse("2007-01-02T13Z");
        assertEquals(end, range.getMaxValue());
    }

    public void testInvalidDualDuration() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");

        boolean exception = false;
        try {
            timeKvpParser.parse(CONTINUOUS_PERIOD_INVALID_DURATION);
            // Verify that an exception was encountered for the invalid duration
            fail("No exception thrown for invalid duration");
        } catch (ParseException ex) {
            assertTrue(ex.getMessage().startsWith("Invalid time period"));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testContinuousRelativeInterval() throws ParseException {
        final int millisInDay = (int) TimeParser.MILLIS_IN_DAY;
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        Calendar back;
        Calendar now;
        Calendar check;
        List<Collection> l;
        DateRange range;

        do {
            now = Calendar.getInstance();
            l = new ArrayList((Collection) timeKvpParser.parse(CONTINUOUS_RELATIVE_PERIOD_H));
            check = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            check.set(Calendar.MILLISECOND, 0);
        } while (!now.equals(check));
        back = (Calendar) now.clone();
        back.add(Calendar.HOUR, -2);
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        range = (DateRange) l.get(0);
        assertEquals(back.getTime(), range.getMinValue());
        assertEquals(now.getTime(), range.getMaxValue());

        do {
            now = Calendar.getInstance();
            l = new ArrayList((Collection) timeKvpParser.parse(CONTINUOUS_RELATIVE_PERIOD_D));
            check = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            check.set(Calendar.MILLISECOND, 0);
        } while (!now.equals(check));
        back = (Calendar) now.clone();
        back.add(Calendar.MILLISECOND, millisInDay * -10);
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        range = (DateRange) l.get(0);
        assertEquals(back.getTime(), range.getMinValue());
        assertEquals(now.getTime(), range.getMaxValue());

        do {
            now = Calendar.getInstance();
            l = new ArrayList((Collection) timeKvpParser.parse(CONTINUOUS_RELATIVE_PERIOD_W));
            check = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            check.set(Calendar.MILLISECOND, 0);
        } while (!now.equals(check));
        back = (Calendar) now.clone();
        back.add(Calendar.MILLISECOND, millisInDay * -2 * 7);
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        range = (DateRange) l.get(0);
        assertEquals(back.getTime(), range.getMinValue());
        assertEquals(now.getTime(), range.getMaxValue());
    }

    public void testMixedValues() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection)  timeKvpParser.parse(CONTINUOUS_PERIOD+",2007-02-01T12Z"));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertTrue(l.get(0) instanceof DateRange);
        final DateRange range=(DateRange) l.get(0);
        assertEquals(format.parse("2007-01-01T12Z"), range.getMinValue());
        Date end = format.parse("2007-01-31T13Z");
        end.setTime(end.getTime() - 1);
        assertEquals(end, range.getMaxValue());
        
        assertRange((DateRange)l.get(1), format.parse("2007-02-01T12Z"), format.parse("2007-02-01T13Z"));
    }
    
    public void testInclusions() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection)  timeKvpParser.parse(CONTINUOUS_PERIOD+",2007-01-29T12Z," +
        		"2007-01-12T12Z,2007-01-17T12Z,2007-01-01T12Z/2007-01-15T12Z"));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertTrue(l.size()==1);
        assertTrue(l.get(0) instanceof DateRange);
        final DateRange range=(DateRange) l.get(0);
        assertRange(range, format.parse("2007-01-01T12Z"), format.parse("2007-01-31T13Z"));
    }
    
    public void testOrderedValues() throws Exception {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = new ArrayList((Collection)  timeKvpParser.parse("2007-01-29T12Z,2007-01-12T12Z," +
        		"2007-01-17T12Z,2007-01-01T12Z,2007-01-05T12Z"));
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertTrue(l.size()==5);
        assertRange((DateRange) l.get(0), format.parse("2007-01-01T12Z"), format.parse("2007-01-01T13Z"));
        assertRange((DateRange) l.get(1), format.parse("2007-01-05T12Z"), format.parse("2007-01-05T13Z"));
        assertRange((DateRange) l.get(2), format.parse("2007-01-12T12Z"), format.parse("2007-01-12T13Z"));
        assertRange((DateRange) l.get(3), format.parse("2007-01-17T12Z"), format.parse("2007-01-17T13Z"));
        assertRange((DateRange) l.get(4), format.parse("2007-01-29T12Z"), format.parse("2007-01-29T13Z"));
    }
    
    public void testNegativeYearCompliance() throws Exception {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        // base assertion - 0001 is year 1
        DateRange date = (DateRange) ((List)timeKvpParser.parse("01-06-01")).get(0);
        cal.setTime(date.getMinValue());
        assertEquals(1, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.AD, cal.get(Calendar.ERA));
        
        date = (DateRange) ((List)timeKvpParser.parse("00-06-01")).get(0);
        cal.setTime(date.getMinValue());
        // calendar calls it year 1, ISO spec means it's year 0
        // but we're just parsing here...
        assertEquals(1, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));
        
        // so, the next year should be 2
        date = (DateRange) ((List)timeKvpParser.parse("-01-06-01")).get(0);
        cal.setTime(date.getMinValue());
        assertEquals(2, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));
        
        // now, big negative year compliance (see the spec, appendix D 2.2, pp 57-58)
        date = (DateRange) ((List)timeKvpParser.parse("-18000-06-01")).get(0);
        cal.setTime(date.getMinValue());
        assertEquals(18001, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));
    }

    private static void assertInstant(Date expected, Object object) {
    	if (object instanceof DateRange) { 
	    	assertEquals(object + " Should start at", expected, ((DateRange)object).getMinValue());
	    	assertEquals(object + " Should end at", expected, ((DateRange)object).getMaxValue());
    	} else if (object instanceof Date) {
    		assertEquals(expected, object);
    	} else {
    		fail("Should have a DateRange: " + object);
    	}
	}
    
    private static void assertRange(DateRange range, Date start, Date end) {
    	assertRangeStarts(range, start);
    	assertRangeEnds(range, new Date(end.getTime() - 1));
    }

    public static void assertRangeLength(DateRange range, long expectedLength) {
    	if (range.getMinValue() == null)
    		fail("Expected finite range, saw: " + range);
    	if (range.getMaxValue() == null)
    		fail("Expected finite range, saw: " + range);
    	long min = range.getMinValue().getTime();
    	long max = range.getMaxValue().getTime();
    	assertEquals("Range " + range + " should have length", expectedLength, max - min);
    }
    
    public static void assertRangeStarts(DateRange range, Date expectedStart) {
    	if (range.getMinValue() == null) 
    		fail("Expected valid start date in range " + range);
    	assertEquals("Range " + range + " should have start", expectedStart, range.getMinValue());
    }
    
    public static void assertRangeEnds(DateRange range, Date expectedEnd) {
    	if (range.getMaxValue() == null)
    		fail("Expected valid end date in range " + range);
    	assertEquals("Range " + range + " should have end", expectedEnd, range.getMaxValue());
    }
}
