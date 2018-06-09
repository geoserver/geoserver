/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureWithLockTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData systemTestData) throws Exception {
        getServiceDescriptor11().getOperations().add("ReleaseLock");
    }

    @Test
    public void testUpdateLockedFeatureWithLockId() throws Exception {
        // get feature
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);

        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\">"
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"1.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\"> "
                        + "    <wfs:Property> "
                        + "      <wfs:Name>cdf:id</wfs:Name> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <ogc:Filter> "
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/> "
                        + "    </ogc:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testUpdateLockedFeatureWithoutLockId() throws Exception {

        // get feature
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertFalse(dom.getElementsByTagName("cdf:Locks").getLength() == 0);

        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\">"
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"1.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "  <wfs:Update typeName=\"cdf:Locks\"> "
                        + "    <wfs:Property> "
                        + "      <wfs:Name>cdf:id</wfs:Name> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <ogc:Filter> "
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/> "
                        + "    </ogc:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        // assertEquals( "wfs:WFS_TransactionResponse",
        // dom.getDocumentElement().getNodeName() );
        assertTrue(
                1 == dom.getElementsByTagName("wfs:FAILED").getLength()
                        || "ServiceExceptionReport".equals(dom.getDocumentElement().getNodeName()));
    }

    @Test
    public void testGetFeatureWithLockReleaseActionSome() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        // release locks
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testGetFeatureWithLockReleaseActionSome2() throws Exception {
        String xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\"Locks.1\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);

        // relase with "some" but actually releases the only locked feature
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\"Locks.1\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        // release locks
        get("cdf/wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testLayerQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/Fifteen/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        // release locks
        get("cdf/Locks/wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }
}
