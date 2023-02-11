/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features.tiled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.features.FeatureService;
import org.geoserver.ogcapi.tiles.TilesLandingPage;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class LandingPageTest extends TiledFeaturesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Features", new Version("1.0.1"));
        assertNotNull(service);
        assertEquals("Features", service.getId());
        assertEquals(new Version("1.0.1"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(FeatureService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getApi",
                        "describeCollection",
                        "getCollections",
                        "getConformanceDeclaration",
                        "getFeature",
                        "getFeatures",
                        "getLandingPage",
                        "getQueryables",
                        "getFunctions",
                        "getTileMatrixSets",
                        "getTileMatrixSet",
                        "describeTileset",
                        "describeTilesets",
                        "getTile",
                        "getTilesMetadata"));
    }

    @Test
    public void testLandingPageExtension() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features", 200);
        // check collection links (just to verify stuff was not wiped out by accident)
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);
        // check we have the tile matrix links (the actual extension)
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/tileMatrixSets.*/)].rel",
                TilesLandingPage.REL_TILING_SCHEMES,
                TilesLandingPage.REL_TILING_SCHEMES,
                TilesLandingPage.REL_TILING_SCHEMES);
        assertEquals("Features 1.0 server", json.read("title"));
    }

    @Test
    public void testLandingPageExtensionHTML() throws Exception {
        Document document = getAsJSoup("ogc/features?f=html");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/tileMatrixSets?f=text%2Fhtml",
                document.select("#tileMatrixSetsLink").attr("href"));
    }
}
