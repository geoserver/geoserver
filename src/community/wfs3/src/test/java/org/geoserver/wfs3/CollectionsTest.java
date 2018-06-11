/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;
import org.w3c.dom.Document;

public class CollectionsTest extends WFS3TestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("wfs3/collections", 200);
        int expected = getCatalog().getFeatureTypes().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // TODO: perform more checks
    }

    @Test
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/wfs3/collections", 200);
        long expected =
                getCatalog()
                        .getFeatureTypes()
                        .stream()
                        .filter(ft -> "cdf".equals(ft.getStore().getWorkspace().getName()))
                        .count();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("wfs3/collections?f=text/xml");
        print(dom);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("wfs3/collections/?f=application/x-yaml");
        System.out.println(yaml);
    }
}
