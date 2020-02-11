/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ConformanceTest extends TilesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(TilesService.CC_CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(TilesService.CC_MULTITILE, json.read("$.conformsTo[1]", String.class));
        assertEquals(TilesService.CC_INFO, json.read("$.conformsTo[2]", String.class));
        // check the others as they get implemented
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/tiles/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }
}
