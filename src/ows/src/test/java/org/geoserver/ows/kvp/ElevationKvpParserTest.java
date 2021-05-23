/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.platform.ServiceException;
import org.geotools.util.NumberRange;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the elevation kvp parser
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ElevationKvpParserTest {

    @Test
    public void testPeriod() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Double> elements = new ArrayList<>((Collection<Double>) parser.parse("1/100/1"));
        Assert.assertTrue(elements.get(0) instanceof Double);
        Assert.assertEquals(100, elements.size());
        Assert.assertEquals(1.0, elements.get(0), 0d);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMixed() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List<Object> elements =
                new ArrayList<>((Collection<Object>) parser.parse("5,3,4,1,2,8.9,1/9"));
        Assert.assertTrue(elements.get(0) instanceof NumberRange);
        Assert.assertEquals(1.0, ((NumberRange<Double>) elements.get(0)).getMinimum(), 0d);
        Assert.assertEquals(9.0, ((NumberRange<Double>) elements.get(0)).getMaximum(), 0d);
    }

    @Test
    public void testOutOfOrderSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Object> elements = new ArrayList<>((Collection) parser.parse("5,3,4,1,2,8.9"));
        Assert.assertEquals(1.0, elements.get(0));
        Assert.assertEquals(2.0, elements.get(1));
        Assert.assertEquals(3.0, elements.get(2));
        Assert.assertEquals(4.0, elements.get(3));
        Assert.assertEquals(5.0, elements.get(4));
        Assert.assertEquals(8.9, elements.get(5));
    }

    @Test
    public void testOrderedSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Object> elements = new ArrayList((Collection) parser.parse("1,2,3,4,5,8.9"));
        Assert.assertEquals(1.0, elements.get(0));
        Assert.assertEquals(2.0, elements.get(1));
        Assert.assertEquals(3.0, elements.get(2));
        Assert.assertEquals(4.0, elements.get(3));
        Assert.assertEquals(5.0, elements.get(4));
        Assert.assertEquals(8.9, elements.get(5));
    }

    @Test
    public void testInfiniteLoopZeroInterval() {
        String value = "0/0/0";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }

    @Test
    public void testInfiniteLoopPositiveInfinity() {
        String value = "Infinity/Infinity/1";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }

    @Test
    public void testInfiniteLoopNegativeInfinity() {
        String value = "-Infinity/-Infinity/1";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }
}
