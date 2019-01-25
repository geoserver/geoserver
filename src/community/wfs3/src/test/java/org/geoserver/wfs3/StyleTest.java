/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.ROAD_SEGMENTS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PropertyStyleHandler;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs3.response.OpenAPIResponse;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Style;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class StyleTest extends WFS3TestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        final Catalog catalog = getCatalog();
        testData.addStyle("dashed", "dashedline.sld", StyleTest.class, catalog);
        final LayerInfo roadSegments = catalog.getLayerByName(getLayerId(ROAD_SEGMENTS));
        catalog.save(roadSegments);
    }

    @Before
    public void cleanup() {
        final Catalog catalog = getCatalog();
        final LayerInfo roadSegments = catalog.getLayerByName(getLayerId(ROAD_SEGMENTS));
        roadSegments.getStyles().clear();
        StyleInfo line = catalog.getStyleByName("line");
        roadSegments.getStyles().add(line);
        catalog.save(roadSegments);

        Optional.ofNullable(catalog.getStyleByName("simplePoint"))
                .ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("testPoint")).ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("circles")).ifPresent(s -> catalog.remove(s));
        Optional.ofNullable(catalog.getStyleByName("cssline")).ifPresent(s -> catalog.remove(s));
    }

    @Test
    public void testGetStyles() throws Exception {
        final DocumentContext doc = getAsJSONPath("wfs3/styles", 200);
        // only the dashed line, put as the only non linked style
        assertEquals(Integer.valueOf(1), doc.read("styles.length()", Integer.class));
        assertEquals("dashed", doc.read("styles..id", List.class).get(0));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/dashed?f=application%2Fvnd.ogc.sld%2Bxml",
                doc.read(
                                "styles..links[?(@.rel=='style' && @.type=='application/vnd.ogc.sld+xml')].href",
                                List.class)
                        .get(0));
    }

    @Test
    public void testGetCollectionStyles() throws Exception {
        String roadSegments = getEncodedName(ROAD_SEGMENTS);
        final DocumentContext doc =
                getAsJSONPath("wfs3/collections/" + roadSegments + "/styles", 200);
        // two stiles, the native one, and the line associated one
        assertEquals(Integer.valueOf(2), doc.read("styles.length()", Integer.class));
        assertEquals("RoadSegments", doc.read("styles..id", List.class).get(0));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments/styles/RoadSegments?f=application%2Fvnd.ogc.sld%2Bxml",
                doc.read(
                                "styles..links[?(@.rel=='style' && @.type=='application/vnd.ogc.sld+xml')].href",
                                List.class)
                        .get(0));
        assertEquals("line", doc.read("styles..id", List.class).get(1));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments/styles/line?f=application%2Fvnd.ogc.sld%2Bxml",
                doc.read(
                                "styles..links[?(@.rel=='style' && @.type=='application/vnd.ogc.sld+xml')].href",
                                List.class)
                        .get(1));
    }

    @Test
    public void testGetStyle() throws Exception {
        final MockHttpServletResponse response = getAsServletResponse("wfs3/styles/dashed?f=sld");
        assertEquals(OK.value(), response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        assertEquals("inline; filename=dashed.sld", response.getHeader("Content-Disposition"));
        final Document dom = dom(response, true);
        assertXpathEvaluatesTo("SLD Cook Book: Dashed line", "//sld:UserStyle/sld:Title", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:LineSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "5 2",
                "//sld:LineSymbolizer/sld:Stroke/sld:CssParameter[@name='stroke-dasharray']",
                dom);
    }

    @Test
    public void testGetCollectionStyle() throws Exception {
        final MockHttpServletResponse response =
                getAsServletResponse("wfs3/collections/cite__RoadSegments/styles/line");
        assertEquals(OK.value(), response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        final Document dom = dom(response, true);
        // print(dom);
        assertXpathEvaluatesTo("A boring default style", "//sld:UserStyle/sld:Title", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:LineSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "#0000FF", "//sld:LineSymbolizer/sld:Stroke/sld:CssParameter[@name='stroke']", dom);
    }

    @Test
    public void testGetCollectionNonAssociatedStyle() throws Exception {
        final MockHttpServletResponse response =
                getAsServletResponse("wfs3/collections/cite__RoadSegments/styles/polygon");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testPostSLDStyleGlobal() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        final MockHttpServletResponse response =
                postAsServletResponse("wfs3/styles", styleBody, SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/simplePoint",
                response.getHeader(HttpHeaders.LOCATION));

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("simplePoint");
        checkSimplePoint(styleInfo, Color.RED);
    }

    @Test
    public void testPostSLDStyleCollection() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        final MockHttpServletResponse response =
                postAsServletResponse(
                        "wfs3/collections/cite__RoadSegments/styles",
                        styleBody,
                        SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments/styles/simplePoint",
                response.getHeader(HttpHeaders.LOCATION));

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("simplePoint");
        checkSimplePoint(styleInfo, Color.RED);

        // check layer association
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(ROAD_SEGMENTS));
        assertThat(layer.getStyles(), hasItem(hasProperty("name", equalTo("simplePoint"))));
    }

    public void checkSimplePoint(StyleInfo styleInfo, Color expectedColor) throws IOException {
        assertNotNull(styleInfo);
        final Style style = styleInfo.getStyle();
        PointSymbolizer ps =
                (PointSymbolizer)
                        style.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        final Mark mark = (Mark) ps.getGraphic().graphicalSymbols().get(0);
        assertEquals("circle", mark.getWellKnownName().evaluate(null, String.class));
        assertEquals(expectedColor, mark.getFill().getColor().evaluate(null, Color.class));
    }

    @Test
    public void testPostSLDStyleInWorkspace() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        final MockHttpServletResponse response =
                postAsServletResponse("cite/wfs3/styles", styleBody, SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/cite/wfs3/styles/simplePoint",
                response.getHeader(HttpHeaders.LOCATION));

        final StyleInfo styleInfo = getCatalog().getStyleByName("cite", "simplePoint");
        checkSimplePoint(styleInfo, Color.RED);
    }

    public String loadStyle(String fileName) throws IOException {
        try (InputStream is = StyleTest.class.getResourceAsStream(fileName)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void testPutSLDStyleGlobal() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        // use a name not found in the style body
        final MockHttpServletResponse response =
                putAsServletResponse("wfs3/styles/testPoint", styleBody, SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("testPoint");
        checkSimplePoint(styleInfo, Color.RED);
    }

    @Test
    public void testPutSLDStyleCollection() throws Exception {
        String styleBody = loadStyle("simplePoint.sld");
        // use a name not found in the style body
        final MockHttpServletResponse response =
                putAsServletResponse(
                        "wfs3/collections/cite__RoadSegments/styles/testPoint",
                        styleBody,
                        SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("testPoint");
        checkSimplePoint(styleInfo, Color.RED);

        // check layer association
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(ROAD_SEGMENTS));
        assertThat(layer.getStyles(), hasItem(hasProperty("name", equalTo("testPoint"))));
    }

    @Test
    public void testPutSLDStyleModify() throws Exception {
        testPutSLDStyleGlobal();

        // use a different style body
        String styleBody = loadStyle("simplePoint2.sld");
        final MockHttpServletResponse response =
                putAsServletResponse("wfs3/styles/testPoint", styleBody, SLDHandler.MIMETYPE_10);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("testPoint");
        checkSimplePoint(styleInfo, Color.BLACK);
    }

    @Test
    public void testDeleteNonExistingStyle() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("wfs3/styles/notThere");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteGlobalStyle() throws Exception {
        // creates "testPoint"
        testPutSLDStyleGlobal();
        // remove it
        MockHttpServletResponse response = deleteAsServletResponse("wfs3/styles/testPoint");
        assertEquals(NO_CONTENT.value(), response.getStatus());

        assertNull(getCatalog().getStyleByName("simplePoint"));
    }

    @Test
    public void testDeleteNonAssociatedBuiltInStyle() throws Exception {
        // add testPoint, but not associated to the road segments layer
        testPostSLDStyleGlobal();
        MockHttpServletResponse response =
                deleteAsServletResponse("wfs3/collections/cite__RoadSegments/styles/polygon");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteNonAssociatedStyle() throws Exception {
        // add testPoint, but not associated to the road segments layer
        testPostSLDStyleGlobal();
        MockHttpServletResponse response =
                deleteAsServletResponse("wfs3/collections/cite__RoadSegments/styles/simplePoint");
        assertEquals(NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void testDeleteAssociatedStyle() throws Exception {
        // add testPoint, but not associated to the road segments layer
        testPostSLDStyleCollection();
        MockHttpServletResponse response =
                deleteAsServletResponse("wfs3/collections/cite__RoadSegments/styles/simplePoint");
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check the style is gone and the association too
        assertNull(getCatalog().getStyleByName("simplePoint"));
        final LayerInfo layer = getCatalog().getLayerByName(getLayerId(ROAD_SEGMENTS));
        System.out.println(layer.getStyles());
        assertEquals(1, layer.getStyles().size());
        assertThat(layer.getStyles(), hasItems(hasProperty("name", equalTo("line"))));
    }

    @Test
    public void testMBStyle() throws Exception {
        String styleBody = loadStyle("mbcircle.json");
        // use a name not found in the style body
        MockHttpServletResponse response =
                postAsServletResponse("wfs3/styles", styleBody, MBStyleHandler.MIME_TYPE);
        assertEquals(CREATED.value(), response.getStatus());
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/circles",
                response.getHeader(HttpHeaders.LOCATION));

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("circles");
        assertNotNull(styleInfo);

        // verify links for it
        DocumentContext doc = getAsJSONPath("wfs3/styles", 200);
        assertEquals(Integer.valueOf(2), doc.read("styles.length()", Integer.class));
        assertEquals(1, doc.read("styles[?(@.id=='circles')]", List.class).size());
        assertEquals(2, doc.read("styles[?(@.id=='circles')].links..href", List.class).size());
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/circles?f=application%2Fvnd.ogc.sld%2Bxml",
                doc.read(
                                "styles[?(@.id=='circles')].links[?(@.rel=='style' && @.type=='application/vnd.ogc.sld+xml')].href",
                                List.class)
                        .get(0));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/circles?f=application%2Fvnd.geoserver.mbstyle%2Bjson",
                doc.read(
                                "styles[?(@.id=='circles')].links[?(@.rel=='style' && @.type=='application/vnd.geoserver.mbstyle+json')].href",
                                List.class)
                        .get(0));

        // check we can get both styles, first SLD
        Document dom = getAsDOM("wfs3/styles/circles?f=application%2Fvnd.ogc.sld%2Bxml", 200);
        // print(dom);
        assertXpathEvaluatesTo("circles", "//sld:StyledLayerDescriptor/sld:Name", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:PointSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "circle", "//sld:PointSymbolizer/sld:Graphic/sld:Mark/sld:WellKnownName", dom);

        // .. then MBStyle
        response =
                getAsMockHttpServletResponse(
                        "wfs3/styles/circles?f=application%2Fvnd.geoserver.mbstyle%2Bjson", 200);
        assertEquals("inline; filename=circles.mbstyle", response.getHeader("Content-Disposition"));
        DocumentContext mbstyle = getAsJSONPath(response);
        assertEquals("circles", mbstyle.read("$.name"));
    }

    @Test
    public void testCSS() throws Exception {
        String styleBody = loadStyle("line.css");
        // create style
        MockHttpServletResponse response =
                putAsServletResponse("wfs3/styles/cssline", styleBody, CssHandler.MIME_TYPE);
        assertEquals(NO_CONTENT.value(), response.getStatus());

        // check style creation
        final StyleInfo styleInfo = getCatalog().getStyleByName("cssline");
        assertNotNull(styleInfo);

        // verify links for it
        DocumentContext doc = getAsJSONPath("wfs3/styles", 200);
        assertEquals(Integer.valueOf(2), doc.read("styles.length()", Integer.class));
        assertEquals(1, doc.read("styles[?(@.id=='cssline')]", List.class).size());
        assertEquals(2, doc.read("styles[?(@.id=='cssline')].links..href", List.class).size());
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/cssline?f=application%2Fvnd.ogc.sld%2Bxml",
                doc.read(
                                "styles[?(@.id=='cssline')].links[?(@.rel=='style' && @.type=='application/vnd.ogc.sld+xml')].href",
                                List.class)
                        .get(0));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/styles/cssline?f=application%2Fvnd.geoserver.geocss%2Bcss",
                doc.read(
                                "styles[?(@.id=='cssline')].links[?(@.rel=='style' && @.type=='application/vnd.geoserver.geocss+css')].href",
                                List.class)
                        .get(0));

        // check we can get both styles, first SLD
        Document dom = getAsDOM("wfs3/styles/cssline?f=application%2Fvnd.ogc.sld%2Bxml", 200);
        // print(dom);
        assertXpathEvaluatesTo("cssline", "//sld:StyledLayerDescriptor/sld:Name", dom);
        assertXpathEvaluatesTo("1", "count(//sld:Rule)", dom);
        assertXpathEvaluatesTo("1", "count(//sld:LineSymbolizer)", dom);
        assertXpathEvaluatesTo(
                "3", "//sld:LineSymbolizer/sld:Stroke/sld:CssParameter[@name='stroke-width']", dom);

        // .. then CSS
        response =
                getAsServletResponse(
                        "wfs3/styles/cssline?f=application%2Fvnd.geoserver.geocss%2Bcss");
        assertEquals(200, response.getStatus());
        assertEquals("inline; filename=cssline.css", response.getHeader("Content-Disposition"));
        assertEqualsIgnoreNewLineStyle(
                "* {\n" + "   stroke: black;\n" + "   stroke-width: 3;\n" + "}",
                response.getContentAsString());
    }

    @Test
    public void testApiExtensions() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("wfs3/api", 200);
        assertEquals(OpenAPIResponse.OPEN_API_MIME, response.getContentType());
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);

        // check paths
        Paths paths = api.getPaths();

        // ... global styles
        PathItem globalStyles = paths.get("/styles");
        assertNotNull(globalStyles);
        assertThat(globalStyles.getGet().getOperationId(), equalTo("getStyles"));
        assertThat(globalStyles.getPost().getOperationId(), equalTo("addStyle"));
        assertBodyMediaTypes(globalStyles.getPost());

        // ... global style
        PathItem globalStyle = paths.get("/styles/{styleId}");
        assertNotNull(globalStyle);
        assertThat(globalStyle.getGet().getOperationId(), equalTo("getStyle"));
        assertThat(globalStyle.getPut().getOperationId(), equalTo("replaceStyle"));
        assertBodyMediaTypes(globalStyle.getPut());
        assertThat(globalStyle.getDelete().getOperationId(), equalTo("deleteStyle"));

        // ... collection styles
        PathItem collectionStyles = paths.get("/collections/{collectionId}/styles");
        assertNotNull(collectionStyles);
        assertThat(collectionStyles.getGet().getOperationId(), equalTo("getCollectionStyles"));
        assertThat(collectionStyles.getPost().getOperationId(), equalTo("addCollectionStyle"));
        assertBodyMediaTypes(collectionStyles.getPost());

        // ... collection style
        PathItem collectionStyle = paths.get("/collections/{collectionId}/styles/{styleId}");
        assertNotNull(collectionStyle);
        assertThat(collectionStyle.getGet().getOperationId(), equalTo("getCollectionStyle"));
        assertThat(collectionStyle.getPut().getOperationId(), equalTo("replaceCollectionStyle"));
        assertBodyMediaTypes(collectionStyle.getPut());
        assertThat(collectionStyle.getDelete().getOperationId(), equalTo("deleteCollectionStyle"));
    }

    private void assertBodyMediaTypes(Operation operation) {
        assertThat(
                operation.getRequestBody().getContent().keySet(),
                Matchers.containsInAnyOrder(
                        SLDHandler.MIMETYPE_10,
                        SLDHandler.MIMETYPE_11,
                        MBStyleHandler.MIME_TYPE,
                        CssHandler.MIME_TYPE,
                        // this one is just a test one, but it's in the classpath nevertheless
                        PropertyStyleHandler.MIMETYPE));
    }
}
