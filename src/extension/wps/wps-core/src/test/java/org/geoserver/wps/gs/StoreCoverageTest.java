/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;

import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class StoreCoverageTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        addWcs11Coverages(testData);
    }

    @Test
    public void testStore() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
                + "              </gml:BoundingBox>\n" + "            </wcs:DomainSubset>\n"
                + "            <wcs:Output format=\"image/tiff\"/>\n"
                + "          </wcs:GetCoverage>\n" + "        </wps:Body>\n"
                + "      </wps:Reference>\n" + "    </wps:Input>\n" + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n" + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>coverageLocation</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n" + "  </wps:ResponseForm>\n" + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getOutputStreamContent();

        // System.out.println(url);
        assertTrue(url.startsWith("http://localhost:8080/geoserver/temp/wps/tazdem"));
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        File wpsTemp = new File(getDataDirectory().root(), "temp/wps");
        File tiffFile = new File(wpsTemp, fileName);

        assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original = getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
                .getGridCoverage(null, null);
        scheduleForDisposal(original);
        
        //
        // check the envelope did not change
        assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);
    }
}
