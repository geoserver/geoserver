/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.integration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class WCSVersionIntegrationTest extends GeoServerSystemTestSupport {

    protected static final QName UTM11 = new QName(MockData.WCS_URI, "utm11", MockData.WCS_PREFIX);

    /** Small dataset that sits slightly across the dateline, enough to trigger the "across the dateline" machinery */
    protected static final QName DATELINE_CROSS = new QName(MockData.WCS_URI, "dateline_cross", MockData.WCS_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
        testData.setUpWcs11RasterLayers();
        testData.setUpRasterLayer(UTM11, "/utm11-2.tiff", null, null, WCSTestSupport.class);
        testData.setUpRasterLayer(DATELINE_CROSS, "/datelinecross.tif", null, null, WCSTestSupport.class);
        testData.setupIAULayers(true, false);
    }

    @Test
    public void testAcceptVersions10() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCapabilities&service=WCS&acceptversions=1.0.0");
        assertEquals("application/xml", response.getContentType());

        // xmlunit is not setup to parse WCS 1.0.0 XML, use string checks
        assertThat(
                response.getContentAsString(),
                allOf(
                        containsString("<wcs:WCS_Capabilities"),
                        containsString("version=\"1.0.0\""),
                        containsString("xmlns:ows=\"http://www.opengis.net/ows/1.1\""),
                        containsString("<wcs:CoverageOfferingBrief")));
    }

    @Test
    public void testAcceptVersions11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wcs?request=GetCapabilities&service=WCS&acceptversions=1.1.0");
        assertEquals("text/xml", response.getContentType());

        // xmlunit is not setup to parse WCS 1.1.1 XML, use string checks
        assertThat(
                response.getContentAsString(),
                allOf(
                        containsString("<wcs:Capabilities"),
                        containsString("version=\"1.1.1\""),
                        containsString("xmlns:ows=\"http://www.opengis.net/ows/1.1\""),
                        containsString("<wcs:CoverageSummary")));
    }
}
