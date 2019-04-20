/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCS2GetCoverageRequestBuilder;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.locationtech.jts.geom.Envelope;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test to check if multiple versions of WCS are supported inside WPS requests.
 *
 * @author driesj
 */
@RunWith(Parameterized.class)
public class ExecuteOnCoverageTest extends WPSTestSupport {

    private final String version;

    @Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[] {"1.1.1"}, new Object[] {"2.0.0"});
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    public ExecuteOnCoverageTest(String version) {
        this.version = version;
    }

    /**
     * We use the crop process as a simple test to see if we requesting a coverage using different
     * WCS versions works.
     */
    @Test
    public void testCrop() throws Exception {
        String wcsRequest =
                WCS2GetCoverageRequestBuilder.newBuilder()
                        .coverageId(getLayerId(MockData.TASMANIA_DEM))
                        .bbox(130.0, 150, -44., -40)
                        .asXML(version);
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:CropCoverage</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>coverage</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + wcsRequest
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>cropShape</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/arcgrid\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        InputStream is = getBinaryInputStream(response);

        ArcGridFormat format = new ArcGridFormat();
        GridCoverage gc = format.getReader(is).read(null);

        assertTrue(
                new Envelope(-145.4, 145.6, -41.8, -42.1)
                        .contains(new ReferencedEnvelope(gc.getEnvelope())));

        double[] valueInside = (double[]) gc.evaluate(new DirectPosition2D(145.55, -42));
        assertEquals(615.0, valueInside[0], 0d);
        double[] valueOutside = (double[]) gc.evaluate(new DirectPosition2D(145.57, -41.9));
        // this should really be NoData
        assertEquals(-9999 & 0xFFFF, (int) valueOutside[0]);
    }
}
