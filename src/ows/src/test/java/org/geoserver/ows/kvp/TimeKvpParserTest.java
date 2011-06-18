/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import junit.framework.TestCase;

        
/**
 * Test for the time parameter in a WMS request.
 * 
 * @author Cedric Briancon
 */
public class TimeKvpParserTest extends TestCase {
    /**
     * A time period for testing.
     */
    private final static String PERIOD = "2007-01-01T12Z/2007-01-31T12Z/P1DT12H";
    
    /**
     * Format of dates.
     */
    private final static DateFormat format;
    static{
    	 format = new SimpleDateFormat("yyyy-MM-dd'T'HH'Z'");
    	 format.setTimeZone(TimeKvpParser.UTC_TZ);

    }

    /**
     * Tests only the increment part of the time parameter.
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testPeriod() throws ParseException {
        final long millisInDay = TimeKvpParser.MILLIS_IN_DAY;
        assertEquals(               millisInDay,  TimeKvpParser.parsePeriod("P1D"));
        assertEquals(             3*millisInDay,  TimeKvpParser.parsePeriod("P3D"));
        assertEquals(            14*millisInDay,  TimeKvpParser.parsePeriod("P2W"));
        assertEquals(             8*millisInDay,  TimeKvpParser.parsePeriod("P1W1D"));
        assertEquals(               millisInDay,  TimeKvpParser.parsePeriod("PT24H"));
        assertEquals(Math.round(1.5*millisInDay), TimeKvpParser.parsePeriod("P1.5D"));
    }

    /**
     * Compares the dates obtained by parsing the time parameter with the expected values.
     *
     * @throws ParseException if the string can't be parsed.
     */
    public void testInterval() throws ParseException {
        TimeKvpParser timeKvpParser = new TimeKvpParser("TIME");
        List l = (List) timeKvpParser.parse(PERIOD);
        // Verify that the list contains at least one element.
        assertFalse(l.isEmpty());
        assertEquals(format.parse("2007-01-01T12Z"), l.get(0));
        assertEquals(format.parse("2007-01-03T00Z"), l.get(1));
        assertEquals(format.parse("2007-01-04T12Z"), l.get(2));
        assertEquals(format.parse("2007-01-06T00Z"), l.get(3));
        assertEquals(format.parse("2007-01-07T12Z"), l.get(4));
        assertEquals(format.parse("2007-01-09T00Z"), l.get(5));
        assertEquals(format.parse("2007-01-10T12Z"), l.get(6));
        assertEquals(format.parse("2007-01-12T00Z"), l.get(7));
    }
}
