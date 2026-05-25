/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;

public class StylesTest extends StylesTestSupport {

    @Test
    public void testStylesHTML() throws Exception {
        getAsJSoup("ogc/styles/v1/styles?f=html");
    }

    @Test
    public void testStylesJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles", 200);
        testStylesJson(json);
    }

    @SuppressWarnings("unchecked") // matcher vararg generics
    private void testStylesJson(DocumentContext json) {
        // check the self link
        assertEquals("self", readSingle(json, "links[?(@.type == 'application/json')].rel"));
        // and the alternates
        assertEquals("alternate", readSingle(json, "links[?(@.type == 'application/yaml')].rel"));

        // check all the styles are there
        assertThat(
                ((List<String>) json.read("styles[*].id")),
                Matchers.containsInAnyOrder(
                        "BasicPolygons",
                        "BasicStyleGroupStyle",
                        "Bridges",
                        "Buildings",
                        "Default",
                        "DividedRoutes",
                        "Forests",
                        "Lakes",
                        "MapNeatline",
                        "NamedPlaces",
                        "PolygonComment",
                        "Ponds",
                        "RoadSegments",
                        "Streams",
                        "ws:NamedPlacesWS",
                        "ws2:NamedPlacesWS2",
                        "cssSample",
                        "generic",
                        "line",
                        "point",
                        "polygon",
                        "raster"));

        // concentrate on one and check title and links
        assertEquals("Default Styler", readSingle(json, "styles[?(@.id == 'Default')].title"));
        // can encode sld 1.0
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/Default?f=application%2Fvnd.ogc.sld%2Bxml",
                readSingle(
                        json,
                        "styles[?(@.id == 'Default')].links[?(@.rel == 'stylesheet' && @.type == 'application/vnd.ogc.sld+xml')].href"));
        // but not css, cannot go from css to sld at the moment
        assertFalse(
                exists(
                        json,
                        "styles[?(@.id == 'Default')].links[?(@.rel == 'stylesheet' && @.type == 'application/vnd.geoserver.geocss+css')]"));
    }

    @Test
    public void testStylesWorkspaceScoped() throws Exception {
        DocumentContext json = getAsJSONPath("ws/ogc/styles/v1/styles", 200);
        List<String> ids = json.read("styles[*].id");

        // styles from the current workspace are visible (name is dequalified under a virtual service)
        assertTrue("expected NamedPlacesWS in " + ids, ids.contains("NamedPlacesWS"));
        // global styles remain accessible from the workspace-scoped listing
        assertTrue("expected global 'Default' style in " + ids, ids.contains("Default"));
        // styles belonging to another workspace must not leak in
        assertFalse("ws2 style leaked into ws listing: " + ids, ids.contains("NamedPlacesWS2"));
        assertFalse("qualified ws2 style leaked into ws listing: " + ids, ids.contains("ws2:NamedPlacesWS2"));
    }

    @Test
    public void testStylesWorkspaceScopedOtherWorkspace() throws Exception {
        DocumentContext json = getAsJSONPath("ws2/ogc/styles/v1/styles", 200);
        List<String> ids = json.read("styles[*].id");

        assertTrue("expected NamedPlacesWS2 in " + ids, ids.contains("NamedPlacesWS2"));
        assertTrue("expected global 'Default' style in " + ids, ids.contains("Default"));
        assertFalse("ws style leaked into ws2 listing: " + ids, ids.contains("NamedPlacesWS"));
        assertFalse("qualified ws style leaked into ws2 listing: " + ids, ids.contains("ws:NamedPlacesWS"));
    }
}
