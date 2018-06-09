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

public class LockFeatureTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData systemTestData) throws Exception {
        getServiceDescriptor10().getOperations().add("ReleaseLock");
    }

    @Test
    public void testLockActionSomeAlreadyLocked() throws Exception {

        // get a feature
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  outputFormat=\"GML2\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\" />"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        // get a fid
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock the feature
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";

        dom = postAsDOM("wfs", xml);
        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());

        String lockId = dom.getElementsByTagName("LockId").item(0).getFirstChild().getNodeValue();

        // try to lock again with releaseAction = SOME
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  lockAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";
        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.0.0&lockId=" + lockId);

        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertFalse(dom.getElementsByTagName("FeaturesNotLocked").getLength() == 0);
    }

    @Test
    public void testDeleteWithoutLockId() throws Exception {
        // get a feature
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  outputFormat=\"GML2\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\" />"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        // get a fid
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock the feature
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());

        String lockId = dom.getElementsByTagName("LockId").item(0).getFirstChild().getNodeValue();

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Delete typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Delete>"
                        + "</wfs:Transaction>";
        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.0.0&lockId=" + lockId);

        assertTrue(
                "ServiceExceptionReport".equals(dom.getDocumentElement().getNodeName())
                        || dom.getElementsByTagName("wfs:FAILED").getLength() == 1);
    }

    @Test
    public void testUpdateWithLockId() throws Exception {
        // get a feature
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  outputFormat=\"GML2\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\" />"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        // get a fid
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock the feature
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";
        dom = postAsDOM("wfs", xml);
        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());

        // get the lockId
        String lockId = dom.getElementsByTagName("LockId").item(0).getFirstChild().getNodeValue();

        // update the feawture
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
                        + "      <wfs:Value>lfbt0002</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";
        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.0.0&lockId=" + lockId);

        assertFalse(dom.getElementsByTagName("wfs:SUCCESS").getLength() == 0);
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        // get a feature
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  outputFormat=\"GML2\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\" />"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("cdf/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        // get a fid
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock the feature
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";

        dom = postAsDOM("cdf/wfs", xml);
        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());

        // get the lockId
        String lockId = dom.getElementsByTagName("LockId").item(0).getFirstChild().getNodeValue();
        // release the lock
        get("wfs?request=ReleaseLock&version=1.0.0&lockId=" + lockId);
    }

    @Test
    public void testLayerQualified() throws Exception {
        // get a feature
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  outputFormat=\"GML2\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\" />"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        // get a fid
        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock the feature
        xml =
                "<wfs:LockFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Lock typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Lock>"
                        + "</wfs:LockFeature>";

        dom = postAsDOM("cdf/Fifteen/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("WFS_LockFeatureResponse", dom.getDocumentElement().getNodeName());

        // get the lockId
        String lockId = dom.getElementsByTagName("LockId").item(0).getFirstChild().getNodeValue();
        // release the lock
        get("wfs?request=ReleaseLock&version=1.0.0&lockId=" + lockId);
    }
}
