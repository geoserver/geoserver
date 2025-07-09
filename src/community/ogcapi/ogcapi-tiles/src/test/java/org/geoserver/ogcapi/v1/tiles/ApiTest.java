/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.gwc.GWC;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends TilesTestSupport {

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/tiles/v1/openapi", 200);
        assertThat(
                response.getContentType(), CoreMatchers.startsWith(OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE));
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testApiHTML() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/tiles/v1/openapi?f=text/html", 200);
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
                containsString("<script src=\"http://localhost:8080/geoserver/swagger-ui/swagger-ui-bundle.js\">"));
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/swagger-ui/swagger-ui-standalone-preset.js\">"));
        assertThat(html, containsString("<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/api.js\">"));
        assertThat(
                html,
                containsString(
                        "<input type=\"hidden\" id=\"apiLocation\" value="
                                + "\"http://localhost:8080/geoserver/ogc/tiles/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0\"/>"));
        assertThat(html, not(containsString("<script>")));
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("ogc/tiles/v1/openapi?f=application/yaml");
        LOGGER.log(Level.INFO, yaml);

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testYamlAsAcceptsHeader() throws Exception {
        MockHttpServletRequest request = createRequest("ogc/tiles/v1/openapi");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith("application/yaml"));
        String yaml =
                string(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        validateApi(api);
    }

    private void validateApi(OpenAPI api) {
        // only one server
        List<Server> servers = api.getServers();
        assertThat(servers, hasSize(1));
        assertThat(servers.get(0).getUrl(), equalTo("http://localhost:8080/geoserver/ogc/tiles/v1"));

        // info version is spec version
        assertEquals("1.0.0", api.getInfo().getVersion());

        // paths
        Paths paths = api.getPaths();

        // ... landing page
        PathItem landing = paths.get("/");
        assertNotNull(landing);
        assertThat(landing.getGet().getOperationId(), equalTo("getLandingPage"));

        // ... conformance
        PathItem conformance = paths.get("/conformance");
        assertNotNull(conformance);
        assertThat(conformance.getGet().getOperationId(), equalTo("getConformanceClasses"));

        // ... collections
        PathItem collections = paths.get("/collections");
        assertNotNull(collections);
        assertThat(collections.getGet().getOperationId(), equalTo("getCollections"));

        // ... style
        PathItem collection = paths.get("/collections/{collectionId}");
        assertNotNull(collection);
        assertThat(collection.getGet().getOperationId(), equalTo("describeCollection"));

        // check the collectionId parameter contains actual collection names from this server
        Parameter collectionId = api.getComponents().getParameters().get("collectionId");
        List<String> expectedCollectionIds = Streams.stream(
                        applicationContext.getBean(GWC.class).getTileLayers())
                .map(tl -> tl.getName())
                .collect(Collectors.toList());
        assertThat(collectionId.getSchema().getEnum(), equalTo(expectedCollectionIds));

        // check the mapCollectionId parameter contains (some) collection names that have raster formats
        Parameter mapCollectionId = api.getComponents().getParameters().get("mapCollectionId");
        List<String> expectedMapCollectionIds = getCollectionsForMimeType("image/");
        assertThat(mapCollectionId.getSchema().getEnum(), equalTo(expectedMapCollectionIds));

        // check the vectorCollectionId parameter contains (some) collection names that have vector formats
        Parameter vectorCollectionId = api.getComponents().getParameters().get("vectorCollectionId");
        List<String> expectedVectorCollectionIds = getCollectionsForMimeType("application/vnd.mapbox-vector");
        assertThat(vectorCollectionId.getSchema().getEnum(), equalTo(expectedVectorCollectionIds));
    }

    private static List<String> getCollectionsForMimeType(String prefix) {
        return Streams.stream(applicationContext.getBean(GWC.class).getTileLayers())
                .filter(tl ->
                        tl.getMimeTypes().stream().anyMatch(m -> m.getMimeType().startsWith(prefix)))
                .map(tl -> tl.getName())
                .collect(Collectors.toList());
    }

    @Test
    @SuppressWarnings("unchecked") // getSchema().getEnum() not fully generified
    public void testWorkspaceQualifiedAPI() throws Exception {
        MockHttpServletRequest request = createRequest("cdf/ogc/tiles/v1/openapi");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertEquals("application/yaml", response.getContentType());
        String yaml =
                string(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        Map<String, Parameter> params = api.getComponents().getParameters();
        Parameter collectionId = params.get("collectionId");
        List<String> collectionIdValues = collectionId.getSchema().getEnum();
        assertThat(
                collectionIdValues,
                containsInAnyOrder("Other", "Inserts", "Nulls", "Fifteen", "Locks", "Seven", "Updates", "Deletes"));
    }
}
