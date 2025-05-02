/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import io.swagger.v3.oas.models.servers.Server;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.test.GeoServerBaseTestSupport;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends OGCApiTestSupport {

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/processes/v1/openapi", 200);
        validateJSONAPI(response);
    }

    private void validateJSONAPI(MockHttpServletResponse response)
            throws UnsupportedEncodingException, JsonProcessingException {
        assertThat(
                response.getContentType(), CoreMatchers.startsWith(OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE));
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);
        validateApi(api);
    }

    @Test
    public void testHTMLEncoding() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/processes/v1?f=text/html", 200);
        assertEquals("text/html", response.getContentType());
        String html = response.getContentAsString();
        GeoServerBaseTestSupport.LOGGER.info(html);
        assertThat(html, containsString("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">"));
    }

    @Test
    public void testApiHTML() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/processes/v1/openapi?f=text/html", 200);
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
                                + "\"http://localhost:8080/geoserver/ogc/processes/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0\"/>"));
        assertThat(html, not(containsString("<script>")));
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("ogc/processes/v1/openapi?f=application/yaml");
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
        MockHttpServletRequest request = createRequest("ogc/processes/v1/openapi");
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
        assertThat(servers.get(0).getUrl(), equalTo("http://localhost:8080/geoserver/ogc/processes/v1"));

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

        // ... processes
        PathItem processes = paths.get("/processes");
        assertNotNull(processes);
        assertThat(processes.getGet().getOperationId(), equalTo("getProcesses"));

        // ... process
        PathItem process = paths.get("/processes/{processID}");
        assertNotNull(process);
        assertThat(process.getGet().getOperationId(), equalTo("getProcessDescription"));

        // ... process execution
        PathItem execution = paths.get("/processes/{processID}/execution");
        assertNotNull(execution);
        Operation executePost = execution.getPost();
        assertThat(executePost.getOperationId(), equalTo("execute"));
        // TODO: execute GET too

        // ... job listing
        PathItem jobs = paths.get("/jobs");
        assertNotNull(jobs);
        assertThat(jobs.getGet().getOperationId(), equalTo("getJobs"));

        // ... single job handling (get and dismiss)
        PathItem job = paths.get("/jobs/{jobId}");
        assertNotNull(job);
        assertThat(job.getGet().getOperationId(), equalTo("getStatus"));
        assertThat(job.getDelete().getOperationId(), equalTo("dismiss"));

        // ... asynch execution results
        PathItem results = paths.get("/jobs/{jobId}/results");
        assertNotNull(results);
        assertThat(results.getGet().getOperationId(), equalTo("getResult"));
    }
}
