/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.*;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureWithLockTest extends WFS20TestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        getServiceDescriptor20().getOperations().add("ReleaseLock");
    }

    @Test
    public void testPOST() throws Exception {
        String xml =
                "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' "
                        + "handle='GetFeatureWithLock-tc1' expiry='50' resultType='results' "
                        + "xmlns:sf='http://cite.opengeospatial.org/gmlsf' xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query handle='qry-1' typeNames='sf:PrimitiveGeoFeature' />"
                        + "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        assertGML32(dom);
        assertNotNull(dom.getDocumentElement().getAttribute("lockId"));
    }

    @Test
    public void testResultTypeHits() throws Exception {
        String xml =
                "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' "
                        + "handle='GetFeatureWithLock-tc1' expiry='50' resultType='hits' "
                        + "xmlns:sf='http://cite.opengeospatial.org/gmlsf' xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query handle='qry-1' typeNames='sf:PrimitiveGeoFeature' />"
                        + "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml, 400);
        checkOws11Exception(dom, "2.0.0", ServiceException.INVALID_PARAMETER_VALUE, "resultType");
    }

    @Test
    public void testUpdateLockedFeatureWithLockId() throws Exception {
        // get feature
        String xml =
                "<wfs:GetFeature service='WFS' version='2.0.0' expiry='100' "
                        + "xmlns:cdf='http://www.opengis.net/cite/data' "
                        + "xmlns:fes='"
                        + FES.NAMESPACE
                        + "' xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query typeNames='cdf:Locks'/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertGML32(dom);

        // get a fid
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);
        String fid =
                ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("gml:id");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock service='WFS' version='2.0.0' expiry='100' "
                        + "xmlns:cdf='http://www.opengis.net/cite/data' "
                        + "xmlns:fes='"
                        + FES.NAMESPACE
                        + "' xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query typeNames='cdf:Locks'>"
                        + "<fes:Filter><fes:ResourceId rid='"
                        + fid
                        + "'/>"
                        + "</fes:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertGML32(dom);

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"2.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' "
                        + "> "
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\" handle='foo'> "
                        + "    <wfs:Property> "
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <fes:Filter> "
                        + "      <fes:ResourceId rid=\""
                        + fid
                        + "\"/> "
                        + "    </fes:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=2.0.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);

        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid + "']", dom);
    }

    @Test
    public void testUpdateLockedFeatureWithoutLockId() throws Exception {

        // get feature
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"2.0.0\" "
                        + "expiry=\"100\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query typeNames=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);

        String fid =
                ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("gml:id");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock "
                        + "service=\"WFS\" "
                        + "version=\"2.0.0\" "
                        + "expiry=\"100\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "<wfs:Query typeNames=\"cdf:Locks\">"
                        + "<fes:Filter>"
                        + "<fes:ResourceId rid=\""
                        + fid
                        + "\"/>"
                        + "</fes:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"2.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' "
                        + "> "
                        + "  <wfs:Update typeName=\"cdf:Locks\"> "
                        + "    <wfs:Property> "
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <fes:Filter> "
                        + "      <fes:ResourceId rid=\""
                        + fid
                        + "\"/> "
                        + "    </fes:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);

        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists(
                "//ows:Exception[@exceptionCode = 'MissingParameterValue']", dom);
    }

    @Test
    public void testGetFeatureWithLockReleaseActionSomeOnTransaction() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"cdf:Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        // release locks
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
    }

    @Test
    public void testGetFeatureWithLockActionSome() throws Exception {
        // get all features
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        int featureCount = locks.getLength();
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        // lock the two fids
        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"cdf:Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//cdf:Locks[@gml:id='" + fid1 + "']", dom);
        XMLAssert.assertXpathExists("//cdf:Locks[@gml:id='" + fid2 + "']", dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//cdf:Locks)", dom);

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // now try to lock everything with "some", only the ones that can be locked should be
        // returned
        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  lockAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"cdf:Locks\"/>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        // print(dom);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathNotExists("//cdf:Locks[@gml:id='" + fid1 + "']", dom);
        XMLAssert.assertXpathNotExists("//cdf:Locks[@gml:id='" + fid2 + "']", dom);
        XMLAssert.assertXpathEvaluatesTo(
                String.valueOf(featureCount - 2), "count(//cdf:Locks)", dom);

        String secondLockId = dom.getDocumentElement().getAttribute("lockId");

        // release locks
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + secondLockId);
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        // release locks
        get("cdf/wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
    }

    @Test
    public void testLayerQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"100\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/Fifteen/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid1 + "']", dom);

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:ValueReference>cdf:id</wfs:ValueReference>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        // release locks
        get("cdf/Locks/wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);

        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "//wfs:totalUpdated/text()", dom);
        XMLAssert.assertXpathExists(
                "//wfs:UpdateResults//fes:ResourceId[@rid = '" + fid2 + "']", dom);
    }

    @Test
    public void testLockTwice() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        // lock for a loooong time
        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"1000\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // issue a lock again, it should be allowed as the lock expired
        dom = postAsDOM("cdf/Locks/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);

        // release the lock
        get("cdf/Locks/wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);
    }

    @Test
    public void testLockExpirySeconds() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("gml:id");
        String fid2 = ((Element) locks.item(1)).getAttribute("gml:id");

        // lock for just one second
        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"2.0.0\""
                        + "  expiry=\"1\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:fes='"
                        + FES.NAMESPACE
                        + "' "
                        + "  xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "'>"
                        + "  <wfs:Query typeNames=\"Locks\">"
                        + "    <fes:Filter>"
                        + "      <fes:ResourceId rid=\""
                        + fid1
                        + "\"/>"
                        + "      <fes:ResourceId rid=\""
                        + fid2
                        + "\"/>"
                        + "    </fes:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        Thread.sleep(2 * 1000);

        // issue a lock again, it should be allowed as the lock expired
        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // release the lock
        get("cdf/Locks/wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);
    }
}
