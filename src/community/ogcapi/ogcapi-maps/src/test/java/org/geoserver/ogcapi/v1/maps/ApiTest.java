/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.SwaggerJSONAPIMessageConverter;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends MapsTestSupport {

    static final String NATURE_GROUP = "nature";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // a layer group is a valid Maps collection and must appear in the collectionId enum
        Catalog catalog = getCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(NATURE_GROUP);
        group.getLayers().add(lakes);
        group.getStyles().add(null);
        new CatalogBuilder(catalog).calculateLayerGroupBounds(group);
        catalog.add(group);
    }

    @Test
    public void testCollectionIdEnumIncludesLayerGroups() throws Exception {
        assertThat(enumOf(readApi(), "collectionId"), hasItems("cite:Lakes", NATURE_GROUP));
    }

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/maps/v1/openapi", 200);
        assertThat(response.getContentType(), startsWith(SwaggerJSONAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE));
        validateApi(Json.mapper().readValue(response.getContentAsString(), OpenAPI.class));
    }

    /** The document identity, the resources the service exposes, and the ones it deliberately does not. */
    private static void validateApi(OpenAPI api) {
        assertNotNull(api);
        assertEquals("Maps 1.0 server", api.getInfo().getTitle());
        assertEquals("1.0.1", api.getInfo().getVersion());
        assertThat(
                api.getPaths().keySet(),
                hasItems(
                        "/",
                        "/conformance",
                        "/collections",
                        "/collections/{collectionId}",
                        "/collections/{collectionId}/styles",
                        "/collections/{collectionId}/map",
                        "/collections/{collectionId}/styles/{styleId}/map",
                        "/collections/{collectionId}/map/info",
                        "/collections/{collectionId}/legend",
                        "/collections/{collectionId}/styles/{styleId}/legend"));
        // dataset maps and tilesets are intentionally out of scope
        assertThat(api.getPaths().keySet(), not(hasItems("/map", "/styles/{styleId}/map", "/map/tiles")));
        assertThat(enumOf(api, "f-map"), hasItems("image/png", "image/jpeg", "image/tiff"));
        assertThat(enumOf(api, "collectionId"), hasItems("cite:Lakes", NATURE_GROUP));
    }

    /**
     * /conf/api-operations/operation-id: the map paths carry the dot separated suffixes the standard assigns to the
     * collection map and the styled collection map, behind an instance prefix that keeps them unique.
     */
    @Test
    public void testMapOperationIdSuffixes() throws Exception {
        OpenAPI api = readApi();
        assertEquals(
                "maps.collection.getMap",
                api.getPaths().get("/collections/{collectionId}/map").getGet().getOperationId());
        assertEquals(
                "maps.collection.style.getMap",
                api.getPaths()
                        .get("/collections/{collectionId}/styles/{styleId}/map")
                        .getGet()
                        .getOperationId());
    }

    @Test
    public void testApiYaml() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/maps/v1/openapi.yaml", 200);
        assertThat(response.getContentType(), startsWith("application/yaml"));
        validateApi(Yaml.mapper().readValue(response.getContentAsString(), OpenAPI.class));
    }

    /** The YAML the format parameter returns is the same document the extension returns. */
    @Test
    public void testApiYamlFormatParameter() throws Exception {
        assertEquals(getAsString("ogc/maps/v1/openapi.yaml"), getAsString("ogc/maps/v1/openapi?f=application/yaml"));
    }

    @Test
    public void testApiHtml() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/maps/v1/openapi?f=text/html", 200);
        assertEquals("text/html", response.getContentType());
        String html = response.getContentAsString();
        // the Swagger UI page is a template, check it expanded against this service and this base URL
        assertThat(
                html,
                containsString("<script src=\"http://localhost:8080/geoserver/swagger-ui/swagger-ui-bundle.js\">"));
        assertThat(html, containsString("<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/api.js\">"));
        assertThat(
                html,
                containsString(
                        "<input type=\"hidden\" id=\"apiLocation\" value="
                                + "\"http://localhost:8080/geoserver/ogc/maps/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0\"/>"));
        // the document is data driven, no inline script may end up in it
        assertThat(html, not(containsString("<script>")));
    }

    @Test
    public void testMapFormats() throws Exception {
        OpenAPI api = readApi();
        // f-map parameter enum and the map 200 response both advertise the standard MIME subset. SVG is left out: its
        // conformance class follows the WMS SVG renderer, and the default streaming one is not conformant
        assertThat(enumOf(api, "f-map"), hasItems("image/png", "image/jpeg", "image/tiff"));
        assertThat(map200Formats(api), hasItems("image/png", "image/jpeg", "image/tiff"));
        assertThat(enumOf(api, "f-map"), not(hasItem("image/svg+xml")));
        // the whole WMS catalog must not leak in
        assertThat(enumOf(api, "f-map"), not(hasItems("application/pdf", "image/gif", "application/json")));
    }

    /** SVG becomes an offered encoding once the service is configured with the conformant Batik renderer. */
    @Test
    public void testSvgOfferedWithBatikRenderer() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put("svgRenderer", WMS.SVG_BATIK);
        gs.save(wms);
        try {
            OpenAPI api = readApi();
            assertThat(enumOf(api, "f-map"), hasItem("image/svg+xml"));
            assertThat(map200Formats(api), hasItem("image/svg+xml"));
        } finally {
            wms.getMetadata().remove("svgRenderer");
            gs.save(wms);
        }
    }

    @Test
    public void testTiffAndSvgDroppedWhenDisabled() throws Exception {
        withConformance(
                MapsConformance::setTiff,
                false,
                () -> withConformance(MapsConformance::setSvg, false, () -> {
                    OpenAPI api = readApi();
                    assertThat(enumOf(api, "f-map"), hasItems("image/png", "image/jpeg"));
                    assertThat(enumOf(api, "f-map"), not(hasItems("image/tiff", "image/svg+xml")));
                    assertThat(map200Formats(api), not(hasItems("image/tiff", "image/svg+xml")));
                }));
    }

    @Test
    public void testMapFormatsHonorWmsConfiguration() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setGetMapMimeTypeCheckingEnabled(true);
        wms.getGetMapMimeTypes().add("image/png");
        gs.save(wms);
        try {
            List<String> formats = enumOf(readApi(), "f-map");
            assertThat(formats, hasItems("image/png"));
            assertThat(formats, not(hasItems("image/jpeg", "image/tiff", "image/svg+xml")));
        } finally {
            wms.setGetMapMimeTypeCheckingEnabled(false);
            wms.getGetMapMimeTypes().clear();
            gs.save(wms);
        }
    }

    @Test
    public void testWidthHeightKeptWhenScalingOffButSubsettingOn() throws Exception {
        // spatial subsetting provides width/height when scaling is off, so the doc must keep them
        withConformance(MapsConformance::setScaling, false, () -> {
            List<String> params = mapParamRefs(readApi());
            assertThat(params, hasItems("#/components/parameters/width", "#/components/parameters/height"));
            assertThat(params, not(hasItems("#/components/parameters/scale-denominator")));
        });
    }

    @Test
    public void testWidthHeightDroppedWhenScalingAndSubsettingOff() throws Exception {
        withConformance(
                MapsConformance::setScaling,
                false,
                () -> withConformance(MapsConformance::setSpatialSubsetting, false, () -> {
                    List<String> params = mapParamRefs(readApi());
                    assertThat(
                            params, not(hasItems("#/components/parameters/width", "#/components/parameters/height")));
                }));
    }

    private static List<String> mapParamRefs(OpenAPI api) {
        return paramRefs(api, "/collections/{collectionId}/map");
    }

    private static List<String> paramRefs(OpenAPI api, String path) {
        return api.getPaths().get(path).getGet().getParameters().stream()
                .map(p -> p.get$ref())
                .toList();
    }

    @Test
    public void testInfoFormats() throws Exception {
        OpenAPI api = readApi();
        assertThat(enumOf(api, "f-info"), hasItems("application/json", "text/html"));
    }

    @Test
    public void testInfoFormatsHonorWmsConfiguration() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(true);
        wms.getGetFeatureInfoMimeTypes().add("application/json");
        gs.save(wms);
        try {
            List<String> formats = enumOf(readApi(), "f-info");
            assertThat(formats, hasItems("application/json"));
            // text/html is a WMS GetFeatureInfo format now disallowed, so it must be dropped
            assertThat(formats, not(hasItems("text/html")));
        } finally {
            wms.setGetFeatureInfoMimeTypeCheckingEnabled(false);
            wms.getGetFeatureInfoMimeTypes().clear();
            gs.save(wms);
        }
    }

    private OpenAPI readApi() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/maps/v1/openapi", 200);
        return Json.mapper().readValue(response.getContentAsString(), OpenAPI.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> enumOf(OpenAPI api, String parameter) {
        return api.getComponents().getParameters().get(parameter).getSchema().getEnum();
    }

    private static Set<String> map200Formats(OpenAPI api) {
        return api.getPaths()
                .get("/collections/{collectionId}/map")
                .getGet()
                .getResponses()
                .get("200")
                .getContent()
                .keySet();
    }

    @Test
    public void testFeatureInfoPathRemovedWhenDisabled() throws Exception {
        withConformance(MapsConformance::setFeatureInfo, false, () -> {
            OpenAPI api = readApi();
            assertThat(api.getPaths().keySet(), not(hasItems("/collections/{collectionId}/map/info")));
        });
    }

    @Test
    public void testFilterParametersDeclared() throws Exception {
        OpenAPI api = readApi();
        assertThat(api.getPaths().keySet(), hasItems("/collections/{collectionId}/queryables"));
        // the filter parameters are on both the map and the info resources
        assertThat(
                mapParamRefs(api),
                hasItems(
                        "#/components/parameters/filter",
                        "#/components/parameters/filter-lang",
                        "#/components/parameters/filter-crs"));
        assertThat(paramRefs(api, "/collections/{collectionId}/map/info"), hasItems("#/components/parameters/filter"));
        // filter-lang carries the enum and the default, both required by the filter conformance class
        Schema<?> lang = api.getComponents().getParameters().get("filter-lang").getSchema();
        assertEquals(List.of("cql2-text", "cql2-json", "ecql-text"), lang.getEnum());
        assertEquals("cql2-text", lang.getDefault());
    }

    @Test
    public void testFilterParametersDroppedWhenDisabled() throws Exception {
        withConformance(MapsConformance::setMapFilter, false, () -> {
            OpenAPI api = readApi();
            assertThat(
                    mapParamRefs(api),
                    not(hasItems(
                            "#/components/parameters/filter",
                            "#/components/parameters/filter-lang",
                            "#/components/parameters/filter-crs")));
            assertThat(paramRefs(api, "/collections/{collectionId}/map/info"), not(hasItems("filter")));
            // the definitions are gone too, nothing is left declaring the parameters
            assertThat(
                    api.getComponents().getParameters().keySet(), not(hasItems("filter", "filter-lang", "filter-crs")));
            // the queryables only describe what the filter accepts, so the resource goes with it
            assertThat(api.getPaths().keySet(), not(hasItems("/collections/{collectionId}/queryables")));
        });
    }

    @Test
    public void testQueryablesPathRemovedWhenDisabled() throws Exception {
        withConformance(
                MapsConformance::setQueryables,
                false,
                () -> assertThat(
                        readApi().getPaths().keySet(), not(hasItems("/collections/{collectionId}/queryables"))));
    }

    @Test
    public void testLegendPathRemovedWhenDisabled() throws Exception {
        withConformance(MapsConformance::setLegend, false, () -> {
            assertThat(
                    readApi().getPaths().keySet(),
                    not(hasItems(
                            "/collections/{collectionId}/legend",
                            "/collections/{collectionId}/styles/{styleId}/legend")));
        });
    }
}
