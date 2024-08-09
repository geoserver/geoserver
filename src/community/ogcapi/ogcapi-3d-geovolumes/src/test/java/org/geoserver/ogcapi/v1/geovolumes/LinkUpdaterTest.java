/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LinkUpdaterTest {

    public static final String BASE = "http://localhost:8080/geoserver";
    public static final String PATH = "ogcapi/v1/geovolumes/collections/test";

    @Test
    public void testUpdateRelativeLink() {
        String href = "3dtiles/tileset.json";
        String expected =
                "http://localhost:8080/geoserver/ogcapi/v1/geovolumes/collections/test/3dtiles/tileset.json";
        String actual = LinkUpdater.updateLink(BASE, PATH, href);
        assertEquals(expected, actual);
    }

    @Test
    public void testUpdateAbsoluteLink() {
        String href = "https://www.geoerver.org/3dtiles/tileset.json";
        String expected = href;
        String actual = LinkUpdater.updateLink(BASE, PATH, href);
        assertEquals(expected, actual);
    }

    @Test
    public void testUpdateInvalidLink() {
        String href = "../../foo.bar";
        assertThrows(
                IllegalArgumentException.class, () -> LinkUpdater.updateLink(BASE, PATH, href));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateLinksWithNestedMaps() {
        Map<String, Object> rootMap = new HashMap<>();
        Map<String, Object> level1Map = new HashMap<>();
        Map<String, Object> level2Map = new HashMap<>();

        // Set up nested maps with href values
        rootMap.put("href", "3dtiles/test.json");
        String absoluteURL = "http://example.com/absolute";
        level1Map.put("href", absoluteURL);
        level2Map.put("href", "i3s/level/0");

        rootMap.put("level1", level1Map);
        level1Map.put("level2", level2Map);

        // Call the updateLinks method with the nested map
        LinkUpdater.updateLinks(BASE, PATH, rootMap);

        // Assert that the href values have been updated correctly
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/v1/geovolumes/collections/test/3dtiles/test.json",
                rootMap.get("href"));
        Map<String, Object> level1 = (Map<String, Object>) rootMap.get("level1");
        assertEquals(absoluteURL, level1.get("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/v1/geovolumes/collections/test/i3s/level/0",
                ((Map<String, Object>) level1.get("level2")).get("href"));
    }
}
