/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsTest extends MapsTestSupport {

    @Test
    public void testCollectionsJsonDefault() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/collections", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/collections?f=json", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/maps/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json, MediaType.parseMediaType("application/x-yaml"));
    }

    private void testCollectionsJson(DocumentContext json, MediaType defaultFormat)
            throws Exception {
        assertEquals(getNumberOfLayers(), (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats =
                GeoServerExtensions.bean(
                                APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                        .getProducibleMediaTypes(CollectionsDocument.class, true);
        formats.forEach(
                format -> {
                    // check rel
                    List items =
                            json.read(
                                    "collections[0].links[?(@.type=='" + format + "')]",
                                    List.class);
                    Map item = (Map) items.get(0);
                    if (defaultFormat.equals(format)) {
                        assertEquals("self", item.get("rel"));
                    } else {
                        assertEquals("alternate", item.get("rel"));
                    }
                });
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/maps/collections?f=html");
        // This may need update if the layout is styled
        assertEquals(getNumberOfLayers(), document.select("#content h2 a[href]").size());
    }

    @Test
    public void testVersionHeader() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/maps/collections?f=html");
        assertTrue(headerHasValue(response, "API-Version", "1.0.1"));
    }

    private int getNumberOfLayers() {
        return getCatalog().getLayers().size();
    }
}
