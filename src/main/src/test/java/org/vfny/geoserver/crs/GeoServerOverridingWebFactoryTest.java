/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import static org.geotools.referencing.AbstractIdentifiedObject.getIdentifier;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.wms.WebCRSFactory;
import org.hamcrest.Matchers;
import org.junit.Test;

public class GeoServerOverridingWebFactoryTest extends GeoServerSystemTestSupport {

    WebCRSFactory GT_FACTORY = new WebCRSFactory();

    /** Setup transformation overrides to verify they work for CRS codes too */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        new File(testData.getDataDirectoryRoot(), "user_projections").mkdir();
        testData.copyTo(
                GeoServerOverridingWebFactoryTest.class.getResourceAsStream("test_epsg_operations.properties"),
                "user_projections/epsg_operations.properties");

        CRS.reset("all");
    }

    @Test
    public void testCRS84() throws Exception {
        GeoServerOverridingWebFactory factory = new GeoServerOverridingWebFactory();
        CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("CRS:84");

        assertNotNull("CRS should not be null", crs);
        assertTrue("CRS should be a GeographicCRS", crs instanceof GeographicCRS);
        assertEquals("4326", getIdentifier(crs, Citations.EPSG).getCode());
        assertEquals("84", getIdentifier(crs, Citations.CRS).getCode());
        // matches the name at https://www.opengis.net/def/crs/OGC/1.3/CRS84
        assertEquals("WGS 84 longitude-latitude", crs.getName().getCode());

        assertTrue(CRS.equalsIgnoreMetadata(crs, GT_FACTORY.createObject("CRS:84")));
    }

    @Test
    public void testCRS83() throws Exception {
        GeoServerOverridingWebFactory factory = new GeoServerOverridingWebFactory();
        CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("CRS:83");

        assertNotNull("CRS should not be null", crs);
        assertTrue("CRS should be a GeographicCRS", crs instanceof GeographicCRS);
        assertEquals("4269", getIdentifier(crs, Citations.EPSG).getCode());
        assertEquals("83", getIdentifier(crs, Citations.CRS).getCode());
        // matches the name at https://www.opengis.net/def/crs/OGC/1.3/CRS83
        assertEquals("NAD83 longitude-latitude", crs.getName().getCode());

        assertTrue(CRS.equalsIgnoreMetadata(crs, GT_FACTORY.createObject("CRS:83")));
    }

    @Test
    public void testCRS27() throws Exception {
        GeoServerOverridingWebFactory factory = new GeoServerOverridingWebFactory();
        CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("CRS:27");

        assertNotNull("CRS should not be null", crs);
        assertTrue("CRS should be a GeographicCRS", crs instanceof GeographicCRS);
        assertEquals("4267", getIdentifier(crs, Citations.EPSG).getCode());
        assertEquals("27", getIdentifier(crs, Citations.CRS).getCode());
        // matches the name at https://www.opengis.net/def/crs/OGC/1.3/CRS27
        assertEquals("NAD27 longitude-latitude", crs.getName().getCode());

        assertTrue(CRS.equalsIgnoreMetadata(crs, GT_FACTORY.createObject("CRS:27")));
    }

    @Test
    public void testCRSToCRSTransform() throws Exception {
        // checking the transforms defined on EPSG codes work the same on CRS codes
        CoordinateReferenceSystem crs83 = CRS.decode("CRS:83");
        CoordinateReferenceSystem crs84 = CRS.decode("CRS:84");
        MathTransform mt = CRS.findMathTransform(crs83, crs84, true);
        checkOverriddenWGS84ToNAD83(mt);
    }

    @Test
    public void testEPSGtoCRSTransform() throws Exception {
        // checking the transforms defined on EPSG codes work the same on CRS codes
        CoordinateReferenceSystem nad83 = CRS.decode("EPSG:4269");
        CoordinateReferenceSystem crs84 = CRS.decode("CRS:84");
        MathTransform mt = CRS.findMathTransform(nad83, crs84, true);
        checkOverriddenWGS84ToNAD83(mt);
    }

    @Test
    public void testCRStoEPSGTransform() throws Exception {
        // checking the transforms defined on EPSG codes work the same on CRS codes
        CoordinateReferenceSystem crs83 = CRS.decode("CRS:83");
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        MathTransform mt = CRS.findMathTransform(crs83, wgs84, true);
        checkOverriddenWGS84ToNAD83(mt);
    }

    private static void checkOverriddenWGS84ToNAD83(MathTransform mt) {
        // drilling into the math transform is not possibly API wise, but we can spot the silly test
        // parameters from the WKT representation
        String wkt = mt.toWKT();
        assertThat(wkt, Matchers.containsString("PARAMETER[\"dx\", 1"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"dy\", 2"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"dz\", 3"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"ex\", 4"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"ey\", 5"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"ez\", 6"));
        assertThat(wkt, Matchers.containsString("PARAMETER[\"ppm\", 7"));
    }
}
