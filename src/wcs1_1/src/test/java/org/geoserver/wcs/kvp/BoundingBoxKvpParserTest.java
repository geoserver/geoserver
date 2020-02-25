/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import net.opengis.ows11.BoundingBoxType;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class BoundingBoxKvpParserTest {

    BoundingBoxKvpParser parser = new BoundingBoxKvpParser();

    @Test
    public void test1DRange() throws Exception {
        executeFailingBBoxTest("10", "This bbox was invalid?");
        executeFailingBBoxTest("10,20", "This bbox was invalid?");
        executeFailingBBoxTest("10,20,30", "This bbox was invalid?");
    }

    @Test
    public void testNonNumericalRange() throws Exception {
        executeFailingBBoxTest("10,20,a,b", "This bbox was invalid?");
        executeFailingBBoxTest("a,20,30,b", "This bbox was invalid?");
    }

    @Test
    public void testOutOfDimRange() throws Exception {
        executeFailingBBoxTest(
                "10,20,30,40,50,60,EPSG:4326", "This bbox has more dimensions than the crs?");
        executeFailingBBoxTest(
                "10,20,30,40,EPSG:4979", "This bbox has less dimensions than the crs?");
    }

    @Test
    public void testUnknownCRS() throws Exception {
        executeFailingBBoxTest(
                "10,20,30,40,50,60,EPSG:MakeNoPrisoners!",
                "This crs should definitely be unknown...");
    }

    void executeFailingBBoxTest(String bbox, String message) throws Exception {
        try {
            parser.parse(bbox);
            fail(message);
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("BoundingBox", e.getLocator());
        }
    }

    @Test
    public void testNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(15.0, bbox.getUpperCorner().get(0));
        assertEquals(30.0, bbox.getUpperCorner().get(1));
        assertNull(bbox.getCrs());
    }

    @Test
    public void test2DNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30,EPSG:4326");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(15.0, bbox.getUpperCorner().get(0));
        assertEquals(30.0, bbox.getUpperCorner().get(1));
        assertEquals("EPSG:4326", bbox.getCrs());
    }

    @Test
    public void test3DNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30,40,50,EPSG:4979");
        assertEquals(3, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(15.0, bbox.getLowerCorner().get(2));
        assertEquals(3, bbox.getUpperCorner().size());
        assertEquals(30.0, bbox.getUpperCorner().get(0));
        assertEquals(40.0, bbox.getUpperCorner().get(1));
        assertEquals(50.0, bbox.getUpperCorner().get(2));
        assertEquals("EPSG:4979", bbox.getCrs());
    }

    @Test
    public void testWgs84FullExtent() throws Exception {
        BoundingBoxType bbox =
                (BoundingBoxType) parser.parse("-180,-90,180,90,urn:ogc:def:crs:EPSG:4326");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(-180.0, bbox.getLowerCorner().get(0));
        assertEquals(-90.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(180.0, bbox.getUpperCorner().get(0));
        assertEquals(90.0, bbox.getUpperCorner().get(1));
        assertEquals("urn:ogc:def:crs:EPSG:4326", bbox.getCrs());
    }
}
