/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Unit tests for ogcapi-maps Styles. */
public class StylesTest extends MapsTestSupport {

    public StylesTest() {}

    /** The styles page offers the map of each style as HTML, and the other map formats in the format picker. */
    @Test
    public void testStylesHtmlMapFormats() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/collections/BlueMarble/styles?f=html");

        Element html = document.select(".card-footer a.btn").first();
        assertEquals("HTML", html.text());
        assertEquals(
                "http://localhost:8080/geoserver/ogc/maps/v1/collections/wcs%3ABlueMarble/styles/raster/map?f=text%2Fhtml",
                html.attr("href"));
        List<String> formats = document.select(".card-footer select option").eachText();
        assertThat(formats, hasItem("image/png"));
        assertThat(formats, hasItem("image/jpeg"));
    }

    /** The collection page offers the default map the same way, its HTML link carrying the identifier of the card. */
    @Test
    public void testCollectionHtmlMapFormats() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/collections/cite:Lakes?f=html");

        Element html = document.select("#html_cite__Lakes_link").first();
        assertEquals("HTML", html.text());
        assertEquals(
                "http://localhost:8080/geoserver/ogc/maps/v1/collections/cite:Lakes/map?f=text%2Fhtml",
                html.attr("href"));
        List<String> formats = document.select(".card-footer select option").eachText();
        assertThat(formats, hasItem("image/png"));
        assertThat(formats, hasItem("image/jpeg"));
    }

    @Test
    public void testCollectionsJsonDefault() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles?f=json", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJsonSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles/?f=json", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/maps/v1/collections/BlueMarble/styles?f=application/yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testStylesJson(json, MediaType.parseMediaType("application/yaml"));
    }

    private void testStylesJson(DocumentContext json, MediaType defaultFormat) {
        assertEquals(1, (int) json.read("styles.length()", Integer.class));
        Collection<MediaType> formats = GeoServerExtensions.bean(
                        APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                .getProducibleMediaTypes(CollectionsDocument.class, true);
        formats.forEach(format -> {
            // check rel
            List items = json.read("links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            if (defaultFormat.equals(format)) {
                assertEquals("self", item.get("rel"));
            } else {
                assertEquals("alternate", item.get("rel"));
            }
        });
    }
}
