/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.geoserver.catalog.ResourcePool;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.Mercator;
import org.geotools.util.factory.Hints;
import org.junit.BeforeClass;
import org.junit.Test;

public class TiledCRSFactoryTest {

    @BeforeClass
    public static void setup() {
        CRS.reset("all");
        System.setProperty("org.geotools.referencing.forceXY", "true");
        Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        Hints.putSystemDefault(Hints.LENIENT_DATUM_SHIFT, true);
    }

    @Test
    public void testCodes() throws Exception {
        Set<String> codes = CRS.getSupportedCodes("MapML");
        assertThat(codes, hasItems("WGS84", "APSTILE", "OSMTILE", "CBMTILE"));
    }

    @Test
    public void testWGS84() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("MapML:WGS84");
        CoordinateReferenceSystem expected = CRS.decode("EPSG:4326", true);
        assertFalse(CRS.isTransformationRequired(crs, expected));
        assertTrue(CRS.equalsIgnoreMetadata(crs, expected));
        assertEquals("MapML", crs.getName().getCodeSpace());
        assertEquals("WGS84", crs.getName().getCode());
        assertEquals("MapML:WGS84", ResourcePool.lookupIdentifier(crs, false));
    }

    @Test
    public void testOSM() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("MapML:OSMTILE");
        CoordinateReferenceSystem expected = CRS.decode("EPSG:3857");
        assertFalse(CRS.isTransformationRequired(crs, expected));
        assertTrue(CRS.equalsIgnoreMetadata(crs, expected));
        assertEquals("MapML", crs.getName().getCodeSpace());
        assertEquals("OSMTILE", crs.getName().getCode());
        assertEquals("MapML:OSMTILE", ResourcePool.lookupIdentifier(crs, false));
    }

    @Test
    public void testAPS() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("MapML:APSTILE");
        CoordinateReferenceSystem expected = CRS.decode("EPSG:5936");
        assertFalse(CRS.isTransformationRequired(crs, expected));
        assertTrue(CRS.equalsIgnoreMetadata(crs, expected));
        assertEquals("MapML", crs.getName().getCodeSpace());
        assertEquals("APSTILE", crs.getName().getCode());
        assertEquals("MapML:APSTILE", ResourcePool.lookupIdentifier(crs, false));
    }

    @Test
    public void testCBM() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("MapML:CBMTILE");
        CoordinateReferenceSystem expected = CRS.decode("EPSG:3978");
        assertFalse(CRS.isTransformationRequired(crs, expected));
        assertTrue(CRS.equalsIgnoreMetadata(crs, expected));
        assertEquals("MapML", crs.getName().getCodeSpace());
        assertEquals("CBMTILE", crs.getName().getCode());
        assertEquals("MapML:CBMTILE", ResourcePool.lookupIdentifier(crs, false));
    }

    /** @throws Exception */
    @Test
    public void testTransform() throws Exception {
        CoordinateReferenceSystem wgs84 = CRS.decode("MapML:WGS84");
        CoordinateReferenceSystem webMarcator = CRS.decode("MapML:OSMTILE");
        MathTransform tx = CRS.findMathTransform(wgs84, webMarcator);
        assertThat(tx, instanceOf(Mercator.class));
    }
}
