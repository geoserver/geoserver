/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class CollectionsTest extends GeoVolumesTestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/3dgeovolumes/v1/collections", 200);
        assertEquals(2, (int) json.read("collections.length()", Integer.class));

        // check the new york collection
        DocumentContext ny = readContext(json, "collections[0]");
        assertEquals("NewYork", ny.read("title", String.class));
        assertEquals("NewYork", ny.read("id", String.class));
        assertEquals(
                "All Supported 3D Containers for the city of NewYork",
                ny.read("description", String.class));
        assertArrayEquals(NY_BBOX, ny.read("extent.spatial.bbox[0]", double[].class), 1e-9);

        // check Stuttgart
        DocumentContext stg = readContext(json, "collections[1]");
        assertEquals("Stuttgart", stg.read("title", String.class));
        assertEquals("Stuttgart", stg.read("id", String.class));
        assertEquals(
                "All Supported 3D Containers for the city of Stuttgart LoD 1 from OSM with Textures",
                stg.read("description", String.class));
        assertArrayEquals(STG_BBOX, stg.read("extent.spatial.bbox[0]", double[].class), 1e-9);
    }

    /** Same as above but for HTML. The bbox in text form has less decimals. */
    @Test
    public void testCollectionsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/3dgeovolumes/v1/collections?f=html");

        // check New York
        String nyHtmlId = "NewYork";
        assertEquals("NewYork", doc.select("#" + nyHtmlId + "_title").text());
        assertEquals(
                "All Supported 3D Containers for the city of NewYork",
                doc.select("#" + nyHtmlId + "_description").text());
        String nyTextBBOX = doc.select("#" + nyHtmlId + "_spatial ul li").text();
        assertArrayEquals(NY_BBOX, parseBBOXText(nyTextBBOX), 1e-3);

        // check Stuttgart
        String stgHtmlId = "Stuttgart";
        assertEquals("Stuttgart", doc.select("#" + stgHtmlId + "_title").text());
        assertEquals(
                "All Supported 3D Containers for the city of Stuttgart LoD 1 from OSM with Textures",
                doc.select("#" + stgHtmlId + "_description").text());
        String stgTextBBOX = doc.select("#" + stgHtmlId + "_spatial ul li").text();
        assertArrayEquals(STG_BBOX, parseBBOXText(stgTextBBOX), 1e-3);
    }
}
