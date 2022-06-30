package org.geoserver.restconfig.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.geoserver.openapi.model.catalog.StyleInfo;
import org.geoserver.openapi.model.catalog.StyleInfo.FormatEnum;
import org.geoserver.openapi.model.catalog.Version;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.geoserver.restconfig.client.StylesClient.StyleFormat;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

/** Integration test suite for {@link DataStoresClient} */
@Ignore // geoserver test docker image doesn't have the mapbox styles extension
public class StylesClientMapBoxIT {

    public static @ClassRule IntegrationTestSupport support = new IntegrationTestSupport();
    private SecureRandom rnd = new SecureRandom();

    public @Rule TestName testName = new TestName();
    public @Rule ExpectedException ex = ExpectedException.none();

    private StylesClient stylesClient;

    private WorkspacesClient workspaces;

    private DataStoresClient dataStores;

    private WorkspaceSummary ws1;

    private WorkspaceSummary ws2;

    private URI roadsShapefile;

    private URI streamsShapefile;

    public @Before void before() {
        Assume.assumeTrue(support.isAlive());
        this.workspaces = support.client().workspaces();
        this.dataStores = support.client().dataStores();
        this.stylesClient = support.client().styles();

        String wsname1 =
                String.format("%s-ws1-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));
        String wsname2 =
                String.format("%s-ws2-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));

        this.workspaces.create(wsname1);
        this.workspaces.create(wsname2);
        this.ws1 = workspaces.findByName(wsname1).get();
        this.ws2 = workspaces.findByName(wsname2).get();

        this.roadsShapefile = support.getRoadsShapefile();
        this.streamsShapefile = support.getStreamsShapefile();

        this.dataStores.createShapefileDataStore(wsname1, "roadsStoreWs1", roadsShapefile);
        this.dataStores.createShapefileDataStore(wsname1, "streams", streamsShapefile);

        this.dataStores.createShapefileDataStore(wsname2, "roadsStoreWs2", roadsShapefile);
        this.dataStores.createShapefileDataStore(wsname2, "streams", streamsShapefile);

        this.roadsShapefile = support.getRoadsShapefile();
        this.streamsShapefile = support.getStreamsShapefile();
    }

    public @After void after() {
        if (this.ws1 != null) {
            this.workspaces.deleteRecursively(this.ws1.getName());
        }
        if (this.ws2 != null) {
            this.workspaces.deleteRecursively(this.ws2.getName());
        }
    }

    public @Test void getStyleList() throws IOException {
        List<Link> globalStyles = stylesClient.getStyles();
        assertNotNull(globalStyles);
        assertFalse(globalStyles.isEmpty());
        globalStyles.forEach(
                l -> {
                    assertNotNull(l.getName());
                    assertNotNull(l.getLink());
                });
        Set<String> names = globalStyles.stream().map(Link::getName).collect(Collectors.toSet());
        // test only default styles
        assertTrue(names.contains("line"));
        assertTrue(names.contains("point"));
        assertTrue(names.contains("polygon"));
        assertTrue(names.contains("raster"));
        assertTrue(names.contains("generic"));
    }

    public @Test void getStyle() throws IOException {
        StyleInfo styleInfo = stylesClient.get("line");
        assertNotNull(styleInfo);
        assertEquals("line", styleInfo.getName());
    }

    public @Test void getStyleListByWorkspace() throws IOException {
        setUpDefaultWorkspaceStyles();
        List<Link> styles = stylesClient.getStyles(ws1.getName());
        assertEquals(2, styles.size());

        styles = stylesClient.getStyles(ws2.getName());
        assertEquals(3, styles.size());
    }

    private void setUpDefaultWorkspaceStyles() {
        StyleInfo line = stylesClient.get("line");
        StyleInfo point = stylesClient.get("point");
        StyleInfo polygon = stylesClient.get("polygon");

        String lineBody = stylesClient.getBody(line);
        String pointBody = stylesClient.getBody(point);
        String polygonBody = stylesClient.getBody(polygon);

        assertEquals(0, stylesClient.getStyles(ws1.getName()).size());
        assertEquals(0, stylesClient.getStyles(ws2.getName()).size());

        { // ws1
            WorkspaceInfo ws = new WorkspaceInfo().name(this.ws1.getName());
            line.workspace(ws).filename("line-ws1.sld");
            point.workspace(ws).filename("point-ws1.sld");
            stylesClient.createStyle(line, lineBody);
            stylesClient.createStyle(point, pointBody);
        }
        { // ws2
            WorkspaceInfo ws = new WorkspaceInfo().name(this.ws2.getName());
            line.workspace(ws).filename("line-ws2.sld");
            point.workspace(ws).filename("point-ws2.sld");
            polygon.workspace(ws).filename("polygon-ws2.sld");
            stylesClient.createStyle(line, lineBody);
            stylesClient.createStyle(point, pointBody);
            stylesClient.createStyle(polygon, polygonBody);
        }
    }

    public @Test void getStyleByWorkspace() throws IOException {
        setUpDefaultWorkspaceStyles();
        assertNotNull(stylesClient.get(ws1.getName(), "line"));
        assertNotNull(stylesClient.get(ws1.getName(), "point"));

        assertNotNull(stylesClient.get(ws2.getName(), "line"));
        assertNotNull(stylesClient.get(ws2.getName(), "point"));
        assertNotNull(stylesClient.get(ws2.getName(), "polygon"));

        // polygon does not exist in ws1
        ex.expect(ServerException.NotFound.class);
        assertNotNull(stylesClient.get(ws1.getName(), "polygon"));
    }

    public @Test void getStyleBodySLDToMapboxUnableToConvert() throws IOException {
        ex.expect(ServerException.InternalServerError.class);
        stylesClient.getBody("line", StyleFormat.MAPBOX);
    }

    public @Test void getStyleBodyMapbox() throws IOException {
        String requestBody = loadResource("v1/styles/body_1_layer.mapbox.json");
        String name = "mbstyle-" + rnd.nextInt(10_0000);
        StyleInfo styleInfo = stylesClient.createMapboxStyle(name, requestBody);
        String styleBody = stylesClient.getBody(styleInfo);
        JsonNode doc = new ObjectMapper().readTree(styleBody);
        assertNotNull(doc);
        JsonNode original = new ObjectMapper().readTree(requestBody);
        assertEquals(original, doc);
    }

    public @Test void getStyleBodyMapboxToSLD() throws IOException {
        String requestBody = loadResource("v1/styles/body_1_layer.mapbox.json");
        String name = "mbstyle-" + rnd.nextInt(10_0000);
        stylesClient.createMapboxStyle(name, requestBody);
        String styleBody = stylesClient.getBody(name, StyleFormat.SLD_1_0_0);
        assertThat(styleBody, StringContains.containsString("<sld:StyledLayerDescriptor"));
    }

    public @Test void getStyleBodySLD_1_0() throws IOException {
        setUpDefaultWorkspaceStyles();

        assertThat(
                stylesClient.getBody("line", StyleFormat.SLD_1_0_0),
                StringContains.containsString("StyledLayerDescriptor"));

        StyleInfo info = stylesClient.get(ws1.getName(), "line");
        String bodyByInfo = stylesClient.getBody(info);
        assertThat(bodyByInfo, StringContains.containsString("StyledLayerDescriptor"));

        String bodyByWsAndName = stylesClient.getBody(ws1.getName(), "line", StyleFormat.SLD_1_0_0);
        assertThat(bodyByWsAndName, StringContains.containsString("StyledLayerDescriptor"));
    }

    public @Test void createStyleMapBoxOneLayer() throws IOException {
        String requestBody = loadResource("v1/styles/body_1_layer.mapbox.json");
        String name = "mbstyle-" + rnd.nextInt(10_0000);
        StyleInfo styleInfo = stylesClient.createMapboxStyle(name, requestBody);
        assertNotNull(styleInfo);
        assertEquals(name, styleInfo.getName());
        assertEquals(FormatEnum.MBSTYLE, styleInfo.getFormat());
        assertEquals(name + ".json", styleInfo.getFilename());
        Version version =
                styleInfo.getFormatVersion() == null
                        ? styleInfo.getLanguageVersion()
                        : styleInfo.getFormatVersion();
        assertEquals("1.0.0", version.getVersion());
        assertNull(styleInfo.getWorkspace());

        String styleBodyMapBox = stylesClient.getBody(name, StyleFormat.MAPBOX);
        assertThat(styleBodyMapBox, StringContains.containsString("\"name\": \"violet polygon\""));
        String styleBodyConvertedToSLD = stylesClient.getBody(name, StyleFormat.SLD_1_0_0);
        assertThat(
                styleBodyConvertedToSLD,
                StringContains.containsString("<sld:StyledLayerDescriptor"));
    }

    public @Test void createStyleMapBoxOneLayerByWorkspace() throws IOException {
        final String requestBody = loadResource("v1/styles/body_1_layer.mapbox.json");
        final String name = "mbstyle";

        final @NonNull StyleInfo styleWs1 =
                createMapboxStyleWorkspace(this.ws1.getName(), name, requestBody);
        final @NonNull StyleInfo styleWs2 =
                createMapboxStyleWorkspace(this.ws2.getName(), name, requestBody);

        String style1Body =
                stylesClient.getBody(ws1.getName(), styleWs1.getName(), StyleFormat.MAPBOX);
        assertThat(style1Body, StringContains.containsString("\"name\": \"violet polygon\""));

        String style2Body =
                stylesClient.getBody(ws2.getName(), styleWs2.getName(), StyleFormat.MAPBOX);
        assertThat(style2Body, StringContains.containsString("\"name\": \"violet polygon\""));

        String style1SLD =
                stylesClient.getBody(ws1.getName(), styleWs1.getName(), StyleFormat.SLD_1_0_0);
        assertThat(style1SLD, StringContains.containsString("<sld:StyledLayerDescriptor"));

        String style2SLD =
                stylesClient.getBody(ws2.getName(), styleWs2.getName(), StyleFormat.SLD_1_0_0);
        assertThat(style2SLD, StringContains.containsString("<sld:StyledLayerDescriptor"));
    }

    public @Test void createStyleMapBoxMultipleLayersByWorkspace() throws IOException {
        final String requestBody = loadResource("v1/styles/body_2_layers.mapbox.json");
        final String name = "mbstyle";
        final JsonNode origDoc = new ObjectMapper().readTree(requestBody);
        assertEquals(2, origDoc.at("/layers").size());
        assertEquals("roads", origDoc.at("/layers/0/source-layer").asText());
        assertEquals("roads", origDoc.at("/layers/1/source-layer").asText());

        final @NonNull StyleInfo styleWs1 =
                createMapboxStyleWorkspace(this.ws1.getName(), name, requestBody);

        String style1Body =
                stylesClient.getBody(ws1.getName(), styleWs1.getName(), StyleFormat.MAPBOX);
        assertThat(style1Body, StringContains.containsString("\"name\": \"violet polygon\""));
        JsonNode json = new ObjectMapper().readTree(style1Body);
        assertEquals(2, json.at("/layers").size());
        assertEquals("roads", json.at("/layers/0/source-layer").asText());
        assertEquals("roads", json.at("/layers/1/source-layer").asText());

        String style1SLD =
                stylesClient.getBody(ws1.getName(), styleWs1.getName(), StyleFormat.SLD_1_0_0);
        assertThat(style1SLD, StringContains.containsString("<sld:StyledLayerDescriptor"));
    }

    /**
     * GeoServer can't handle styles with multiple layers and no source-layer property on each
     *
     * @throws IOException
     */
    public @Test void createStyleMapBoxMultipleLayersNoSourceLayerSpecified() throws IOException {
        final String requestBody = loadResource("v1/styles/body_2_layers.mapbox.json");
        final String name = "mbstyle";
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode origDoc = mapper.readTree(requestBody);
        assertEquals(2, origDoc.at("/layers").size());
        assertEquals("roads", origDoc.at("/layers/0/source-layer").asText());
        assertEquals("roads", origDoc.at("/layers/1/source-layer").asText());

        ((ObjectNode) origDoc.at("/layers/0")).remove("source-layer");
        ((ObjectNode) origDoc.at("/layers/1")).remove("source-layer");

        String docWithoutSourceLayer = mapper.writeValueAsString(origDoc);

        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Every layer on a multi-layer style must contain a source-layer property");
        createMapboxStyleWorkspace(this.ws1.getName(), name, docWithoutSourceLayer);
    }

    public @Test void createStyleMapBoxMalformedDocument() throws IOException {
        final String requestBody = loadResource("v1/styles/body_2_layers.mapbox.json");
        String body = requestBody.substring(0, requestBody.lastIndexOf("}") - 1);

        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Malformed JSON document");
        createMapboxStyleWorkspace(this.ws1.getName(), "malformed-style", body);
    }

    private @NonNull StyleInfo createMapboxStyleWorkspace(
            String workspaceName, String name, String styleBody) {
        StyleInfo styleInfo = stylesClient.createMapboxStyle(workspaceName, name, styleBody);
        assertNotNull(styleInfo);
        assertEquals(name, styleInfo.getName());
        assertEquals(FormatEnum.MBSTYLE, styleInfo.getFormat());
        assertEquals(name + ".json", styleInfo.getFilename());
        Version version =
                styleInfo.getFormatVersion() == null
                        ? styleInfo.getLanguageVersion()
                        : styleInfo.getFormatVersion();
        assertEquals("1.0.0", version.getVersion());
        assertNotNull(styleInfo.getWorkspace());
        assertEquals(workspaceName, styleInfo.getWorkspace().getName());
        return styleInfo;
    }

    public @Test void modifyStyleInfo() throws IOException {
        StyleInfo info = stylesClient.get("point");
        String body = stylesClient.getBody(info);

        info.setName("point-" + rnd.nextInt(10_0000));
        info.setFilename(info.getName() + ".sld");
        StyleInfo created = stylesClient.createStyle(info, body);
        assertEquals(info.getName(), created.getName());

        String oldname = created.getName();
        String newname = oldname + "-modified";
        created.setName(newname);
        stylesClient.update(oldname, created);

        StyleInfo updated = stylesClient.get(newname);
        assertNotNull(updated);
        ex.expect(ServerException.NotFound.class);
        stylesClient.get(oldname);
    }

    public @Test void modifyStyleInfoByWorkspace() throws IOException {
        StyleInfo info = stylesClient.get("point");
        String body = stylesClient.getBody(info);

        info.setWorkspace(new WorkspaceInfo().name(this.ws1.getName()));
        info.setName("point-in-ws");
        info.setFilename(info.getName() + ".sld");
        StyleInfo created = stylesClient.createStyle(info, body);
        assertEquals(info.getName(), created.getName());

        String oldname = created.getName();
        String newname = oldname + "-modified";
        created.setName(newname);
        stylesClient.update(this.ws1.getName(), oldname, created);

        StyleInfo updated = stylesClient.get(this.ws1.getName(), newname);
        assertNotNull(updated);
        ex.expect(ServerException.NotFound.class);
        stylesClient.get(oldname);
    }

    public @Test void modifyStyleBody() throws IOException {
        final String requestBody = loadResource("v1/styles/body_2_layers.mapbox.json");
        final String name = "mbstyle";
        ObjectMapper mapper = new ObjectMapper();
        final JsonNode origDoc = mapper.readTree(requestBody);
        assertEquals(2, origDoc.at("/layers").size());
        assertEquals("roads", origDoc.at("/layers/0/source-layer").asText());
        assertEquals("roads", origDoc.at("/layers/1/source-layer").asText());

        final @NonNull StyleInfo styleWs1 =
                createMapboxStyleWorkspace(this.ws1.getName(), name, requestBody);

        String style1Body =
                stylesClient.getBody(ws1.getName(), styleWs1.getName(), StyleFormat.MAPBOX);
        assertThat(style1Body, StringContains.containsString("\"name\": \"violet polygon\""));
        JsonNode json = new ObjectMapper().readTree(style1Body);
        ArrayNode layers = (ArrayNode) json.at("/layers");

        assertEquals(2, layers.size());

        layers.remove(1);
        String updatedBody = mapper.writeValueAsString(json);
        stylesClient.updateBody(styleWs1, updatedBody);

        String savedBodyContents = stylesClient.getBody(styleWs1);
        JsonNode savedBody = mapper.readTree(savedBodyContents);
        layers = (ArrayNode) savedBody.at("/layers");
        assertEquals(1, layers.size());
    }

    public @Test void deleteDefaultStyle() throws IOException {
        ex.expect(ServerException.InternalServerError.class);
        stylesClient.delete("line");
    }

    public @Test void deleteStyle() throws IOException {
        StyleInfo styleInfo = stylesClient.get("line");
        styleInfo.setName(testName.getMethodName() + "-" + rnd.nextInt(10_000));
        styleInfo.setFilename(null);
        final String requestBody = stylesClient.getBody("line", StyleFormat.SLD_1_0_0);
        StyleInfo created = stylesClient.createStyle(styleInfo, requestBody);
        assertNotNull(created);
        assertNotNull(stylesClient.getBody(created));

        stylesClient.delete(created.getName());

        ex.expect(ServerException.NotFound.class);
        stylesClient.get(created.getName());
    }

    public @Test void deleteStyleByWorkspace() throws IOException {
        setUpDefaultWorkspaceStyles();
        stylesClient.delete(ws1.getName(), "line");
        stylesClient.delete(ws1.getName(), "point");

        stylesClient.delete(ws2.getName(), "line");
        stylesClient.delete(ws2.getName(), "point");
        stylesClient.delete(ws2.getName(), "polygon");

        ex.expect(ServerException.NotFound.class);
        stylesClient.delete(ws2.getName(), "point");
    }

    private String loadResource(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(name)) {
            if (null == in) {
                throw new IOException("Resouce " + name + " not found");
            }
            String content =
                    new BufferedReader(new InputStreamReader(in, UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n"));
            return content;
        }
    }
}
