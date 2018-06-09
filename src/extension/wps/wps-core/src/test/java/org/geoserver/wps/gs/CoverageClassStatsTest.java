/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class CoverageClassStatsTest extends WPSTestSupport {

    public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add extra data used by this test
        // addWcs11Coverages(testData);

        Map<SystemTestData.LayerProperty, Object> props =
                new HashMap<SystemTestData.LayerProperty, Object>();
        props.put(SystemTestData.LayerProperty.SRS, 26713);

        testData.addRasterLayer(DEM, "sfdem.tiff", ".tiff", props, getClass(), getCatalog());
    }

    @Test
    public void testClassStats() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" "
                        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "    xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                        + "    xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "    xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "    xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                        + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "    xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                        + "  <ows:Identifier>ras:CoverageClassStats</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>coverage</ows:Identifier>"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">"
                        + "        <wps:Body>"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">"
                        + "            <ows:Identifier>sf:sfdem</ows:Identifier>"
                        + "            <wcs:DomainSubset>"
                        + "              <ows:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#26713\">"
                        + "                <ows:LowerCorner>589980.0 4913700.0</ows:LowerCorner>"
                        + "                <ows:UpperCorner>609000.0 4928010.0</ows:UpperCorner>"
                        + "              </ows:BoundingBox>"
                        + "            </wcs:DomainSubset>"
                        + "            <wcs:Output format=\"image/tiff\"/>"
                        + "          </wcs:GetCoverage>"
                        + "        </wps:Body>"
                        + "      </wps:Reference>"
                        + "    </wps:Input>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>stats</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:LiteralData>mean</wps:LiteralData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>method</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:LiteralData>QUANTILE</wps:LiteralData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>noData</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:LiteralData>-9.99999993381581251e+36</wps:LiteralData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>classes</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:LiteralData>2</wps:LiteralData>"
                        + "      </wps:Data>"
                        + "    </wps:Input>"
                        + "  </wps:DataInputs>"
                        + "  <wps:ResponseForm>"
                        + "    <wps:RawDataOutput mimeType=\"text/xml\">"
                        + "      <ows:Identifier>results</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), xml);
        // print(dom);

        assertEquals("Results", dom.getDocumentElement().getNodeName());
        assertXpathExists("//Class", dom);
        assertXpathEvaluatesTo("2", "count(//Class)", dom);
        assertXpathExists("//Class[@count='145883']", dom);
        assertXpathExists("//Class[@count='146434']", dom);
    }
}
