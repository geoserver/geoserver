/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class UTFGridIntegrationTest extends WMSTestSupport {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

    /**
     * The UTF grid format shows up in the caps document. WMS 1.3 requires the usage of mime types that will match the
     * result content type
     */
    @Test
    public void testCapabilities13() throws Exception {
        Document dom = getAsDOM("wms?service=WMS&request=GetCapabilities&version=1.3.0");
        // print(dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals("1", xpath.evaluate("count(//wms:GetMap[wms:Format='application/json;type=utfgrid'])", dom));
    }
}
