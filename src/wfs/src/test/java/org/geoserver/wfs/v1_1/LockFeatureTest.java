/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertEquals;

import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.filter.v1_1.OGC;
import org.junit.Test;
import org.w3c.dom.Document;

public class LockFeatureTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        Service service = (Service) GeoServerExtensions.bean("wfsService-1.1.0");
        // register fake operation to ease testing
        service.getOperations().add("ReleaseLock");
    }

    @Test
    public void testLock() throws Exception {
        String xml =
                "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" xmlns:wfs=\"http://www.opengis.net/wfs\" expiry=\"5\" handle=\"LockFeature-tc1\" "
                        + " lockAction=\"ALL\" "
                        + " service=\"WFS\" "
                        + " version=\"1.1.0\">"
                        + "<wfs:Lock handle=\"lock-1\" typeName=\"sf:PrimitiveGeoFeature\"/>"
                        + "</wfs:LockFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(5, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }

    @Test
    public void testLockGet() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typename=sf:GenericEntity",
                        200);

        print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(3, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }

    @Test
    public void testLockWithNamespacesGet() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typename=ns53:GenericEntity"
                                + "&namespace=xmlns(ns53=http://cite.opengeospatial.org/gmlsf)",
                        200);

        // print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(3, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());
        releaseLock(dom);
    }

    public void releaseLock(Document dom) throws Exception {
        // release the lock
        String lockId = XMLUnit.newXpathEngine().evaluate("//wfs:LockId", dom);
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);
    }

    @Test
    public void testLockByBBOX() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typeName=sf:PrimitiveGeoFeature"
                                + "&BBOX=57.0,-4.5,62.0,1.0,EPSG:4326",
                        200);

        // print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }
}
