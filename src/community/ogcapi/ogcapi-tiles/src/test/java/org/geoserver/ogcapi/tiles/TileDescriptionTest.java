package org.geoserver.ogcapi.tiles;

import static org.geoserver.data.test.MockData.CITE_PREFIX;
import static org.geoserver.data.test.MockData.CITE_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geotools.feature.NameImpl;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class TileDescriptionTest extends TilesTestSupport {

    static final String FOREST_WITH_SCALES_STYLE = "ForestsWithScaleDenominator";

    static final String LAKES_WITH_SCALES_STYLE = "LakesWithScaleDenominator";

    static final String NATURES_ZOOM_GROUP = "NatureZoom";

    public static QName FORESTS_ZOOM = new QName(CITE_URI, "ForestsZoom", CITE_PREFIX);

    public static QName LAKES_ZOOM = new QName(CITE_URI, "LakesZoom", CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle(FOREST_WITH_SCALES_STYLE, getClass(), getCatalog());
        testData.addStyle(LAKES_WITH_SCALES_STYLE, getClass(), getCatalog());

        Map<SystemTestData.LayerProperty, Object> properties = new HashMap<>();
        properties.put(SystemTestData.LayerProperty.STYLE, FOREST_WITH_SCALES_STYLE);
        testData.addVectorLayer(
                FORESTS_ZOOM, properties, "ForestsZoomTest.properties", getClass(), catalog);
        properties.put(SystemTestData.LayerProperty.STYLE, LAKES_WITH_SCALES_STYLE);
        testData.addVectorLayer(
                LAKES_ZOOM, properties, "LakesZoomTest.properties", getClass(), catalog);

        LayerInfo lakesZoom =
                catalog.getLayerByName(
                        new NameImpl(LAKES_ZOOM.getNamespaceURI(), LAKES_ZOOM.getLocalPart()));
        LayerInfo forestsZoom =
                catalog.getLayerByName(
                        new NameImpl(FORESTS_ZOOM.getNamespaceURI(), FORESTS_ZOOM.getLocalPart()));

        addVectorTileFormats(getLayerId(LAKES_ZOOM), true);
        addVectorTileFormats(getLayerId(FORESTS_ZOOM), true);

        createsLayerGroup(
                catalog,
                NATURES_ZOOM_GROUP,
                null,
                null,
                null,
                Arrays.asList(lakesZoom, forestsZoom));
    }

    @Test
    public void testGetTileMatrixSets() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/tileMatrixSets", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/tileMatrixSets\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/tileMatrixSets\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check some of the basic tile matrix sets are there
        assertThat(
                json.read("tileMatrixSets[*].identifier"),
                hasItems("GlobalCRS84Pixel", "EPSG:4326", "EPSG:900913"));

        // verify a specific one
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.identifier == 'EPSG:4326')].links[?(@.type == 'application/json')].href"),
                contains(
                        "http://localhost:8080/geoserver/ogc/tiles/tileMatrixSets/EPSG%3A4326?f=application%2Fjson"));
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.identifier == 'EPSG:4326')].links[?(@.type == 'application/json')].rel"),
                contains("tileMatrixSet"));
    }

    @Test
    public void testGetTileMatrixSetsHTML() throws Exception {
        Document document = getAsJSoup("ogc/tiles/tileMatrixSets?f=html");
        // TODO: add ids and actual checks in the generated HTML
    }

    @Test
    public void testGetTileMatrixSet() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/tileMatrixSets/EPSG:4326", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/tileMatrixSets\\/EPSG%3A4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/tileMatrixSets\\/EPSG%3A4326\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check basic properties
        assertThat(json.read("identifier"), equalTo("EPSG:4326"));
        assertThat(
                json.read("supportedCRS"), equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(json.read("title"), startsWith("A default WGS84"));

        // check a tile matrix definitions
        assertThat(json.read("tileMatrix[0].identifier"), equalTo("EPSG:4326:0"));
        assertThat(
                json.read("tileMatrix[0].scaleDenominator", Double.class),
                closeTo(2.7954112E8, 1e3));
        assertThat(json.read("tileMatrix[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrix[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrix[0].matrixWidth"), equalTo(2));
        assertThat(json.read("tileMatrix[0].matrixHeight"), equalTo(1));
        assertThat(
                json.read("tileMatrix[0].topLeftCorner"),
                contains(closeTo(90, 0), closeTo(-180, 0)));
    }

    @Test
    public void getDataTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/tiles/collections/" + roadSegments + "/tiles", 200);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                readSingle(
                        json,
                        "$.links[?(@.rel=='item' && @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/collections\\/cite:RoadSegments\\/tiles\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/collections\\/cite:RoadSegments\\/tiles\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // test the describedBy template
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite:RoadSegments/tiles/{tileMatrixSetId}/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].href"));
        assertEquals(
                Boolean.TRUE,
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].templated"));

        checkRoadSegmentsTileMatrix(json);
    }

    @Test
    public void testTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/tiles/EPSG:4326/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
        assertThat(doc.read("minzoom"), equalTo(0));
        assertThat(doc.read("maxzoom"), equalTo(21));
        assertThat(doc.read("center"), equalTo(Arrays.asList(0d, 0d, 0d)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-0.0042, -0.0024, 0.0042, 0.0024)));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].geometry_type"),
                equalTo("line"));
    }

    @Test
    public void testTileJSONLayerGroup() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/collections/"
                                + NATURE_GROUP
                                + "/tiles/EPSG:900913/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("nature"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/collections/nature/tiles/EPSG:900913/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
        assertThat(
                doc.read("center"),
                hasItems(closeTo(0d, 1e-6), closeTo(0d, 1e-6), closeTo(0d, 1e-6)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-180.0, -90.0, 180.0, 90.0)));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Lakes')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Lakes')].geometry_type"),
                equalTo("polygon"));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Forests')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Forests')].geometry_type"),
                equalTo("polygon"));
    }

    @Test
    public void getDataTilesMetadataHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        Document doc = getAsJSoup("ogc/tiles/collections/" + roadSegments + "/tiles?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    public void checkRoadSegmentsTileMatrix(DocumentContext json) {
        // check the tile matrices
        assertEquals(Integer.valueOf(2), json.read("$.tileMatrixSetLinks.size()"));
        // EPSG:4326
        assertEquals("EPSG:4326", json.read("$.tileMatrixSetLinks[0].tileMatrixSet"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/tileMatrixSets/EPSG%3A4326",
                json.read("$.tileMatrixSetLinks[0].tileMatrixSetURI"));
        assertEquals(
                Integer.valueOf(22),
                json.read("$.tileMatrixSetLinks[0].tileMatrixSetLimits.size()"));
        String crs84Limit0 = "$.tileMatrixSetLinks[0].tileMatrixSetLimits[0]";
        assertEquals("EPSG:4326:0", json.read(crs84Limit0 + ".tileMatrix"));
        // both tiles as it spans the origin
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".maxTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileCol"));
        assertEquals(Integer.valueOf(1), json.read(crs84Limit0 + ".maxTileCol"));
        // one more zoom level just for satefy
        String crs84Limit10 = "$.tileMatrixSetLinks[0].tileMatrixSetLimits[10]";
        assertEquals("EPSG:4326:10", json.read(crs84Limit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(crs84Limit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(crs84Limit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(1023), json.read(crs84Limit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(1024), json.read(crs84Limit10 + ".maxTileCol"));
        // checking one in web marcator too (only one root tile)
        String webMercatorLimit10 = "$.tileMatrixSetLinks[1].tileMatrixSetLimits[10]";
        assertEquals("EPSG:900913:10", json.read(webMercatorLimit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileCol"));
    }

    @Test
    public void getMapTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/tiles/collections/" + roadSegments + "/map/tiles", 200);

        // check the rendered tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fpng",
                readSingle(json, "$.links[?(@.rel=='item' && @.type=='image/png')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fjpeg",
                readSingle(json, "$.links[?(@.rel=='item' && @.type=='image/jpeg')].href"));
        // check the info links for the rendered outputs
        List<String> infoFormats =
                ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        for (String infoFormat : infoFormats) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}/info?f="
                            + ResponseUtils.urlEncode(infoFormat),
                    readSingle(
                            json,
                            "$.links[?(@.rel=='info' && @.type=='" + infoFormat + "')].href"));
        }

        // test the describedBy template
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite:RoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/metadata?f=application%2Fjson&tileFormat={tileFormat}",
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].href"));
        assertEquals(
                Boolean.TRUE,
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].templated"));

        // check the tile matrices
        checkRoadSegmentsTileMatrix(json);
    }

    @Test
    public void testMapTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/collections/cite:RoadSegments/map/RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson&tileFormat=image%2Fpng8",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/RoadSegments/tiles/EPSG:4326/{z}/{y}/{x}?f=image%2Fpng8"));
        assertThat(doc.read("center"), equalTo(Arrays.asList(0d, 0d, 0d)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-0.0042, -0.0024, 0.0042, 0.0024)));
        assertFalse(exists(doc, "vector_layers"));
    }

    @Test
    public void testTileJSONLayerGroupZoomLevelsLayerMetadata() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/collections/"
                                + NATURES_ZOOM_GROUP
                                + "/tiles/EPSG:900913/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("NatureZoom"));

        assertThat(readSingle(doc, "vector_layers[?(@.id == 'LakesZoom')].minzoom"), equalTo(10));

        assertThat(readSingle(doc, "vector_layers[?(@.id == 'LakesZoom')].maxzoom"), equalTo(13));

        assertThat(readSingle(doc, "vector_layers[?(@.id == 'ForestsZoom')].minzoom"), equalTo(13));

        assertThat(readSingle(doc, "vector_layers[?(@.id == 'ForestsZoom')].maxzoom"), equalTo(16));

        assertThat(doc.read("minzoom"), equalTo(10));
        assertThat(doc.read("maxzoom"), equalTo(16));
    }

    @Test
    public void testAdditionalAttributesTileJson() throws Exception {

        Catalog cat = getCatalog();
        LayerGroupInfo group = cat.getLayerGroupByName(NATURES_ZOOM_GROUP);

        AttributionInfo attributionInfo = new AttributionInfoImpl();
        attributionInfo.setTitle("This is the attribution title");

        String abstractTxt = "This is the layerGroup description";

        group.setAttribution(attributionInfo);
        group.setAbstract(abstractTxt);

        cat.save(group);

        LayerInfo li =
                cat.getLayerByName(
                        new NameImpl(FORESTS_ZOOM.getNamespaceURI(), FORESTS_ZOOM.getLocalPart()));
        String abstractTxtLi = "This is the ForestZoom layer description";
        li.setAbstract(abstractTxtLi);
        cat.save(li);

        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/collections/"
                                + NATURES_ZOOM_GROUP
                                + "/tiles/EPSG:900913/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("NatureZoom"));

        assertThat(doc.read("attribution"), equalTo("This is the attribution title"));

        assertThat(doc.read("description"), equalTo("This is the layerGroup description"));

        assertThat(doc.read("format"), equalTo("application/vnd.mapbox-vector-tile"));

        assertThat(doc.read("tilejson"), equalTo("2.2.0"));

        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'ForestsZoom')].description"),
                equalTo("This is the ForestZoom layer description"));
    }
}
