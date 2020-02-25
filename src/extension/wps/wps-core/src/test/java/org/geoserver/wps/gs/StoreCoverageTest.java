/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

public class StoreCoverageTest extends WPSTestSupport {

    private static final QName CUST_WATTEMP =
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);
    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
        testData.addRasterLayer(
                CUST_WATTEMP, "custwatertemp.zip", null, null, SystemTestData.class, getCatalog());
    }

    @Test
    public void testStore() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:StoreCoverage</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>coverage</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                        + "            <ows:Identifier>"
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "</ows:Identifier>\n"
                        + "            <wcs:DomainSubset>\n"
                        + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                        + "                <ows:LowerCorner>-180.0 -90.0</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>180.0 90.0</ows:UpperCorner>\n"
                        + "              </gml:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>coverageLocation</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getContentAsString();

        // System.out.println(url);

        Map<String, Object> query = KvpUtils.parseQueryString(url);
        assertEquals("GetExecutionResult", query.get("request"));
        String executionId = (String) query.get("executionId");
        String fileName = (String) query.get("outputId");

        WPSResourceManager resources = applicationContext.getBean(WPSResourceManager.class);
        Resource outputResource = resources.getOutputResource(executionId, fileName);
        File tiffFile = outputResource.file();

        assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original =
                getCatalog()
                        .getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
                        .getGridCoverage(null, null);
        scheduleForDisposal(original);

        //
        // check the envelope did not change
        assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);
    }

    @Test
    public void testStoreWCS10() throws Exception {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<wps:Execute service=\"WPS\" version=\"1.0.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\">"
                        + "<ows:Identifier>gs:StoreCoverage</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "  <wps:Input>"
                        + "    <ows:Identifier>coverage</ows:Identifier>"
                        + "    <wps:Reference xlink:href=\"http://geoserver/wcs\" method=\"POST\" mimeType=\"image/grib\">"
                        + "      <wps:Body>"
                        + "        <wcs:GetCoverage service=\"WCS\" version=\"1.0.0\" xmlns:wcs=\"http://www.opengis.net/wcs\" xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "          <wcs:sourceCoverage>"
                        + getLayerId(CUST_WATTEMP)
                        + "</wcs:sourceCoverage>"
                        + "          <wcs:domainSubset>"
                        + "            <wcs:spatialSubset>"
                        + "              <gml:Envelope srsName=\"EPSG:4326\">"
                        + "                <gml:pos>0.0 -91.0</gml:pos>"
                        + "                <gml:pos>360.0 90.0</gml:pos>"
                        + "              </gml:Envelope>"
                        + "              <gml:Grid dimension=\"2\">"
                        + "                <gml:limits>"
                        + "                  <gml:GridEnvelope>"
                        + "                    <gml:low>0 0</gml:low>"
                        + "                    <gml:high>360 181</gml:high>"
                        + "                  </gml:GridEnvelope>"
                        + "                </gml:limits>"
                        + "                <gml:axisName>x</gml:axisName>"
                        + "                <gml:axisName>y</gml:axisName>"
                        + "              </gml:Grid>"
                        + "            </wcs:spatialSubset>"
                        + "          </wcs:domainSubset>"
                        + "          <wcs:output>"
                        + "            <wcs:crs>EPSG:4326</wcs:crs>"
                        + "            <wcs:format>GEOTIFF</wcs:format>"
                        + "          </wcs:output>"
                        + "        </wcs:GetCoverage>"
                        + "      </wps:Body>"
                        + "    </wps:Reference>"
                        + "  </wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "  <wps:RawDataOutput>"
                        + "    <ows:Identifier>coverageLocation</ows:Identifier>"
                        + "  </wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getContentAsString();

        Map<String, Object> query = KvpUtils.parseQueryString(url);
        assertEquals("GetExecutionResult", query.get("request"));
        String executionId = (String) query.get("executionId");
        String fileName = (String) query.get("outputId");

        WPSResourceManager resources = applicationContext.getBean(WPSResourceManager.class);
        Resource outputResource = resources.getOutputResource(executionId, fileName);
        File tiffFile = outputResource.file();

        Assert.assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original =
                getCatalog()
                        .getCoverageByName(getLayerId(CUST_WATTEMP))
                        .getGridCoverage(null, null);
        scheduleForDisposal(original);

        //
        // check the envelope did not change
        Assert.assertEquals(
                original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);
    }
}
