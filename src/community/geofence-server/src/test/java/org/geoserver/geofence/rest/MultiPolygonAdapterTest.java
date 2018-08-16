/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.geofence.rest.xml.MultiPolygonAdapter;
import org.geotools.gml3.bindings.GML3MockData;
import org.geotools.gml3.v3_2.GML;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MultiPolygonAdapterTest {

    private MultiPolygonAdapter adapter = new MultiPolygonAdapter();

    @Test
    public void testMarshal() throws Exception {
        MultiPolygon geometry = GML3MockData.multiPolygon();
        Element element = adapter.marshal(geometry);
        assertEquals(
                2, element.getElementsByTagNameNS(GML.NAMESPACE, "geometryMember").getLength());
        NodeList children =
                element.getElementsByTagNameNS(GML.NAMESPACE, GML.Polygon.getLocalPart());
        assertEquals(2, children.getLength());
    }

    @Test
    public void testUnmarshal() throws Exception {
        GML3MockData.setGML(GML.getInstance());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element allowedArea =
                GML3MockData.element(new QName(null, "allowedArea"), document, document);
        Element multiGeometry =
                GML3MockData.element(GML3MockData.qName("MultiGeometry"), document, allowedArea);
        Element geometryMember =
                GML3MockData.element(GML3MockData.qName("geometryMember"), document, multiGeometry);
        GML3MockData.polygon(document, geometryMember);
        geometryMember =
                GML3MockData.element(GML3MockData.qName("geometryMember"), document, multiGeometry);
        GML3MockData.polygon(document, geometryMember);

        try {
            MultiPolygon multiPolygon = adapter.unmarshal(allowedArea);
            assertNotNull(multiPolygon);
            assertEquals(2, multiPolygon.getNumGeometries());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
