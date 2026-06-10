package org.geoserver.wfs;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExternalEntitiesTest extends WFSTestSupport {
    private static final String WFS_1_0_0_REQUEST =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE wfs:GetFeature [
            <!ENTITY c SYSTEM "FILE:///this/file/does/not/exist?.XSD">
            ]>
            <wfs:GetFeature service="WFS" version="1.0.0"
              outputFormat="GML2"
              xmlns:cdf="http://www.opengis.net/cite/data"
              xmlns:wfs="http://www.opengis.net/wfs"
              xmlns:ogc="http://www.opengis.net/ogc"
              xmlns:gml="http://www.opengis.net/gml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://www.opengis.net/wfs
                                  http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
              <wfs:Query typeName="cdf:Fifteen" handle="test">
                    <ogc:Literal>&c;</ogc:Literal>
                <ogc:Filter>
                  <ogc:BBOX>
                    <ogc:PropertyName>the_geom</ogc:PropertyName>
                    <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                       <gml:coordinates>-75.102613,40.212597 -72.361859,41.512517</gml:coordinates>
                    </gml:Box>
                  </ogc:BBOX>
               </ogc:Filter>
              </wfs:Query>
            </wfs:GetFeature>""";

    private static final String WFS_1_1_0_REQUEST =
            """
            <!DOCTYPE wfs:GetFeature [
            <!ELEMENT wfs:GetFeature (wfs:Query*)>
            <!ATTLIST wfs:GetFeature
                            service CDATA #FIXED "WFS"
                            version CDATA #FIXED "1.1.0"
                    xmlns:wfs CDATA #FIXED "http://www.opengis.net/wfs"
                            xmlns:ogc CDATA #FIXED "http://www.opengis.net/ogc">
            <!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>
            <!ATTLIST wfs:Query typeName CDATA #FIXED "cdf:Fifteen">
            <!ELEMENT wfs:PropertyName (#PCDATA) >
            <!ELEMENT ogc:Filter (ogc:FeatureId*)>
            <!ELEMENT ogc:FeatureId EMPTY>
            <!ATTLIST ogc:FeatureId fid CDATA #FIXED "states.3">

            <!ENTITY passwd  SYSTEM "FILE:///this/file/does/not/exist?.XSD">]>
            <wfs:GetFeature service="WFS" version="1.1.0"
              xmlns:wfs="http://www.opengis.net/wfs"
              xmlns:ogc="http://www.opengis.net/ogc">
              <wfs:Query typeName="cdf:Fifteen">
                <wfs:PropertyName>&passwd;</wfs:PropertyName>
                    <ogc:Filter>
                   <ogc:FeatureId fid="states.3"/>
                </ogc:Filter>
              </wfs:Query>
            </wfs:GetFeature>""";

    @Test
    public void testWfs1_0() throws Exception {
        Document dom;
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        try {
            // enable entity parsing: server tries to read file on local file system
            dom = postAsDOM("wfs", WFS_1_0_0_REQUEST);
            assertTrue(
                    "SAXException",
                    checkLegacyException(dom, null, null)
                            .contains("xml request is most probably not compliant to GetFeature element"));

            // disable entity parsing: DOCTYPE must be rejected
            System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
            dom = postAsDOM("wfs", WFS_1_0_0_REQUEST);
            assertTrue(checkLegacyException(dom, null, null).contains("DOCTYPE"));
        } finally {
            System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        }
        // default (entity parsing disabled): DOCTYPE must be rejected
        dom = postAsDOM("wfs", WFS_1_0_0_REQUEST);
        assertTrue(checkLegacyException(dom, null, null).contains("DOCTYPE"));
    }

    @Test
    public void testWfs1_1() throws Exception {
        Document dom;
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        try {
            // enable entity parsing: server tries to read file on local file system
            dom = postAsDOM("wfs", WFS_1_1_0_REQUEST);
            assertTrue(
                    "SAXException",
                    dom.getElementsByTagName("ows:ExceptionText")
                            .item(0)
                            .getTextContent()
                            .contains("xml request is most probably not compliant to GetFeature element"));

            // disable entity parsing: DOCTYPE must be rejected
            System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
            dom = postAsDOM("wfs", WFS_1_1_0_REQUEST);
            assertTrue(dom.getElementsByTagName("ows:ExceptionText")
                    .item(0)
                    .getTextContent()
                    .contains("DOCTYPE"));
        } finally {
            System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        }
        // default (entity parsing disabled): DOCTYPE must be rejected
        dom = postAsDOM("wfs", WFS_1_1_0_REQUEST);
        assertTrue(dom.getElementsByTagName("ows:ExceptionText")
                .item(0)
                .getTextContent()
                .contains("DOCTYPE"));
    }

    @Test
    public void testKvpEntityExpansion() throws Exception {
        // prepare the file to be expanded
        File messageFile = new File("./target/message.txt");
        FileUtils.writeStringToFile(messageFile, "broken!", "UTF-8");
        String filePath = messageFile.getCanonicalPath().replace('\\', '/');

        // filter with entity expansion to a ./message.txt file
        String filter =
                "%3C%3Fxml%20version%3D%221.0%22%20encoding%3D%22ISO-8859-1%22%3F%3E%20%3C!DOCTYPE%20foo%20%5B%20%3C!ENTITY%20xxe%20SYSTEM%20%22file%3A%2F%2F"
                        + filePath.replace("/", "%2F")
                        + "%22%20%3E%5D%3E%3CFilter%20%3E%3E%3CPropertyIsEqualTo%3E%3CPropertyName%3E%26xxe%3B%3C%2FPropertyName%3E%3CLiteral%3EUtrecht%3C%2FLiteral%3E%3C%2FPropertyIsEqualTo%3E%3C%2FFilter%3E";
        String request = "wfs?request=GetFeature&SERVICE=WFS&VERSION=1.0.0&TYPENAME="
                + getLayerId(MockData.FIFTEEN)
                + "&FILTER="
                + filter;
        Document doc = getAsDOM(request, false);
        XpathEngine xp = XMLUnit.newXpathEngine();
        String errorMessage = xp.evaluate("//ogc:ServiceException", doc);
        // print(doc);
        assertTrue(errorMessage.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }
}
