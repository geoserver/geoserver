/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static java.util.Map.entry;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.geoserver.data.test.MockData.BUILDINGS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.IOUtils;
import org.geowebcache.mime.ApplicationMime;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

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
    public static final String BUILDINGS_LAKES = "buildingsLakes";

    @Before
    public void clearMetadata() {
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
        testData.addStyle(BUILDINGS_LABEL_STYLE, "BuildingsLabel.sld", StyleMetadataTest.class, getCatalog());
        testData.addStyle(
                BUILDINGS_LABEL_ASSOCIATED_STYLE, "BuildingsLabel.sld", StyleMetadataTest.class, getCatalog());
        testData.addVectorLayer(
                BUILDINGS_LABEL,
                Map.ofEntries(
                        entry(SystemTestData.LayerProperty.STYLE, BUILDINGS_LABEL_ASSOCIATED_STYLE),
                        entry(SystemTestData.LayerProperty.NAME, BUILDINGS_LABEL.getLocalPart())),
                StyleMetadataTest.class,
                getCatalog());

        // add vector tiles as a format for it
        String buildingsLabelId = getLayerId(BUILDINGS_LABEL);
        GWC gwc = GeoServerExtensions.bean(GWC.class);
        GeoServerTileLayer buildingTiles = (GeoServerTileLayer) gwc.getTileLayerByName(buildingsLabelId);
        Set<String> formats = buildingTiles.getInfo().getMimeFormats();
        formats.add(ApplicationMime.mapboxVector.getFormat());
        formats.add(ApplicationMime.topojson.getFormat());
        formats.add(ApplicationMime.geojson.getFormat());
        gwc.save(buildingTiles);

        // a multi-layer style
        testData.addStyle(TASMANIA, "tasmania.sld", StyleMetadataTest.class, getCatalog());

        // a style group kind, with the layers that it needs
        testData.addVectorLayer(MockData.LAKES, getCatalog());
        testData.addStyle(BUILDINGS_LAKES, "buildingsLakes.sld", StyleMetadataTest.class, getCatalog());
    }

    @Test
    public void testGetMetadataFromRasterStyle() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/raster/metadata", 200);
        assertEquals("raster", json.read("id"));
        assertEquals("Raster", json.read("title"));
        assertEquals("A sample style for rasters, good for displaying imagery", json.read("description"));
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
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/polygon/metadata", 200);
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
    public void testGetMetadataHTML() throws Exception {
        org.jsoup.nodes.Document doc = getAsJSoup("ogc/styles/v1/styles/polygon/metadata?f=html");

        assertEquals("Title: " + POLYGON_TITLE, doc.select("#title").text());
        assertEquals(
                "Description: " + POLYGON_ABSTRACT, doc.select("#description").text());
        assertEquals("Point of contact: " + POLYGON_POC, doc.select("#poc").text());

        assertEquals(
                "Stylesheet as SLD 1.0.0 (native)",
                doc.select("#stylesheets>ul>:eq(0)").text());
        assertEquals(
                "Default Polygon: polygon.",
                doc.select("#layers>ul>li").first().textNodes().get(0).text().trim());
    }

    @Test
    public void testGetMetadataHTMLRemovedInlineJS() throws Exception {
        String html = getAsString("ogc/styles/v1/styles/polygon/metadata?f=html");
        assertThat(
                html,
                containsString(
                        "<script src=\"http://localhost:8080/geoserver/webresources/ogcapi/common.js\"></script>"));
        assertThat(html, containsString("form-select-open-basic"));
        assertThat(html, not(containsString("onchange")));
    }

    @Test
    public void testGetMetadataAttributesFromStyle() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/" + BUILDINGS_LABEL_STYLE + "/metadata", 200);
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
                getAsJSONPath("ogc/styles/v1/styles/" + BUILDINGS_LABEL_ASSOCIATED_STYLE + "/metadata", 200);
        assertEquals("BuildingsLabelAssociated", json.read("id"));

        // layers
        assertEquals(Integer.valueOf(1), (Integer) json.read("layers.size()"));
        assertEquals("Buildings", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));

        // sample data, vector items
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ABuildingsLabels/items?f=application%2Fgeo%2Bjson&limit=50",
                readSingle(
                        json,
                        "layers[?(@.id == 'Buildings')].sampleData[?(@.rel == 'data' && @.type == 'application/geo+json')].href"));
        // sample data, tiles
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ABuildingsLabels%2Ftiles?f=application%2Fvnd.mapbox-vector-tile",
                readSingle(
                        json,
                        "layers[?(@.id == 'Buildings')].sampleData[?(@.rel == 'tiles' && @.type == 'application/vnd.mapbox-vector-tile')].href"));

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
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/" + TASMANIA + "/metadata", 200);
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
    public void testGetMetadataMultilayerSampleData() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/" + BUILDINGS_LAKES + "/metadata", 200);
        assertEquals("buildingsLakes", json.read("id"));
        assertEquals(Integer.valueOf(2), (Integer) json.read("layers.size()"));

        // Water Bodies
        assertEquals("Lakes", json.read("layers[0].id"));
        assertEquals("polygon", json.read("layers[0].type"));
        // ... check the sample data link, make sure there is only one (there used to be extra
        // layers)
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ALakes/items?f=application%2Fgeo%2Bjson&limit=50",
                readSingle(json, "layers[0].sampleData[?(@.type=='application/geo+json')].href"));

        // BasicPolygons
        assertEquals("Buildings", json.read("layers[1].id"));
        assertEquals("polygon", json.read("layers[1].type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ABuildings/items?f=application%2Fgeo%2Bjson&limit=50",
                readSingle(json, "layers[1].sampleData[?(@.type=='application/geo+json')].href"));
    }

    @Test
    public void testGetMetadataFromCSSStyle() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/styles/cssSample/metadata", 200);
        assertEquals("cssSample", json.read("id"));
        assertEquals("A CSS style", json.read("title"));
        assertEquals("A simple polygon fill in CSS", json.read("description"));
        assertEquals("Andrea Aime", json.read("pointOfContact"));
        assertEquals("style", json.read("scope"));
        assertEquals("unclassified", json.read("accessConstraints"));

        // at least CSS, SLD 1.0 and 1.1
        assertThat(json.read("stylesheets.size()", Integer.class), Matchers.greaterThanOrEqualTo(3));

        // sld 1.is not native, CSS is
        assertFalse((boolean) readSingle(json, "stylesheets[?(@.title =~ /.*SLD 1.0.*/)].native"));
        assertTrue((boolean) readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].native"));

        // some checks on the CSS one
        assertEquals("Stylesheet as CSS 1.0.0", readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].title"));
        assertEquals("1.0.0", readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].version"));
        assertEquals(
                "https://docs.geoserver.org/latest/en/user/styling/css/index.html",
                readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].specification"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/cssSample?f=application%2Fvnd.geoserver.geocss%2Bcss",
                readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].link.href"));
        assertEquals(
                "application/vnd.geoserver.geocss+css",
                readSingle(json, "stylesheets[?(@.title =~ /.*CSS.*/)].link.type"));
    }

    @Test
    public void testPutStyleMetadata() throws Exception {
        String metadataJson = IOUtils.toString(StyleTest.class.getResourceAsStream("polygonStyleMetadata.json"));
        MockHttpServletResponse response = putAsServletResponse(
                "ogc/styles/v1/styles/polygon/metadata", metadataJson, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(204, response.getStatus());

        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        StyleMetadataInfo metadata = polygon.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        assertEquals("A polygon style with a twist", metadata.getTitle());
        assertEquals("Draws polygons with gray fill. Gray is the new black!", metadata.getAbstract());
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

        String jsonPatch = IOUtils.toString(StyleTest.class.getResourceAsStream("metadataPatch.json"));
        MockHttpServletResponse response = patchAsServletResponse(
                "ogc/styles/v1/styles/polygon/metadata", jsonPatch, MediaType.APPLICATION_JSON_VALUE);
        assertEquals(204, response.getStatus());

        StyleInfo polygon = getCatalog().getStyleByName("polygon");
        StyleMetadataInfo metadata = polygon.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
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

    /** Read and put back, check it can be written as it was produced */
    @Test
    public void testRoundTrip() throws Exception {
        String metadatPath = "ogc/styles/v1/styles/raster/metadata";
        String payload = getAsString(metadatPath);
        MockHttpServletResponse response = putAsServletResponse(metadatPath, payload, "application/json");
        assertEquals(response.getContentAsString(), 204, response.getStatus());

        // check nothing changed
        testGetMetadataFromRasterStyle();
    }
}
