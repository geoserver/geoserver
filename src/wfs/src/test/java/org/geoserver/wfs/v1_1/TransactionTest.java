/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml3.GML;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TransactionTest extends WFSTestSupport {

    public static final QName WITH_GML =
            new QName(SystemTestData.SF_URI, "WithGMLProperties", SystemTestData.SF_PREFIX);

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.ROAD_SEGMENTS);
        getTestData().addVectorLayer(WITH_GML, Collections.EMPTY_MAP, getClass(), getCatalog());
    }

    @Test
    public void testInsert1() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\">"
                        + "<wfs:Insert handle=\"insert-1\">"
                        + " <sf:PrimitiveGeoFeature gml:id=\"cite.gmlsf0-f01\">"
                        + "  <gml:description>"
                        + "Fusce tellus ante, tempus nonummy, ornare sed, accumsan nec, leo."
                        + "Vivamus pulvinar molestie nisl."
                        + "</gml:description>"
                        + "<gml:name>Aliquam condimentum felis sit amet est.</gml:name>"
                        // + "<gml:name
                        // codeSpace=\"http://cite.opengeospatial.org/gmlsf\">cite.gmlsf0-f01</gml:name>"
                        + "<sf:curveProperty>"
                        + "  <gml:LineString gml:id=\"cite.gmlsf0-g01\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "   <gml:posList>47.608284 19.034142 51.286873 16.7836 49.849854 15.764992</gml:posList>"
                        + " </gml:LineString>"
                        + "</sf:curveProperty>"
                        + "<sf:intProperty>1025</sf:intProperty>"
                        + "<sf:measurand>7.405E2</sf:measurand>"
                        + "<sf:dateTimeProperty>2006-06-23T12:43:12+01:00</sf:dateTimeProperty>"
                        + "<sf:decimalProperty>90.62</sf:decimalProperty>"
                        + "</sf:PrimitiveGeoFeature>"
                        + "</wfs:Insert>"
                        + "<wfs:Insert handle=\"insert-2\">"
                        + "<sf:AggregateGeoFeature gml:id=\"cite.gmlsf0-f02\">"
                        + " <gml:description>"
                        + "Duis nulla nisi, molestie vel, rhoncus a, ullamcorper eu, justo. Sed bibendum."
                        + " Ut sem. Mauris nec nunc a eros aliquet pharetra. Mauris nonummy, pede et"
                        + " tincidunt ultrices, mauris lectus fermentum massa, in ullamcorper lectus"
                        + "felis vitae metus. Sed imperdiet sollicitudin dolor."
                        + " </gml:description>"
                        + " <gml:name codeSpace=\"http://cite.opengeospatial.org/gmlsf\">cite.gmlsf0-f02</gml:name>"
                        + " <gml:name>Quisqué viverra</gml:name>"
                        + " <gml:boundedBy>"
                        + "   <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "     <gml:lowerCorner>36.1 8.0</gml:lowerCorner>"
                        + "    <gml:upperCorner>52.0 21.1</gml:upperCorner>"
                        + "   </gml:Envelope>"
                        + "  </gml:boundedBy>"
                        + "   <sf:multiPointProperty>"
                        + "<gml:MultiPoint srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "<gml:pointMember>"
                        + " <gml:Point><gml:pos>49.325176 21.036873</gml:pos></gml:Point>"
                        + "</gml:pointMember>"
                        + "<gml:pointMember>"
                        + "  <gml:Point><gml:pos>36.142586 13.56189</gml:pos></gml:Point>"
                        + "</gml:pointMember>"
                        + "<gml:pointMember>"
                        + "  <gml:Point><gml:pos>51.920937 8.014193</gml:pos></gml:Point>"
                        + "</gml:pointMember>"
                        + "</gml:MultiPoint>"
                        + "</sf:multiPointProperty>"
                        + "<sf:doubleProperty>2012.78</sf:doubleProperty>"
                        + "  <sf:intRangeProperty>43</sf:intRangeProperty>"
                        + " <sf:strProperty>"
                        + "Donec ligulä pede, sodales iń, vehicula eu, sodales et, lêo."
                        + "</sf:strProperty>"
                        + "<sf:featureCode>AK121</sf:featureCode>"
                        + "</sf:AggregateGeoFeature>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getElementsByTagName("ogc:FeatureId").getLength() > 0);
    }

    @Test
    public void testInsertWithNoSRS() throws Exception {
        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Points\"> "
                        + "<wfs:PropertyName>cite:id</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert > "
                        + "<cgf:Points>"
                        + "<cgf:pointProperty>"
                        + "<gml:Point>"
                        + "<gml:pos>20 40</gml:pos>"
                        + "</gml:Point>"
                        + "</cgf:pointProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:Points>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);
        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());
        String fid = getFirstElementByTagName(dom, "ogc:FeatureId").getAttribute("fid");

        // check insertion occurred
        dom = postAsDOM("wfs", getFeature);
        assertEquals(n + 1, dom.getElementsByTagName("cgf:Points").getLength());

        // check coordinate order is preserved
        getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Points\"> "
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>cgf:id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter></wfs:Query> "
                        + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);
        //        print(dom);
        assertEquals(
                "20 40", getFirstElementByTagName(dom, "gml:pos").getFirstChild().getNodeValue());
    }

    @Test
    public void testInsertWithSRS() throws Exception {

        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Points\"/> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        //        print(dom);
        int n = dom.getElementsByTagName("cgf:Points").getLength();

        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert srsName=\"EPSG:32615\"> "
                        + "<cgf:Points>"
                        + "<cgf:pointProperty>"
                        + "<gml:Point>"
                        + "<gml:pos>1 1</gml:pos>"
                        + "</gml:Point>"
                        + "</cgf:pointProperty>"
                        + "<cgf:id>t0003</cgf:id>"
                        + "</cgf:Points>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);

        NodeList numberInserteds = dom.getElementsByTagName("wfs:totalInserted");
        Element numberInserted = (Element) numberInserteds.item(0);

        assertNotNull(numberInserted);
        assertEquals("1", numberInserted.getFirstChild().getNodeValue());

        // do another get feature
        getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Points\"> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", getFeature);
        //        print(dom);

        NodeList pointsList = dom.getElementsByTagName("cgf:Points");
        assertEquals(n + 1, pointsList.getLength());
    }

    @Test
    public void testInsertWithGMLProperties() throws Exception {

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "<wfs:Insert>"
                        + "<sf:WithGMLProperties>"
                        + "<gml:location>"
                        + "<gml:Point>"
                        + "<gml:coordinates>2,2</gml:coordinates>"
                        + "</gml:Point>"
                        + "</gml:location>"
                        + "<gml:name>two</gml:name>"
                        + "<sf:foo>2</sf:foo>"
                        + "</sf:WithGMLProperties>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        Element inserted = getFirstElementByTagName(dom, "wfs:totalInserted");
        assertEquals("1", inserted.getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");
        NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals(2, features.getLength());

        Element feature = (Element) features.item(1);
        assertEquals(
                "two",
                getFirstElementByTagName(feature, "gml:name").getFirstChild().getNodeValue());
        assertEquals(
                "2", getFirstElementByTagName(feature, "sf:foo").getFirstChild().getNodeValue());

        Element location = getFirstElementByTagName(feature, "gml:location");
        Element pos = getFirstElementByTagName(location, "gml:pos");

        assertEquals("2 2", pos.getFirstChild().getNodeValue());

        xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "<wfs:Insert>"
                        + "<sf:WithGMLProperties>"
                        + "<sf:location>"
                        + "<gml:Point>"
                        + "<gml:coordinates>3,3</gml:coordinates>"
                        + "</gml:Point>"
                        + "</sf:location>"
                        + "<sf:name>three</sf:name>"
                        + "<sf:foo>3</sf:foo>"
                        + "</sf:WithGMLProperties>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");

        features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals(3, features.getLength());

        feature = (Element) features.item(2);
        assertEquals(
                "three",
                getFirstElementByTagName(feature, "gml:name").getFirstChild().getNodeValue());
        assertEquals(
                "3", getFirstElementByTagName(feature, "sf:foo").getFirstChild().getNodeValue());

        location = getFirstElementByTagName(feature, "gml:location");
        pos = getFirstElementByTagName(location, "gml:pos");

        assertEquals("3 3", pos.getFirstChild().getNodeValue());
    }

    @Test
    public void testUpdateWithGMLProperties() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"sf:WithGMLProperties\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>gml:name</wfs:Name>"
                        + "     <wfs:Value>two</wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>gml:location</wfs:Name>"
                        + "     <wfs:Value>"
                        + "        <gml:Point>"
                        + "          <gml:coordinates>2,2</gml:coordinates>"
                        + "        </gml:Point>"
                        + "     </wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>sf:foo</wfs:Name>"
                        + "     <wfs:Value>2</wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>foo</ogc:PropertyName>"
                        + "       <ogc:Literal>1</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals("1", updated.getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");
        //        print( dom );
        NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals(1, features.getLength());

        Element feature = (Element) features.item(0);
        assertEquals(
                "two",
                getFirstElementByTagName(feature, "gml:name").getFirstChild().getNodeValue());
        assertEquals(
                "2", getFirstElementByTagName(feature, "sf:foo").getFirstChild().getNodeValue());

        Element location = getFirstElementByTagName(feature, "gml:location");
        Element pos = getFirstElementByTagName(location, "gml:pos");

        assertEquals("2 2", pos.getFirstChild().getNodeValue());

        xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"sf:WithGMLProperties\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>sf:name</wfs:Name>"
                        + "     <wfs:Value>trhee</wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>sf:location</wfs:Name>"
                        + "     <wfs:Value>"
                        + "        <gml:Point>"
                        + "          <gml:coordinates>3,3</gml:coordinates>"
                        + "        </gml:Point>"
                        + "     </wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>sf:foo</wfs:Name>"
                        + "     <wfs:Value>3</wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>foo</ogc:PropertyName>"
                        + "       <ogc:Literal>2</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals("1", updated.getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");

        features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals(1, features.getLength());

        feature = (Element) features.item(0);
        assertEquals(
                "trhee",
                getFirstElementByTagName(feature, "gml:name").getFirstChild().getNodeValue());
        assertEquals(
                "3", getFirstElementByTagName(feature, "sf:foo").getFirstChild().getNodeValue());

        location = getFirstElementByTagName(feature, "gml:location");
        pos = getFirstElementByTagName(location, "gml:pos");

        assertEquals("3 3", pos.getFirstChild().getNodeValue());
    }

    @Test
    public void testInsertWithBoundedBy() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:cite=\"http://www.opengis.net/cite\">"
                        + "<wfs:Insert>"
                        + " <cite:BasicPolygons>"
                        + " <gml:boundedBy>"
                        + "  <gml:Envelope>"
                        + "<gml:lowerCorner>-1.0 2.0</gml:lowerCorner>"
                        + "<gml:upperCorner>2.0 5.0</gml:upperCorner>"
                        + "  </gml:Envelope>"
                        + " </gml:boundedBy>"
                        + "  <cite:the_geom>"
                        + "    <gml:MultiPolygon>"
                        + "      <gml:polygonMember>"
                        + "         <gml:Polygon>"
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>-1.0 5.0 2.0 5.0 2.0 2.0 -1.0 2.0 -1.0 5.0</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
                        + "         </gml:Polygon>"
                        + "      </gml:polygonMember>"
                        + "    </gml:MultiPolygon>"
                        + "  </cite:the_geom>"
                        + "  <cite:ID>foo</cite:ID>"
                        + " </cite:BasicPolygons>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        print(dom);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
        assertTrue(dom.getElementsByTagName("ogc:FeatureId").getLength() > 0);
    }

    @Test
    public void testInsert2() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:cite=\"http://www.opengis.net/cite\">"
                        + "<wfs:Insert>"
                        + " <cite:RoadSegments>"
                        + "  <cite:the_geom>"
                        + "<gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\""
                        + "    srsName=\"EPSG:4326\">"
                        + " <gml:lineStringMember>"
                        + "                  <gml:LineString>"
                        + "                   <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                        + "                 </gml:LineString>"
                        + "               </gml:lineStringMember>"
                        + "             </gml:MultiLineString>"
                        + "  </cite:the_geom>"
                        + "  <cite:FID>foo</cite:FID>"
                        + "  <cite:NAME>bar</cite:NAME>"
                        + " </cite:RoadSegments>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());

        dom =
                getAsDOM(
                        "wfs?version=1.1.0&request=getfeature&typename=cite:RoadSegments&srsName=EPSG:4326&"
                                + "cql_filter=FID%3D'foo'");
        //        print(dom);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(1, dom.getElementsByTagName("cite:RoadSegments").getLength());

        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments");
        Element posList = getFirstElementByTagName(roadSegment, "gml:posList");
        String[] pos = posList.getFirstChild().getTextContent().split(" ");
        assertEquals(4, pos.length);
        assertEquals(4.2582, Double.parseDouble(pos[0]), 1E-4);
        assertEquals(52.0643, Double.parseDouble(pos[1]), 1E-4);
        assertEquals(4.2584, Double.parseDouble(pos[2]), 1E-4);
        assertEquals(52.0648, Double.parseDouble(pos[3]), 1E-4);
    }

    @Test
    public void testUpdateForcedSRS() throws Exception {
        testUpdate("srsName=\"EPSG:4326\"", this::updateSrsOnGeometry);
        testUpdate("srsName=\"EPSG:4326\"", this::updateSrsOnRoot);
    }

    @Test
    public void testUpdateForcedUrnSRS() throws Exception {
        testUpdate("srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"", this::updateSrsOnGeometry);
        testUpdate("srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"", this::updateSrsOnRoot);
    }

    @Test
    public void testUpdateNoSRS() throws Exception {
        testUpdate("", this::updateSrsOnGeometry);
    }

    private void testUpdate(String srs, Function<String, String> updateStatementBuilder)
            throws Exception {
        String xml = updateStatementBuilder.apply(srs);

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());

        String srsBlock = "".equals(srs) ? "" : "&" + srs.replaceAll("\"", "");
        dom =
                getAsDOM(
                        "wfs?version=1.1.0&request=getfeature&typename=cite:RoadSegments"
                                + srsBlock
                                + "&"
                                + "cql_filter=FID%3D'102'");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(1, dom.getElementsByTagName("cite:RoadSegments").getLength());

        Element roadSegment = getFirstElementByTagName(dom, "cite:RoadSegments");
        Element posList = getFirstElementByTagName(roadSegment, "gml:posList");
        String[] pos = posList.getFirstChild().getTextContent().split(" ");
        assertEquals(4, pos.length);
        assertEquals(4.2582, Double.parseDouble(pos[0]), 1E-4);
        assertEquals(52.0643, Double.parseDouble(pos[1]), 1E-4);
        assertEquals(4.2584, Double.parseDouble(pos[2]), 1E-4);
        assertEquals(52.0648, Double.parseDouble(pos[3]), 1E-4);
    }

    private String updateSrsOnGeometry(String srs) {
        return "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                + " xmlns:cite=\"http://www.opengis.net/cite\""
                + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                + " xmlns:gml=\"http://www.opengis.net/gml\""
                + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                + " <wfs:Update typeName=\"cite:RoadSegments\">"
                + "   <wfs:Property>"
                + "     <wfs:Name>cite:the_geom</wfs:Name>"
                + "     <wfs:Value>"
                + "      <gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\" "
                + srs
                + ">"
                + "       <gml:lineStringMember>"
                + "         <gml:LineString>"
                + "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                + "         </gml:LineString>"
                + "       </gml:lineStringMember>"
                + "      </gml:MultiLineString>"
                + "     </wfs:Value>"
                + "   </wfs:Property>"
                + "   <ogc:Filter>"
                + "     <ogc:PropertyIsEqualTo>"
                + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                + "       <ogc:Literal>102</ogc:Literal>"
                + "     </ogc:PropertyIsEqualTo>"
                + "   </ogc:Filter>"
                + " </wfs:Update>"
                + "</wfs:Transaction>";
    }

    private String updateSrsOnRoot(String srs) {
        return "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                + " xmlns:cite=\"http://www.opengis.net/cite\""
                + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                + " xmlns:gml=\"http://www.opengis.net/gml\""
                + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                + " <wfs:Update typeName=\"cite:RoadSegments\" "
                + srs
                + ">"
                + "   <wfs:Property>"
                + "     <wfs:Name>cite:the_geom</wfs:Name>"
                + "     <wfs:Value>"
                + "      <gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\">"
                + "       <gml:lineStringMember>"
                + "         <gml:LineString>"
                + "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                + "         </gml:LineString>"
                + "       </gml:lineStringMember>"
                + "      </gml:MultiLineString>"
                + "     </wfs:Value>"
                + "   </wfs:Property>"
                + "   <ogc:Filter>"
                + "     <ogc:PropertyIsEqualTo>"
                + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                + "       <ogc:Literal>102</ogc:Literal>"
                + "     </ogc:PropertyIsEqualTo>"
                + "   </ogc:Filter>"
                + " </wfs:Update>"
                + "</wfs:Transaction>";
    }

    @Test
    public void testUpdateWithInvalidProperty() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:cite=\"http://www.opengis.net/cite\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"cite:RoadSegments\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>INVALID</wfs:Name>"
                        + "     <wfs:Value>INVALID</wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                        + "       <ogc:Literal>102</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testInsertLayerQualified() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:cite=\"http://www.opengis.net/cite\">"
                        + "<wfs:Insert>"
                        + " <cite:RoadSegments>"
                        + "  <cite:the_geom>"
                        + "<gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\""
                        + "    srsName=\"EPSG:4326\">"
                        + " <gml:lineStringMember>"
                        + "                  <gml:LineString>"
                        + "                   <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                        + "                 </gml:LineString>"
                        + "               </gml:lineStringMember>"
                        + "             </gml:MultiLineString>"
                        + "  </cite:the_geom>"
                        + "  <cite:FID>foo</cite:FID>"
                        + "  <cite:NAME>bar</cite:NAME>"
                        + " </cite:RoadSegments>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("cite/Forests/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);

        dom = postAsDOM("cite/RoadSegments/wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
    }

    @Test
    public void testInsertLocalNamespaces() throws Exception {
        String xml =
                "<Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns=\"http://www.opengis.net/wfs\" >"
                        + "<Insert>"
                        + " <RoadSegments xmlns=\"http://www.opengis.net/cite\">"
                        + "  <the_geom>"
                        + "<MultiLineString xmlns=\"http://www.opengis.net/gml\""
                        + "    srsName=\"EPSG:4326\">"
                        + " <lineStringMember>"
                        + "                  <LineString>"
                        + "                   <posList>4.2582 52.0643 4.2584 52.0648</posList>"
                        + "                 </LineString>"
                        + "               </lineStringMember>"
                        + "             </MultiLineString>"
                        + "  </the_geom>"
                        + "  <FID>foo</FID>"
                        + "  <NAME>bar</NAME>"
                        + " </RoadSegments>"
                        + "</Insert>"
                        + "</Transaction>";

        Document dom = postAsDOM("cite/Forests/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);

        dom = postAsDOM("cite/RoadSegments/wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild().getNodeValue());
    }

    @Test
    public void testUpdateLayerQualified() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:cite=\"http://www.opengis.net/cite\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"RoadSegments\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>cite:the_geom</wfs:Name>"
                        + "     <wfs:Value>"
                        + "      <gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "       <gml:lineStringMember>"
                        + "         <gml:LineString>"
                        + "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                        + "         </gml:LineString>"
                        + "       </gml:lineStringMember>"
                        + "      </gml:MultiLineString>"
                        + "     </wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                        + "       <ogc:Literal>102</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("cite/Forests/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);

        dom = postAsDOM("cite/RoadSegments/wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(
                "1",
                getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
    }

    @Test
    public void testUpdateWithDifferentPrefix() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update xmlns:foo=\"http://www.opengis.net/cite\" typeName=\"foo:RoadSegments\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>foo:the_geom</wfs:Name>"
                        + "     <wfs:Value>"
                        + "     </wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                        + "       <ogc:Literal>102</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        Element updated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals("1", updated.getFirstChild().getNodeValue());
    }

    @Test
    public void testInsertUseExistingId() throws Exception {
        // create a store that can actually handle user specified ids
        // TODO: factor this out into base class or something
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath());
        cat.add(ds);

        FeatureSource fs1 = getFeatureSource(SystemTestData.FIFTEEN);
        FeatureSource fs2 = getFeatureSource(SystemTestData.SEVEN);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("bar");
        tb.add("name", String.class);
        tb.add("geom", Point.class);

        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("bar");
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(fs.getSchema());
        b.add("one");
        b.add(new WKTReader().read("POINT(1 1)"));

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(b.buildFeature(null));
        fs.addFeatures(fc);

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:gs='"
                        + SystemTestData.DEFAULT_URI
                        + "'>"
                        + "<wfs:Insert idgen='UseExisting'>"
                        + " <gs:bar gml:id='bar.1234'>"
                        + "    <gs:name>acme</gs:name>"
                        + " </gs:bar>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//ogc:FeatureId[@fid = 'bar.1234']", dom);
        dom = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&featureId=bar.1234");
        XMLAssert.assertXpathExists("//gs:bar[@gml:id = 'bar.1234']", dom);
    }

    @Test
    public void testEmptyUpdate() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:cite=\"http://www.opengis.net/cite\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"cite:RoadSegments\">"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(
                "0",
                getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
    }

    @Test
    public void elementHandlerOrder() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo1");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath());
        cat.add(ds);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("baz");
        tb.add("name", String.class);
        tb.add("geom", Point.class);

        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("baz");

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:gs='"
                        + SystemTestData.DEFAULT_URI
                        + "'>"
                        + "<wfs:Insert idgen='UseExisting'>"
                        + " <gs:baz gml:id='1'>"
                        + "    <gs:name>acme</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='2'>"
                        + "    <gs:name>wiley</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='3'>"
                        + "    <gs:name>bugs</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='4'>"
                        + "    <gs:name>roadrunner</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='5'>"
                        + "    <gs:name>daffy</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='6'>"
                        + "    <gs:name>elmer</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='7'>"
                        + "    <gs:name>tweety</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='8'>"
                        + "    <gs:name>sylvester</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='9'>"
                        + "    <gs:name>marvin</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='10'>"
                        + "    <gs:name>yosemite</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='11'>"
                        + "    <gs:name>porky</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='12'>"
                        + "    <gs:name>speedy</gs:name>"
                        + " </gs:baz>"
                        + " <gs:baz gml:id='13'>"
                        + "    <gs:name>taz</gs:name>"
                        + " </gs:baz>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        dom = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&typeNames=gs:baz");

        NodeList elementsByTagName = dom.getElementsByTagName("gml:id");
        for (int i = 0; i < elementsByTagName.getLength(); i++) {
            String id = elementsByTagName.item(i).getAttributes().item(0).getNodeValue();
            assertEquals("baz" + i, id);
        }

        dom = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&featureId=baz.5");
        XMLAssert.assertXpathEvaluatesTo("daffy", "//gml:name/text()", dom);
    }

    @Test
    public void testInsertUnknownFeatureType() throws Exception {
        // perform an insert on an invalid feature type
        String insert =
                "<wfs:Transaction service='WFS' version='1.1.0' "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' "
                        + "xmlns:gml='"
                        + GML.NAMESPACE
                        + "'> "
                        + "<wfs:Insert > "
                        + "<cgf:FooBar>"
                        + "<cgf:pointProperty>"
                        + "<gml:Point>"
                        + "<gml:pos>20 40</gml:pos>"
                        + "</gml:Point>"
                        + "</cgf:pointProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:FooBar>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        MockHttpServletResponse response = postAsServletResponse("wfs", insert);
        assertEquals(200, response.getStatus());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        checkOws10Exception(dom, "InvalidParameterValue");
    }

    /** Tests XML entity expansion limit on parsing with system property configuration. */
    @Test
    public void testEntityExpansionLimitOnTransaction() throws Exception {
        try {
            System.getProperties().setProperty(WFSXmlUtils.ENTITY_EXPANSION_LIMIT, "1");
            Document dom = postAsDOM("wfs", xmlEntityExpansionLimitBody());
            NodeList serviceExceptionList = dom.getElementsByTagName("ows:ExceptionText");
            assertEquals(1, serviceExceptionList.getLength());
            Node serviceException = serviceExceptionList.item(0);
            // the service exception should contain the JAXP00010001 error code, that means entity
            // expansion limit is working.
            assertTrue(serviceException.getTextContent().contains("JAXP00010001"));
        } finally {
            System.getProperties().remove(WFSXmlUtils.ENTITY_EXPANSION_LIMIT);
        }
    }

    private String xmlEntityExpansionLimitBody() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE convert [ <!ENTITY lol \"lol\"><!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;\"> ]>\n"
                + "<Transaction xmlns=\"http://www.opengis.net/wfs\" service=\"WFS\" xmlns:xxx=\"https://www.be/cbb\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:gml=\"http://www.opengis.net/gml\" version=\"1.1.0\" xsi:schemaLocation=\"\">\n"
                + "   <Insert xmlns=\"http://www.opengis.net/wfs\">\n"
                + "    <xxx_all_service_city xmlns=\"https://www.be/cbb\">\n"
                + "      <NAME xmlns=\"https://www.be/cbb\">GENT PTEST1</NAME>\n"
                + "      <DESCRIPTION xmlns=\"https://www.be/cbb\">ptest1</DESCRIPTION>\n"
                + "      <STATUS xmlns=\"https://www.be/cbb\">default</STATUS>\n"
                + "      <CREATED_BY xmlns=\"https://www.be/cbb\">upload service</CREATED_BY>\n"
                + "      <CREATED_DT xmlns=\"https://www.be/cbb\">2019-04-04Z</CREATED_DT>\n"
                + "      <EXTERNAL_ID xmlns=\"https://www.be/cbb\">City1ptest1</EXTERNAL_ID>\n"
                + "      <EXTERNAL_SOURCE xmlns=\"https://www.be/cbb\">RIAN</EXTERNAL_SOURCE>\n"
                + "      <TYPE xmlns=\"https://www.be/cbb\">TYPE.CITY</TYPE>\n"
                + "      <WAVE xmlns=\"https://www.be/cbb\">3</WAVE>\n"
                + "      <GEOM xmlns=\"https://www.be/cbb\">\n"
                + "        <gml:Polygon srsName=\"EPSG:31370\">\n"
                + "          <gml:outerBoundaryIs>\n"
                + "            <gml:LinearRing>\n"
                + "              <gml:coordinates cs=\",\" ts=\" \">&lol1;</gml:coordinates>\n"
                + "            </gml:LinearRing>\n"
                + "          </gml:outerBoundaryIs>\n"
                + "        </gml:Polygon>\n"
                + "      </GEOM>\n"
                + "    </xxx_all_service_city>\n"
                + "  </Insert>\n"
                + "</Transaction>";
    }
}
