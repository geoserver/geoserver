/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GeometrylessTest extends WFSTestSupport {

    @Test
    public void testGetFeature10() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        //        print(doc);

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
        NodeList features = doc.getElementsByTagName("cite:Geometryless");
        assertEquals(3, featureMembers.getLength());
    }

    @Test
    public void testGetFeatureReproject10() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs&srsName=EPSG:900913");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        //        print(doc);

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
        NodeList features = doc.getElementsByTagName("cite:Geometryless");
        assertEquals(3, featureMembers.getLength());
    }

    @Test
    public void testGetFeature11() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.1.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        //        print(doc);

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMembers");
        assertFalse(featureMembers.getLength() == 0);
        NodeList features = doc.getElementsByTagName("cite:Geometryless");
        assertEquals(3, features.getLength());
    }

    @Test
    public void testGetFeatureReproject11() throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.1.0&service=wfs&srsName=EPSG:900913");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMembers");
        assertFalse(featureMembers.getLength() == 0);
        NodeList features = doc.getElementsByTagName("cite:Geometryless");
        assertEquals(3, features.getLength());
    }

    @Test
    public void testGetFeatureReprojectPost() throws Exception {
        String request =
                "<wfs:GetFeature service=\"WFS\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "version=\"1.0.0\"  outputFormat=\"GML2\" "
                        + "xmlns:topp=\"http://www.openplans.org/topp\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://www.opengis.net/wfs "
                        + "http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">"
                        + "<wfs:Query typeName=\"cite:Geometryless\" srsName=\"EPSG:900913\"/></wfs:GetFeature>";
        // System.out.println(request);
        Document doc = postAsDOM("wfs", request);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(3, featureMembers.getLength());
        NodeList features = doc.getElementsByTagName("cite:Geometryless");
        assertEquals(3, features.getLength());
    }
}
