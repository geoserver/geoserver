/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class ReaderDimensionAccessorTest {

    static class MockDimensionReader extends AbstractGridCoverage2DReader {

        Map<String, String> metadata = new HashMap<>();

        @Override
        public Format getFormat() {

            return null;
        }

        @Override
        public GridCoverage2D read(GeneralParameterValue[] parameters)
                throws IllegalArgumentException, IOException {
            return null;
        }

        @Override
        public String[] getMetadataNames() {
            Set<String> keys = metadata.keySet();
            return keys.toArray(new String[keys.size()]);
        }

        @Override
        public String getMetadataValue(String coverageName, String name) {
            return super.getMetadataValue(name);
        }

        @Override
        public String getMetadataValue(String name) {
            return metadata.get(name);
        }
    };

    @Test
    public void testMixedTimeExtraction() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_TIME_DOMAIN, "true");
        reader.metadata.put(
                GridCoverage2DReader.TIME_DOMAIN,
                "2016-02-23T03:00:00.000Z/2016-02-23T03:00:00.000Z/PT1S,2016-02-23T06:00:00.000Z,2016-02-23T09:00:00.000Z/2016-02-23T12:00:00.000Z/PT1S");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        TreeSet<Object> domain = accessor.getTimeDomain();
        assertEquals(3, domain.size());
        Iterator<Object> it = domain.iterator();
        Date firstEntry = (Date) it.next();
        assertEquals(accessor.getTimeFormat().parse("2016-02-23T03:00:00.000Z"), firstEntry);
        Date secondEntry = (Date) it.next();
        assertEquals(accessor.getTimeFormat().parse("2016-02-23T06:00:00.000Z"), secondEntry);
        DateRange thirdEntry = (DateRange) it.next();
        assertEquals(
                accessor.getTimeFormat().parse("2016-02-23T09:00:00.000Z"),
                thirdEntry.getMinValue());
        assertEquals(
                accessor.getTimeFormat().parse("2016-02-23T12:00:00.000Z"),
                thirdEntry.getMaxValue());
    }

    @Test
    public void testMixedElevationExtraction() throws IOException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_ELEVATION_DOMAIN, "true");
        reader.metadata.put(GridCoverage2DReader.ELEVATION_DOMAIN, "0/0/0,10,15/20/1");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        TreeSet<Object> domain = accessor.getElevationDomain();
        assertEquals(3, domain.size());
        Iterator<Object> it = domain.iterator();
        Number firstEntry = (Number) it.next();
        assertEquals(0, firstEntry.doubleValue(), 0d);
        Number secondEntry = (Number) it.next();
        assertEquals(10, secondEntry.doubleValue(), 0d);
        NumberRange thirdEntry = (NumberRange) it.next();
        assertEquals(15, thirdEntry.getMinimum(), 0d);
        assertEquals(20, thirdEntry.getMaximum(), 0d);
    }

    private static SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    static {
        DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testCustomTimeDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.util.Date");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted =
                accessor.convertDimensionValue(
                        "MYDIM",
                        Arrays.asList(
                                "2001-05-01T00:00:00.000Z",
                                "2001-05-02T00:00:00.000Z",
                                "2001-05-03T00:00:00.000Z"));
        assertEquals(3, converted.size());
        assertEquals(DF.parse("2001-05-01 00:00:00"), converted.get(0));
        assertEquals(DF.parse("2001-05-02 00:00:00"), converted.get(1));
        assertEquals(DF.parse("2001-05-03 00:00:00"), converted.get(2));
    }

    @Test
    public void testCustomDepthDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.lang.Double");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted = accessor.convertDimensionValue("MYDIM", Arrays.asList("10/20"));
        assertEquals(1, converted.size());
        NumberRange<Double> expected = new NumberRange<>(Double.class, 10d, 20d);
        assertEquals(expected, converted.get(0));
    }

    @Test
    public void testCustomCloudCoverDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.lang.Integer");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted = accessor.convertDimensionValue("MYDIM", Arrays.asList("75/100"));
        assertEquals(1, converted.size());
        NumberRange<Double> expected = new NumberRange<>(Double.class, 75d, 100d);
        assertEquals(expected, converted.get(0));
    }

    /** Checks that the hasAnyTime method works as expected for the non structured reader case */
    @Test
    public void testHasAnyTime() throws Exception {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_TIME_DOMAIN, "true");
        reader.metadata.put(
                GridCoverage2DReader.TIME_DOMAIN,
                "2016-02-23T03:00:00.000Z/2016-02-23T03:00:00.000Z/PT1S,2016-02-23T06:00:00.000Z,2016-02-23T09:00:00.000Z/2016-02-23T12:00:00.000Z/PT1S");

        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);

        SimpleDateFormat format = accessor.getTimeFormat();
        // test a few positive cases
        // ... exact instant match
        assertHasTimes(accessor, format, "2016-02-23T06:00:00.000Z");
        // ... contained instant
        assertHasTimes(accessor, format, "2016-02-23T10:00:00.000Z");
        // ... intersecting range from below
        DateRange rangeBelow =
                new DateRange(
                        format.parse("2016-02-23T00:00:00.000Z"),
                        format.parse("2016-02-23T05:00:00.000Z"));
        assertTrue(accessor.hasAnyTime(Arrays.asList(rangeBelow)));
        // ... intersecting range from above
        DateRange rangeAbove =
                new DateRange(
                        format.parse("2016-02-23T11:00:00.000Z"),
                        format.parse("2016-02-23T15:00:00.000Z"));
        assertTrue(accessor.hasAnyTime(Arrays.asList(rangeAbove)));

        // test a few negative cases
        // ... before, mid, and after instants
        assertNoTimes(accessor, format, "2016-02-23T02:00:00.000Z");
        assertNoTimes(accessor, format, "2016-02-23T05:00:00.000Z");
        assertNoTimes(accessor, format, "2016-02-23T23:00:00.000Z");
        // ... before, mid, and after ranges
        DateRange rangeBefore =
                new DateRange(
                        format.parse("2016-02-22T00:00:00.000Z"),
                        format.parse("2016-02-22T23:00:00.000Z"));
        assertFalse(accessor.hasAnyTime(Arrays.asList(rangeBefore)));
        DateRange rangeMid =
                new DateRange(
                        format.parse("2016-02-23T04:00:00.000Z"),
                        format.parse("2016-02-23T05:00:00.000Z"));
        assertFalse(accessor.hasAnyTime(Arrays.asList(rangeMid)));
        DateRange rangeAfter =
                new DateRange(
                        format.parse("2016-02-24T00:00:00.000Z"),
                        format.parse("2016-02-24T23:00:00.000Z"));
        assertFalse(accessor.hasAnyTime(Arrays.asList(rangeAfter)));
    }

    private static void assertHasTimes(
            ReaderDimensionsAccessor accessor, SimpleDateFormat format, String source)
            throws IOException, ParseException {
        assertTrue(accessor.hasAnyTime(Arrays.asList(format.parse(source))));
    }

    private static void assertNoTimes(
            ReaderDimensionsAccessor accessor, SimpleDateFormat format, String source)
            throws IOException, ParseException {
        assertFalse(accessor.hasAnyTime(Arrays.asList(format.parse(source))));
    }

    @Test
    public void testHasAnyElevation() throws Exception {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_ELEVATION_DOMAIN, "true");
        reader.metadata.put(GridCoverage2DReader.ELEVATION_DOMAIN, "0/0/0,10,15/20/1");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);

        // test a few positive cases
        // ... exact match
        assertTrue(accessor.hasAnyElevation(Arrays.asList(10d)));
        // ... contained value
        assertTrue(accessor.hasAnyElevation(Arrays.asList(16d)));
        // ... intersecting range from below
        NumberRange<Double> rangeBelow = new NumberRange<>(Double.class, -3d, 3d);
        assertTrue(accessor.hasAnyElevation(Arrays.asList(rangeBelow)));
        // ... intersecting range from above
        NumberRange<Double> rangeAbove = new NumberRange<>(Double.class, 17d, 23d);
        assertTrue(accessor.hasAnyElevation(Arrays.asList(rangeAbove)));

        // test a few negative cases
        // ... before, mid, and after instants
        assertFalse(accessor.hasAnyElevation(Arrays.asList(-3d)));
        assertFalse(accessor.hasAnyElevation(Arrays.asList(5d)));
        assertFalse(accessor.hasAnyElevation(Arrays.asList(23d)));
        // ... before, mid, and after ranges
        NumberRange<Double> rangeBefore = new NumberRange<>(Double.class, -3d, -1d);
        assertFalse(accessor.hasAnyElevation(Arrays.asList(rangeBefore)));
        NumberRange<Double> rangeMid = new NumberRange<>(Double.class, 5d, 7d);
        assertFalse(accessor.hasAnyElevation(Arrays.asList(rangeMid)));
        NumberRange<Double> rangeAfter = new NumberRange<>(Double.class, 23d, 27d);
        assertFalse(accessor.hasAnyElevation(Arrays.asList(rangeAfter)));
    }
}
