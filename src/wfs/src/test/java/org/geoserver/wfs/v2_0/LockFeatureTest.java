package org.geoserver.wfs.v2_0;

import java.io.ByteArrayInputStream;

import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LockFeatureTest extends WFS20TestSupport {

    public void testLock() throws Exception {
        String xml = 
            "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" " +
            "   xmlns:wfs='" + WFS.NAMESPACE + "' expiry=\"5\" handle=\"LockFeature-tc1\" "
                + " lockAction=\"ALL\" "
                + " service=\"WFS\" "
                + " version=\"2.0.0\">"
                + "<wfs:Query handle=\"lock-1\" typeNames=\"sf:PrimitiveGeoFeature\"/>"
            + "</wfs:LockFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(5, dom.getElementsByTagNameNS(FES.NAMESPACE, "ResourceId").getLength());
    }

    public void testSOAP() throws Exception {
        String xml = 
           "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
                " <soap:Header/> " + 
                " <soap:Body>"
             +   "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
             +   "   xmlns:wfs='" + WFS.NAMESPACE + "' expiry=\"5\" handle=\"LockFeature-tc1\" "
                    + " lockAction=\"ALL\" "
                    + " service=\"WFS\" "
                    + " version=\"2.0.0\">"
                    + "<wfs:Query handle=\"lock-1\" typeNames=\"sf:PrimitiveGeoFeature\"/>"
                + "</wfs:LockFeature>" + 
                " </soap:Body> " + 
            "</soap:Envelope> "; 
              
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());
        
        Document dom = dom(new ByteArrayInputStream(resp.getOutputStreamContent().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:LockFeatureResponse").getLength());
    }
}
