package org.geoserver.ogcapi.v1.tiles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class TilesetDescriptionTest extends TilesTestSupport {

    @Test
    public void getDataTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/tiles/v1/collections/" + roadSegments + "/tiles/EPSG:4326", 200);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/tiles/EPSG:4326/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                readSingle(
                        json,
                        "$.links[?(@.rel=='item' && @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\/EPSG:4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\/EPSG:4326\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // test the describedBy template
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].href"));
        assertEquals(
                Boolean.TRUE,
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].templated"));

        checkRoadSegmentsTileMatrix(json, Tileset.DataType.vector, true);
    }

    @Test
    public void testTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/tiles/EPSG:4326/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
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
    @SuppressWarnings("unchecked")
    public void testTileJSONLayerGroup() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/"
                                + NATURE_GROUP
                                + "/tiles/EPSG:900913/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("nature"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/nature/tiles/EPSG:900913/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
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
        getAsJSoup("ogc/tiles/v1/collections/" + roadSegments + "/tiles?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    @Test
    public void getMapTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/tiles/v1/collections/" + roadSegments + "/map/tiles/EPSG:4326", 200);

        // check the rendered tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/map/tiles/EPSG:4326/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fpng",
                readSingle(json, "$.links[?(@.rel=='item' && @.type=='image/png')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/map/tiles/EPSG:4326/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fjpeg",
                readSingle(json, "$.links[?(@.rel=='item' && @.type=='image/jpeg')].href"));
        // check the info links for the rendered outputs
        List<String> infoFormats =
                ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        for (String infoFormat : infoFormats) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/map/tiles/EPSG:4326/{tileMatrix}/{tileRow}/{tileCol}/info?f="
                            + ResponseUtils.urlEncode(infoFormat),
                    readSingle(
                            json,
                            "$.links[?(@.rel=='info' && @.type=='" + infoFormat + "')].href"));
        }

        // test the describedBy template
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/map/tiles/EPSG:4326/metadata?f=application%2Fjson&tileFormat={tileFormat}",
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].href"));
        assertEquals(
                Boolean.TRUE,
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].templated"));

        // check the tile matrices
        checkRoadSegmentsTileMatrix(json, Tileset.DataType.map, true);
    }

    @Test
    public void testMapTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/cite:RoadSegments/map/tiles/EPSG:4326/metadata?f=application%2Fjson&tileFormat=image%2Fpng8",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/map/tiles/EPSG:4326/{z}/{y}/{x}?f=image%2Fpng8"));
        assertThat(doc.read("center"), equalTo(Arrays.asList(0d, 0d, 0d)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-0.0042, -0.0024, 0.0042, 0.0024)));
        assertFalse(exists(doc, "vector_layers"));
    }

    @Test
    public void testTileJSONLayerGroupZoomLevelsLayerMetadata() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/"
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
                        "/ogc/tiles/v1/collections/"
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

    public void checkRoadSegmentsTileMatrix(
            DocumentContext json, Tileset.DataType dataType, boolean checkLimits) {
        // check the tile matrices
        String matrixDefinition =
                "http://localhost:8080/geoserver/ogc/tiles/v1/tileMatrixSets/EPSG%3A4326";
        assertEquals(matrixDefinition, json.read("tileMatrixSetURI"));
        assertEquals(matrixDefinition, json.read("tileMatrixSetDefinition"));
        assertEquals(dataType.toString(), json.read("dataType"));
        if (checkLimits) {
            assertEquals(Integer.valueOf(22), json.read("tileMatrixSetLimits.size()"));
            String crs84Limit0 = "tileMatrixSetLimits[0]";
            assertEquals("EPSG:4326:0", json.read(crs84Limit0 + ".tileMatrix"));
            // both tiles as it spans the origin
            assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileRow"));
            assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".maxTileRow"));
            assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileCol"));
            assertEquals(Integer.valueOf(1), json.read(crs84Limit0 + ".maxTileCol"));
            // one more zoom level just for satefy
            String crs84Limit10 = "tileMatrixSetLimits[10]";
            assertEquals("EPSG:4326:10", json.read(crs84Limit10 + ".tileMatrix"));
            assertEquals(Integer.valueOf(511), json.read(crs84Limit10 + ".minTileRow"));
            assertEquals(Integer.valueOf(512), json.read(crs84Limit10 + ".maxTileRow"));
            assertEquals(Integer.valueOf(1023), json.read(crs84Limit10 + ".minTileCol"));
            assertEquals(Integer.valueOf(1024), json.read(crs84Limit10 + ".maxTileCol"));
        }
    }
}
