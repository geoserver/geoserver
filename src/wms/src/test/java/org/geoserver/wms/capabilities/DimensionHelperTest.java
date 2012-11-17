/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

import org.junit.Test;

/**
 * A test for proper ISO8601 formatting.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class DimensionHelperTest {
    
    @Test
    public void testNegativeYears() {
        DimensionHelper.ISO8601Formatter fmt = new DimensionHelper.ISO8601Formatter();
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.clear();

        // base assertion
        cal.set(Calendar.YEAR, 1);
        assertEquals("0001-01-01T00:00:00.000Z", fmt.format(cal.getTime()));
        
        // according to the spec, the year before is year 0000
        cal.add(Calendar.YEAR, -1);
        assertEquals("0000-01-01T00:00:00.000Z", fmt.format(cal.getTime()));
        
        // and now where negative territory
        cal.add(Calendar.YEAR, -1);
        assertEquals("-0001-01-01T00:00:00.000Z", fmt.format(cal.getTime()));
        
        // and real negative
        cal.set(Calendar.YEAR, 265000001);
        assertEquals("-265000000-01-01T00:00:00.000Z", fmt.format(cal.getTime()));
    }
    
    /**
     * The goal if this test is to verify behavior of a similar, but not complete,
     * format provided by the standard libraries. The incomplete pattern does
     * not support BC dates properly, so we will not test compliance here.
     * 
     * The random seed is not specified to allow various test runs broader coverage.
     */
    @Test
    public void testFormatterFuzz() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        DimensionHelper.ISO8601Formatter fmt = new DimensionHelper.ISO8601Formatter();
        
        GregorianCalendar cal = new GregorianCalendar();
        Random r = new Random();
        for (int i = 0; i < 1000; i++) {
            cal.set(Calendar.YEAR, 1 + r.nextInt(3000));
            cal.set(Calendar.DAY_OF_YEAR, 1 + r.nextInt(365));
            cal.set(Calendar.HOUR_OF_DAY, r.nextInt(24));
            cal.set(Calendar.MINUTE, r.nextInt(60));
            cal.set(Calendar.SECOND, r.nextInt(60));
            cal.set(Calendar.MILLISECOND, r.nextInt(1000));
            assertEquals(df.format(cal.getTime()), fmt.format(cal.getTime()));
        }
    }

    @Test
    public void testPadding() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        DimensionHelper.ISO8601Formatter fmt = new DimensionHelper.ISO8601Formatter();
        
        assertEquals("0010-01-01T00:01:10.001Z", fmt.format(df.parse("0010-01-01T00:01:10.001")));
        assertEquals("0100-01-01T00:01:10.011Z", fmt.format(df.parse("0100-01-01T00:01:10.011")));
        assertEquals("1000-01-01T00:01:10.111Z", fmt.format(df.parse("1000-01-01T00:01:10.111")));
    }
    
}
