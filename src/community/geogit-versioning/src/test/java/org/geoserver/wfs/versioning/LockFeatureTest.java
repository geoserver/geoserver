package org.geoserver.wfs.versioning;

import org.geoserver.data.test.MockData;
import org.geotools.wfs.v2_0.WFS;
import org.w3c.dom.Document;

public class LockFeatureTest extends WFS20VersioningTestSupport {

    public void testLock() throws Exception {
        String xml = 
            "<wfs:LockFeature xmlns:cite='"+ MockData.CITE_URI +"' " +
            "   xmlns:wfs='" + WFS.NAMESPACE + "' expiry=\"5\""
                + " service=\"WFS\" "
                + " version=\"2.0.0\">"
                + "<wfs:Query typeNames=\"cite:Bridges\"/>"
            + "</wfs:LockFeature>";
        Document dom = postAsDOM("wfs", xml);
        print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getDocumentElement().hasAttribute("lockId"));
    }
}
