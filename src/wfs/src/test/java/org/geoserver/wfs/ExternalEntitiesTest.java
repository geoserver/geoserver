/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExternalEntitiesTest extends WFSTestSupport {

    private static final String WFS_1_0_0_REQUEST =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                    + "<!DOCTYPE wfs:GetFeature [\r\n"
                    + "<!ENTITY c SYSTEM \"FILE:///this/file/does/not/exist?.XSD\">\r\n"
                    + "]>\r\n"
                    + "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" \r\n"
                    + "  outputFormat=\"GML2\"\r\n"
                    + "  xmlns:cdf=\"http://www.opengis.net/cite/data\"\r\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs\r\n"
                    + "                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">\r\n"
                    + "  <wfs:Query typeName=\"cdf:Fifteen\" handle=\"test\">\r\n"
                    + "        <ogc:Literal>&c;</ogc:Literal>\r\n"
                    + "    <ogc:Filter>\r\n"
                    + "      <ogc:BBOX>\r\n"
                    + "        <ogc:PropertyName>the_geom</ogc:PropertyName>\r\n"
                    + "        <gml:Box srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\r\n"
                    + "           <gml:coordinates>-75.102613,40.212597 -72.361859,41.512517</gml:coordinates>\r\n"
                    + "        </gml:Box>\r\n"
                    + "      </ogc:BBOX>\r\n"
                    + "   </ogc:Filter>\r\n"
                    + "  </wfs:Query>\r\n"
                    + "</wfs:GetFeature>";

    private static final String WFS_1_1_0_REQUEST =
            "<!DOCTYPE wfs:GetFeature [\r\n"
                    + "<!ELEMENT wfs:GetFeature (wfs:Query*)>\r\n"
                    + "<!ATTLIST wfs:GetFeature\r\n"
                    + "                service CDATA #FIXED \"WFS\"\r\n"
                    + "                version CDATA #FIXED \"1.1.0\"\r\n"
                    + "        xmlns:wfs CDATA #FIXED \"http://www.opengis.net/wfs\"\r\n"
                    + "                xmlns:ogc CDATA #FIXED \"http://www.opengis.net/ogc\">\r\n"
                    + "<!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>\r\n"
                    + "<!ATTLIST wfs:Query typeName CDATA #FIXED \"cdf:Fifteen\">\r\n"
                    + "<!ELEMENT wfs:PropertyName (#PCDATA) >\r\n"
                    + "<!ELEMENT ogc:Filter (ogc:FeatureId*)>\r\n"
                    + "<!ELEMENT ogc:FeatureId EMPTY>\r\n"
                    + "<!ATTLIST ogc:FeatureId fid CDATA #FIXED \"states.3\">\r\n"
                    + "\r\n"
                    + "<!ENTITY passwd  SYSTEM \"FILE:///this/file/does/not/exist?.XSD\">]>\r\n"
                    + "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" \r\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n"
                    + "  <wfs:Query typeName=\"cdf:Fifteen\">\r\n"
                    + "    <wfs:PropertyName>&passwd;</wfs:PropertyName>\r\n"
                    + "        <ogc:Filter>\r\n"
                    + "       <ogc:FeatureId fid=\"states.3\"/>\r\n"
                    + "    </ogc:Filter>\r\n"
                    + "  </wfs:Query>\r\n"
                    + "</wfs:GetFeature>";

    private static final String WFS_2_0_0_REQUEST =
            "<?xml version=\"1.0\" ?>\r\n"
                    + "<!DOCTYPE wfs:GetFeature [\r\n"
                    + "<!ELEMENT wfs:GetFeature (wfs:Query*)>\r\n"
                    + "<!ATTLIST wfs:GetFeature\r\n"
                    + "                service   CDATA #FIXED \"WFS\"\r\n"
                    + "                version   CDATA #FIXED \"2.0.0\"\r\n"
                    + "                outputFormat CDATA #FIXED \"application/gml+xml; version=3.2\"\r\n"
                    + "        xmlns:wfs CDATA #FIXED \"http://www.opengis.net/wfs\"\r\n"
                    + "                xmlns:ogc CDATA #FIXED \"http://www.opengis.net/ogc\"\r\n"
                    + "                xmlns:fes CDATA #FIXED \"http://www.opengis.net/fes/2.0\">\r\n"
                    + "<!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>\r\n"
                    + "<!ATTLIST wfs:Query typeName CDATA #FIXED \"cdf:Fifteen\">\r\n"
                    + "<!ELEMENT wfs:PropertyName (#PCDATA) >\r\n"
                    + "<!ELEMENT ogc:Filter (fes:ResourceId*)>\r\n"
                    + "<!ELEMENT fes:ResourceId EMPTY>\r\n"
                    + "<!ATTLIST fes:ResourceId rid CDATA #FIXED \"states.3\">\r\n"
                    + "\r\n"
                    + "<!ENTITY passwd  SYSTEM \"FILE:///thisfiledoesnotexist?.XSD\">\r\n"
                    + "]>\r\n"
                    + "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" outputFormat=\"application/gml+xml; version=3.2\"\r\n"
                    + "        xmlns:wfs=\"http://www.opengis.net/wfs/2.0\"\r\n"
                    + "        xmlns:fes=\"http://www.opengis.net/fes/2.0\">\r\n"
                    + "        <wfs:Query typeName=\"cdf:Fifteen\">\r\n"
                    + "                <wfs:PropertyName>&passwd;</wfs:PropertyName>\r\n"
                    + "                <fes:Filter>\r\n"
                    + "                        <fes:ResourceId rid=\"states.3\"/>\r\n"
                    + "                </fes:Filter>\r\n"
                    + "        </wfs:Query>\r\n"
                    + "</wfs:GetFeature>";

    @Test
    public void testWfs1_0() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(cfg);

            String output = string(post("wfs", WFS_1_0_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("java.io.FileNotFoundException") > -1);

            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("Entity resolution disallowed") > -1);

            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("Entity resolution disallowed") > -1);
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);
        }
    }

    @Test
    public void testWfs1_1() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(cfg);

            String output = string(post("wfs", WFS_1_1_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("java.io.FileNotFoundException") > -1);

            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_1_0_REQUEST));
            Assert.assertTrue(output.indexOf("Entity resolution disallowed") > -1);

            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_1_1_0_REQUEST));
            Assert.assertTrue(output.indexOf("Entity resolution disallowed") > -1);
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);
        }
    }

    @Test
    public void testWfs2_0() throws Exception {
        GeoServerInfo cfg = getGeoServer().getGlobal();
        try {
            // enable entity parsing
            cfg.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(cfg);

            String output = string(post("wfs", WFS_2_0_0_REQUEST));
            // the server tried to read a file on local file system
            Assert.assertTrue(output.indexOf("thisfiledoesnotexist") > -1);

            // disable entity parsing
            cfg.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_2_0_0_REQUEST));
            // System.out.println(output);
            Assert.assertTrue(output.indexOf("Request parsing failed") > -1);
            Assert.assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));

            // set default (entity parsing disabled);
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);

            output = string(post("wfs", WFS_2_0_0_REQUEST));
            Assert.assertTrue(output.indexOf("Request parsing failed") > -1);
            Assert.assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
        } finally {
            cfg.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(cfg);
        }
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
        String request =
                "wfs?request=GetFeature&SERVICE=WFS&VERSION=1.0.0&TYPENAME="
                        + getLayerId(MockData.FIFTEEN)
                        + "&FILTER="
                        + filter;
        Document doc = getAsDOM(request);
        XpathEngine xp = XMLUnit.newXpathEngine();
        String errorMessage = xp.evaluate("//ogc:ServiceException", doc);
        // print(doc);
        assertTrue(errorMessage.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }
}
