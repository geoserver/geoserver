/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.api.APIDispatcher;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;

public class CollectionsTest extends ImagesTestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images/collections", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/images/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json, MediaType.parseMediaType("application/x-yaml"));
    }

    private void testCollectionsJson(DocumentContext json, MediaType defaultFormat)
            throws Exception {
        int expected = (int) getStructuredCoverages().count();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats =
                GeoServerExtensions.bean(
                                APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                        .getProducibleMediaTypes(ImagesCollectionsDocument.class, true);
        assertThat(
                formats.size(),
                lessThanOrEqualTo((int) json.read("collections[0].links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check rel
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            if (defaultFormat.equals(format)) {
                assertEquals("self", item.get("rel"));
            } else {
                assertEquals("alternate", item.get("rel"));
            }
        }

        // check one well known collection is there
        assertEquals(1, json.read("collections[?(@.id=='sf:watertemp')]", List.class).size());
        assertEquals(
                WATER_TEMP_TITLE, readSingle(json, "collections[?(@.id=='sf:watertemp')].title"));
        assertEquals(
                WATER_TEMP_DESCRIPTION,
                readSingle(json, "collections[?(@.id=='sf:watertemp')].description"));
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/images/collections?f=html");

        // check collection links
        getStructuredCoverages()
                .map(c -> c.prefixedName())
                .forEach(
                        id -> {;
                            String htmlId = id.replace(":", "__");
                            assertNotNull(document.select("#html_" + htmlId + "_link"));
                            assertEquals(
                                    "http://localhost:8080/geoserver/ogc/images/collections/"
                                            + ResponseUtils.urlEncode(id)
                                            + "?f=text%2Fhtml",
                                    document.select("#html_" + htmlId + "_link").attr("href"));
                        });

        // go and check a specific collection title and description
        CoverageInfo waterTemp = getCatalog().getCoverageByName(getLayerId(WATER_TEMP));
        String waterTempName = waterTemp.prefixedName().replace(":", "__");
        assertEquals(WATER_TEMP_TITLE, document.select("#" + waterTempName + "_title").text());
        assertEquals(
                WATER_TEMP_DESCRIPTION,
                document.select("#" + waterTempName + "_description").text());
    }
}
