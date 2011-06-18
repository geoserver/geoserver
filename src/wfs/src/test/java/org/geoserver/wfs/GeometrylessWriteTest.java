package org.geoserver.wfs;

import org.w3c.dom.Document;

public class GeometrylessWriteTest extends WFSTestSupport {
    public void testUpdate() throws Exception {
        // perform an update
        String update = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Update typeName=\"cite:Geometryless\" > " + "<wfs:Property>"
                + "<wfs:Name>name</wfs:Name>" + "<wfs:Value>AnotherName</wfs:Value>"
                + "</wfs:Property>" + "<ogc:Filter>"
                + "<ogc:FeatureId fid=\"Geometryless.2\"/>"
                + "</ogc:Filter>"
                + "</wfs:Update>" + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", update);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);

        // do another get feature
        dom = getAsDOM("wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs&featureId=Geometryless.2");
        assertEquals("AnotherName", dom.getElementsByTagName("cite:name").item(0)
                .getFirstChild().getNodeValue());
    }
    
    public void testDelete() throws Exception {
        // perform an update
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Delete typeName=\"cite:Geometryless\" > " 
                + "<ogc:Filter>"
                + "<ogc:FeatureId fid=\"Geometryless.2\"/>"
                + "</ogc:Filter>"
                + "</wfs:Delete>" + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);

        // do another get feature
        dom = getAsDOM("wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs&featureId=Geometryless.2");
        assertEquals(0, dom.getElementsByTagName("cite:Geometryless").getLength());
    }
    
    public void testInsert() throws Exception {
        // perform an insert
        String insert = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                + "xmlns:cite=\"http://www.opengis.net/cite\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                + "<wfs:Insert > "
                + "<cite:Geometryless fid=\"Geometryless.4\">"
                + "<cite:name>Gimbo</cite:name>"
                + "<cite:number>1000</cite:number>"
                + "</cite:Geometryless>"
                + "</wfs:Insert>" + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        print(dom);
        assertTrue(dom.getElementsByTagName("wfs:SUCCESS").getLength() != 0);
        assertTrue(dom.getElementsByTagName("wfs:InsertResult").getLength() != 0);

        // do another get feature
        dom = getAsDOM("wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs");
        assertEquals(4, dom.getElementsByTagName("cite:Geometryless").getLength());
    }
}
