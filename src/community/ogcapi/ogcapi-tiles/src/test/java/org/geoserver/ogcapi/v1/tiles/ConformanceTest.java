/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.ConformanceClass;
import org.junit.Test;

public class ConformanceTest extends TilesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertEquals(9, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(ConformanceClass.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(ConformanceClass.COLLECTIONS, json.read("$.conformsTo[1]", String.class));
        assertEquals(TilesService.CC_TILE_CORE, json.read("$.conformsTo[2]", String.class));
        assertEquals(TilesService.CC_TILESET, json.read("$.conformsTo[3]", String.class));
        assertEquals(TilesService.CC_MULTITILES, json.read("$.conformsTo[4]", String.class));
        assertEquals(TilesService.CC_INFO, json.read("$.conformsTo[5]", String.class));
        assertEquals(
                TilesService.CC_TILES_TILE_MATRIX_SET, json.read("$.conformsTo[6]", String.class));
        assertEquals(TilesService.CC_TILE_MATRIX_SET, json.read("$.conformsTo[7]", String.class));
        assertEquals(
                TilesService.CC_TILE_MATRIX_SET_JSON, json.read("$.conformsTo[8]", String.class));
        // check the others as they get implemented
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/tiles/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/tiles/v1/conformance?f=text/html");
        assertEquals("GeoServer OGC API Tiles Conformance", document.select("#title").text());
        assertEquals(ConformanceClass.CORE, document.select("#content li:eq(0)").text());
        assertEquals(ConformanceClass.COLLECTIONS, document.select("#content li:eq(1)").text());
        assertEquals(TilesService.CC_TILE_CORE, document.select("#content li:eq(2)").text());
        assertEquals(TilesService.CC_TILESET, document.select("#content li:eq(3)").text());
        assertEquals(TilesService.CC_MULTITILES, document.select("#content li:eq(4)").text());
        assertEquals(TilesService.CC_INFO, document.select("#content li:eq(5)").text());
    }
}
