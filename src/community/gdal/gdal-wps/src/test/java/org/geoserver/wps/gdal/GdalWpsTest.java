/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.gdal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogr.core.Format;
import org.geoserver.wcs.response.GdalConfigurator;
import org.geoserver.wcs.response.GdalCoverageResponseDelegate;
import org.geoserver.wcs.response.GdalTestUtil;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridFormat;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GdalWpsTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(GdalTestUtil.isGdalAvailable());

        GdalConfigurator.DEFAULT.setExecutable(GdalTestUtil.getGdalTranslate());
        GdalConfigurator.DEFAULT.setEnvironment(GdalTestUtil.getGdalData());

        // force reload of the config, some tests may alter it
        GdalConfigurator configurator = applicationContext.getBean(GdalConfigurator.class);
        configurator.loadConfiguration();
    }

    @Test
    public void testDescribeProcess() throws Exception {
        GdalCoverageResponseDelegate delegate =
                applicationContext.getBean(GdalCoverageResponseDelegate.class);

        Document d =
                getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:CropCoverage");
        String base = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        for (Format f : delegate.getFormats()) {
            assertXpathExists(
                    base
                            + "/Output[1]/ComplexOutput/Supported/Format[MimeType='"
                            + delegate.getMimeType(f.getGeoserverFormat())
                            + "; subtype="
                            + f.getGeoserverFormat()
                            + "']",
                    d);
        }
    }

    @Test
    public void testCrop() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:CropCoverage</ows:Identifier>\n"
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
                        + "      <ows:Identifier>cropShape</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/zip; subtype=GDAL-ArcInfoGrid\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);

        ZipInputStream is = new ZipInputStream(getBinaryInputStream(response));
        ZipEntry entry = null;
        boolean arcGridFound = false;
        while ((entry = is.getNextEntry()) != null && !arcGridFound) {
            if (entry.getName().endsWith(".asc")) {
                ArcGridFormat format = new ArcGridFormat();
                GridCoverage2D gc = format.getReader(is).read(null);

                assertTrue(
                        new Envelope(-145.4, 145.6, -41.8, -42.1)
                                .contains(new ReferencedEnvelope(gc.getEnvelope())));

                double[] valueInside = (double[]) gc.evaluate(new DirectPosition2D(145.55, -42));
                assertEquals(615.0, valueInside[0], 1E-12);
                double[] valueOutside = (double[]) gc.evaluate(new DirectPosition2D(145.57, -41.9));
                // this should really be NoData... (-9999 & 0xFFFF)
                assertEquals(55537.0, valueOutside[0], 1E-12);

                gc.dispose(true);

                arcGridFound = true;
            }
        }

        assertTrue(arcGridFound);
    }

    @Test
    public void testCropText() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:CropCoverage</ows:Identifier>\n"
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
                        + "      <ows:Identifier>cropShape</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/plain; subtype=GDAL-XYZ\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(getBinaryInputStream(response)));
        String line = null;
        boolean valueInsideFound = false, valueOutsideFound = false;
        double x1 = -145.4, x2 = 145.6, y1 = -42.1, y2 = -41.8;
        while ((line = reader.readLine()) != null) {
            String[] cols = line.split(" ");
            assertTrue(cols.length == 3);

            double x = round(Double.valueOf(cols[0]));
            double y = round(Double.valueOf(cols[1]));
            double value = Double.valueOf(cols[2]);
            assertTrue(x >= x1 && x <= x2);
            assertTrue(y >= y1 && y <= y2);
            if (x == 145.55 && y == -42 && !valueInsideFound) {
                assertEquals(550.0, value, 1E-12);
                valueInsideFound = true;
            }
            if (x == 145.57 && y == -41.9 && !valueOutsideFound) {
                assertEquals(55537.0, value, 1E-12);
                valueOutsideFound = true;
            }
        }

        assertTrue(valueInsideFound);
        assertTrue(valueOutsideFound);
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
