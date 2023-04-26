/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wfs;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** WFS service GetCapabilities test for features templating module */
public class WfsGetCapabilitiesTest extends WFSTestSupport {

    /**
     * Checks if features templating specific result formats are present in output and represented
     * correctly.
     *
     * @throws Exception
     */
    @Test
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=1.0.0");

        Element outputFormats = getFirstElementByTagName(doc, "ResultFormat");
        NodeList formats = outputFormats.getChildNodes();

        Set<String> s1 = new TreeSet<>();
        for (int i = 0; i < formats.getLength(); i++) {
            String format = formats.item(i).getNodeName();
            s1.add(format);
        }

        assertTrue(s1.contains("JSON-LD"));
        assertTrue(s1.contains("HTML"));
    }
}
