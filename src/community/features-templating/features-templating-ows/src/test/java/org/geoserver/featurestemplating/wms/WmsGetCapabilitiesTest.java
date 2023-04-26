/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wms;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** WMS service GetCapabilities test for features templating module */
public class WmsGetCapabilitiesTest extends WMSTestSupport {

    /**
     * Checks if features templating specific result formats are present in output and represented
     * correctly.
     *
     * @throws Exception
     */
    @Test
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.0.0");

        Element outputFormats = getFirstElementByTagName(doc, "GetFeatureInfo");
        NodeList childNodes = outputFormats.getChildNodes();

        Set<String> formats = new TreeSet<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if ("Format".equals(item.getNodeName())) {
                formats.add(item.getTextContent());
            }
        }
        assertTrue(formats.contains("application/ld+json"));
    }
}
