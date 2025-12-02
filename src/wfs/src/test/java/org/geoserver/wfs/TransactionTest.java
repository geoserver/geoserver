/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This test must be run with the server configured with the wfs 1.0 cite configuration, with data initialized.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TransactionTest extends WFSTestSupport {

    public static final String STATIC_CAUSE = "Unit test expected: Trigger is not happy with data conditions";

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.POINTS);
        revertLayer(CiteTestData.FIFTEEN);
        revertLayer(CiteTestData.LINES);
        revertLayer(CiteTestData.POLYGONS);
        revertLayer(CiteTestData.NAMED_PLACES);
        revertLayer(CiteTestData.BUILDINGS);
    }

    @Test
    public void testDelete() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Points\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cgf:Points\"> "
                + "<ogc:Filter> "
                + "<ogc:PropertyIsEqualTo> "
                + "<ogc:PropertyName>cgf:id</ogc:PropertyName> "
                + "<ogc:Literal>t0000</ogc:Literal> "
                + "</ogc:PropertyIsEqualTo> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);

        assertEquals(0, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testDoubleDelete() throws Exception {
        // see

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cdf:Fifteen\"/> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("15", "count(//gml:featureMember)", dom);

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.2\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        XMLAssert.assertXpathEvaluatesTo("13", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testInsert() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testInsertWithGetFeatureInThePath() throws Exception {
        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cgf:Lines", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());
    }

    @Test
    public void testBatchDoubleInsert() throws Exception {
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0003</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());

        dom = postAsDOM("wfs", getFeature);
        assertEquals(3, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testBatchDoubleDelete() throws Exception {
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cdf:Fifteen\"/> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("15", "count(//gml:featureMember)", dom);

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.2\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        XMLAssert.assertXpathEvaluatesTo("13", "count(//gml:featureMember)", dom);
    }

    /**
     * Verifies two deletes on different types having the same attributes are aggreated properly respecting the types.
     *
     * @throws Exception
     */
    @Test
    public void testBatchDoubleDifferentTypesDelete() throws Exception {
        String getFeatureNamedPlaces = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cite:NamedPlaces\"/> "
                + "</wfs:GetFeature>";

        String getFeatureBuildings = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cite:Buildings\"/> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeatureNamedPlaces);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);

        dom = postAsDOM("wfs", getFeatureBuildings);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);

        // perform a delete
        String delete =
                """
            <wfs:Transaction service="WFS" version="1.0.0" \
            xmlns:cite="http://www.opengis.net/cite" \
            xmlns:ogc="http://www.opengis.net/ogc" \
            xmlns:wfs="http://www.opengis.net/wfs"> \
            <wfs:Delete typeName="cite:NamedPlaces"> \
            <ogc:Filter> \
            <ogc:PropertyIsEqualTo>
                    <ogc:PropertyName>cite:NAME</ogc:PropertyName>
                    <ogc:Literal>ASHTON</ogc:Literal>
            </ogc:PropertyIsEqualTo>\
            </ogc:Filter> \
            </wfs:Delete> \
            <wfs:Delete typeName="cite:Buildings"> \
            <ogc:Filter> \
            <ogc:PropertyIsEqualTo>
                    <ogc:PropertyName>cite:ADDRESS</ogc:PropertyName>
                    <ogc:Literal>123 Main Street</ogc:Literal>
            </ogc:PropertyIsEqualTo>\
            </ogc:Filter> \
            </wfs:Delete> \
            </wfs:Transaction>\
            """;

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeatureBuildings);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);

        dom = postAsDOM("wfs", getFeatureBuildings);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testBatchDoubleDeleteWithBatchSizeOne() throws Exception {
        System.setProperty("org.geoserver.wfs.deleteBatchSize", "1");
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cdf:Fifteen\"/> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("15", "count(//gml:featureMember)", dom);

        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "<wfs:Delete typeName=\"cdf:Fifteen\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"Fifteen.2\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", delete);
        assertEquals("WFS_TransactionResponse", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        XMLAssert.assertXpathEvaluatesTo("13", "count(//gml:featureMember)", dom);
    }

    @Test
    public void testBatchInsertAndDelete() throws Exception {
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        String insertAndDelete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0003</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "<wfs:Delete typeName=\"cgf:Lines\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"new1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insertAndDelete);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        dom = postAsDOM("wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testBatchInsertDeleteAndUpdate() throws Exception {
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Lines\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        String insertAndDelete = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0003</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "<wfs:Delete typeName=\"cgf:Lines\"> "
                + "<ogc:Filter> "
                + "<ogc:FeatureId fid=\"new1\"/> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "<wfs:Update typeName=\"cgf:Lines\" > "
                + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>"
                + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>"
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>"
                + "</ogc:Filter>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insertAndDelete);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        dom = postAsDOM("wfs", getFeature);
        assertNotNull(dom.getElementsByTagName("<cgf:id>t0003</cgf:id>"));
    }

    @Test
    public void testUpdate() throws Exception {
        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Polygons\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(
                "t0002",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());

        // perform an update
        String update = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:Polygons\" > "
                + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>"
                + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>"
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>"
                + "</ogc:Filter>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";

        postAsDOM("wfs", update);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(
                "t0003",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());
    }

    @Test
    public void testUpdateLayerQualified() throws Exception {
        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"Polygons\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("cgf/Polygons/wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(
                "t0002",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());

        // perform an update
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"Polygons\" > "
                + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>"
                + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>"
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>"
                + "</ogc:Filter>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("cgf/Lines/wfs", insert);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        dom = postAsDOM("cgf/Polygons/wfs", insert);

        // do another get feature
        dom = postAsDOM("cgf/Polygons/wfs", getFeature);
        assertEquals(
                "t0003",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());
    }

    @Test
    public void testInsertWithBoundedBy() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + " xmlns:gml=\"http://www.opengis.net/gml\" "
                + " xmlns:cite=\"http://www.opengis.net/cite\">"
                + "<wfs:Insert>"
                + " <cite:BasicPolygons>"
                + "<gml:boundedBy>"
                + "<gml:Box>"
                + "<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">-2,-1 2,6</gml:coordinates>"
                + "</gml:Box>"
                + "</gml:boundedBy>"
                + "  <cite:the_geom>"
                + "<gml:MultiPolygon>"
                + "<gml:polygonMember>"
                + "<gml:Polygon>"
                + "<gml:outerBoundaryIs>"
                + "<gml:LinearRing>"
                + "<gml:coordinates cs=\",\" decimal=\".\" ts=\" \">-1,0 0,1 1,0 0,-1 -1,0</gml:coordinates>"
                + "</gml:LinearRing>"
                + "</gml:outerBoundaryIs>"
                + "</gml:Polygon>"
                + "</gml:polygonMember>"
                + "</gml:MultiPolygon>"
                + "  </cite:the_geom>"
                + "  <cite:ID>foo</cite:ID>"
                + " </cite:BasicPolygons>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getElementsByTagName("ogc:FeatureId").getLength() > 0);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() > 0);
    }

    @Test
    public void testInsertWorkspaceQualified() throws Exception {
        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"Lines\"> "
                + "<ogc:PropertyName>id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("cgf/wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        dom = postAsDOM("cgf/wfs", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());

        dom = postAsDOM("sf/wfs", insert);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        // do another get feature
        dom = postAsDOM("cgf/wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testInsertLayerQualified() throws Exception {
        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"Lines\"> "
                + "<ogc:PropertyName>id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("cgf/Lines/wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cgf:Lines>"
                + "<cgf:lineStringProperty>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</cgf:lineStringProperty>"
                + "<cgf:id>t0002</cgf:id>"
                + "</cgf:Lines>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        dom = postAsDOM("cgf/Lines/wfs", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());

        dom = postAsDOM("cgf/Polygons/wfs", insert);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        // do another get feature
        dom = postAsDOM("cgf/Lines/wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testUpdateWithDifferentPrefix() throws Exception {

        // 1. do a getFeature
        String getFeature = "<wfs:GetFeature "
                + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "> "
                + "<wfs:Query typeName=\"cgf:Polygons\"> "
                + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                + "</wfs:Query> "
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(
                "t0002",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());

        // perform an update
        String update = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update xmlns:foo=\"http://www.opengis.net/cite/geometry\" typeName=\"foo:Polygons\" > "
                + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>"
                + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>"
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>"
                + "</ogc:Filter>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", update);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(
                "t0003",
                dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());
    }

    @Test
    public void elementHandlerOrder() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = getDataStoreInfo(cat);
        DataStore store = (DataStore) ds.getDataStore(null);

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("bar");

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + " xmlns:gml=\"http://www.opengis.net/gml\" "
                + " xmlns:gs='"
                + SystemTestData.DEFAULT_URI
                + "'>"
                + "<wfs:Insert idgen='UseExisting'>"
                + " <gs:bar gml:id='1'>"
                + "    <gs:name>acme</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='2'>"
                + "    <gs:name>wiley</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='3'>"
                + "    <gs:name>bugs</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='4'>"
                + "    <gs:name>roadrunner</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='5'>"
                + "    <gs:name>daffy</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='6'>"
                + "    <gs:name>elmer</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='7'>"
                + "    <gs:name>tweety</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='8'>"
                + "    <gs:name>sylvester</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='9'>"
                + "    <gs:name>marvin</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='10'>"
                + "    <gs:name>yosemite</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='11'>"
                + "    <gs:name>porky</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='12'>"
                + "    <gs:name>speedy</gs:name>"
                + " </gs:bar>"
                + " <gs:bar gml:id='13'>"
                + "    <gs:name>taz</gs:name>"
                + " </gs:bar>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        dom = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeNames=gs:bar");

        NodeList elementsByTagName = dom.getElementsByTagName("gs:bar");
        for (int i = 1; i <= elementsByTagName.getLength(); i++) {
            String id = elementsByTagName.item(i - 1).getAttributes().item(0).getNodeValue();
            assertEquals("bar." + i, id);
        }

        dom = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&featureId=bar.5");
        XMLAssert.assertXpathEvaluatesTo("daffy", "//gs:name/text()", dom);
        dispose(cat, ds, store, ft);
    }

    @Test
    public void testBuildGeotoolsTransaction() throws IOException {
        Authentication authentication = null;
        // no authentication, defaults to "anonymous"
        testBuildGeotoolsTransaction(authentication);

        // principal being a String
        Object principal = "John Smith";
        authentication = new TestingAuthenticationToken(principal, null);
        testBuildGeotoolsTransaction(authentication);

        // principal being an org.springframework.security.core.userdetails.UserDetails
        principal = new GeoServerUser("Akira Kurosawa");
        authentication = new TestingAuthenticationToken(principal, null);
        testBuildGeotoolsTransaction(authentication);
    }

    private void testBuildGeotoolsTransaction(Authentication authentication) throws IOException {

        final String wfsReqHandle = "Request handle";
        final Map<Object, Object> extendedProperties = new HashMap<>();
        extendedProperties.put("extKey", "extValue");

        TransactionRequest request = new TransactionRequest.WFS11(null) {
            public @Override Map<?, ?> getExtendedProperties() {
                return extendedProperties;
            }

            public @Override String getHandle() {
                return wfsReqHandle;
            }
        };

        final SecurityContext ctxBackup = SecurityContextHolder.getContext();
        @SuppressWarnings("PMD.CloseResource")
        DefaultTransaction gtTransaction;
        try {
            SecurityContext tmpSecurityCtx = new SecurityContextImpl();
            tmpSecurityCtx.setAuthentication(authentication);
            SecurityContextHolder.setContext(tmpSecurityCtx);

            @SuppressWarnings("PMD.CloseResource")
            ApplicationContext context = GeoServerSystemTestSupport.applicationContext;
            Transaction transaction = new Transaction(getWFS(), getCatalog(), context);
            gtTransaction = transaction.getDatastoreTransaction(request);
        } finally {
            SecurityContextHolder.setContext(ctxBackup);
        }
        assertNotNull(gtTransaction);
        assertEquals("extValue", gtTransaction.getProperty("extKey"));
    }

    /** Tests XML entity expansion limit on parsing with system property configuration. */
    @Test
    public void testEntityExpansionLimitOnTransaction() throws Exception {
        try {
            System.getProperties().setProperty(WFSXmlUtils.ENTITY_EXPANSION_LIMIT, "1");
            Document dom = postAsDOM("wfs", xmlEntityExpansionLimitBody());
            NodeList serviceExceptionList = dom.getElementsByTagName("ServiceException");
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
        return """
            <?xml version="1.0" encoding="utf-8"?><!DOCTYPE convert [ <!ENTITY lol "lol"><!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;"> ]>
            <Transaction xmlns="http://www.opengis.net/wfs" service="WFS" xmlns:xxx="https://www.be/cbb" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:gml="http://www.opengis.net/gml" version="1.0.0" xsi:schemaLocation="">
               <Insert xmlns="http://www.opengis.net/wfs">
                <xxx_all_service_city xmlns="https://www.be/cbb">
                  <NAME xmlns="https://www.be/cbb">GENT PTEST1</NAME>
                  <DESCRIPTION xmlns="https://www.be/cbb">ptest1</DESCRIPTION>
                  <STATUS xmlns="https://www.be/cbb">default</STATUS>
                  <CREATED_BY xmlns="https://www.be/cbb">upload service</CREATED_BY>
                  <CREATED_DT xmlns="https://www.be/cbb">2019-04-04Z</CREATED_DT>
                  <EXTERNAL_ID xmlns="https://www.be/cbb">City1ptest1</EXTERNAL_ID>
                  <EXTERNAL_SOURCE xmlns="https://www.be/cbb">RIAN</EXTERNAL_SOURCE>
                  <TYPE xmlns="https://www.be/cbb">TYPE.CITY</TYPE>
                  <WAVE xmlns="https://www.be/cbb">3</WAVE>
                  <GEOM xmlns="https://www.be/cbb">
                    <gml:Polygon srsName="EPSG:31370">
                      <gml:outerBoundaryIs>
                        <gml:LinearRing>
                          <gml:coordinates cs="," ts=" ">&lol1;</gml:coordinates>
                        </gml:LinearRing>
                      </gml:outerBoundaryIs>
                    </gml:Polygon>
                  </GEOM>
                </xxx_all_service_city>
              </Insert>
            </Transaction>\
            """;
    }

    @Test
    public void testBrokenDelete() throws Exception {
        // perform a delete
        String delete = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                + "xmlns:cgf=\"cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                + "<wfs:Delete typeName=\"cgf:Points\"> "
                + "<ogc:Filter> "
                + "<ogc:PropertyIsEqualTo> "
                + "<ogc:PropertyName>id</ogc:PropertyName> "
                + "<ogc:Literal>t0000</ogc:Literal> "
                + "</ogc:PropertyIsEqualTo> "
                + "</ogc:Filter> "
                + "</wfs:Delete> "
                + "</wfs:Transaction>";

        Document resp = postAsDOM("wfs", delete);
        checkOws10Exception(resp);
        String text = resp.getElementsByTagName("ows:ExceptionText").item(0).getTextContent();
        assertTrue(text.contains("Feature type 'Points' is not available"));
        String code = ((Element) resp.getElementsByTagName("ows:Exception").item(0)).getAttribute("exceptionCode");
        assertEquals("InvalidParameterValue", code);
    }

    @Test
    public void testBrokenUpdate() throws Exception {

        // perform an update
        String update = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                + "xmlns:cgf=\"cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:Polygons\" > "
                + "<wfs:Property>"
                + "<wfs:Name>id</wfs:Name>"
                + "<wfs:Value>t0003</wfs:Value>"
                + "</wfs:Property>"
                + "<ogc:Filter>"
                + "<ogc:PropertyIsEqualTo>"
                + "<ogc:PropertyName>id</ogc:PropertyName>"
                + "<ogc:Literal>t0002</ogc:Literal>"
                + "</ogc:PropertyIsEqualTo>"
                + "</ogc:Filter>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";

        Document resp = postAsDOM("wfs", update);
        checkOws10Exception(resp);
        String text = resp.getElementsByTagName("ows:ExceptionText").item(0).getTextContent();
        assertTrue(text.contains("Feature type 'Polygons' is not available"));
        String code = ((Element) resp.getElementsByTagName("ows:Exception").item(0)).getAttribute("exceptionCode");
        assertEquals("InvalidParameterValue", code);
    }

    /**
     * This tests the situation where a Polygon-based FeatureType is updated with a LineString value. Since these are
     * different dimensions, this should be disallowed. This test is based on CITE test
     * "wfs:wfs-1.1.0-Transaction-tc10.1".
     */
    @Test
    public void testBadGeometryTypeUpdate() throws Exception {
        // perform an update to the_geom property (type is Polygon) with a LineString value
        String update = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                + "xmlns:cgf=\"cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cgf:BasicPolygons\" > "
                + "<wfs:Property>"
                + "<wfs:Name>the_geom</wfs:Name>"
                + "<wfs:Value>"
                + "<gml:LineString>"
                + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                + "</gml:coordinates>"
                + "</gml:LineString>"
                + "</wfs:Value>"
                + "</wfs:Property>"
                + "</wfs:Update>"
                + "</wfs:Transaction>";
        Document resp = postAsDOM("wfs", update);
        // response is an OWS Exception of type "InvalidValue", with locator "the_geom"
        // ie.   <ows:Exception exceptionCode="InvalidValue" locator="the_geom">
        checkOws10Exception(resp);
        Node exceptionNode = resp.getElementsByTagName("ows:Exception").item(0);
        assertEquals(
                "InvalidValue",
                exceptionNode.getAttributes().getNamedItem("exceptionCode").getTextContent());
        assertEquals(
                "the_geom",
                exceptionNode.getAttributes().getNamedItem("locator").getTextContent());
    }

    /** Assert no trigger details leaked by default */
    @Test
    public void testOptionalTransactionCauseNotInException() throws Exception {
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setVerboseExceptions(false);
        getGeoServer().save(gs);

        Catalog cat = getCatalog();
        DataStoreInfo ds = getDataStoreInfo(cat);
        DataStore store = (DataStore) ds.getDataStore(null);

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("bar");

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        try (Connection conn = ((JDBCDataStore) store).getConnection(org.geotools.api.data.Transaction.AUTO_COMMIT);
                Statement stmt = conn.createStatement()) {
            // Create the trigger
            stmt.execute("CREATE TRIGGER IF NOT EXISTS my_trigger BEFORE INSERT ON \"bar\" BEGIN SELECT RAISE(ABORT, '"
                    + STATIC_CAUSE + "'); END;");
            String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                    + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                    + " xmlns:gml=\"http://www.opengis.net/gml\" "
                    + " xmlns:gs='"
                    + SystemTestData.DEFAULT_URI
                    + "'>"
                    + "<wfs:Insert idgen='UseExisting'>"
                    + " <gs:bar gml:id='1'>"
                    + "    <gs:name>acme</gs:name>"
                    + " </gs:bar>"
                    + "</wfs:Insert>"
                    + "</wfs:Transaction>";

            assertWfs10TransactionFailureContainsText(insert, STATIC_CAUSE, false);
        } finally {
            dispose(cat, ds, store, ft);
        }
    }

    /** Assert trigger details in error message, if exception details are configured */
    @Test
    public void testOptionalTransactionCauseInException() throws Exception {
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setVerboseExceptions(true);
        getGeoServer().save(gs);

        Catalog cat = getCatalog();
        DataStoreInfo ds = getDataStoreInfo(cat);
        DataStore store = (DataStore) ds.getDataStore(null);

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("bar");

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        try (Connection conn = ((JDBCDataStore) store).getConnection(org.geotools.api.data.Transaction.AUTO_COMMIT);
                Statement stmt = conn.createStatement()) {
            // Create the trigger
            stmt.execute("CREATE TRIGGER IF NOT EXISTS my_trigger BEFORE INSERT ON \"bar\" BEGIN SELECT RAISE(ABORT, '"
                    + STATIC_CAUSE + "'); END;");

            String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                    + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                    + " xmlns:gml=\"http://www.opengis.net/gml\" "
                    + " xmlns:gs='"
                    + SystemTestData.DEFAULT_URI
                    + "'>"
                    + "<wfs:Insert idgen='UseExisting'>"
                    + " <gs:bar gml:id='1'>"
                    + "    <gs:name>acme</gs:name>"
                    + " </gs:bar>"
                    + "</wfs:Insert>"
                    + "</wfs:Transaction>";

            assertWfs10TransactionFailureContainsText(insert, STATIC_CAUSE, true);
        } finally {
            dispose(cat, ds, store, ft);
        }
    }

    private void assertWfs10TransactionFailureContainsText(String wfsTransaction, String pattern, boolean expected)
            throws Exception {
        Document resp = postAsDOM("wfs", wfsTransaction);
        assertEquals("WFS_TransactionResponse", resp.getDocumentElement().getLocalName());
        assertEquals(1, resp.getElementsByTagName("wfs:FAILED").getLength());
        assertEquals(1, resp.getElementsByTagName("wfs:Message").getLength());
        String failureMessage = resp.getElementsByTagName("wfs:Message").item(0).getTextContent();
        if (expected) {
            assertTrue(failureMessage.contains(pattern));
        } else {
            assertFalse(failureMessage.contains(pattern));
        }
    }

    private DataStoreInfo getDataStoreInfo(Catalog cat) throws IOException {
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "geopkg");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo.gpkg");
        params.put("read_only", false);
        cat.add(ds);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("bar");
        tb.add("name", String.class);
        tb.add("geom", Point.class, DefaultGeographicCRS.WGS84);

        store.createSchema(tb.buildFeatureType());

        return ds;
    }

    private void dispose(Catalog cat, DataStoreInfo ds, DataStore store, FeatureTypeInfo ft) throws IOException {
        store.removeSchema(ft.getName());
        store.dispose();
        cat.remove(ft);
        cat.remove(ds);
    }
}
