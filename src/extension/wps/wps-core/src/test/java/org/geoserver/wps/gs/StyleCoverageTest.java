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
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class StyleCoverageTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    @Test
    public void testStyle() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:StyleCoverage</ows:Identifier>\n"
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
                        + "      <ows:Identifier>style</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=sld/1.0.0\"><![CDATA[<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                        + "<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                        + "  xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd\">\n"
                        + "  <NamedLayer>\n"
                        + "    <Name>rain</Name>\n"
                        + "    <UserStyle>\n"
                        + "      <Name>rain</Name>\n"
                        + "      <Title>Rain distribution</Title>\n"
                        + "      <FeatureTypeStyle>\n"
                        + "        <Rule>\n"
                        + "          <RasterSymbolizer>\n"
                        + "            <Opacity>1.0</Opacity>\n"
                        + "            <ColorMap>\n"
                        + "              <ColorMapEntry color=\"#FF0000\" quantity=\"0\" />\n"
                        + "              <ColorMapEntry color=\"#FFFFFF\" quantity=\"100\"/>\n"
                        + "              <ColorMapEntry color=\"#00FF00\" quantity=\"2000\"/>\n"
                        + "            </ColorMap>\n"
                        + "          </RasterSymbolizer>\n"
                        + "        </Rule>\n"
                        + "      </FeatureTypeStyle>\n"
                        + "    </UserStyle>\n"
                        + "  </NamedLayer>\n"
                        + "</StyledLayerDescriptor>]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"image/tiff\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        assertEquals("attachment; filename=result.tiff", response.getHeader("Content-Disposition"));
        InputStream is = getBinaryInputStream(response);

        // very odd, the tiff reader is not able to read the tiff file, yet desktop apps
        // can read that file just fine...

        //        IOUtils.copy(is, new FileOutputStream("/tmp/testfile.tiff"));
        //
        //        GeoTiffFormat format = new GeoTiffFormat();
        //        GridCoverage2D gc = format.getReader(new
        // FileInputStream("/tmp/testfile.tiff")).read(null);
        //
        //        GridCoverage original =
        // getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
        //                .getGridCoverage(null, null);
        //
        //        // check the envelope did not change
        //        assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0),
        // EPS);
        //        assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1),
        // EPS);
        //        assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0),
        // EPS);
        //        assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1),
        // EPS);
        //
        //        // check the color model is the expected one
        //        assertTrue(gc.getRenderedImage().getColorModel() instanceof ComponentColorModel);
        //        assertEquals(3, gc.getRenderedImage().getSampleModel().getNumBands());

    }
}
