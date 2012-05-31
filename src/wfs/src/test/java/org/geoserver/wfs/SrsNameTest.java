package org.geoserver.wfs;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SrsNameTest extends WFSTestSupport {
    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save( wfs );
    }

    public void testWfs10() throws Exception {
        String q = "wfs?request=getfeature&service=wfs&version=1.0.0"
                + "&typename=cgf:Points";
        Document d = getAsDOM(q);
        assertEquals("wfs:FeatureCollection", d.getDocumentElement()
                .getNodeName());

        NodeList boxes = d.getElementsByTagName("gml:Box");
        assertFalse(boxes.getLength() == 0);
        for (int i = 0; i < boxes.getLength(); i++) {
            Element box = (Element) boxes.item(i);
            assertEquals("http://www.opengis.net/gml/srs/epsg.xml#32615", box
                    .getAttribute("srsName"));
        }

        NodeList points = d.getElementsByTagName("gml:Point");
        assertFalse(points.getLength() == 0);
        for (int i = 0; i < points.getLength(); i++) {
            Element point = (Element) points.item(i);
            assertEquals("http://www.opengis.net/gml/srs/epsg.xml#32615", point
                    .getAttribute("srsName"));
        }

    }

    public void testWfs11() throws Exception {
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save( wfs );
        
        try {
            String q = "wfs?request=getfeature&service=wfs&version=1.1.0"
                    + "&typename=cgf:Points";
            Document d = getAsDOM(q);
            assertEquals("wfs:FeatureCollection", d.getDocumentElement()
                    .getNodeName());
    
            NodeList boxes = d.getElementsByTagName("gml:Envelope");
            assertFalse(boxes.getLength() == 0);
            for (int i = 0; i < boxes.getLength(); i++) {
                Element box = (Element) boxes.item(i);
                assertEquals("urn:x-ogc:def:crs:EPSG:32615", box
                        .getAttribute("srsName"));
            }
    
            NodeList points = d.getElementsByTagName("gml:Point");
            assertFalse(points.getLength() == 0);
            for (int i = 0; i < points.getLength(); i++) {
                Element point = (Element) points.item(i);
                assertEquals("urn:x-ogc:def:crs:EPSG:32615", point
                        .getAttribute("srsName"));
            }
        }
        finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save( wfs );
        }
    }

    public void testSrsNameSyntax11() throws Exception {
        doTestSrsNameSyntax11(SrsNameStyle.URN, false);
        doTestSrsNameSyntax11(SrsNameStyle.URN2, true);
        doTestSrsNameSyntax11(SrsNameStyle.URL, true);
        doTestSrsNameSyntax11(SrsNameStyle.NORMAL, true);
        doTestSrsNameSyntax11(SrsNameStyle.XML, true);
    }

    void doTestSrsNameSyntax11(SrsNameStyle srsNameStyle, boolean doSave) throws Exception {
        if (doSave) {
            WFSInfo wfs = getWFS();
            GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
            gml.setSrsNameStyle(srsNameStyle);
            getGeoServer().save(wfs);
        }

        String q = "wfs?request=getfeature&service=wfs&version=1.1.0&typename=cgf:Points";
        Document d = getAsDOM(q);
        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        
        XMLAssert.assertXpathExists("//gml:Envelope[@srsName = '"+srsNameStyle.getPrefix()+"32615']", d);
        XMLAssert.assertXpathExists("//gml:Point[@srsName = '"+srsNameStyle.getPrefix()+"32615']", d);
    }
}
