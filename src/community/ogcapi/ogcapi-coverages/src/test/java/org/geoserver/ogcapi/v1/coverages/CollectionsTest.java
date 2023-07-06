/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.minidev.json.JSONArray;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.WCSInfo;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsTest extends CoveragesTestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1/collections", 200);
        testCollectionsJson(json);
    }

    @SuppressWarnings("unchecked") // generics varargs creation by hamcrest
    private void testCollectionsJson(DocumentContext json) throws Exception {
        int expected = getCatalog().getCoverages().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel"
        Collection<MediaType> formats =
                GeoServerExtensions.bean(APIDispatcher.class, applicationContext)
                        .getProducibleMediaTypes(CoveragesResponse.class, false);
        assertThat(
                formats.size(),
                lessThanOrEqualTo(json.read("collections[0].links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check rel.
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals(CollectionDocument.REL_COVERAGE, item.get("rel"));
        }

        // check the global crs list
        List<String> crs = json.read("crs");
        assertThat(
                crs.size(),
                Matchers.greaterThan(
                        5000)); // lots... the list is growing, hopefully will stay above 5k
        assertThat(
                crs,
                hasItems(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/4326",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/IAU/0/1000"));
        crs.remove("http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        for (String c : crs) {
            assertTrue(
                    c + " is not using the expect CRS URI format",
                    c.matches("http://www.opengis.net/def/crs/[\\w]+/\\d+/\\d+"));
        }

        // check a coverage with default CRSs
        assertThat(
                json.read("collections[?(@.id=='rs:BlueMarble')].crs"),
                contains(
                        Arrays.asList(
                                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                                "http://www.opengis.net/def/crs/EPSG/0/4326")));

        // a SRS list that has been customized in #onSetup
        // assignign to a variable makes the library return a List rather than a JSONContext
        List<String> rsDemCRSList = json.read("collections[?(@.id=='rs:DEM')].crs");
        assertThat(
                rsDemCRSList,
                contains(
                        Arrays.asList(
                                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                                "http://www.opengis.net/def/crs/EPSG/0/4326",
                                "http://www.opengis.net/def/crs/EPSG/0/3857",
                                "http://www.opengis.net/def/crs/EPSG/0/3003")));
    }

    @Test
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("rs/ogc/coverages/v1/collections", 200);
        long expected =
                getCatalog().getCoverages().stream()
                        .filter(ci -> "rs".equals(ci.getStore().getWorkspace().getName()))
                        .count();
        // check the filtering
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // check the workspace prefixes have been removed
        assertThat(json.read("collections[?(@.id=='BlueMarble')]"), not(empty()));
        assertThat(json.read("collections[?(@.id=='rs:BlueMarble')]"), empty());
        // check the url points to a ws qualified url
        final String bmHrefPath =
                "collections[?(@.id=='BlueMarble')].links[?(@.rel=='"
                        + CollectionDocument.REL_COVERAGE
                        + "' && @.type=='image/geotiff')].href";
        assertEquals(
                "http://localhost:8080/geoserver/rs/ogc/coverages/v1/collections/BlueMarble/coverage?f=image%2Fgeotiff",
                ((JSONArray) json.read(bmHrefPath)).get(0));
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/coverages/v1/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json);
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/coverages/v1/collections?f=html");

        // check collection links
        List<CoverageInfo> coverages = getCatalog().getCoverages();
        for (CoverageInfo coverage : coverages) {
            String encodedName = coverage.prefixedName().replace(":", "__");
            assertNotNull(document.select("#html_" + encodedName + "_link"));
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/coverages/v1/collections/"
                            + coverage.prefixedName()
                            + "/coverage?f=image%2Fgeotiff",
                    document.select("#html_" + encodedName + "_link").attr("href"));
        }

        // go and check a specific collection title and description
        CoverageInfo coverage = getCatalog().getCoverageByName("rs:DEM");
        String tazDemName = coverage.prefixedName();
        String tazDemHtmlId = tazDemName.replace(":", "__");
        assertEquals(TAZDEM_TITLE, document.select("#" + tazDemHtmlId + "_title").text());
        assertEquals(
                TAZDEM_DESCRIPTION, document.select("#" + tazDemHtmlId + "_description").text());
    }

    @Test
    public void testCollectionsHTMLWithProxyBase() throws Exception {
        GeoServer gs = getGeoServer();
        GeoServerInfo info = gs.getGlobal();
        SettingsInfo settings = info.getSettings();
        settings.setProxyBaseUrl("http://testHost/geoserver");
        gs.save(info);
        try {
            org.jsoup.nodes.Document document = getAsJSoup("ogc/coverages/v1/collections?f=html");

            // check collection links
            List<CoverageInfo> coverages = getCatalog().getCoverages();
            for (CoverageInfo coverage : coverages) {
                String encodedName = coverage.prefixedName().replace(":", "__");
                assertNotNull(document.select("#html_" + encodedName + "_link"));
                assertEquals(
                        "http://testHost/geoserver/ogc/coverages/v1/collections/"
                                + coverage.prefixedName()
                                + "/coverage?f=image%2Fgeotiff",
                        document.select("#html_" + encodedName + "_link").attr("href"));
            }
        } finally {
            info = gs.getGlobal();
            settings = info.getSettings();
            settings.setProxyBaseUrl(null);
            gs.save(info);
        }
    }

    @Test
    public void testCollectionsHTMLWithProxyBaseHeader() throws Exception {
        GeoServer gs = getGeoServer();
        GeoServerInfo info = gs.getGlobal();
        SettingsInfo settings = info.getSettings();
        settings.setProxyBaseUrl("${X-Forwarded-Proto}://test-headers/geoserver/");
        info.getSettings().setUseHeadersProxyURL(true);
        gs.save(info);
        try {
            MockHttpServletRequest request = createRequest("ogc/coverages/v1/collections?f=html");
            request.setMethod("GET");
            request.setContent(new byte[] {});
            request.addHeader("X-Forwarded-Proto", "http");
            MockHttpServletResponse response = dispatch(request, null);
            assertEquals(200, response.getStatus());
            assertEquals("text/html", response.getContentType());
            LOGGER.log(Level.INFO, "Last request returned\n:" + response.getContentAsString());

            // parse the HTML
            org.jsoup.nodes.Document document = Jsoup.parse(response.getContentAsString());

            // check collection links
            List<CoverageInfo> coverages = getCatalog().getCoverages();
            for (CoverageInfo coverage : coverages) {
                String encodedName = coverage.prefixedName().replace(":", "__");
                assertNotNull(document.select("#html_" + encodedName + "_link"));
                assertEquals(
                        "http://test-headers/geoserver/ogc/coverages/v1/collections/"
                                + coverage.prefixedName()
                                + "/coverage?f=image%2Fgeotiff",
                        document.select("#html_" + encodedName + "_link").attr("href"));
            }
        } finally {
            info = gs.getGlobal();
            settings = info.getSettings();
            settings.setProxyBaseUrl(null);
            info.getSettings().setUseHeadersProxyURL(null);
            gs.save(info);
        }
    }

    @Test
    public void testCustomCRSList() throws Exception {
        GeoServer gs = getGeoServer();
        WCSInfo wcs = gs.getService(WCSInfo.class);
        List<String> srs = wcs.getSRS();
        srs.add("3857");
        srs.add("32632");
        try {
            gs.save(wcs);

            // check the global CRS list changed
            DocumentContext json = getAsJSONPath("cdf/ogc/coverages/v1/collections", 200);
            List<String> crs = json.read("crs");
            assertThat(
                    crs,
                    contains(
                            "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                            "http://www.opengis.net/def/crs/EPSG/0/3857",
                            "http://www.opengis.net/def/crs/EPSG/0/32632"));
        } finally {
            wcs.getSRS().clear();
            gs.save(wcs);
        }
    }
}
