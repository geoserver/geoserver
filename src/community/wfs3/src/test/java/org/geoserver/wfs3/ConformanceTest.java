/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.sf.json.JSON;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConformanceTest extends WFS3TestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        JSON json = getAsJSON("wfs3/conformance");
        print(json);
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("wfs3/conformance?f=application/xml");
        print(dom);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("wfs3/conformance/?f=application/x-yaml");
        // System.out.println(yaml);
    }
}
