package org.geoserver.wfs.v1_1;

import org.geoserver.wfs.WFSTestSupport;
import org.w3c.dom.Document;

public class GetFeatureWithLockTest extends WFSTestSupport {

     public void test() throws Exception {
        String xml = "<wfs:GetFeatureWithLock service=\"WFS\" version=\"1.1.0\" "
                + "	  handle=\"GetFeatureWithLock-tc1\""
                + "	  expiry=\"5\""
                + "	  resultType=\"results\""
                + "	  xmlns:wfs=\"http://www.opengis.net/wfs\""
                + "	  xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\">"
                + "	  <wfs:Query handle=\"qry-1\" typeName=\"sf:PrimitiveGeoFeature\" />"
                + "	</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        //print( dom );
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        assertNotNull( dom.getDocumentElement().getAttribute("lockId") );
    }
}
