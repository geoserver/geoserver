/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static org.junit.Assert.assertEquals;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeoHashCalculatorTest {

    WKTReader wkt = new WKTReader();
    GeoHashCalculator calculator = GeoHashCalculator.DEFAULT;

    @BeforeClass
    public static void setup() {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        CRS.reset("all");
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty("org.geotools.referencing.forceXY");
        CRS.reset("all");
    }

    @Test
    public void testPoint() throws ParseException, TransformException {
        String hash = calculator.compute(wkt.read("POINT(0 0)"));
        assertEquals("s00000000000", hash);
    }

    @Test
    public void testLinestring() throws ParseException, TransformException {
        String hash = calculator.compute(wkt.read("LINESTRING(-10 -10, -9 -9)"));
        assertEquals("7y3", hash);
    }

    @Test
    public void testLinestringP1() throws ParseException, TransformException {
        String hash = calculator.compute(wkt.read("LINESTRING(-10 -10, -9.9 -9.9)"));
        assertEquals("7y0z", hash);
    }

    @Test
    public void testLinestringP2() throws ParseException, TransformException {
        String hash = calculator.compute(wkt.read("LINESTRING(-10 -10, -9.99 -9.99)"));
        assertEquals("7y0zhs", hash);
    }

    @Test
    public void testLinestringP3() throws ParseException, TransformException {
        String hash = calculator.compute(wkt.read("LINESTRING(-10 -10, -9.9999 -9.9999)"));
        assertEquals("7y0zh7w6", hash);
    }

    @Test
    public void testLargePolygon() throws ParseException, TransformException {
        String hash =
                calculator.compute(wkt.read("POLYGON((-180 -90, 180 90, 0 90, 0 -90, -180 -90))"));
        assertEquals("s", hash);
    }

    @Test
    public void testPointYTM32N() throws ParseException, TransformException, FactoryException {
        GeoHashCalculator calculator = GeoHashCalculator.get(CRS.decode("EPSG:32632", true));
        String hash = calculator.compute(wkt.read("POINT(1000000 4000000)"));
        assertEquals("sq6kgh81t2z9", hash);
    }
}
