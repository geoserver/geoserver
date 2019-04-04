/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class RetypingTransactionTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        Catalog catalog = getCatalog();

        FeatureTypeInfo featureType1 =
                catalog.getFeatureTypeByName(SystemTestData.POLYGONS.getLocalPart());
        featureType1.setName("MyPolygons");
        getCatalog().save(featureType1);

        FeatureTypeInfo featureType2 =
                catalog.getFeatureTypeByName(SystemTestData.LINES.getLocalPart());
        featureType2.setName("MyLines");
        getCatalog().save(featureType2);

        FeatureTypeInfo featureType3 =
                catalog.getFeatureTypeByName(SystemTestData.POINTS.getLocalPart());
        featureType3.setName("MyPoints");
        getCatalog().save(featureType3);
    }

    @Before
    public void revert() throws Exception {
        revertLayer(SystemTestData.POLYGONS);
        revertLayer(SystemTestData.LINES);
        revertLayer(SystemTestData.POINTS);
    }

    @Test
    public void testInsert() throws Exception {

        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:MyLines\"> "
                        + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert > "
                        + "<cgf:MyLines>"
                        + "<cgf:lineStringProperty>"
                        + "<gml:LineString>"
                        + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                        + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                        + "</gml:coordinates>"
                        + "</gml:LineString>"
                        + "</cgf:lineStringProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:MyLines>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", insert);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
        assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(2, dom.getElementsByTagName("gml:featureMember").getLength());
    }

    @Test
    public void testUpdate() throws Exception {
        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:MyPolygons\"> "
                        + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(
                "t0002", dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());

        // perform an update
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update typeName=\"cgf:MyPolygons\" > "
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

        dom = postAsDOM("wfs", insert);

        // do another get feature
        dom = postAsDOM("wfs", getFeature);
        assertEquals(
                "t0003", dom.getElementsByTagName("cgf:id").item(0).getFirstChild().getNodeValue());
    }

    @Test
    public void testDelete() throws Exception {

        // 1. do a getFeature
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:MyPoints\"> "
                        + "<ogc:PropertyName>cite:id</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", getFeature);
        assertEquals(1, dom.getElementsByTagName("gml:featureMember").getLength());

        // perform a delete
        String delete =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                        + "<wfs:Delete typeName=\"cgf:MyPoints\"> "
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
}
