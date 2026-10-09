/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The links that lead to a map resource, and the operations they point at: {@code /conf/collection-map/desc-links},
 * {@code /conf/collection-map/map-operation}, {@code /conf/styled-map/desc-links} and
 * {@code /conf/styled-map/map-operation}.
 */
public class MapLinksTest extends MapsTestSupport {

    private static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

    /**
     * /conf/collection-map/desc-links: the collection description carries a map link, and
     * /conf/collection-map/map-operation: the href it points at answers with a map.
     */
    @Test
    public void testCollectionMapLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/cite:Lakes", 200);
        List<String> rels = json.read("links[*].rel");
        assertThat(rels, hasItem(REL_MAP));

        // the PNG map link points at the collection map resource, and that resource delivers a PNG
        String href = readSingle(json, "links[?(@.rel=='" + REL_MAP + "' && @.type=='image/png')].href");
        assertThat(href, endsWith("/collections/cite:Lakes/map?f=image%2Fpng"));
        MockHttpServletResponse response = getAsServletResponse(href.substring(href.indexOf("/ogc/") + 1));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", getBaseMimeType(response.getContentType()));
    }

    /**
     * /conf/styled-map/desc-links: every style in the list of styles carries a map link, and
     * /conf/styled-map/map-operation: the href it points at answers with a map for that style.
     */
    @Test
    public void testStyledMapLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/cite:Lakes/styles", 200);
        // the two styles of the layer, the default one and the alternative red one
        assertEquals(Integer.valueOf(2), json.read("styles.length()"));
        for (int i = 0; i < 2; i++) {
            assertThat(json.read("styles[" + i + "].links[*].rel"), hasItem(REL_MAP));
        }

        String href = readSingle(
                json, "styles[?(@.id=='red')].links[?(@.rel=='" + REL_MAP + "' && @.type=='image/png')].href");
        assertThat(href, endsWith("/collections/cite%3ALakes/styles/red/map?f=image%2Fpng"));
        MockHttpServletResponse response = getAsServletResponse(href.substring(href.indexOf("/ogc/") + 1));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", getBaseMimeType(response.getContentType()));
    }
}
