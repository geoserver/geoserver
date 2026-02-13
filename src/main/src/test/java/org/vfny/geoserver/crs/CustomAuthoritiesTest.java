/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

public class CustomAuthoritiesTest extends GeoServerSystemTestSupport {

    String WGS84_TEMPLATE =
            "GEOGCS[\"WGS84(DD) - %s\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]],"
                    + " PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic latitude\","
                    + " NORTH], AXIS[\"Geodetic longitude\", EAST],  AUTHORITY[\"%s\",\"%s\"]]";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not set up standard data, but we need some using the custom authority to verify
        // the configuration loads up properly
    }

    @Before
    public void resetCustomAuthorities() throws IOException {
        // add authorities
        addAuthorityList(Map.of("FCA", "First custom authority", "SCA", "Second custom authority"));
        addTestAuthorityProperties("FCA");
        addTestAuthorityProperties("SCA");

        // make sure custom authorities are reloaded
        getGeoServer().reset();
    }

    private void addAuthorityList(Map<String, String> authorities) throws IOException {
        File root = testData.getDataDirectoryRoot();
        File userProjections = new File(root, "user_projections");
        userProjections.mkdirs();
        File authoritiesFile = new File(userProjections, "authorities.properties");
        try (PrintWriter out = new PrintWriter(new FileWriter(authoritiesFile))) {
            for (Map.Entry<String, String> entry : authorities.entrySet()) {
                out.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    private void addTestAuthorityProperties(String authority) throws IOException {
        File root = testData.getDataDirectoryRoot();
        File userProjections = new File(root, "user_projections");
        File scaProperties = new File(userProjections, authority + ".properties");
        try (PrintWriter out = new PrintWriter(new FileWriter(scaProperties))) {
            out.println("1000=" + StringEscapeUtils.escapeJava(WGS84_TEMPLATE.formatted(authority, authority, "1000")));
        }
    }

    @Test
    public void testLoadCustomAuthorities() throws Exception {
        // the test method is called after setup, so the custom authorities should be loaded by now
        assertThat(CRS.getSupportedAuthorities(false), hasItems("FCA", "SCA"));
    }

    @Test
    public void testDecodeAxisOrderFCA() throws Exception {
        // default, should be lon/lat
        CoordinateReferenceSystem wgs84xy = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem fca1000xy = CRS.decode("FCA:1000");
        assertEquals(CRS.AxisOrder.EAST_NORTH, CRS.getAxisOrder(fca1000xy));
        assertTrue(CRS.equalsIgnoreMetadata(wgs84xy, fca1000xy));

        // switch to lat/lon using the urn URI
        CoordinateReferenceSystem wgs84yx = CRS.decode("urn:ogc:def:crs:EPSG::4326", true);
        CoordinateReferenceSystem fca1000yx = CRS.decode("urn:ogc:def:crs:FCA::1000", true);
        assertEquals(CRS.AxisOrder.NORTH_EAST, CRS.getAxisOrder(fca1000yx));
        assertTrue(CRS.equalsIgnoreMetadata(wgs84yx, fca1000yx));
    }

    @Test
    public void testDecodeAxisOrderSCA() throws Exception {
        // default, should be lon/lat
        CoordinateReferenceSystem wgs84xy = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem sca1000xy = CRS.decode("SCA:1000");
        assertEquals(CRS.AxisOrder.EAST_NORTH, CRS.getAxisOrder(sca1000xy));
        assertTrue(CRS.equalsIgnoreMetadata(wgs84xy, sca1000xy));

        // switch to lat/lon using the urn URI
        CoordinateReferenceSystem wgs84yx = CRS.decode("urn:ogc:def:crs:EPSG::4326", true);
        CoordinateReferenceSystem sca1000yx = CRS.decode("urn:ogc:def:crs:SCA::1000", true);
        assertEquals(CRS.AxisOrder.NORTH_EAST, CRS.getAxisOrder(sca1000yx));
        assertTrue(CRS.equalsIgnoreMetadata(wgs84yx, sca1000yx));
    }

    @Test
    public void testChangeAuthoritiesAndReset() throws Exception {
        // initial state
        assertThat(CRS.getSupportedAuthorities(false), allOf(hasItems("FCA", "SCA"), not(hasItems("TCA"))));
        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, CRS.decode("FCA:1000"))); // should work
        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, CRS.decode("SCA:1000"))); // should work
        assertThrows(NoSuchAuthorityCodeException.class, () -> CRS.decode("TCA:1000")); // should fail

        // retain FCA, remove SCA, adding TCA. Removing it from the authorities property file should be enough
        addAuthorityList(Map.of("FCA", "First custom authority", "TCA", "Third custom authority"));
        addTestAuthorityProperties("FCA");
        addTestAuthorityProperties("TCA");

        // make sure custom authorities are reloaded
        getGeoServer().reset();

        // verify new state
        assertThat(CRS.getSupportedAuthorities(false), allOf(hasItems("FCA", "TCA"), not(hasItems("SCA"))));
        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, CRS.decode("FCA:1000"))); // should work
        assertThrows(NoSuchAuthorityCodeException.class, () -> CRS.decode("SCA:1000")); // should fail
        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, CRS.decode("TCA:1000"))); // should work
    }
}
