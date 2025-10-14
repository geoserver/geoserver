/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends StylesTestSupport {

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/styles/v1/openapi", 200);
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
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/styles/v1/openapi?f=text/html", 200);
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
                                + "\"http://localhost:8080/geoserver/ogc/styles/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0\"/>"));
        assertThat(html, not(containsString("<script>")));
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("ogc/styles/v1/openapi?f=application/yaml");
        LOGGER.log(Level.INFO, yaml);

        ObjectMapper mapper = Yaml.mapper();
        OpenAPI api = mapper.readValue(yaml, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testYamlAsAcceptsHeader() throws Exception {
        MockHttpServletRequest request = createRequest("ogc/styles/v1/openapi");
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

    @SuppressWarnings("unchecked") // matcher vararg generics
    private void validateApi(OpenAPI api) {
        // only one server
        List<Server> servers = api.getServers();
        assertThat(servers, hasSize(1));
        assertThat(servers.get(0).getUrl(), equalTo("http://localhost:8080/geoserver/ogc/styles/v1"));

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

        // ... styles
        PathItem collections = paths.get("/styles");
        assertNotNull(collections);
        assertThat(collections.getGet().getOperationId(), equalTo("getStyleSet"));

        // ... style
        PathItem collection = paths.get("/styles/{styleId}");
        assertNotNull(collection);
        assertThat(collection.getGet().getOperationId(), equalTo("getStyle"));

        // check the styleId parameter contains actual style names from this server
        Parameter styleId = api.getComponents().getParameters().get("styleId");
        assertThat(
                (List<String>) styleId.getSchema().getEnum(),
                containsInAnyOrder(
                        "BasicStyleGroupStyle",
                        "cssSample",
                        "PolygonComment",
                        "ws:NamedPlacesWS",
                        "generic",
                        "polygon",
                        "line",
                        "point",
                        "Streams",
                        "RoadSegments",
                        "Ponds",
                        "NamedPlaces",
                        "MapNeatline",
                        "Lakes",
                        "Forests",
                        "DividedRoutes",
                        "Buildings",
                        "Bridges",
                        "BasicPolygons",
                        "raster",
                        "Default"));
    }

    @Test
    public void testWorkspaceQualifiedAPI() throws Exception {
        OpenAPI api = getOpenAPI("ws/ogc/styles/v1/openapi");
        Map<String, Parameter> params = api.getComponents().getParameters();
        Parameter styleId = params.get("styleId");
        @SuppressWarnings("unchecked")
        List<String> styleIdValues = styleId.getSchema().getEnum();
        List<String> expectedStyleIds = getCatalog().getStyles().stream()
                .filter(s ->
                        s.getWorkspace() == null || "ws".equals(s.getWorkspace().getName()))
                .map(StyleInfo::getName)
                .collect(Collectors.toList());
        // does not work and I cannot fathom why, both lists have the same size and same elements
        // by visual inspection
        // assertThat(styleIdValues, Matchers.containsInAnyOrder(expectedStyleIds));
        Collections.sort(styleIdValues);
        Collections.sort(expectedStyleIds);
        assertEquals(styleIdValues, expectedStyleIds);
    }

    @Test
    public void testWorkspaceQualifiedAPIGlobalOnly() throws Exception {
        // cdf has no local styles, only global ones
        OpenAPI api = getOpenAPI("cdf/ogc/styles/v1/openapi");
        Map<String, Parameter> params = api.getComponents().getParameters();
        Parameter collectionId = params.get("styleId");
        @SuppressWarnings("unchecked")
        List<String> collectionIdValues = collectionId.getSchema().getEnum();
        List<String> expectedStyleIds = getCatalog().getStyles().stream()
                .filter(s -> s.getWorkspace() == null)
                .map(StyleInfo::getName)
                .collect(Collectors.toList());
        assertThat(collectionIdValues, equalTo(expectedStyleIds));
    }

    private OpenAPI getOpenAPI(String path) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertEquals("application/yaml", response.getContentType());
        String yaml =
                string(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        ObjectMapper mapper = Yaml.mapper();
        return mapper.readValue(yaml, OpenAPI.class);
    }
}
