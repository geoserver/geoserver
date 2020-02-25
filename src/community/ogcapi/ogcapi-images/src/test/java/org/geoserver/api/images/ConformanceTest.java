/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ConformanceTest extends ImagesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(ImagesService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(ImagesService.COLLECTIONS, json.read("$.conformsTo[1]", String.class));
        assertEquals(ImagesService.IMAGES_CORE, json.read("$.conformsTo[2]", String.class));
        assertEquals(
                ImagesService.IMAGES_TRANSACTIONAL, json.read("$.conformsTo[3]", String.class));
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/images/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }
}
