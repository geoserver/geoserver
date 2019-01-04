/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Before;
import org.w3c.dom.Document;

public class WFS20TestSupport extends WFSTestSupport {

    @Before
    public void beforeTest() {
        // deactivate GML MIME type overriding
        setGmlMimeTypeOverride(null);
    }

    /**
     * Helper method that just sets the GML 3.2 MIME type to force. If NULL is provided then GML
     * MIME type overriding will be deactivated. This changes will be applied to WFS global
     * configuration.
     */
    protected void setGmlMimeTypeOverride(String mimeTypeToForce) {
        WFSInfo info = getGeoServer().getService(WFSInfo.class);
        GMLInfo gmlInfo = info.getGML().get(WFSInfo.Version.V_20);
        gmlInfo.setMimeTypeToForce(mimeTypeToForce);
        getGeoServer().save(info);
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        // override some namespaces
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
    }

    /** @return The 2.0 service descriptor. */
    protected Service getServiceDescriptor20() {
        return (Service) GeoServerExtensions.bean("wfsService-2.0");
    }

    /** Asserts a document is valid gml 3.2 */
    protected void assertGML32(Document doc) {
        assertEquals(WFS.NAMESPACE, doc.getDocumentElement().getAttribute("xmlns:wfs"));

        String schemaLocation = doc.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertTrue(schemaLocation.contains(WFS.NAMESPACE));

        String[] parts = schemaLocation.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(WFS.NAMESPACE)) {
                assertTrue(parts[i + 1].endsWith("2.0/wfs.xsd"));
            }
        }
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }
}
