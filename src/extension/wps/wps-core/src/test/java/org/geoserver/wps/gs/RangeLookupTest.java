/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Daniele Romagnoli, GeoSolutions */
public class RangeLookupTest extends WPSTestSupport {

    private static final double DELTA = 1E-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    @Test
    public void testRangeLookup() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>ras:RangeLookup</ows:Identifier>\n"
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
                        + "                <ows:LowerCorner>145.0 -43.0</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>146.0 -41.0</ows:UpperCorner>\n"
                        + "              </gml:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>ranges</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>[1;100]</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>ranges</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>[101;300]</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>ranges</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>[301;700]</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>ranges</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>[701;1500]</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>ranges</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>[1401;30000]</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>outputPixelValues</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>5</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>outputPixelValues</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>20</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>outputPixelValues</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>50</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>outputPixelValues</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>110</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>outputPixelValues</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>255</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>noData</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>0</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/arcgrid\">\n"
                        + "      <ows:Identifier>reclassified</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        // System.out.println(response.getOutputStreamContent());
        InputStream is = getBinaryInputStream(response);

        ArcGridFormat format = new ArcGridFormat();
        GridCoverage gc = format.getReader(is).read(null);

        assertTrue(
                new Envelope(144.9, 146.1, -40.9, -43.1)
                        .contains(new ReferencedEnvelope(gc.getEnvelope())));

        double[] valueOnRangeA = (double[]) gc.evaluate(new DirectPosition2D(145.55, -42));
        double[] valueOnRangeB = (double[]) gc.evaluate(new DirectPosition2D(145.9584, -41.6587));
        double[] valueOutsideRange = (double[]) gc.evaluate(new DirectPosition2D(145.22, -42.66));

        assertEquals(50.0, valueOnRangeA[0], DELTA);
        assertEquals(110.0, valueOnRangeB[0], DELTA);
        assertEquals(0.0, valueOutsideRange[0], DELTA);
    }
}
