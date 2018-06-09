/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionTrimType;
import org.geoserver.platform.OWS20Exception;
import org.junit.Test;

public class SubsetKvpParserTest {

    SubsetKvpParser parser = new SubsetKvpParser();

    @Test
    public void testInvalidValues() throws Exception {
        try {
            parser.parse("test");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("test,EPSG:4326");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("test(,");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("test(,)");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("test(\"abc,def\",abc)");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
    }

    @Test
    public void testSliceString() throws Exception {
        DimensionSliceType result = (DimensionSliceType) parser.parse("mydim(\"myvalue\")");
        assertEquals("mydim", result.getDimension());
        assertEquals("myvalue", result.getSlicePoint());
    }

    @Test
    public void testSliceNumber() throws Exception {
        DimensionSliceType result = (DimensionSliceType) parser.parse("mydim(12345)");
        assertEquals("mydim", result.getDimension());
        assertEquals("12345", result.getSlicePoint());
    }

    @Test
    public void testTrimOpen() throws Exception {
        // not sure if this makes any sense, but the sytax allows it, and it's like not
        // specifying anything
        DimensionTrimType result = (DimensionTrimType) parser.parse("lon,EPSG:4326(*,*)");
        assertEquals("lon", result.getDimension());
        assertEquals("EPSG:4326", result.getCRS());
        assertNull(result.getTrimLow());
        assertNull(result.getTrimHigh());
    }

    @Test
    public void testTrimNumbers() throws Exception {
        // not sure if this makes any sense, but the sytax allows it, and it's like not
        // specifying anything
        DimensionTrimType result = (DimensionTrimType) parser.parse("lon,EPSG:4326(10,20)");
        assertEquals("lon", result.getDimension());
        assertEquals("EPSG:4326", result.getCRS());
        assertEquals("10", result.getTrimLow());
        assertEquals("20", result.getTrimHigh());
    }

    @Test
    public void testTrimStringsCommas() throws Exception {
        // not sure if this makes any sense, but the sytax allows it, and it's like not
        // specifying anything
        DimensionTrimType result = (DimensionTrimType) parser.parse("mydim(\"a,b\",\"c,d\")");
        assertEquals("mydim", result.getDimension());
        assertNull(result.getCRS());
        assertEquals("a,b", result.getTrimLow());
        assertEquals("c,d", result.getTrimHigh());
    }

    private void checkInvalidSyntaxException(OWS20Exception e) {
        assertNotNull(e.getHttpCode());
        assertEquals(400, e.getHttpCode().intValue());
        assertEquals("InvalidEncodingSyntax", e.getCode());
        assertEquals("subset", e.getLocator());
    }
}
