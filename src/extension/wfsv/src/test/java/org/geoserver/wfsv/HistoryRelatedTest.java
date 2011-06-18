package org.geoserver.wfsv;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import junit.framework.Test;

import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;

public class HistoryRelatedTest extends WFSVTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new HistoryRelatedTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        if (getTestData().isTestDataAvailable()) {
            // build some history the other tests will use
            String transaction = //
            "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"\r\n"
                    + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                    + "  xmlns:gml=\"http://www.opengis.net/gml\"\r\n"
                    + "  handle=\"Inserting, updating and deleting\">\r\n"
                    + "  <wfs:Insert>\r\n"
                    + "    <topp:archsites>\r\n"
                    + "      <topp:cat>2</topp:cat>\r\n"
                    + "      <topp:str1>Alien crash site</topp:str1>\r\n"
                    + "      <topp:the_geom>\r\n"
                    + "        <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#26713\">\r\n"
                    + "          <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">604000,4930000</gml:coordinates>\r\n"
                    + "        </gml:Point>\r\n" //
                    + "      </topp:the_geom>\r\n"
                    + "    </topp:archsites>\r\n" // 
                    + "  </wfs:Insert>\r\n"
                    + "  <wfs:Update typeName=\"topp:archsites\">\r\n" // 
                    + "    <wfs:Property>\r\n" + "      <wfs:Name>str1</wfs:Name>\r\n"
                    + "      <wfs:Value>Signature Rock, updated</wfs:Value>\r\n"
                    + "    </wfs:Property>\r\n" // 
                    + "    <ogc:Filter>\r\n" // 
                    + "      <ogc:FeatureId fid=\"archsites.1\" />\r\n" // 
                    + "    </ogc:Filter>\r\n" // 
                    + "  </wfs:Update>\r\n" // 
                    + "  <wfs:Delete typeName=\"topp:archsites\">\r\n" // 
                    + "    <ogc:Filter>\r\n" // 
                    + "      <ogc:FeatureId fid=\"archsites.2\" />\r\n"// 
                    + "    </ogc:Filter>\r\n" // 
                    + "  </wfs:Delete>\r\n" + "</wfs:Transaction>\r\n";
            Document doc = postAsDOM(root(), transaction);

            // let's just ensure the transaction was successful
            assertXpathEvaluatesTo("1", "count(/wfs:WFS_TransactionResponse)", doc);
            assertXpathEvaluatesTo("archsites.5",
                    "/wfs:WFS_TransactionResponse/wfs:InsertResult/ogc:FeatureId/@fid", doc);
            assertXpathEvaluatesTo(
                    "1",
                    "count(/wfs:WFS_TransactionResponse/wfs:TransactionResult/wfs:Status/wfs:SUCCESS)",
                    doc);
        }
    }

    public void testGetFeatureBeforeAfter() throws Exception {
        // ask the old state, make sure the updates do not appear
        String before = "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\"\r\n"
                + "  outputFormat=\"GML2\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfs\r\n"
                + "                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">\r\n"
                + "  <wfs:Query typeName=\"topp:archsites\" featureVersion=\"1\"/>\r\n"
                + "</wfs:GetFeature>\r\n";
        Document doc = postAsDOM(root(), before);
        //print(doc);
        assertXpathEvaluatesTo("4", "count(/wfs:FeatureCollection/gml:featureMember)", doc);
        assertXpathEvaluatesTo("Signature Rock",
                "//topp:archsites[@fid=\"archsites.1\"]/topp:str1", doc);
        assertXpathEvaluatesTo("1", "count(//topp:archsites[@fid=\"archsites.2\"])", doc);
        assertXpathEvaluatesTo("0", "count(//topp:archsites[@fid=\"archsites.5\"])", doc);

        // ask the current state, make sure the updates do show
        String current = "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\"\r\n"
                + "  outputFormat=\"GML2\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfs\r\n"
                + "                      http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd\">\r\n"
                + "  <wfs:Query typeName=\"topp:archsites\"/>\r\n" + "</wfs:GetFeature>\r\n";
        doc = postAsDOM(root(), current);
        assertXpathEvaluatesTo("4", "count(/wfs:FeatureCollection/gml:featureMember)", doc);
        assertXpathEvaluatesTo("Signature Rock, updated",
                "//topp:archsites[@fid=\"archsites.1\"]/topp:str1", doc);
        assertXpathEvaluatesTo("0", "count(//topp:archsites[@fid=\"archsites.2\"])", doc);
        assertXpathEvaluatesTo("1", "count(//topp:archsites[@fid=\"archsites.5\"])", doc);
    }

    public void testVersionedFeatureCollection10() throws Exception {
        String request = "<wfsv:GetVersionedFeature service=\"WFSV\" version=\"1.0.0\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioninig.xsd\">\r\n"
                + "  <wfs:Query typeName=\"topp:archsites\">\r\n" + "    <ogc:Filter>\r\n"
                + "       <ogc:FeatureId fid=\"archsites.5\"/>\r\n" + "    </ogc:Filter>\r\n"
                + "  </wfs:Query>\r\n" + "</wfsv:GetVersionedFeature>";
        Document doc = postAsDOM(root(), request);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection/gml:featureMember)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:archsites[@fid=\"archsites.5\"])", doc);
        assertXpathEvaluatesTo("1", "count(//topp:createdBy)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:creationDate)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:creationMessage)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateVersion)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdatedBy)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateDate)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateMessage)", doc);
        assertXpathEvaluatesTo("anonymous",
                "//topp:archsites[@fid=\"archsites.5\"]/topp:createdBy", doc);
        assertXpathEvaluatesTo("Inserting, updating and deleting",
                "//topp:archsites[@fid=\"archsites.5\"]/topp:lastUpdateMessage", doc);
    }
    
    public void testValidateInvalidRequest() throws Exception {
        String request = "<wfsv:GetVersionedFeature service=\"WFSV\" version=\"1.0.0\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioninig.xsd\">\r\n"
                + "  <wfs:Query unknownAttribute=\"topp:archsites\">"
                + "  </wfs:Query>\r\n" + "</wfsv:GetVersionedFeature>";

        Document dom = postAsDOM(root(true), request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    
    public void testVersionedFeatureCollection10Reproject() throws Exception {
        // prepare the expected transformation results
        MathTransform tx = CRS.findMathTransform(CRS.decode("EPSG:26713"), CRS.decode("EPSG:900913"));
        double[] original = new double[] {604000,4930000};
        double[] expected = new double[2];
        tx.transform(original, 0, expected, 0, 1);
        
        // gather the results and extract the coordinates using xpath
        Document doc = getAsDOM(root() + "service=wfsv&version=1.0.0&request=GetVersionedFeature&srsName=EPSG:900913&typeName=topp:archsites&featureId=archsites.5");
        String transformed = xpath.evaluate("//topp:archsites[@fid=\"archsites.5\"]/topp:the_geom/gml:Point/gml:coordinates/text()", doc);
        
        // parse the coordinate list and check
        String[] strCoords = transformed.split(",");
        double[] actual = new double[] {Double.parseDouble(strCoords[0]), Double.parseDouble(strCoords[1])};
        assertEquals(expected[0], actual[0], 0.000001);
        assertEquals(expected[1], actual[1], 0.000001);
    }
    
    public void testVersionedFeatureCollection11() throws Exception {
        String request = "<wfsv:GetVersionedFeature service=\"WFSV\" version=\"1.1.0\"\r\n"
                + "  outputFormat=\"GML3\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\">\r\n"
                + "  <wfs:Query typeName=\"topp:archsites\">\r\n" + "    <ogc:Filter>\r\n"
                + "       <ogc:FeatureId fid=\"archsites.5\"/>\r\n" + "    </ogc:Filter>\r\n"
                + "  </wfs:Query>\r\n" + "</wfsv:GetVersionedFeature>";
        Document doc = postAsDOM(root(), request);
        //print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection/gml:featureMembers/topp:archsites)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:archsites[@gml:id=\"archsites.5\"])", doc);
        assertXpathEvaluatesTo("1", "count(//topp:createdBy)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:creationDate)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:creationMessage)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateVersion)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdatedBy)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateDate)", doc);
        assertXpathEvaluatesTo("1", "count(//topp:lastUpdateMessage)", doc);
        assertXpathEvaluatesTo("anonymous",
                "//topp:archsites[@gml:id=\"archsites.5\"]/topp:createdBy", doc);
        assertXpathEvaluatesTo("Inserting, updating and deleting",
                "//topp:archsites[@gml:id=\"archsites.5\"]/topp:lastUpdateMessage", doc);
    }
    
    public void testVersionedFeatureCollection11Reproject() throws Exception {
        // prepare the expected transformation results
        MathTransform tx = CRS.findMathTransform(CRS.decode("EPSG:26713"), CRS.decode("EPSG:900913"));
        double[] original = new double[] {604000,4930000};
        double[] expected = new double[2];
        tx.transform(original, 0, expected, 0, 1);
        
        // gather the results and extract the coordinates using xpath
        Document doc = getAsDOM(root() + "service=WFSV&version=1.1.0&request=GetVersionedFeature&srsName=EPSG:900913&typeName=topp:archsites&featureId=archsites.5");
        print(doc);
        String transformed = xpath.evaluate("//topp:archsites[@gml:id=\"archsites.5\"]/topp:the_geom/gml:Point/gml:pos/text()", doc);
        
        // parse the coordinate list and check
        String[] strCoords = transformed.split(" ");
        double[] actual = new double[] {Double.parseDouble(strCoords[0]), Double.parseDouble(strCoords[1])};
        assertEquals(expected[0], actual[0], 0.000001);
        assertEquals(expected[1], actual[1], 0.000001);
    }

    public void testLog10() throws Exception {
        String request = "<wfsv:GetLog service=\"WFSV\" version=\"1.0.0\" outputFormat=\"GML2\" \r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"0\" toFeatureVersion=\"100\"/>\r\n"
                + "</wfsv:GetLog>";
        Document doc = postAsDOM(root(), request);
        //print(doc);
        assertXpathEvaluatesTo("1", "count(//topp:changesets)", doc);
        // version 2 and 3 are taken to version enable roads and restricted
        assertXpathEvaluatesTo("Inserting, updating and deleting",
                "//topp:changesets[@fid=\"changesets.4\"]/topp:message", doc);
        assertXpathEvaluatesTo("anonymous", "//topp:changesets[@fid=\"changesets.4\"]/topp:author",
                doc);
    }
    
    public void testLog11() throws Exception {
        String request = "<wfsv:GetLog service=\"WFSV\" version=\"1.1.0\" outputFormat=\"GML3\" \r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n>\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"0\" toFeatureVersion=\"100\"/>\r\n"
                + "</wfsv:GetLog>";
        Document doc = postAsDOM(root(), request);
        print(doc);
        assertXpathEvaluatesTo("1", "count(//topp:changesets)", doc);
        // version 2 and 3 are taken to version enable roads and restricted
        assertXpathEvaluatesTo("Inserting, updating and deleting",
                "//topp:changesets[@gml:id=\"changesets.4\"]/topp:message", doc);
        assertXpathEvaluatesTo("anonymous", "//topp:changesets[@gml:id=\"changesets.4\"]/topp:author",
                doc);
    }

    public void testLog10Html() throws Exception {
        String request = "<wfsv:GetLog service=\"WFSV\" version=\"1.0.0\"\r\n"
                + "  outputFormat=\"HTML\""
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "                      http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"0\" toFeatureVersion=\"100\"/>\r\n"
                + "</wfsv:GetLog>";
        Document doc = postAsDOM(root(), request);
        // test it's html and there are 1 history rows in the table (tr owning a
        // td, not tr owning a th)
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr[td])", doc);
    }

    public void testGetDiff11() throws Exception {
        String request = "<wfsv:GetDiff service=\"WFSV\" version=\"1.1.0\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "  http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"1\"/>\r\n"
                + "</wfsv:GetDiff>";
        Document doc = postAsDOM(root(), request);
        //print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Insert)", doc);
        assertXpathEvaluatesTo("archsites.5", "/wfs:Transaction/wfs:Insert/topp:archsites/@gml:id",
                doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update)", doc);
        assertXpathEvaluatesTo("archsites.1",
                "/wfs:Transaction/wfs:Update/ogc:Filter/ogc:FeatureId/@fid", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update/wfs:Property)", doc);
        assertXpathEvaluatesTo("Signature Rock, updated",
                "/wfs:Transaction/wfs:Update/wfs:Property/wfs:Value", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Delete)", doc);
        assertXpathEvaluatesTo("archsites.2",
                "/wfs:Transaction/wfs:Delete/ogc:Filter/ogc:FeatureId/@fid", doc);
    }
    
    public void testGetDiff10() throws Exception {
        String request = "<wfsv:GetDiff service=\"WFSV\" version=\"1.0.0\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"1\"/>\r\n"
                + "</wfsv:GetDiff>";
        Document doc = postAsDOM(root(), request);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Insert)", doc);
        assertXpathEvaluatesTo("archsites.5", "/wfs:Transaction/wfs:Insert/topp:archsites/@gml:id",
                doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update)", doc);
        assertXpathEvaluatesTo("archsites.1",
                "/wfs:Transaction/wfs:Update/ogc:Filter/ogc:FeatureId/@fid", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update/wfs:Property)", doc);
        assertXpathEvaluatesTo("Signature Rock, updated",
                "/wfs:Transaction/wfs:Update/wfs:Property/wfs:Value", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Delete)", doc);
        assertXpathEvaluatesTo("archsites.2",
                "/wfs:Transaction/wfs:Delete/ogc:Filter/ogc:FeatureId/@fid", doc);
    }

    public void testGetDiff11Html() throws Exception {
        String request = "<wfsv:GetDiff service=\"WFSV\" version=\"1.1.0\"\r\n"
                + "outputFormat=\"HTML\""
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "  http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-versioning.xsd\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"1\"/>\r\n"
                + "</wfsv:GetDiff>";
        // just make sure html is valid xml as well
        postAsDOM(root(), request);
    }

    public void testGetDiff11Reverse() throws Exception {
        String request = "<wfsv:GetDiff service=\"WFSV\" version=\"1.1.0\"\r\n"
                + "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\r\n"
                + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n"
                + "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n"
                + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "  xsi:schemaLocation=\"http://www.opengis.net/wfsv\r\n"
                + "                      http://localhost:8080/geoserver/schemas/wfs/1.1.0/wfsv.xsd\">\r\n"
                + "  <wfsv:DifferenceQuery typeName=\"topp:archsites\" fromFeatureVersion=\"CURRENT\"  "
                + "toFeatureVersion=\"1\"/>\r\n" + "</wfsv:GetDiff>";
        Document doc = postAsDOM(root(), request);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Insert)", doc);
        assertXpathEvaluatesTo("archsites.2", "/wfs:Transaction/wfs:Insert/topp:archsites/@gml:id",
                doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update)", doc);
        assertXpathEvaluatesTo("archsites.1",
                "/wfs:Transaction/wfs:Update/ogc:Filter/ogc:FeatureId/@fid", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Update/wfs:Property)", doc);
        assertXpathEvaluatesTo("Signature Rock",
                "/wfs:Transaction/wfs:Update/wfs:Property/wfs:Value", doc);
        assertXpathEvaluatesTo("1", "count(/wfs:Transaction/wfs:Delete)", doc);
        assertXpathEvaluatesTo("archsites.5",
                "/wfs:Transaction/wfs:Delete/ogc:Filter/ogc:FeatureId/@fid", doc);
    }
}
