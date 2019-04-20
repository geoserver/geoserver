/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import static org.junit.Assert.assertEquals;

import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class SextanteIntegrationTest extends WPSTestSupport {

    /** Tests raster input and output as arcgrid */
    @Test
    public void testArcGridInOut() throws Exception {
        // the baby that we want to parse
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>sxt:divide</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>LAYER</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/arcgrid\"><![CDATA[ncols         2\n"
                        + "nrows         2\n"
                        + "xllcorner     0.0\n"
                        + "yllcorner     0.0\n"
                        + "cellsize      1.0\n"
                        + "NODATA_value  -9999\n"
                        + "1 2\n"
                        + "3 4\n]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>LAYER2</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/arcgrid\"><![CDATA[ncols         2\n"
                        + "nrows         2\n"
                        + "xllcorner     0.0\n"
                        + "yllcorner     0.0\n"
                        + "cellsize      1.0\n"
                        + "NODATA_value  -9999\n"
                        + "2 2\n"
                        + "4 4\n]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/arcgrid\">\n"
                        + "      <ows:Identifier>RESULT</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse sr = postAsServletResponse(root(), xml);

        String expected =
                "NCOLS 2\n"
                        + "NROWS 2\n"
                        + "XLLCORNER 0.0\n"
                        + "YLLCORNER 0.0\n"
                        + "CELLSIZE 1.0\n"
                        + "NODATA_VALUE -9999\n"
                        + "0.5 1.0\n"
                        + "0.75 1.0\n";

        String actual = sr.getContentAsString();
        actual = actual.replace("\r", "");

        assertEquals(expected, actual);
    }
}
