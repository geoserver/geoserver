/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.jayway.jsonpath.DocumentContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ogcapi.APIException;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

public class ManageStylesTest extends StylesTestSupport {

    private static final String POLYGON = "ogc/styles/v1/styles/polygon";
    private static final String POLYGON_METADATA = POLYGON + "/metadata";

    private static final List<String> READ_CLASSES = List.of(
            StylesService.CORE,
            StylesService.HTML,
            StylesService.JSON,
            StylesService.MAPBOX,
            StylesService.SLD10,
            StylesService.SLD11);

    @Before
    @After
    public void resetManageStyles() {
        setManageStyles(null);
    }

    @Test
    public void testManageStylesDefault() throws Exception {
        int styleCount = getCatalog().getStyles().size();
        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        String sld = IOUtils.toString(getClass().getResourceAsStream("simplePoint.sld"), "UTF-8");
        String metadata = IOUtils.toString(getClass().getResourceAsStream("polygonStyleMetadata.json"), "UTF-8");
        String patch = IOUtils.toString(getClass().getResourceAsStream("metadataPatch.json"), "UTF-8");

        assertNotEnabled(postAsServletResponse("ogc/styles/v1/styles", sld, SLDHandler.MIMETYPE_10));
        assertNotEnabled(putAsServletResponse(POLYGON, sld, SLDHandler.MIMETYPE_10));
        assertNotEnabled(deleteAsServletResponse(POLYGON));
        assertNotEnabled(putAsServletResponse(POLYGON_METADATA, metadata, MediaType.APPLICATION_JSON_VALUE));
        assertNotEnabled(patchAsServletResponse(POLYGON_METADATA, patch, MediaType.APPLICATION_JSON_VALUE));

        // the catalog is exactly as before
        assertEquals(styleCount, getCatalog().getStyles().size());
        StyleInfo current = getCatalog().getStyleByName("polygon");
        assertEquals(polygon.getFilename(), current.getFilename());
        assertEquals(polygon.getMetadata(), current.getMetadata());
        assertEquals(
                polygon.getSLD().getStyledLayers()[0].getName(),
                current.getSLD().getStyledLayers()[0].getName());
        assertNull(getCatalog().getStyleByName("CookbookSimplePoint"));
    }

    private void assertNotEnabled(MockHttpServletResponse response) throws Exception {
        assertEquals(405, response.getStatus());
        assertEquals("GET", response.getHeader(HttpHeaders.ALLOW));
        DocumentContext json = getAsJSONPath(response);
        assertEquals(APIException.METHOD_NOT_ALLOWED, json.read("type"));
        assertEquals("Style management is not enabled on this server", json.read("title"));
    }

    @Test
    public void testManageStylesEnabled() throws Exception {
        setManageStyles(true);
        String sld = IOUtils.toString(getClass().getResourceAsStream("simplePoint.sld"), "UTF-8");
        MockHttpServletResponse response = putAsServletResponse(POLYGON, sld, SLDHandler.MIMETYPE_10);
        assertEquals(204, response.getStatus());
        assertEquals(
                "CookbookSimplePoint",
                getCatalog()
                        .getStyleByName("polygon")
                        .getSLD()
                        .getStyledLayers()[0]
                        .getName());
        // restore the original style for the other tests
        getTestData().addStyle("polygon", getCatalog());
    }

    @Test
    public void testManageStylesServiceRule() throws Exception {
        setManageStyles(true);
        ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();
        ServiceAccessRule rule = new ServiceAccessRule("styles", "addStyle", "ROLE_ADMINISTRATOR");
        dao.addRule(rule);
        dao.storeRules();
        String sld = IOUtils.toString(getClass().getResourceAsStream("simplePoint.sld"), "UTF-8");
        try {
            // the test dispatcher runs without the security filters, so the denial arrives as the exception
            ServletException e = assertThrows(
                    ServletException.class,
                    () -> postAsServletResponse("ogc/styles/v1/styles", sld, SLDHandler.MIMETYPE_10));
            assertThat(e.getCause(), instanceOf(InsufficientAuthenticationException.class));
            assertNull(getCatalog().getStyleByName("SimplePoint"));

            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
            MockHttpServletResponse response =
                    postAsServletResponse("ogc/styles/v1/styles", sld, SLDHandler.MIMETYPE_10);
            assertEquals(201, response.getStatus());
            assertEquals(
                    "SimplePoint", getCatalog().getStyleByName("SimplePoint").getName());
        } finally {
            logout();
            dao.removeRule(rule);
            dao.storeRules();
            StyleInfo created = getCatalog().getStyleByName("SimplePoint");
            if (created != null) getCatalog().remove(created);
        }
    }

    @Test
    public void testConformanceDefault() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/conformance", 200);
        assertEquals(READ_CLASSES, json.read("conformsTo"));
    }

    @Test
    public void testConformanceManageStyles() throws Exception {
        setManageStyles(true);
        DocumentContext json = getAsJSONPath("ogc/styles/v1/conformance", 200);
        List<String> expected = new ArrayList<>(READ_CLASSES);
        expected.add(StylesService.MANAGE);
        assertEquals(expected, json.read("conformsTo"));
    }

    @Test
    public void testApiDefault() throws Exception {
        OpenAPI api = getApi();
        assertEquals(List.of("Capabilities", "Use styles", "Fetch resources", "Manage resources"), tagNames(api));
        Paths paths = api.getPaths();
        PathItem styles = paths.get("/styles");
        PathItem style = paths.get("/styles/{styleId}");
        PathItem metadata = paths.get("/styles/{styleId}/metadata");
        assertEquals("getStyleSet", styles.getGet().getOperationId());
        assertNull(styles.getPost());
        assertEquals("getStyle", style.getGet().getOperationId());
        assertNull(style.getPut());
        assertNull(style.getDelete());
        assertEquals("getStyleMetadata", metadata.getGet().getOperationId());
        assertNull(metadata.getPut());
        assertNull(metadata.getPatch());
    }

    @Test
    public void testApiManageStyles() throws Exception {
        setManageStyles(true);
        OpenAPI api = getApi();
        assertEquals(
                List.of("Capabilities", "Use styles", "Manage styles", "Fetch resources", "Manage resources"),
                tagNames(api));
        Paths paths = api.getPaths();
        assertEquals("addStyle", paths.get("/styles").getPost().getOperationId());
        assertEquals("updateStyle", paths.get("/styles/{styleId}").getPut().getOperationId());
        assertEquals("deleteStyle", paths.get("/styles/{styleId}").getDelete().getOperationId());
        PathItem metadata = paths.get("/styles/{styleId}/metadata");
        assertEquals("updateStyleMetadata", metadata.getPut().getOperationId());
        assertEquals("patchStyleMetadata", metadata.getPatch().getOperationId());
    }

    private static List<String> tagNames(OpenAPI api) {
        return api.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    }

    private OpenAPI getApi() throws Exception {
        String json = getAsMockHttpServletResponse("ogc/styles/v1/openapi", 200).getContentAsString();
        return Json.mapper().readValue(json, OpenAPI.class);
    }
}
