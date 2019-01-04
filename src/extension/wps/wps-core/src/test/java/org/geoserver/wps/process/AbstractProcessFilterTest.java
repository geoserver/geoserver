/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public abstract class AbstractProcessFilterTest extends WPSTestSupport {

    @Test
    public void testCapabilitesFiltering() throws Exception {
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        // print(d);
        assertXpathEvaluatesTo("0", "count(//wps:Process[ows:Identifier = 'gs:Unique'])", d);
        assertXpathEvaluatesTo("0", "count(//wps:Process[ows:Identifier = 'JTS:intersects'])", d);
        assertXpathEvaluatesTo("1", "count(//wps:Process[ows:Identifier = 'JTS:buffer'])", d);
    }

    @Test
    public void testDescribeProcessFiltering() throws Exception {
        // this one we can describe
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=JTS:buffer");
        assertXpathExists("/wps:ProcessDescriptions", d);

        // not this one, it's filtered out
        d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=JTS:intersection");
        checkOws11Exception(d);
        assertXpathEvaluatesTo("No such process: JTS:intersection", "//ows:ExceptionText", d);

        // and not this one, filtered out too
        d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:Unique");
        checkOws11Exception(d);
        assertXpathEvaluatesTo("No such process: gs:Unique", "//ows:ExceptionText", d);
    }

    @Test
    public void testExecuteFilteringAllow() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>JTS:buffer</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>distance</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>1</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>geom</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + "<gml:Polygon xmlns:gml='http://www.opengis.net/gml'>"
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:coordinates>1 1 2 1 2 2 1 2 1 1</gml:coordinates>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
                        + "</gml:Polygon>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='false'>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        checkValidationErrors(d);

        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());

        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessSucceeded", d);
        assertXpathExists(
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/gml:Polygon",
                d);
    }

    @Test
    public void testExecuteFilteringDeny() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Unique</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(MockData.PRIMITIVEGEOFEATURE)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>attribute</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>intProperty</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document doc = postAsDOM(root(), xml);
        checkOws11Exception(doc);
        assertXpathEvaluatesTo("Unknown process gs:Unique", "//ows:ExceptionText", doc);
    }
}
