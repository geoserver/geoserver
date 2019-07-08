/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.springframework.mock.web.MockHttpServletResponse;

public class ScaleCoverageTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    @Test
    public void testScale() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:ScaleCoverage</ows:Identifier>\n"
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
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>xScale</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>2</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>yScale</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>2</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>xTranslate</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>0</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>yTranslate</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>0</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/arcgrid\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        // System.out.println(response.getOutputStreamContent());
        InputStream is = getBinaryInputStream(response);

        ArcGridFormat format = new ArcGridFormat();
        GridCoverage gc = format.getReader(is).read(null);

        GridCoverage2D original =
                (GridCoverage2D)
                        getCatalog()
                                .getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
                                .getGridCoverage(null, null);
        scheduleForDisposal(original);

        // check the envelope did not change
        assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);

        // check this has been resized properly
        GridEnvelope originalRange = original.getGridGeometry().getGridRange();
        GridEnvelope resultRange = gc.getGridGeometry().getGridRange();
        assertEquals(originalRange.getSpan(0) * 2, resultRange.getSpan(0));
        assertEquals(originalRange.getSpan(1) * 2, resultRange.getSpan(1));
    }
}
