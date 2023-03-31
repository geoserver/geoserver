/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Unit tests for ogcapi-maps Styles. */
public class StylesTest extends MapsTestSupport {

    public StylesTest() {}

    @Test
    public void testCollectionsJsonDefault() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles?f=json", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJsonSlash() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/BlueMarble/styles/?f=json", 200);
        testStylesJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/maps/v1/collections/BlueMarble/styles?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testStylesJson(json, MediaType.parseMediaType("application/x-yaml"));
    }

    private void testStylesJson(DocumentContext json, MediaType defaultFormat) {
        assertEquals(1, (int) json.read("styles.length()", Integer.class));
        Collection<MediaType> formats =
                GeoServerExtensions.bean(
                                APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                        .getProducibleMediaTypes(CollectionsDocument.class, true);
        formats.forEach(
                format -> {
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
