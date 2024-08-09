/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class CollectionTest extends GeoVolumesTestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        DocumentContext ny = getAsJSONPath("ogc/3dgeovolumes/v1/collections/NewYork", 200);

        // check the new york collection
        assertEquals("NewYork", ny.read("title", String.class));
        assertEquals("NewYork", ny.read("id", String.class));
        assertEquals(
                "All Supported 3D Containers for the city of NewYork",
                ny.read("description", String.class));
        assertArrayEquals(NY_BBOX, ny.read("extent.spatial.bbox[0]", double[].class), 1e-9);

        // basic links, self and alternate
        DocumentContext nySelf =
                readSingleContext(ny, "links[?(@.rel=='self' && @.type=='application/json')]");
        assertNotNull(nySelf);
        String base = "http://localhost:8080/geoserver/ogc/3dgeovolumes/v1/collections/NewYork";
        assertEquals(base + "?f=application%2Fjson", nySelf.read("href"));
        DocumentContext nyAlternate =
                readSingleContext(ny, "links[?(@.rel=='alternate' && @.type=='text/html')]");
        assertNotNull(nyAlternate);
        assertEquals(base + "?f=text%2Fhtml", nyAlternate.read("href"));

        // relative content links
        DocumentContext ny3dtiles =
                readSingleContext(
                        ny, "content[?(@.rel=='original' && @.type=='application/json+3dtiles')]");
        assertNotNull(ny3dtiles);
        assertEquals(base + "/3dtiles/tileset.json", ny3dtiles.read("href"));

        // absolute content links
        DocumentContext nyi3s =
                readSingleContext(
                        ny, "content[?(@.rel=='original' && @.type=='application/json+i3s')]");
        assertNotNull(nyi3s);
        assertEquals(
                "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_NewYork_17/SceneServer/layers/0/",
                nyi3s.read("href"));
    }

    /** Same as above but for HTML. The bbox in text form has less decimals. */
    @Test
    public void testCollectionHTML() throws Exception {
        Document doc = getAsJSoup("ogc/3dgeovolumes/v1/collections/NewYork?f=html");

        // check New York
        String nyHtmlId = "NewYork";
        assertEquals("NewYork", doc.select("#" + nyHtmlId + "_title").text());
        assertEquals(
                "All Supported 3D Containers for the city of NewYork",
                doc.select("#" + nyHtmlId + "_description").text());
        String nyTextBBOX = doc.select("#" + nyHtmlId + "_spatial ul li").text();
        assertArrayEquals(NY_BBOX, parseBBOXText(nyTextBBOX), 1e-3);

        // check the links

    }
}
