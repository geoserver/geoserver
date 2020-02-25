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
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class FeatureClassStatsTest extends WPSTestSupport {

    public static QName STATES = new QName(MockData.DEFAULT_URI, "states", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(
                STATES, new HashMap<>(), "states.properties", getClass(), getCatalog());
    }

    @Test
    public void testClassStats() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "    xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                        + "    xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "    xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" "
                        + "    xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "    xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                        + "  <ows:Identifier>vec:FeatureClassStats</ows:Identifier>"
                        + "  <wps:DataInputs>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>features</ows:Identifier>"
                        + "      <wps:Reference mimeType=\"text/xml\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">"
                        + "        <wps:Body>"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" xmlns:topp=\"http://www.openplans.org/topp\">"
                        + "            <wfs:Query typeName=\"gs:states\"/>"
                        + "          </wfs:GetFeature>"
                        + "        </wps:Body>"
                        + "      </wps:Reference>"
                        + "    </wps:Input>"
                        + "    <wps:Input>"
                        + "      <ows:Identifier>attribute</ows:Identifier>"
                        + "      <wps:Data>"
                        + "        <wps:LiteralData>persons</wps:LiteralData>"
                        + "      </wps:Data>"
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
        assertXpathEvaluatesTo("2", "count(//Class[@count='5'])", dom);
    }
}
