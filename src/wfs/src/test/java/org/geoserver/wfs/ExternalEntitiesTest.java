/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.AllowListEntityResolver;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wfs.kvp.Filter_1_1_0_KvpParser;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExternalEntitiesTest extends WFSTestSupport {

    private static final String FILTER =
            """
            <Filter xmlns="http://www.opengis.net/ogc">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_OGC_NAMESPACE = "<ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\" "
            + "xmlns:gml=\"http://www.opengis.net/gml\">"
            + "<ogc:Intersects><ogc:PropertyName>the_geom</ogc:PropertyName>"
            + "<gml:Polygon><gml:exterior><gml:LinearRing>"
            + "<gml:posList>-112 46 -109 46 -109 47 -112 47 -112 46</gml:posList>"
            + "</gml:LinearRing></gml:exterior></gml:Polygon></ogc:Intersects></ogc:Filter>";

    private static final String FILTER_OGC_SCHEMA_LOCATION =
            """
            <Filter xmlns="http://www.opengis.net/ogc"
                  xsi:schemaLocation="http://www.opengis.net/ogc http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_RESTRICTED_SCHEMA_SCHEMA_LOCATION =
            """
            <Filter xmlns="http://invalid/schema"
                  xsi:schemaLocation="http://invalid/schema http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

    private static final String FILTER_RESTRICTED_NAMESPACE =
            """
            <Filter xmlns="http://invalid/schema"
                  xsi:schemaLocation="http://invalid/schema http://schemas.opengis.net/filter/1.1.0/filter.xsd">
              <FeatureId fid="states.1"/>
            </Filter>""";

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

    private static final String WFS_2_0_0_REQUEST =
            """
            <?xml version="1.0" ?>
            <!DOCTYPE wfs:GetFeature [
            <!ELEMENT wfs:GetFeature (wfs:Query*)>
            <!ATTLIST wfs:GetFeature
                            service   CDATA #FIXED "WFS"
                            version   CDATA #FIXED "2.0.0"
                            outputFormat CDATA #FIXED "application/gml+xml; version=3.2"
                    xmlns:wfs CDATA #FIXED "http://www.opengis.net/wfs"
                            xmlns:ogc CDATA #FIXED "http://www.opengis.net/ogc"
                            xmlns:fes CDATA #FIXED "http://www.opengis.net/fes/2.0">
            <!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>
            <!ATTLIST wfs:Query typeName CDATA #FIXED "cdf:Fifteen">
            <!ELEMENT wfs:PropertyName (#PCDATA) >
            <!ELEMENT ogc:Filter (fes:ResourceId*)>
            <!ELEMENT fes:ResourceId EMPTY>
            <!ATTLIST fes:ResourceId rid CDATA #FIXED "states.3">

            <!ENTITY passwd  SYSTEM "FILE:///thisfiledoesnotexist?.XSD">
            ]>
            <wfs:GetFeature service="WFS" version="2.0.0" outputFormat="application/gml+xml; version=3.2"
                    xmlns:wfs="http://www.opengis.net/wfs/2.0"
                    xmlns:fes="http://www.opengis.net/fes/2.0">
                    <wfs:Query typeName="cdf:Fifteen">
                            <wfs:PropertyName>&passwd;</wfs:PropertyName>
                            <fes:Filter>
                                    <fes:ResourceId rid="states.3"/>
                            </fes:Filter>
                    </wfs:Query>
            </wfs:GetFeature>""";

    @After
    public void clearEntityResolutionUnrestrictedProperty() {
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
    }

    @Test
    public void testAllowListFilter() throws Exception {
        try {
            EntityResolverProvider.setEntityResolver(
                    new AllowListEntityResolver(getGeoServer(), "http://localhost:8080/"));

            Filter_1_1_0_KvpParser kvpParser = new Filter_1_1_0_KvpParser(getGeoServer());

            List filters = (List) kvpParser.parse(FILTER);
            assertEquals("parsed id filter", 1, filters.size());
            Id id = (Id) filters.get(0);
            assertTrue("parsed id filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_OGC_NAMESPACE);
            assertEquals("parsed intsersect filter", 1, filters.size());
            Intersects intersect = (Intersects) filters.get(0);
            assertEquals(
                    "parsed intsersect filter",
                    "the_geom",
                    ((PropertyName) intersect.getExpression1()).getPropertyName());

            filters = (List) kvpParser.parse(FILTER_OGC_SCHEMA_LOCATION);
            assertEquals("parsed ogc filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed ogc filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_RESTRICTED_SCHEMA_SCHEMA_LOCATION);
            assertEquals("parsed restricted filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed restricted filter", id.getIDs().contains("states.1"));

            filters = (List) kvpParser.parse(FILTER_RESTRICTED_NAMESPACE);
            assertEquals("parsed restricted namespace filter", 1, filters.size());
            id = (Id) filters.get(0);
            assertTrue("parsed restricted namespace filter", id.getIDs().contains("states.1"));

            final String LOCALHOST =
                    "<dmt xmlns=\"http://a.b/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://a.b/ http://localhost:8080/dmt.xsd\">dmt</dmt>";
            filters = (List) kvpParser.parse(LOCALHOST);
            assertTrue(filters.isEmpty());

            EntityResolverProvider.setEntityResolver(new AllowListEntityResolver(getGeoServer()));
            filters = (List) kvpParser.parse(LOCALHOST);
            assertTrue(filters.isEmpty());
        } finally {
            EntityResolverProvider.setEntityResolver(GeoServerSystemTestSupport.RESOLVE_DISABLED_PROVIDER_DEVMODE);
        }
    }

    @Test
    public void testWfs1_0() throws Exception {
        // enable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        String output = string(post("wfs", WFS_1_0_0_REQUEST));
        // the server tried to read a file on local file system
        assertTrue(
                "FileNotFoundException",
                output.indexOf("xml request is most probably not compliant to GetFeature element") > -1);

        // disable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
        output = string(post("wfs", WFS_1_0_0_REQUEST));
        assertTrue("disallowed", output.indexOf("Entity resolution disallowed") > -1);

        // set default (entity parsing disabled);
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        output = string(post("wfs", WFS_1_0_0_REQUEST));
        assertTrue("disallowed", output.indexOf("Entity resolution disallowed") > -1);
    }

    @Test
    public void testWfs1_1() throws Exception {
        // enable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        String output = string(post("wfs", WFS_1_1_0_REQUEST));
        // the server tried to read a file on local file system
        assertTrue(
                "FileNotFoundException",
                output.indexOf("xml request is most probably not compliant to GetFeature element") > -1);

        // disable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
        output = string(post("wfs", WFS_1_1_0_REQUEST));
        assertTrue("disallowed", output.indexOf("Entity resolution disallowed") > -1);

        // set default (entity parsing disabled);
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        output = string(post("wfs", WFS_1_1_0_REQUEST));
        assertTrue("disallowed", output.indexOf("Entity resolution disallowed") > -1);
    }

    @Test
    public void testWfs2_0() throws Exception {
        // enable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        String output = string(post("wfs", WFS_2_0_0_REQUEST));
        // the server tried to read a file on local file system
        assertTrue(output.indexOf("xml request is most probably not compliant to GetFeature element") > -1);

        // disable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
        output = string(post("wfs", WFS_2_0_0_REQUEST));
        assertTrue(output.indexOf("Request parsing failed") > -1);
        assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));

        // set default (entity parsing disabled);
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        output = string(post("wfs", WFS_2_0_0_REQUEST));
        assertTrue(output.indexOf("Request parsing failed") > -1);
        assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
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
        Document doc = getAsDOM(request);
        XpathEngine xp = XMLUnit.newXpathEngine();
        String errorMessage = xp.evaluate("//ogc:ServiceException", doc);
        // print(doc);
        assertTrue(errorMessage.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }
}
