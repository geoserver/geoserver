/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.versioning;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureWithLockTest extends WFS20VersioningTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        getServiceDescriptor20().getOperations().add( "ReleaseLock");
    }

    public void testPOST() throws Exception {
        String xml = "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' " +
            "handle='GetFeatureWithLock-tc1' expiry='5' resultType='results' " + 
            "xmlns:cite='" + MockData.CITE_URI + "' xmlns:wfs='" + WFS.NAMESPACE + "'>" + 
                "<wfs:Query handle='qry-1' typeNames='cite:Bridges' />" + 
            "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        //print(dom);
        assertGML32(dom);
        assertNotNull( dom.getDocumentElement().getAttribute("lockId") );
    }
    
    public void testUpdateLockedFeatureWithLockId() throws Exception {
        // get feature
        String xml = 
            "<wfs:GetFeature service='WFS' version='2.0.0' expiry='10' "
                + "xmlns:cite='" +MockData.CITE_URI + "' "
                + "xmlns:fes='" + FES.NAMESPACE +  "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
            + "<wfs:Query typeNames='cite:Bridges'/>" 
          + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        print(dom);
        assertGML32(dom);
        
        // get a fid
        assertFalse(dom.getElementsByTagName("cite:Bridges").getLength() == 0);
        String fid = ((Element) dom.getElementsByTagName("cite:Bridges").item(0)).getAttribute("gml:id");

        // lock a feature
        xml = "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' expiry='10' "
                + "xmlns:cite='" + MockData.CITE_URI + "' "
                + "xmlns:fes='" + FES.NAMESPACE +  "' xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames='cite:Bridges'>" 
                  + "<fes:Filter><fes:ResourceId rid='" + fid + "'/>" + "</fes:Filter>"
                + "</wfs:Query>" 
              + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        print(dom);
        assertGML32(dom);
        
        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml = "<wfs:Transaction " + "  service=\"WFS\" "
                + "  version=\"2.0.0\" "
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\" "
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "' " + "> "
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cite:Bridges\" handle='foo'> "
                + "    <wfs:Property> " 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference> "
                + "      <wfs:Value>115</wfs:Value> "
                + "    </wfs:Property> " + "    <fes:Filter> "
                + "      <fes:ResourceId rid=\"" + fid + "\"/> "
                + "    </fes:Filter> " + "  </wfs:Update> "
                + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);
        print(dom);

        // release the lock
        get("wfs?request=ReleaseLock&version=2.0.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement() .getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        
        // get the updated rid
        String updatedRid = xpath.evaluate("//wfs:UpdateResults//fes:ResourceId/@rid", dom);
        String[] rid1 = fid.split("@");
        String[] rid2 = updatedRid.split("@");
        assertEquals(rid1[0], rid2[0]);
        assertNotNull(rid1[1]);
        assertNotNull(rid2[1]);
        assertFalse(rid1[1].equals(rid2[1]));
    }
    
    public void testUpdateLockedFeatureWithoutLockId() throws Exception {

        // get feature
        String xml = "<wfs:GetFeature " + "service=\"WFS\" "
                + "version=\"2.0.0\" " + "expiry=\"10\" "
                + "xmlns:cite=\"" + MockData.CITE_URI + "\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cite:Bridges\"/>" 
                + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());
        assertFalse(dom.getElementsByTagName("cite:Bridges").getLength() == 0);

        String rid = ((Element) dom.getElementsByTagName("cite:Bridges").item(0))
                .getAttribute("gml:id");

        // lock a feature
        xml = "<wfs:GetFeatureWithLock " + "service=\"WFS\" "
                + "version=\"2.0.0\" " + "expiry=\"10\" "
                + "xmlns:cite=\"" + MockData.CITE_URI + "\" "
                + "xmlns:fes='" + FES.NAMESPACE + "' "
                + "xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "<wfs:Query typeNames=\"cite:Bridges\">" + "<fes:Filter>"
                + "<fes:ResourceId rid=\"" + rid + "\"/>" + "</fes:Filter>"
                + "</wfs:Query>" + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml = "<wfs:Transaction " + "  service=\"WFS\" "
                + "  version=\"2.0.0\" "
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\" "
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "' " + "> "
                + "  <wfs:Update typeName=\"cite:Bridges\"> "
                + "    <wfs:Property> " 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference> "
                + "      <wfs:Value>115</wfs:Value> "
                + "    </wfs:Property> "
                + "    <fes:Filter> "
                + "      <fes:ResourceId rid=\"" + rid + "\"/> "
                + "    </fes:Filter> " + "  </wfs:Update> "
                + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);
        
        // release the lock
        get("wfs?request=ReleaseLock&lockId=" + lockId);

        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        // assertEquals("wfs:TransactionResponse", dom.getDocumentElement() .getNodeName());
        // XMLAssert.assertXpathEvaluatesTo("0", "//wfs:totalUpdated/text()", dom);
    }

    public void testGetFeatureWithLockReleaseActionSome() throws Exception {
        //let be two buildings
        {
            SimpleFeatureType type = (SimpleFeatureType) getCatalog().getFeatureTypeByName(
                    CITE_BUILDINGS).getFeatureType();
            SimpleFeature building = new SimpleFeatureBuilder(type).buildFeature("Buildings.2");
            recordInsertCommit(ggitFacade, "New building", CITE_BUILDINGS, building);
        }
        
        String xml = "<wfs:GetFeature" 
                + "  service=\"WFS\""
                + "  version=\"2.0.0\"" 
                + "  expiry=\"10\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"cite:Buildings\"/>" 
                + "</wfs:GetFeature>";
        
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cite:Buildings");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        xml = "<wfs:GetFeatureWithLock" 
                + "  service=\"WFS\""
                + "  version=\"2.0.0\"" 
                + "  expiry=\"10\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"cite:Buildings\">" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" + "  </wfs:Query>"
                + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement()
                .getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        xml = "<wfs:Transaction" 
                + "  service=\"WFS\"" 
                + "  version=\"2.0.0\""
                + "  releaseAction=\"SOME\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId 
                + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cite:Buildings\">"
                + "    <wfs:Property>" 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference>"
                + "      <wfs:Value>120</wfs:Value>"
                + "    </wfs:Property>" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "    </fes:Filter>" 
                + "  </wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);
        print(dom);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        
        String updatedRid = xpath.evaluate("//wfs:UpdateResults//fes:ResourceId/@rid", dom);
        assertEquals(fid1.split("@")[0], updatedRid.split("@")[0]);
        assertFalse(fid1.split("@")[1].equals(updatedRid.split("@")[1]));
        
        xml = "<wfs:Transaction" 
                + "  service=\"WFS\"" 
                + "  version=\"2.0.0\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"cite:Buildings\">"
                + "    <wfs:Property>" 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference>"
                + "      <wfs:Value>121</wfs:Value>"
                + "    </wfs:Property>" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" 
                + "  </wfs:Update>"
                + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        // release locks
        get("wfs?request=ReleaseLock&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);

        updatedRid = xpath.evaluate("//wfs:UpdateResults//fes:ResourceId/@rid", dom);
        assertEquals(fid2.split("@")[0], updatedRid.split("@")[0]);
        assertFalse(fid2.split("@")[1].equals(updatedRid.split("@")[1]));
    }
 
    public void testWorkspaceQualified() throws Exception {
        //let be two buildings
        {
            SimpleFeatureType type = (SimpleFeatureType) getCatalog().getFeatureTypeByName(
                    CITE_BUILDINGS).getFeatureType();
            SimpleFeature building = new SimpleFeatureBuilder(type).buildFeature("Buildings.2");
            recordInsertCommit(ggitFacade, "New building", CITE_BUILDINGS, building);
        }

        String xml = "<wfs:GetFeature" 
                + "  service=\"WFS\""
                + "  version=\"2.0.0\"" 
                + "  expiry=\"10\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "   <wfs:Query typeNames=\"Buildings\"/>" 
                + "</wfs:GetFeature>";
        Document dom = postAsDOM("cite/wfs", xml);
        
        // get two fids
        NodeList locks = dom.getElementsByTagName("cite:Buildings");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");
        
        xml = "<wfs:GetFeatureWithLock" 
                + "  service=\"WFS\""
                + "  version=\"2.0.0\"" 
                + "  expiry=\"10\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:Query typeNames=\"Buildings\">" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" 
                + "  </wfs:Query>"
                + "</wfs:GetFeatureWithLock>";
        
        dom = postAsDOM("cite/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        
        String lockId = dom.getDocumentElement().getAttribute("lockId");
        //System.out.println(lockId);
        xml = "<wfs:Transaction" 
                + "  service=\"WFS\"" 
                + "  version=\"2.0.0\""
                + "  releaseAction=\"SOME\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Buildings\">"
                + "    <wfs:Property>" 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference>"
                + "      <wfs:Value>120</wfs:Value>"
                + "    </wfs:Property>" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid1 + "\"/>"
                + "    </fes:Filter>" 
                + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cite/wfs", xml);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);

        String updatedRid = xpath.evaluate("//wfs:UpdateResults//fes:ResourceId/@rid", dom);
        assertEquals(fid1.split("@")[0], updatedRid.split("@")[0]);
        assertFalse(fid1.split("@")[1].equals(updatedRid.split("@")[1]));
        
        xml = "<wfs:Transaction" 
                + "  service=\"WFS\"" 
                + "  version=\"2.0.0\""
                + "  xmlns:cite=\"" + MockData.CITE_URI + "\""
                + "  xmlns:fes='" + FES.NAMESPACE + "' "
                + "  xmlns:wfs='" + WFS.NAMESPACE + "'>"
                + "  <wfs:LockId>" + lockId + "</wfs:LockId>"
                + "  <wfs:Update typeName=\"Buildings\">"
                + "    <wfs:Property>" 
                + "      <wfs:ValueReference>cite:FID</wfs:ValueReference>"
                + "      <wfs:Value>121</wfs:Value>"
                + "    </wfs:Property>" 
                + "    <fes:Filter>"
                + "      <fes:ResourceId rid=\"" + fid2 + "\"/>"
                + "    </fes:Filter>" 
                + "  </wfs:Update>"
                + "</wfs:Transaction>";
        
        dom = postAsDOM("cite/wfs", xml);
        
        // release locks
        get("cite/wfs?request=ReleaseLock&lockId=" + lockId);
        
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);

        updatedRid = xpath.evaluate("//wfs:UpdateResults//fes:ResourceId/@rid", dom);
        assertEquals(fid2.split("@")[0], updatedRid.split("@")[0]);
        assertFalse(fid2.split("@")[1].equals(updatedRid.split("@")[1]));
        
    }

}
