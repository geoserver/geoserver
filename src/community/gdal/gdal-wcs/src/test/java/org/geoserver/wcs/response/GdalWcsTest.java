/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wcs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.kvp.WCSKVPTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GdalWcsTest extends WCSKVPTestSupport {

    private static final QName GRAYALPHA =
            new QName(MockData.SF_URI, "grayAlpha", MockData.SF_PREFIX);
    private static final QName PALETTED =
            new QName(MockData.SF_URI, "paletted", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(
                GRAYALPHA, "gray_alpha.tif", "tif", null, GdalWcsTest.class, getCatalog());
        testData.addRasterLayer(
                PALETTED, "paletted.tif", "tif", null, GdalWcsTest.class, getCatalog());
    }

    @Before
    public void setup() {
        assumeTrue(GdalTestUtil.isGdalAvailable());
        GdalConfigurator.DEFAULT.setExecutable(GdalTestUtil.getGdalTranslate());
        GdalConfigurator.DEFAULT.setEnvironment(GdalTestUtil.getGdalData());

        // force reload of the config, some tests may alter it
        GdalConfigurator configurator = applicationContext.getBean(GdalConfigurator.class);
        configurator.loadConfiguration();
    }

    public boolean isFormatSupported(String mimeType) throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");

        String exprResult =
                xpath.evaluate(
                        "count(//wcs:ServiceMetadata/wcs:formatSupported[text()='"
                                + mimeType
                                + "'])",
                        dom);
        return "1".equals(exprResult);
    }

    @Test
    public void testUnsupportedFormat() throws Exception {
        // MrSID format is not among the default ones
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&Format=image/x-mrsid");

        checkOws20Exception(response, 400, "InvalidParameterValue", "format");
    }

    @Test
    public void testGetCoverageJP2K() throws Exception {
        assumeTrue(isFormatSupported("image/jp2"));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&Format=image/jp2");

        assertEquals("image/jp2", response.getContentType());
    }

    @Test
    public void testGetCoveragePdfByMimeType() throws Exception {
        assumeTrue(isFormatSupported("application/pdf"));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&Format=application/pdf");

        assertEquals("application/pdf", response.getContentType());
    }

    @Test
    public void testGrayAlphaGetCoveragePdf() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId="
                                + getLayerId(GRAYALPHA)
                                + "&Format=application/pdf");

        assertEquals("application/pdf", response.getContentType());
    }

    @Test
    public void testPalettedGetCoveragePdf() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId="
                                + getLayerId(PALETTED)
                                + "&Format=application/pdf");

        assertEquals("application/pdf", response.getContentType());
    }

    @Test
    public void testGetCoveragePdfByName() throws Exception {
        assumeTrue(isFormatSupported("application/pdf"));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=BlueMarble&Format=gdal-pdf");

        // assertEquals("application/pdf", response.getContentType());
        assertEquals("gdal-pdf", response.getContentType());
    }

    @Test
    public void testGetCoverageArcInfoGrid() throws Exception {
        assumeTrue(isFormatSupported("application/zip"));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=DEM&Format=GDAL-ArcInfoGrid");

        // assertEquals("application/zip", response.getContentType());
        assertEquals("GDAL-ArcInfoGrid", response.getContentType());

        GdalTestUtil.checkZippedGridData(getBinaryInputStream(response));
    }

    @Test
    public void testGetCoverageXyzGrid() throws Exception {
        assumeTrue(isFormatSupported("text/plain"));

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=DEM&Format=GDAL-XYZ");

        // assertEquals("text/plain", response.getContentType());
        assertEquals("GDAL-XYZ", response.getContentType());

        GdalTestUtil.checkXyzData(getBinaryInputStream(response));
    }
}
