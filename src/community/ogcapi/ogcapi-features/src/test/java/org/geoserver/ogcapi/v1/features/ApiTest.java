/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.test.GeoServerBaseTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends FeaturesTestSupport {

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/features/v1/openapi", 200);
        validateJSONAPI(response);
    }

    @Test
    public void testApiJsonExtension() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/features/v1/openapi.json", 200);
        validateJSONAPI(response);
    }

    private void validateJSONAPI(MockHttpServletResponse response)
            throws UnsupportedEncodingException, JsonProcessingException {
        assertThat(
                response.getContentType(),
                CoreMatchers.startsWith(OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE));
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testHTMLEncoding() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/features/v1?f=text/html", 200);
        assertEquals("text/html", response.getContentType());
        String html = response.getContentAsString();
        GeoServerBaseTestSupport.LOGGER.info(html);
        assertThat(
                html,
                containsString(
                        "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">"));
    }

    @Test
    public void testApiHTML() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/features/v1/openapi?f=text/html", 200);
        assertEquals("text/html", response.getContentType());
        String html = response.getContentAsString();
        LOGGER.info(html);

        // check template expansion worked properly
        assertThat(
                html,
                containsString(
                        "<link rel=\"icon\" type=\"image/png\" href=\"http://localhost:8080/geoserver/swagger-ui/favicon-32x32.png\" sizes=\"32x32\" />"));
        assertThat(
                html,
                containsString(
                        "<link rel=\"icon\" type=\"image/png\" href=\"http://localhost:8080/geoserver/swagger-ui/favicon-16x16.png\" sizes=\"16x16\" />"));
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/swagger-ui/swagger-ui-bundle.js\">"));
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/swagger-ui/swagger-ui-standalone-preset.js\">"));
        assertThat(
                html,
                containsString(
                        "url: \"http://localhost:8080/geoserver/ogc/features/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0"));
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("ogc/features/v1/openapi?f=application/x-yaml");
        validateYAMLApi(yaml);
    }

    @Test
    public void testApiYamlExtension() throws Exception {
        String yaml = getAsString("ogc/features/v1/openapi?f=application/x-yaml");
        validateYAMLApi(yaml);
    }

    private void validateYAMLApi(String yaml) throws JsonProcessingException {
        GeoServerBaseTestSupport.LOGGER.log(Level.INFO, yaml);

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testYamlAsAcceptsHeader() throws Exception {
        MockHttpServletRequest request = createRequest("ogc/features/v1/openapi");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/x-yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith("application/x-yaml"));
        String yaml = string(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        validateApi(api);
    }

    private void validateApi(OpenAPI api) {
        // only one server
        List<Server> servers = api.getServers();
        assertThat(servers, hasSize(1));
        assertThat(
                servers.get(0).getUrl(),
                equalTo("http://localhost:8080/geoserver/ogc/features/v1"));

        // info version is spec version
        assertEquals("1.0.1", api.getInfo().getVersion());

        // paths
        Paths paths = api.getPaths();

        // ... landing page
        PathItem landing = paths.get("/");
        assertNotNull(landing);
        assertThat(landing.getGet().getOperationId(), equalTo("getLandingPage"));

        // ... conformance
        PathItem conformance = paths.get("/conformance");
        assertNotNull(conformance);
        assertThat(conformance.getGet().getOperationId(), equalTo("getConformanceDeclaration"));

        // ... collections
        PathItem collections = paths.get("/collections");
        assertNotNull(collections);
        assertThat(collections.getGet().getOperationId(), equalTo("getCollections"));

        // ... collection
        PathItem collection = paths.get("/collections/{collectionId}");
        assertNotNull(collection);
        assertThat(collection.getGet().getOperationId(), equalTo("describeCollection"));

        // ... features
        PathItem items = paths.get("/collections/{collectionId}/items");
        assertNotNull(items);
        Operation itemsGet = items.getGet();
        assertThat(itemsGet.getOperationId(), equalTo("getFeatures"));
        List<Parameter> parameters = itemsGet.getParameters();
        List<String> itemGetParamNames =
                parameters.stream().map(p -> p.get$ref()).collect(Collectors.toList());
        assertThat(
                itemGetParamNames,
                containsInAnyOrder(
                        "#/components/parameters/collectionId",
                        "#/components/parameters/limit",
                        "#/components/parameters/bbox",
                        "#/components/parameters/datetime",
                        "#/components/parameters/filter",
                        "#/components/parameters/filter-lang",
                        "#/components/parameters/filter-crs",
                        "#/components/parameters/sortby",
                        "#/components/parameters/crs",
                        "#/components/parameters/bbox-crs",
                        "#/components/parameters/otherParameters",
                        "#/components/parameters/ids"));

        // filter languages
        Parameter langs = api.getComponents().getParameters().get("filter-lang");
        assertEquals(
                langs.getSchema().getEnum(), new ArrayList<>(APIFilterParser.SUPPORTED_ENCODINGS));

        // ... feature
        PathItem item = paths.get("/collections/{collectionId}/items/{featureId}");
        assertNotNull(item);
        assertThat(item.getGet().getOperationId(), equalTo("getFeature"));

        // check collectionId parameter
        Map<String, Parameter> params = api.getComponents().getParameters();
        Parameter collectionId = params.get("collectionId");
        @SuppressWarnings("unchecked")
        List<String> collectionIdValues = collectionId.getSchema().getEnum();
        List<String> expectedCollectionIds =
                getCatalog().getFeatureTypes().stream()
                        .map(ft -> ft.prefixedName())
                        .collect(Collectors.toList());
        assertThat(collectionIdValues, equalTo(expectedCollectionIds));

        // check the limit parameter
        Parameter limit = params.get("limit");
        Schema limitSchema = limit.getSchema();
        Assert.assertEquals(BigDecimal.valueOf(1), limitSchema.getMinimum());
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        Assert.assertEquals(wfs.getMaxFeatures(), limitSchema.getMaximum().intValue());
        assertEquals(wfs.getMaxFeatures(), ((Number) limitSchema.getDefault()).intValue());
    }

    @Test
    public void testWorkspaceQualifiedAPI() throws Exception {
        MockHttpServletRequest request = createRequest("cdf/ogc/features/v1/openapi");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/x-yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertEquals("application/x-yaml", response.getContentType());
        String yaml = string(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        // System.out.println(yaml);

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        Map<String, Parameter> params = api.getComponents().getParameters();
        Parameter collectionId = params.get("collectionId");
        @SuppressWarnings("unchecked")
        List<String> collectionIdValues = collectionId.getSchema().getEnum();
        List<String> expectedCollectionIds =
                getCatalog().getFeatureTypesByNamespace(getCatalog().getNamespaceByPrefix("cdf"))
                        .stream()
                        .map(ft -> ft.getName())
                        .collect(Collectors.toList());
        assertThat(collectionIdValues, equalTo(expectedCollectionIds));
    }
}
