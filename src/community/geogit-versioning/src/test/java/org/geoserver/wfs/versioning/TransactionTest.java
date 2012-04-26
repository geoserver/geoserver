package org.geoserver.wfs.versioning;

import static org.geotools.feature.type.DateUtil.serializeDateTime;

import java.util.Date;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;

public class TransactionTest extends WFS20VersioningTestSupport {

    public void testInsert() throws Exception {
        Document dom = getAsDOM("wfs?request=GetFeature&version=2.0.0&typeName="+"cite:Buildings");

        XMLAssert.assertXpathEvaluatesTo("1", "count(//"+"cite:Buildings"+")", dom);

        //do an insert
        String xml = "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" "
                + " xmlns:wfs='" + WFS.NAMESPACE + "' "
                + " xmlns:gml='" + GML.NAMESPACE + "' "
                + " xmlns:cite=\"http://www.opengis.net/cite\">"
                + "<wfs:Insert>"
                + " <cite:Buildings>"
                + "  <cite:the_geom>" + 
                "<gml:MultiSurface> " + 
                " <gml:surfaceMember> " + 
                "  <gml:Polygon> " + 
                "   <gml:exterior> " + 
                "    <gml:LinearRing> " + 
                "     <gml:posList>-123.9 40.0 -124.0 39.9 -124.1 40.0 -124.0 40.1 -123.9 40.0</gml:posList>" + 
                "    </gml:LinearRing> " + 
                "   </gml:exterior> " + 
                "  </gml:Polygon> " + 
                " </gml:surfaceMember> " + 
                "</gml:MultiSurface> " 
                + "  </cite:the_geom>"
                + "  <cite:FID>115</cite:FID>"
                + "  <cite:ADDRESS>987 Foo St</cite:ADDRESS>" 
                + " </cite:Buildings>"
                + "</wfs:Insert>"
                + "</wfs:Transaction>";

        dom = postAsDOM( "wfs", xml );
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + "wfs:totalInserted" + ")", dom);
        XMLAssert.assertXpathEvaluatesTo("1",
                "count(wfs:TransactionResponse/wfs:InsertResults/wfs:Feature/fes:ResourceId)", dom);
        
        final String insertedRid = xpath.evaluate("wfs:TransactionResponse/wfs:InsertResults/wfs:Feature/fes:ResourceId/@rid", dom);
        assertTrue(insertedRid, insertedRid.indexOf('@') > 0);
        
        //get the feature id
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings&srsName=EPSG:4326&" +
                "cql_filter=FID%3D'115'");
        print(dom);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + "cite:Buildings" + ")", dom);

        String id = getFirstElementByTagName(dom, "cite:Buildings").getAttribute("gml:id");
        assertEquals(insertedRid, id);
        String fid = id.split("@")[0];
        String ver = id.split("@")[1];

        //do a regular query with both rid = fid@version
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + " xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + id + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertInsertFeature(dom, "cite:Buildings");
        
        //do a query with version
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + " xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + id + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertInsertFeature(dom, "cite:Buildings");
        
        //do a query with just regular fid 
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + " xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + fid + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertInsertFeature(dom, "cite:Buildings");
        
        // do a query with a bum version, should throw exception
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + " xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + fid + "' version='foobar'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals( "ows:ExceptionReport", dom.getDocumentElement().getNodeName() );

        //do  a query with endDate before first commit... should return nothing
        final String endDate = serializeDateTime(commit1.getTimestamp() - 5000);
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
            + " xmlns:fes='" + FES.NAMESPACE + "' "
            + "xmlns:cite='http://www.opengis.net/cite' "
            + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
            + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
            + "<fes:Filter>"
              + "<fes:ResourceId rid = '" + fid + "' endDate='" + endDate+"'/>"
            + "</fes:Filter>"
            + "</wfs:Query> "
            + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + "cite:Buildings" + ")", dom);
    }

    void assertInsertFeature(Document dom, String typeName) throws XpathException {
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + ")", dom);
        XMLAssert.assertXpathExists("//" + typeName + "/cite:FID[text()='115']", dom);
    }

    public void testUpdate() throws Exception {
        Document dom = getAsDOM("wfs?version=2.0.0&request=getfeature&typename=cite:Buildings"
                + "&cql_filter=FID%3D'114'");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        
        //get the fid and version
        String id = getFirstElementByTagName(dom, "cite:Buildings").getAttribute("gml:id");
        String fid = id.split("@")[0];
        String ver1 = id.split("@")[1];

        //do an update
        String xml = 
        "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
        "xmlns:cite=\"http://www.opengis.net/cite\" " +
        "xmlns:fes='" + FES.NAMESPACE + "' " + 
        "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
        "xmlns:gml='" + GML.NAMESPACE + "'>" + 
        " <wfs:Update typeName=\"cite:Buildings\">" +
        "   <wfs:Property>" +
        "     <wfs:ValueReference>ADDRESS</wfs:ValueReference>" +
        "     <wfs:Value>148 Lafayette Street</wfs:Value>" +
        "   </wfs:Property>" + 
        "   <fes:Filter>" +
        "     <fes:PropertyIsEqualTo>" +
        "       <fes:ValueReference>FID</fes:ValueReference>" + 
        "       <fes:Literal>114</fes:Literal>" + 
        "     </fes:PropertyIsEqualTo>" + 
        "   </fes:Filter>" +
        " </wfs:Update>" +
       "</wfs:Transaction>"; 
        
        dom = postAsDOM( "wfs", xml );
        print(dom);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
        
        //ensure the updated fid is the new one
        XMLAssert.assertXpathNotExists("//wfs:UpdateResults/wfs:Feature/fes:ResourceId[@rid='"+id+"']", dom);

        //get the reported updated version
        String updatedRid = xpath.evaluate("//wfs:UpdateResults/wfs:Feature/fes:ResourceId/@rid", dom);
        
        //get the updated version
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings" +
                "&cql_filter=FID%3D'114'");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
            
        //get the new version
        id = getFirstElementByTagName(dom, "cite:Buildings").getAttribute("gml:id");
       
        assertEquals(updatedRid, id);
        
        assertEquals(fid, id.split("@")[0]);
        String ver2 = id.split("@")[1];
        assertFalse(ver1.equals(ver2));

        //query for the old version
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
            + "xmlns:fes='" + FES.NAMESPACE + "' "  
            + "xmlns:cite='http://www.opengis.net/cite' "
            + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
            + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
            + "<fes:Filter>"
              + "<fes:ResourceId rid = '" + fid + "@" + ver1 + "'/>"
            + "</fes:Filter>"
            + "</wfs:Query> "
            + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathExists("//cite:Buildings/cite:ADDRESS[text()='215 Main Street']", dom);
        
        //query for the new version
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:fes='" + FES.NAMESPACE + "' "  
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                + " <fes:ResourceId rid = '" + fid + "@" + ver2 + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathExists("//cite:Buildings/cite:ADDRESS[text()='148 Lafayette Street']", dom);
        
        //do an update with a current version filter, it shall success
        xml = 
        "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
        "xmlns:cite=\"http://www.opengis.net/cite\" " +
        "xmlns:fes='" + FES.NAMESPACE + "' " + 
        "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
        "xmlns:gml='" + GML.NAMESPACE + "'>" + 
        " <wfs:Update typeName=\"cite:Buildings\">" +
        "   <wfs:Property>" +
        "     <wfs:ValueReference>FID</wfs:ValueReference>" +
        "     <wfs:Value>115</wfs:Value>" +
        "   </wfs:Property>" + 
        "   <fes:Filter>" +
        "      <fes:ResourceId rid = '" + fid + "@" + ver2 + "'/>" +
        "   </fes:Filter>" +
        " </wfs:Update>" +
       "</wfs:Transaction>"; 
        
        dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());

        //get the updated version
        dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings" +
                "&cql_filter=FID%3D'115'");

        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
            
        //get the new version
        id = getFirstElementByTagName(dom, "cite:Buildings").getAttribute("gml:id");
        assertEquals(fid, id.split("@")[0]);
            
        String ver3 = id.split("@")[1];
        assertFalse(ver2.equals(ver3));
        
        //but an update with a non current version filter shall fail
        //reuse the previous request, using ver2 as the filter...
        dom = postAsDOM( "wfs", xml );
        // print(dom);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        // assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        // assertEquals( "0", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild().getNodeValue());
    }

    public void testDelete() throws Exception {
        
        Document dom = getAsDOM( "wfs?version=2.0.0&request=getfeature&typename=cite:Buildings" +
                "&cql_filter=FID%3D'114'");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        
        //get the fid and version
        String id = getFirstElementByTagName(dom, "cite:Buildings").getAttribute("gml:id");
        String fid = id.split("@")[0];
        String ver = id.split("@")[1];

        //do an delete
        String xml = 
        "<wfs:Transaction service=\"WFS\" version=\"2.0.0\" " + 
        "xmlns:cite=\"http://www.opengis.net/cite\" " +
        "xmlns:fes='" + FES.NAMESPACE + "' " + 
        "xmlns:wfs='" + WFS.NAMESPACE + "' " + 
        "xmlns:gml='" + GML.NAMESPACE + "'>" + 
        " <wfs:Delete typeName=\"cite:Buildings\">" +
        "   <fes:Filter>" +
        "     <fes:PropertyIsEqualTo>" +
        "       <fes:ValueReference>FID</fes:ValueReference>" + 
        "       <fes:Literal>114</fes:Literal>" + 
        "     </fes:PropertyIsEqualTo>" + 
        "   </fes:Filter>" +
        " </wfs:Delete>" +
       "</wfs:Transaction>";
        dom = postAsDOM( "wfs", xml );
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals( "1", getFirstElementByTagName(dom, "wfs:totalDeleted").getFirstChild().getNodeValue());

        //do a query just based on fid, ensure not returned
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:fes='" + FES.NAMESPACE + "' "  
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + fid + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        print(dom);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        XMLAssert.assertXpathEvaluatesTo("0", "count(//cite:Buildings)", dom);

        //do the query with version and ensure returned 
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:fes='" + FES.NAMESPACE + "' "  
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + fid + "@" + ver + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);

        Date then = new Date();
        //query by date, if end date before time of deletion then we should return features
        xml = "<wfs:GetFeature service='WFS' version='2.0.0' "
                + "xmlns:fes='" + FES.NAMESPACE + "' "  
                + "xmlns:cite='http://www.opengis.net/cite' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " + "> "
                + "<wfs:Query typeNames='" + "cite:Buildings" + "'> "
                + "<fes:Filter>"
                  + "<fes:ResourceId rid = '" + fid + "' endDate='" + serializeDateTime(then) + "'/>"
                + "</fes:Filter>"
                + "</wfs:Query> "
                + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        // 2 instances expected, the insert at commit1 and the update at commit4, as per
        // oneTimeSetUp()
        XMLAssert.assertXpathEvaluatesTo("2", "count(//cite:Buildings)", dom);
    }
}
