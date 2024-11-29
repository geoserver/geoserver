/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.geotools.util.DateRange;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DateTimeConverterTest {

    private DateTimeConverter dateTimeConverter;
    private SimpleDateFormat dateFormat;

    @Before
    public void setUp() {
        // Ensure tests are not timezone dependent
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        dateTimeConverter = new DateTimeConverter();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testSingleDate() throws ParseException {
        String input = "2023-10-01T12:00:00.000Z";
        Date expected = dateFormat.parse("2023-10-01T12:00:00.000Z");
        DateTimeList result = dateTimeConverter.convert(input);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof Date);
        assertEquals(expected, result.get(0));
    }

    @Test
    public void testIntervalClosed() throws ParseException {
        String input = "2023-10-01T12:00:00.000Z/2023-10-01T14:00:00.000Z";
        Date start = dateFormat.parse("2023-10-01T12:00:00.000Z");
        Date end = dateFormat.parse("2023-10-01T14:00:00.000Z");
        DateTimeList result = dateTimeConverter.convert(input);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof DateRange);
        DateRange range = (DateRange) result.get(0);
        assertEquals(start, range.getMinValue());
        assertEquals(end, range.getMaxValue());
    }

    @Test
    @Ignore // see TODO in DateTimeConverter
    public void testIntervalOpenStart() throws ParseException {
        String input = "../2023-10-01T14:00:00.000Z";
        Date end = dateFormat.parse("2023-10-01T14:00:00.000Z");
        DateTimeList result = dateTimeConverter.convert(input);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof DateRange);
        DateRange range = (DateRange) result.get(0);
        assertNull(range.getMinValue());
        assertEquals(end, range.getMaxValue());
    }

    @Test
    @Ignore // see TODO in DateTimeConverter
    public void testIntervalOpenEnd() throws ParseException {
        String input = "2023-10-01T12:00:00.000Z/..";
        Date start = dateFormat.parse("2023-10-01T12:00:00.000Z");
        DateTimeList result = dateTimeConverter.convert(input);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof DateRange);
        DateRange range = (DateRange) result.get(0);
        assertEquals(start, range.getMinValue().toInstant());
        assertNull(range.getMaxValue());
    }

    @Test
    public void testSequenceWithSingleTimesAndIntervals() throws ParseException {
        String input =
                "2023-10-01T11:00:00.000Z,2023-10-01T12:00:00.000Z/2023-10-01T14:00:00.000Z,2023-10-01T15:00:00.000Z";
        Date date1 = dateFormat.parse("2023-10-01T11:00:00.000Z");
        Date date2 = dateFormat.parse("2023-10-01T12:00:00.000Z");
        Date date3 = dateFormat.parse("2023-10-01T14:00:00.000Z");
        Date date4 = dateFormat.parse("2023-10-01T15:00:00.000Z");
        DateTimeList result = dateTimeConverter.convert(input);
        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof Date);
        assertEquals(date1, result.get(0));
        assertTrue(result.get(1) instanceof DateRange);
        DateRange range = (DateRange) result.get(1);
        assertEquals(date2, range.getMinValue());
        assertEquals(date3, range.getMaxValue());
        assertTrue(result.get(2) instanceof Date);
        assertEquals(date4, result.get(2));
    }
}
