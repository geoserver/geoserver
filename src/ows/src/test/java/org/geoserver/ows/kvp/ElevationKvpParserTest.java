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

import org.geotools.util.NumberRange;

import junit.framework.TestCase;

/**
 * Test for the elevation kvp parser
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ElevationKvpParserTest extends TestCase {

    public void testPeriod() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List elements = new ArrayList((Collection) parser.parse("1/100/1"));
        assertTrue(elements.get(0) instanceof Double);
        assertTrue(elements.size() == 100);
        assertEquals(1.0, ((Double) elements.get(0)));
    }

    public void testMixed() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List elements = new ArrayList((Collection) parser.parse("5,3,4,1,2,8.9,1/9"));
        assertTrue(elements.get(0) instanceof NumberRange);
        assertEquals(1.0, ((NumberRange<Double>) elements.get(0)).getMinimum());
        assertEquals(9.0, ((NumberRange<Double>) elements.get(0)).getMaximum());
    }

    public void testOutOfOrderSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List elements = new ArrayList((Collection) parser.parse("5,3,4,1,2,8.9"));
        assertEquals(1.0, elements.get(0));
        assertEquals(2.0, elements.get(1));
        assertEquals(3.0, elements.get(2));
        assertEquals(4.0, elements.get(3));
        assertEquals(5.0, elements.get(4));
        assertEquals(8.9, elements.get(5));
    }

    public ElevationKvpParser testOrderedSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List elements = new ArrayList((Collection) parser.parse("1,2,3,4,5,8.9"));
        assertEquals(1.0, elements.get(0));
        assertEquals(2.0, elements.get(1));
        assertEquals(3.0, elements.get(2));
        assertEquals(4.0, elements.get(3));
        assertEquals(5.0, elements.get(4));
        assertEquals(8.9, elements.get(5));
        return parser;
    }

}
