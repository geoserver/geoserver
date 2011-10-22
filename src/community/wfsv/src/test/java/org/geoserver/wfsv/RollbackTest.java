package org.geoserver.wfsv;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RollbackTest extends WFSVTestSupport {
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        if (getTestData().isTestDataAvailable()) {
            // perform a transaction
            String transaction = //
            "<wfs:Transaction service=\"WFSV\" version=\"1.1.0\"\r\n" + 
            "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
            "  xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
            "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n" + 
            "  handle=\"Updating Signature rock label\">\r\n" + 
            "  <wfs:Insert>\r\n" + 
            "    <topp:archsites>\r\n" + 
            "      <topp:cat>2</topp:cat>\r\n" + 
            "      <topp:str1>Alien crash site</topp:str1>\r\n" + 
            "      <topp:the_geom>\r\n" + 
            "        <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#26713\">\r\n" + 
            "          <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">604000,4930000</gml:coordinates>\r\n" + 
            "        </gml:Point>\r\n" + 
            "      </topp:the_geom>\r\n" + 
            "    </topp:archsites>\r\n" + 
            "  </wfs:Insert>\r\n" + 
            "  <wfs:Update typeName=\"topp:archsites\">\r\n" + 
            "    <wfs:Property>\r\n" + 
            "      <wfs:Name>str1</wfs:Name>\r\n" + 
            "      <wfs:Value>Signature Rock, updated</wfs:Value>\r\n" + 
            "    </wfs:Property>\r\n" + 
            "    <ogc:Filter>\r\n" + 
            "      <ogc:FeatureId fid=\"archsites.1\" />\r\n" + 
            "    </ogc:Filter>\r\n" + 
            "  </wfs:Update>\r\n" + 
            "  <wfs:Delete typeName=\"topp:archsites\">\r\n" + 
            "    <ogc:Filter>\r\n" + 
            "      <ogc:FeatureId fid=\"archsites.2\" />\r\n" + 
            "    </ogc:Filter>\r\n" + 
            "  </wfs:Delete>\r\n" + 
            "</wfs:Transaction>\r\n" + 
            "";
            Document doc = postAsDOM(root(), transaction);
            // print(doc);
    
            // let's just ensure the transaction was successful
            assertXpathEvaluatesTo("1", "count(/wfs:TransactionResponse)", doc);
            assertXpathEvaluatesTo("archsites.5",
                    "/wfs:TransactionResponse/wfs:InsertResults/wfs:Feature/ogc:FeatureId/@fid", doc);
            assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalInserted", doc);
            assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalUpdated", doc);
            assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalDeleted", doc);
            
            // ask the current state, make sure the updates do show
            String current = "<wfs:GetFeature service=\"WFSV\" version=\"1.1.0\"\r\n"
                    + "  outputFormat=\"GML2\"\r\n"
                    + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n"
                    + "  <wfs:Query typeName=\"topp:archsites\"/>\r\n" + "</wfs:GetFeature>\r\n";
            
            doc = postAsDOM(root(), current);
            assertXpathEvaluatesTo("4", "count(/wfs:FeatureCollection/gml:featureMember)", doc);
            assertXpathEvaluatesTo("Signature Rock, updated",
                    "//topp:archsites[@fid=\"archsites.1\"]/topp:str1", doc);
            assertXpathEvaluatesTo("0", "count(//topp:archsites[@fid=\"archsites.2\"])", doc);
            assertXpathEvaluatesTo("1", "count(//topp:archsites[@fid=\"archsites.5\"])", doc);
        }
    }

    public void testRollbackWfs11() throws Exception {
        // perform the rollback
        String rollback = "<wfs:Transaction service=\"WFSV\" version=\"1.1.0\"\r\n" + 
        		"  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
        		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n" + 
        		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
        		"  xmlns:gml=\"http://www.opengis.net/gml\"\r\n" + 
        		"  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n" + 
        		"  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
        		"  handle=\"Rolling back previous changes\">\r\n" + 
        		"  <wfsv:Rollback safeToIgnore=\"false\" vendorId=\"TOPP\" typeName=\"topp:archsites\" toFeatureVersion=\"1\"/>\r\n" + 
        		"</wfs:Transaction>\r\n";
        Document doc = postAsDOM(root(), rollback);
//        print(doc);
        
        // let's ensure the rollback was successful
        assertXpathEvaluatesTo("1", "count(/wfs:TransactionResponse)", doc);
        assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalInserted", doc);
        assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalUpdated", doc);
        assertXpathEvaluatesTo("1", "/wfs:TransactionResponse/wfs:TransactionSummary/wfs:totalDeleted", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:TransactionResponse)", doc);
        assertXpathEvaluatesTo("archsites.2",
                "/wfs:TransactionResponse/wfs:InsertResults/wfs:Feature/ogc:FeatureId/@fid", doc);
    }
    
    public void testRollbackWfs10() throws Exception {
        // perform the rollback
        String rollback = "<wfs:Transaction service=\"WFSV\" version=\"1.0.0\"\r\n" + 
                "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
                "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n" + 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
                "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n" + 
                "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n" + 
                "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
                "  handle=\"Rolling back previous changes\">\r\n" + 
                "  <wfsv:Rollback safeToIgnore=\"false\" vendorId=\"TOPP\" typeName=\"topp:archsites\" toFeatureVersion=\"1\"/>\r\n" + 
                "</wfs:Transaction>\r\n";
        Document doc = postAsDOM(root(), rollback);
        // print(doc);
        
        // let's ensure the rollback was successful
        assertXpathEvaluatesTo("1", "count(/wfs:WFS_TransactionResponse)", doc);
        assertXpathEvaluatesTo("1", "count(//wfs:WFS_TransactionResponse/wfs:TransactionResult/wfs:Status/wfs:SUCCESS)", doc);
        assertXpathEvaluatesTo("archsites.2",
                "/wfs:WFS_TransactionResponse/wfs:InsertResult/ogc:FeatureId/@fid", doc);
    }
    
    public void testInvalidRollback() throws Exception {
        // perform the rollback, same as above, but no 
        String rollback = "<wfs:Transaction service=\"WFSV\" version=\"1.0.0\"\r\n" + 
                "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
                "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n" + 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
                "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n" + 
                "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n" + 
                "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
                "  handle=\"Rolling back previous changes\">\r\n" + 
                "  <wfsv:Rollback vendorId=\"TOPP\" typeName=\"topp:archsites\" toFeatureVersion=\"1\"/>\r\n" + 
                "</wfs:Transaction>\r\n";
        
        // make sure you get a decent service exception even without the schema validation
        Document doc = postAsDOM(root(false), rollback);
        // print(doc);
        final Element root = doc.getDocumentElement();
        assertEquals("ServiceExceptionReport", root.getNodeName());
        String message = root.getElementsByTagName("ServiceException").item(0).getTextContent();
        assertTrue(message.contains("safeToIgnore"));
        
    }


    
}
