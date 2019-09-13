package org.geoserver.api.tiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class TileDescriptionTest extends TilesTestSupport {

    @Test
    public void getDataTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/tiles/collections/" + roadSegments + "/tiles", 200);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                getSingle(
                        json,
                        "$.links[?(@.rel=='tile' && @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/collections\\/cite:RoadSegments\\/tiles\\?.*/)].rel"),
                Matchers.containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/collections\\/cite:RoadSegments\\/tiles\\?.*/ && @.rel == 'alternate')].type"),
                Matchers.hasItems("application/xml", "application/x-yaml"));

        checkRoadSegmentsTileMatrix(json);
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
                "http://temporary/url/EPSG:4326",
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
                getSingle(json, "$.links[?(@.rel=='tile' && @.type=='image/png')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fjpeg",
                getSingle(json, "$.links[?(@.rel=='tile' && @.type=='image/jpeg')].href"));
        // check the info links for the rendered outputs
        List<String> infoFormats =
                ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        for (String infoFormat : infoFormats) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/map/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}/info?f="
                            + ResponseUtils.urlEncode(infoFormat),
                    getSingle(
                            json,
                            "$.links[?(@.rel=='info' && @.type=='" + infoFormat + "')].href"));
        }

        // check the tile matrices
        checkRoadSegmentsTileMatrix(json);
    }
}
