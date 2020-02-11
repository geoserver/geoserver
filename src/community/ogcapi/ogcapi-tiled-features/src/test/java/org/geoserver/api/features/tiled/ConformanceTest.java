/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features.tiled;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.api.features.FeatureService;
import org.geoserver.api.tiles.TilesService;
import org.junit.Test;

public class ConformanceTest extends TiledFeaturesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/conformance", 200);
        assertEquals(FeatureService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(FeatureService.OAS30, json.read("$.conformsTo[1]", String.class));
        assertEquals(FeatureService.HTML, json.read("$.conformsTo[2]", String.class));
        assertEquals(FeatureService.GEOJSON, json.read("$.conformsTo[3]", String.class));
        assertEquals(FeatureService.GMLSF0, json.read("$.conformsTo[4]", String.class));
        assertEquals(FeatureService.CQL_TEXT, json.read("$.conformsTo[5]", String.class));
        // check the document got extended
        assertEquals(TilesService.CC_CORE, json.read("$.conformsTo[6]", String.class));
    }
}
