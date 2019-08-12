package org.geoserver.api.styles;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.geoserver.data.test.MockData.BUILDINGS;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class StyleMetadataTest extends StylesTestSupport {

    public static final String POLYGON_TITLE = "A polygon style";
    public static final String POLYGON_ABSTRACT = "Draws polygons with gray fill, black outline";
    public static final String POLYGON_CONTRAINTS = "restricted";
    public static final String POLYGON_POC = "Claudius";
    public static final QName BUILDINGS_LABEL =
            new QName(BUILDINGS.getNamespaceURI(), "BuildingsLabels", BUILDINGS.getPrefix());
    public static final String BUILDINGS_LABEL_ASSOCIATED_STYLE = "BuildingsLabelAssociated";
    public static final String BUILDINGS_LABEL_STYLE = "BuildingsLabel";
    public static final String TASMANIA = "tasmania";

    @Before
    public void clearMetadata() {
        StyleInfo si = getCatalog().getStyleByName("polygon");
        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        StyleMetadataInfo metadata = new StyleMetadataInfo();
        metadata.setTitle(POLYGON_TITLE);
        metadata.setAbstract(POLYGON_ABSTRACT);
        metadata.setAccessConstraints(POLYGON_CONTRAINTS);
        // keep arraylist, for xstream happiness
        metadata.setKeywords(new ArrayList<>(Arrays.asList("polygon", "test")));
        metadata.setPointOfContact(POLYGON_POC);
        polygon.getMetadata().put(StyleMetadataInfo.METADATA_KEY, metadata);
        getCatalog().save(polygon);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Extra styles and layers to play with metadata and attributes
        testData.addStyle(
                BUILDINGS_LABEL_STYLE, "BuildingsLabel.sld", StyleMetadataTest.class, getCatalog());
        testData.addStyle(
                BUILDINGS_LABEL_ASSOCIATED_STYLE,
                "BuildingsLabel.sld",
                StyleMetadataTest.class,
                getCatalog());
        testData.addVectorLayer(
                BUILDINGS,
                new HashMap() {
                    {
                        put(SystemTestData.LayerProperty.STYLE, BUILDINGS_LABEL_ASSOCIATED_STYLE);
                        put(SystemTestData.LayerProperty.NAME, BUILDINGS_LABEL.getLocalPart());
                    }
                },
                StyleMetadataTest.class,
                getCatalog());

        // a multi-layer style
        testData.addStyle(TASMANIA, "tasmania.sld", StyleMetadataTest.class, getCatalog());
    }

    @Test
    public void testGetMetadataFromRasterStyle() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/raster/metadata", 200);
        assertEquals("raster", json.read("id"));
        assertEquals("Raster", json.read("title"));
        assertEquals(
                "A sample style for rasters, good for displaying imagery",
                json.read("description"));
        assertEquals("Andrea Aime", json.read("pointOfContact"));
        assertEquals("style", json.read("scope"));
        assertEquals("unclassified", json.read("accessConstraints"));

        // layers
        assertEquals(Integer.valueOf(1), (Integer) json.read("layers.size()"));
        assertEquals("raster", json.read("layers[0].id"));
        assertEquals("raster", json.read("layers[0].type"));
    }

    @Test
    public void testGetMetadataFromConfiguredMetadata() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/polygon/metadata", 200);
        assertEquals("polygon", json.read("id"));
        assertEquals(POLYGON_TITLE, json.read("title"));
        assertEquals(POLYGON_ABSTRACT, json.read("description"));
        assertEquals(POLYGON_POC, json.read("pointOfContact"));
        assertEquals("style", json.read("scope"));
        assertEquals(POLYGON_CONTRAINTS, json.read("accessConstraints"));
        assertEquals("polygon", json.read("keywords[0]"));
        assertEquals("test", json.read("keywords[1]"));

        // layers
        assertEquals(Integer.valueOf(1), (Integer) json.read("layers.size()"));
        assertEquals("Default Polygon", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));
    }

    @Test
    public void testGetMetadataAttributesFromStyle() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/styles/styles/" + BUILDINGS_LABEL_STYLE + "/metadata", 200);
        assertEquals("BuildingsLabel", json.read("id"));

        // layers
        assertEquals(Integer.valueOf(1), (Integer) json.read("layers.size()"));
        assertEquals("Buildings", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));

        // attributes
        assertEquals("FID", json.read("layers[0].attributes[0].id"));
        assertEquals("string", json.read("layers[0].attributes[0].type"));
        assertEquals("ADDRESS", json.read("layers[0].attributes[1].id"));
        assertEquals("string", json.read("layers[0].attributes[1].type"));
    }

    @Test
    public void testGetMetadataAttributesFromAssociatedStyle() throws Exception {
        DocumentContext json =
                getAsJSONPath(
                        "ogc/styles/styles/" + BUILDINGS_LABEL_ASSOCIATED_STYLE + "/metadata", 200);
        assertEquals("BuildingsLabelAssociated", json.read("id"));

        // layers
        assertEquals(Integer.valueOf(1), (Integer) json.read("layers.size()"));
        assertEquals("Buildings", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));
        assertEquals("data", json.read("layers[0].sampleData.rel"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections/cite__BuildingsLabels",
                json.read("layers[0].sampleData.href"));
        assertEquals("application/json", json.read("layers[0].sampleData.type"));

        // attributes
        assertEquals("FID", json.read("layers[0].attributes[0].id"));
        assertEquals("integer", json.read("layers[0].attributes[0].type"));
        assertEquals("ADDRESS", json.read("layers[0].attributes[1].id"));
        assertEquals("string", json.read("layers[0].attributes[1].type"));
        assertEquals("DATE", json.read("layers[0].attributes[2].id"));
        assertEquals("dateTime", json.read("layers[0].attributes[2].type"));
        assertEquals("YESNO", json.read("layers[0].attributes[3].id"));
        assertEquals("boolean", json.read("layers[0].attributes[3].type"));
    }

    @Test
    public void testGetMetadataMultilayer() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/" + TASMANIA + "/metadata", 200);
        assertEquals("tasmania", json.read("id"));
        assertEquals(Integer.valueOf(4), (Integer) json.read("layers.size()"));

        // Water Bodies
        assertEquals("WaterBodies", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));
        // need to improve heuristics for attribues used in filters, right now we get no info
        // about the potential target type, but in a binary comparison against a numerical literal
        // we could guess that info
        assertEquals("PERIMETER", json.read("layers[0].attributes[0].id"));
        assertEquals("string", json.read("layers[0].attributes[0].type"));
        assertEquals("AREA", json.read("layers[0].attributes[1].id"));
        assertEquals("string", json.read("layers[0].attributes[1].type"));
        assertEquals("name", json.read("layers[0].attributes[2].id"));
        assertEquals("string", json.read("layers[0].attributes[2].type"));
        assertEquals("WATER_TYPE", json.read("layers[0].attributes[3].id"));
        assertEquals("string", json.read("layers[0].attributes[3].type"));

        // other layers are using no attributes
        assertEquals("Roads", json.read("layers[1].id"));
        assertEquals("line", json.read("layers[1].type"));
        assertEquals("Cities", json.read("layers[2].id"));
        assertEquals("point", json.read("layers[2].type"));
        assertEquals("Land", json.read("layers[3].id"));
        assertEquals("polygon", json.read("layers[3].type"));
    }

    @Test
    public void testMetadataSerialization() throws Exception {
        Resource styleResource = getDataDirectory().getStyles("polygon.xml");
        Document dom = dom(styleResource.in(), true);
        print(dom);
        String metadataPath =
                "//metadata/entry[@key='" + StyleMetadataInfo.METADATA_KEY + "']/styleMetadata";
        assertXpathExists(metadataPath, dom);
        assertXpathEvaluatesTo(POLYGON_TITLE, metadataPath + "/title", dom);
        assertXpathEvaluatesTo(POLYGON_ABSTRACT, metadataPath + "/abstract", dom);
        assertXpathEvaluatesTo("polygon", metadataPath + "/keywords/string[1]", dom);
        assertXpathEvaluatesTo("test", metadataPath + "/keywords/string[2]", dom);

        // force reload to ensure proper white-listing
        getGeoServer().reload();
        StyleInfo polygonInfo = getCatalog().getStyleByName("polygon");
        assertNotNull(polygonInfo);
        StyleMetadataInfo metadata =
                polygonInfo
                        .getMetadata()
                        .get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        assertNotNull("metadata");
        assertEquals(POLYGON_TITLE, metadata.getTitle());
        assertEquals(POLYGON_ABSTRACT, metadata.getAbstract());
        assertEquals("polygon", metadata.getKeywords().get(0));
        assertEquals("test", metadata.getKeywords().get(1));
    }

    @Test
    public void testGetMetadataFromCSSStyle() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/styles/cssSample/metadata", 200);
        assertEquals("cssSample", json.read("id"));
        assertEquals("A CSS style", json.read("title"));
        assertEquals("A simple polygon fill in CSS", json.read("description"));
        assertEquals("Andrea Aime", json.read("pointOfContact"));
        assertEquals("style", json.read("scope"));
        assertEquals("unclassified", json.read("accessConstraints"));

        // at least CSS, SLD 1.0 and 1.1
        assertThat(
                json.read("stylesheets.size()", Integer.class), Matchers.greaterThanOrEqualTo(3));

        // sld 1.is not native, CSS is
        assertEquals(false, getSingle(json, "stylesheets[?(@.title =~ /.*SLD 1.0.*/)].native"));
        assertEquals(true, getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].native"));

        // some checks on the CSS one
        assertEquals(
                "Stylesheet as CSS 1.0.0",
                getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].title"));
        assertEquals("1.0.0", getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].version"));
        assertEquals(
                "https://docs.geoserver.org/latest/en/user/styling/css/index.html",
                getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].specification"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/styles/cssSample?f=application%2Fvnd.geoserver.geocss%2Bcss",
                getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].link.href"));
        assertEquals(
                "application/vnd.geoserver.geocss+css",
                getSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].link.type"));
    }

    @Test
    public void testPutStyleMetadata() throws Exception {
        String metadataJson =
                IOUtils.toString(StyleTest.class.getResourceAsStream("polygonStyleMetadata.json"));
        MockHttpServletResponse response =
                putAsServletResponse(
                        "ogc/styles/styles/polygon/metadata",
                        metadataJson,
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(204, response.getStatus());

        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        StyleMetadataInfo metadata =
                polygon.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        assertEquals("A polygon style with a twist", metadata.getTitle());
        assertEquals(
                "Draws polygons with gray fill. Gray is the new black!", metadata.getAbstract());
        assertEquals(Arrays.asList("polygon", "test", "hip"), metadata.getKeywords());
        // check dates
        StyleDates dates = metadata.getDates();

        assertEquals(parseDate("2019-01-01T10:05:00Z"), dates.getCreation());
        assertEquals(parseDate("2019-01-01T11:05:00Z"), dates.getPublication());
        assertEquals(parseDate("2019-02-01T11:05:00Z"), dates.getRevision());
        assertEquals(parseDate("2019-05-01T11:05:00Z"), dates.getValidTill());
        assertEquals(parseDate("2019-02-01T11:05:00Z"), dates.getReceivedOn());
    }

    public Date parseDate(String date) {
        return DatatypeConverter.parseDate(date).getTime();
    }

    @Test
    public void testPatchStyleMetadata() throws Exception {
        // init with some custom metadata
        testPutStyleMetadata();

        String jsonPatch =
                IOUtils.toString(StyleTest.class.getResourceAsStream("metadataPatch.json"));
        MockHttpServletResponse response =
                patchAsServletResponse(
                        "ogc/styles/styles/polygon/metadata",
                        jsonPatch,
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(204, response.getStatus());

        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        StyleMetadataInfo metadata =
                polygon.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        assertEquals("A polygon style with a twist", metadata.getTitle()); // not modified
        assertNull(metadata.getAbstract()); // explicitly set to null
        assertEquals("Jane Doe", metadata.getPointOfContact()); // modified
        // array modified
        assertEquals(Arrays.asList("polygon", "test", "hip"), metadata.getKeywords());
        // sub-object modified
        StyleDates dates = metadata.getDates();
        assertEquals(parseDate("2019-05-17T11:46:12Z"), dates.getRevision());
        assertNull(dates.getValidTill());
    }
}
