/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.ows.URLMangler;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.junit.BeforeClass;
import org.junit.Test;

/** Test for the {@link ProxyBaseExtUrlMangler}. */
public class ProxyBaseExtensionTest {
    private static ProxyBaseExtUrlMangler mangler;

    @BeforeClass
    public static void setup() {
        List<ProxyBaseExtensionRule> manglerRules = new ArrayList<>();
        manglerRules.add(
                new ProxyBaseExtensionRule(
                        "1",
                        "ogc/stac/v1/collections2/",
                        "https://stac.example.com/v1/collections/",
                        true,
                        1));
        manglerRules.add(
                new ProxyBaseExtensionRule(
                        "2",
                        ".*/collections/yourCollection",
                        "https://stac.example.com:8081/v1/collections/${fixedCollection}/items/",
                        true,
                        2));
        mangler = new ProxyBaseExtUrlMangler(manglerRules);
    }

    @Test
    public void testMangleWildCard() throws Exception {
        StringBuilder baseURL = new StringBuilder("http://localhost:8080/geoserver");
        StringBuilder path = new StringBuilder("ogc/stac/v1/collections2/");
        mangler.mangleURL(baseURL, path, Collections.emptyMap(), URLMangler.URLType.SERVICE);
        assertEquals("https://stac.example.com", baseURL.toString());
        assertEquals("/v1/collections/", path.toString());
    }

    @Test
    public void testMangleTemplateLiteralNoHeader() throws Exception {
        StringBuilder baseURL = new StringBuilder("http://localhost:8080/geoserver");
        StringBuilder path = new StringBuilder("ogc/stac/v1/collections/yourCollection");
        mangler.mangleURL(baseURL, path, Collections.emptyMap(), URLMangler.URLType.SERVICE);
        assertEquals(
                "http://localhost:8080/geoserver",
                baseURL.toString()); // no change, because no header
        assertEquals(
                "ogc/stac/v1/collections/yourCollection",
                path.toString()); // no change, because no header
    }

    @Test
    public void testMangleTemplateLiteralHeader() throws Exception {
        StringBuilder baseURL = new StringBuilder("http://localhost:8080/geoserver");
        StringBuilder path = new StringBuilder("ogc/stac/v1/collections/yourCollection");
        String urlString =
                mangler.transformURL(
                        baseURL.toString() + path.toString(), "fixedCollection=myCollection");
        assertEquals("https://stac.example.com:8081/v1/collections/myCollection/items/", urlString);
    }
}
