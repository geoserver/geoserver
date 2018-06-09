/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LockFeatureTest extends WFS20TestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        getServiceDescriptor20().getOperations().add("ReleaseLock");
    }

    @Test
    public void testLock() throws Exception {
        String xml =
                "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "   xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' expiry=\"5\" handle=\"LockFeature-tc1\" "
                        + " lockAction=\"ALL\" "
                        + " service=\"WFS\" "
                        + " version=\"2.0.0\">"
                        + "<wfs:Query handle=\"lock-1\" typeNames=\"sf:PrimitiveGeoFeature\"/>"
                        + "</wfs:LockFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(5, dom.getElementsByTagNameNS(FES.NAMESPACE, "ResourceId").getLength());

        // release the lock
        print(dom);
        String lockId = dom.getDocumentElement().getAttribute("lockId");
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);
    }

    @Test
    public void testSOAP() throws Exception {
        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> "
                        + " <soap:Header/> "
                        + " <soap:Body>"
                        + "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "   xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' expiry=\"5\" handle=\"LockFeature-tc1\" "
                        + " lockAction=\"ALL\" "
                        + " service=\"WFS\" "
                        + " version=\"2.0.0\">"
                        + "<wfs:Query handle=\"lock-1\" typeNames=\"sf:PrimitiveGeoFeature\"/>"
                        + "</wfs:LockFeature>"
                        + " </soap:Body> "
                        + "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:LockFeatureResponse").getLength());

        // release the lock
        String lockId = XMLUnit.newXpathEngine().evaluate("//wfs:LockFeatureResponse/@lockId", dom);
        get("wfs?request=ReleaseLock&version=2.0&lockId=" + lockId);
    }
}
