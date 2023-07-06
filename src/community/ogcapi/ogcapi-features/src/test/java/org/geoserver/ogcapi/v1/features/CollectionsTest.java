/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minidev.json.JSONArray;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSInfo;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CollectionsTest extends FeaturesTestSupport {

    public static final String BASIC_POLYGONS_TITLE = "Basic polygons";
    public static final String BASIC_POLYGONS_DESCRIPTION = "I love basic polygons!";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // customize metadata and set custom CRS too
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setTitle(BASIC_POLYGONS_TITLE);
        basicPolygons.setAbstract(BASIC_POLYGONS_DESCRIPTION);
        basicPolygons.setOverridingServiceSRS(true);
        basicPolygons.getResponseSRS().addAll(Arrays.asList("3857", "32632"));
        getCatalog().save(basicPolygons);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections", 200);
        testCollectionsJson(json);
    }

    @SuppressWarnings("unchecked") // generic varargs in matcher
    private void testCollectionsJson(DocumentContext json) throws Exception {
        int expected = getCatalog().getFeatureTypes().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel"
        // relation
        Collection<MediaType> formats =
                GeoServerExtensions.bean(APIDispatcher.class, applicationContext)
                        .getProducibleMediaTypes(FeaturesResponse.class, true);
        assertThat(
                formats.size(),
                lessThanOrEqualTo((int) json.read("collections[0].links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("items", item.get("rel"));
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

        // check that collections point back to the global CRS list
        List<String> collectionCRS = json.read("collections[?(@.id == 'cite:Bridges')].crs");
        assertThat(collectionCRS, contains(Arrays.asList("#/crs")));
        // check one that is specific instead
        assertThat(
                json.read("collections[?(@.id=='cite:BasicPolygons')].crs"),
                contains(
                        Arrays.asList(
                                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                                "http://www.opengis.net/def/crs/EPSG/0/3857",
                                "http://www.opengis.net/def/crs/EPSG/0/32632")));
    }

    @Test
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/ogc/features/v1/collections", 200);
        long expected =
                getCatalog().getFeatureTypes().stream()
                        .filter(ft -> "cdf".equals(ft.getStore().getWorkspace().getName()))
                        .count();
        // check the filtering
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // check the workspace prefixes have been removed
        assertThat(json.read("collections[?(@.id=='Deletes')]"), not(empty()));
        assertThat(json.read("collections[?(@.id=='cdf__Deletes')]"), empty());
        // check the url points to a ws qualified url
        final String deleteHrefPath =
                "collections[?(@.id=='Deletes')].links[?(@.rel=='items' && @.type=='application/geo+json')].href";
        assertEquals(
                "http://localhost:8080/geoserver/cdf/ogc/features/v1/collections/Deletes/items?f=application%2Fgeo%2Bjson",
                ((JSONArray) json.read(deleteHrefPath)).get(0));
    }

    @Test
    @Ignore
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("ogc/features/v1/collections?f=application/xml");
        print(dom);
        // TODO: add actual tests
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/v1/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json);
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/v1/collections?f=html");

        // check collection links
        List<FeatureTypeInfo> featureTypes = getCatalog().getFeatureTypes();
        for (FeatureTypeInfo featureType : featureTypes) {
            String encodedName = featureType.prefixedName().replace(":", "__");
            assertNotNull(document.select("#html_" + encodedName + "_link"));
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                            + featureType.prefixedName()
                            + "/items?f=text%2Fhtml&limit=50",
                    document.select("#html_" + encodedName + "_link").attr("href"));
        }

        // go and check a specific collection title and description
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        String basicPolygonsName = basicPolygons.prefixedName();
        String basicPolygonsHtmlId = basicPolygonsName.replace(":", "__");
        assertEquals(
                BASIC_POLYGONS_TITLE, document.select("#" + basicPolygonsHtmlId + "_title").text());
        assertEquals(
                BASIC_POLYGONS_DESCRIPTION,
                document.select("#" + basicPolygonsHtmlId + "_description").text());
        assertEquals(
                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                document.select("#" + basicPolygonsHtmlId + "_storageCrs").text());
    }

    @Test
    public void testCollectionsHTMLWithProxyBase() throws Exception {
        GeoServer gs = getGeoServer();
        GeoServerInfo info = gs.getGlobal();
        SettingsInfo settings = info.getSettings();
        settings.setProxyBaseUrl("http://testHost/geoserver");
        gs.save(info);
        try {
            org.jsoup.nodes.Document document = getAsJSoup("ogc/features/v1/collections?f=html");

            // check collection links
            List<FeatureTypeInfo> featureTypes = getCatalog().getFeatureTypes();
            for (FeatureTypeInfo featureType : featureTypes) {
                String encodedName = featureType.prefixedName().replace(":", "__");
                assertNotNull(document.select("#html_" + encodedName + "_link"));
                assertEquals(
                        "http://testHost/geoserver/ogc/features/v1/collections/"
                                + featureType.prefixedName()
                                + "/items?f=text%2Fhtml&limit=50",
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
            MockHttpServletRequest request = createRequest("ogc/features/v1/collections?f=html");
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
            List<FeatureTypeInfo> featureTypes = getCatalog().getFeatureTypes();
            for (FeatureTypeInfo featureType : featureTypes) {
                String encodedName = featureType.prefixedName().replace(":", "__");
                assertNotNull(document.select("#html_" + encodedName + "_link"));
                assertEquals(
                        "http://test-headers/geoserver/ogc/features/v1/collections/"
                                + featureType.prefixedName()
                                + "/items?f=text%2Fhtml&limit=50",
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
        WFSInfo wfs = gs.getService(WFSInfo.class);
        List<String> srs = wfs.getSRS();
        srs.add("3857");
        srs.add("32632");
        try {
            gs.save(wfs);

            // check the global CRS list changed
            DocumentContext json = getAsJSONPath("cdf/ogc/features/v1/collections", 200);
            List<String> crs = json.read("crs");
            assertThat(
                    crs,
                    contains(
                            "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                            "http://www.opengis.net/def/crs/EPSG/0/3857",
                            "http://www.opengis.net/def/crs/EPSG/0/32632"));
        } finally {
            wfs.getSRS().clear();
            gs.save(wfs);
        }
    }

    @Test
    public void testCustomLinks() throws Exception {
        GeoServerInfo gsi = getGeoServer().getGlobal();
        LinkInfoImpl link1 =
                new LinkInfoImpl(
                        "enclosure",
                        "application/geopackage+sqlite3",
                        "http://example.com/fullDataset.gpkg");
        LinkInfoImpl link2 = new LinkInfoImpl("license", "text/html", "http://example.com/license");
        link2.setService("Coverages");
        ArrayList<LinkInfo> links =
                Stream.of(link1, link2).collect(Collectors.toCollection(ArrayList::new));
        gsi.getSettings().getMetadata().put(LinkInfo.LINKS_METADATA_KEY, links);
        getGeoServer().save(gsi);

        DocumentContext json = getAsJSONPath("cite/ogc/features/v1/collections/", 200);

        // check first link
        DocumentContext l1c = readSingleContext(json, "$.links[?(@.rel=='enclosure')]");
        assertEquals(link1.getHref(), l1c.read("href"));
        assertEquals(link1.getType(), l1c.read("type"));

        // second link should not be there, service does not match
        List l2List = json.read("$.links[?(@.rel=='rasterized')]", List.class);
        assertTrue(l2List.isEmpty());
    }
}
