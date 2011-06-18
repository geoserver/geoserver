package org.geoserver.wfsv;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class LogOrderTest extends WFSVTestSupport {
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new LogOrderTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        if (getTestData().isTestDataAvailable()) {
            // build some history the other tests will use
            String transaction = //
            "<wfs:Transaction service=\"WFSV\" version=\"1.0.0\"\r\n"
                    + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n"
                    + "  handle=\"Inserting new data\">\r\n"
                    + "  <wfs:Insert>\r\n"
                    + "    <topp:archsites>\r\n"
                    + "      <topp:cat>2</topp:cat>\r\n"
                    + "      <topp:str1>Alien crash site</topp:str1>\r\n"
                    + "      <topp:the_geom>\r\n"
                    + "        <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#26713\">\r\n"
                    + "          <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">604000,4930000</gml:coordinates>\r\n"
                    + "        </gml:Point>\r\n" //
                    + "      </topp:the_geom>\r\n" + "    </topp:archsites>\r\n" // 
                    + "  </wfs:Insert>\r\n" + "</wfs:Transaction>\r\n";
            // let's just ensure the transaction was successful
            Document doc = postAsDOM(root(), transaction);
            assertXpathEvaluatesTo("1", "count(/wfs:WFS_TransactionResponse)", doc);
            assertXpathEvaluatesTo("archsites.5",
                    "/wfs:WFS_TransactionResponse/wfs:InsertResult/ogc:FeatureId/@fid", doc);
            assertXpathEvaluatesTo(
                    "1",
                    "count(/wfs:WFS_TransactionResponse/wfs:TransactionResult/wfs:Status/wfs:SUCCESS)",
                    doc);

            transaction = //
            "<wfs:Transaction service=\"WFSV\" version=\"1.0.0\"\r\n"
                    + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n"
                    + "  handle=\"Updating data\">\r\n"
                    + "  <wfs:Update typeName=\"topp:archsites\">\r\n" // 
                    + "    <wfs:Property>\r\n" + "      <wfs:Name>str1</wfs:Name>\r\n"
                    + "      <wfs:Value>Signature Rock, updated</wfs:Value>\r\n"
                    + "    </wfs:Property>\r\n" // 
                    + "    <ogc:Filter>\r\n" // 
                    + "      <ogc:FeatureId fid=\"archsites.1\" />\r\n" // 
                    + "    </ogc:Filter>\r\n" // 
                    + "  </wfs:Update>\r\n" // 
                    + "</wfs:Transaction>\r\n";
            doc = postAsDOM(root(), transaction);
            assertXpathEvaluatesTo("1", "count(/wfs:WFS_TransactionResponse)", doc);
            assertXpathEvaluatesTo(
                    "1",
                    "count(/wfs:WFS_TransactionResponse/wfs:TransactionResult/wfs:Status/wfs:SUCCESS)",
                    doc);
        }
    }

    public void testLogForward() throws Exception {
        String request = "<wfsv:GetLog service=\"WFSV\" version=\"1.0.0\" outputFormat=\"GML2\" \r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"0\" toFeatureVersion=\"100\"/>\r\n"
                + "</wfsv:GetLog>";
        Document doc = postAsDOM(root(), request);
        //print(doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodes = xpath.getMatchingNodes("//wfs:FeatureCollection/gml:featureMember/topp:changesets/@fid", doc);
        assertEquals(2, nodes.getLength());
        assertEquals("changesets.5", nodes.item(0).getTextContent());
        assertEquals("changesets.4", nodes.item(1).getTextContent());
    }
    
    public void testLogBackwards() throws Exception {
        String request = "<wfsv:GetLog service=\"WFSV\" version=\"1.0.0\" outputFormat=\"GML2\" \r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"100\" toFeatureVersion=\"0\"/>\r\n"
                + "</wfsv:GetLog>";
        Document doc = postAsDOM(root(), request);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodes = xpath.getMatchingNodes("//wfs:FeatureCollection/gml:featureMember/topp:changesets/@fid", doc);
        assertEquals(2, nodes.getLength());
        assertEquals("changesets.4", nodes.item(0).getTextContent());
        assertEquals("changesets.5", nodes.item(1).getTextContent());
    }
}
