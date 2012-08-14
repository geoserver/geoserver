/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geoserver.wcs.test.CoverageTestSupport;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Tests for custom dimensions in WCS requests.
 * 
 * @author Mike Benowitz
 */
public class CustomDimensionsTest extends CoverageTestSupport {

    private static final String DIMENSION_NAME = CustomFormat.CUSTOM_DIMENSION_NAME;
    private static final QName CUST_WATTEMP = 
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);


    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        setupRasterDimension(DIMENSION_NAME, DimensionPresentation.LIST);
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // add org.geoserver.catalog.testReader.CustomFormat coverage
        dataDirectory.addCoverageFromZip(CUST_WATTEMP, TestData.class.getResource("custwatertemp.zip"),
                null, null);
    }
    
    public void testGetCoverageBadValue() throws Exception {
        // check that we get no data when requesting an incorrect value for custom dimension
        String request = getWaterTempRequest("bad_dimension_value");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNull(image);
    }
    
    public void testGetCoverageGoodValue() throws Exception {
        // check that we get data when requesting a correct value for custom dimension
        String request = getWaterTempRequest("CustomDimValueA");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNotNull(image);
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
    }

    private String getWaterTempRequest(String dimensionValue) {
        String request =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<GetCoverage version=\"1.0.0\" service=\"WCS\"\n" + 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wcs\"\n" + 
                "  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
                "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
                "  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
                "  <sourceCoverage>" + getLayerId(CUST_WATTEMP) + "</sourceCoverage>\n" + 
                "  <domainSubset>\n" + 
                "    <spatialSubset>\n" + 
                "      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
                "        <gml:pos>0.237 40.562</gml:pos>\n" + 
                "        <gml:pos>14.593 44.558</gml:pos>\n" + 
                "      </gml:Envelope>\n" + 
                "      <gml:Grid dimension=\"2\">\n" + 
                "        <gml:limits>\n" + 
                "          <gml:GridEnvelope>\n" + 
                "            <gml:low>0 0</gml:low>\n" + 
                "            <gml:high>25 24</gml:high>\n" + 
                "          </gml:GridEnvelope>\n" + 
                "        </gml:limits>\n" + 
                "        <gml:axisName>x</gml:axisName>\n" + 
                "        <gml:axisName>y</gml:axisName>\n" + 
                "      </gml:Grid>\n" + 
                "    </spatialSubset>\n" + 
                "  </domainSubset>\n" + 
                "  <rangeSubset>\n" + 
                "    <axisSubset name=\"" + DIMENSION_NAME + "\">\n" + 
                "      <singleValue>" + dimensionValue + "</singleValue>\n" + 
                "    </axisSubset>\n" + 
                "  </rangeSubset>\n" + 
                "  <output>\n" + 
                "    <crs>EPSG:4326</crs>\n" + 
                "    <format>GeoTIFF</format>\n" + 
                "  </output>\n" + 
                "</GetCoverage>";
        return request;
    }

    private void setupRasterDimension(String metadata, DimensionPresentation presentation) {
        CoverageInfo info = getCatalog().getCoverageByName(CUST_WATTEMP.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }
}
