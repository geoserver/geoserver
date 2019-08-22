/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.junit.Test;

public class CollectionTest extends TilesTestSupport {

    @Test
    public void testRoadsCollectionJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/tiles/collections/" + roadSegments, 200);

        testRoadsCollectionJson(json);
    }

    public void testRoadsCollectionJson(DocumentContext json) {
        assertEquals("cite:RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));
        assertEquals(-0.0042, json.read("$.extent.spatial[0]", Double.class), 0d);
        assertEquals(-0.0024, json.read("$.extent.spatial[1]", Double.class), 0d);
        assertEquals(0.0042, json.read("$.extent.spatial[2]", Double.class), 0d);
        assertEquals(0.0024, json.read("$.extent.spatial[3]", Double.class), 0d);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                getSingle(
                        json,
                        "$.links[?(@.rel=='tiles' && @.type=='application/vnd.mapbox-vector-tile')].href"));
        // and the rendered tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/maps/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fpng",
                getSingle(json, "$.links[?(@.rel=='tiles' && @.type=='image/png')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/maps/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=image%2Fjpeg",
                getSingle(json, "$.links[?(@.rel=='tiles' && @.type=='image/jpeg')].href"));
        // check the info links for the rendered outputs
        List<String> infoFormats =
                ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        for (String infoFormat : infoFormats) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ARoadSegments/maps/{styleId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}/info?f="
                            + ResponseUtils.urlEncode(infoFormat),
                    getSingle(
                            json,
                            "$.links[?(@.rel=='info' && @.type=='" + infoFormat + "')].href"));
        }

        // check the tile matrices
        assertEquals(Integer.valueOf(2), json.read("$.tiles.tileMatrixSetLinks.size()"));
        // EPSG:4326
        assertEquals("EPSG:4326", json.read("$.tiles.tileMatrixSetLinks[0].tileMatrixSet"));
        assertEquals(
                "http://temporary/url/EPSG:4326",
                json.read("$.tiles.tileMatrixSetLinks[0].tileMatrixSetURI"));
        assertEquals(
                Integer.valueOf(22),
                json.read("$.tiles.tileMatrixSetLinks[0].tileMatrixSetLimits.size()"));
        String crs84Limit0 = "$.tiles.tileMatrixSetLinks[0].tileMatrixSetLimits[0]";
        assertEquals("EPSG:4326:0", json.read(crs84Limit0 + ".tileMatrix"));
        // both tiles as it spans the origin
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".maxTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileCol"));
        assertEquals(Integer.valueOf(1), json.read(crs84Limit0 + ".maxTileCol"));
        // one more zoom level just for satefy
        String crs84Limit10 = "$.tiles.tileMatrixSetLinks[0].tileMatrixSetLimits[10]";
        assertEquals("EPSG:4326:10", json.read(crs84Limit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(crs84Limit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(crs84Limit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(1023), json.read(crs84Limit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(1024), json.read(crs84Limit10 + ".maxTileCol"));
        // checking one in web marcator too (only one root tile)
        String webMercatorLimit10 = "$.tiles.tileMatrixSetLinks[1].tileMatrixSetLimits[10]";
        assertEquals("EPSG:900913:10", json.read(webMercatorLimit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileCol"));

        // styles
        assertEquals(Integer.valueOf(2), json.read("$.styles.size()"));
        assertEquals("RoadSegments", json.read("$.styles[0].id"));
        assertEquals("Default Styler", json.read("$.styles[0].title"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/styles/RoadSegments?f=application%2Fvnd.ogc.sld%2Bxml",
                getSingle(
                        json,
                        "$.styles[0].links[?(@.rel=='stylesheet' && @.type=='application/vnd.ogc.sld+xml')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/styles/RoadSegments/metadata?f=application%2Fjson",
                getSingle(
                        json,
                        "$.styles[0].links[?(@.rel=='describedBy' && @.type=='application/json')].href"));

        assertEquals("generic", json.read("$.styles[1].id"));
        assertEquals("Generic", json.read("$.styles[1].title"));
    }

    @Test
    public void testRoadsCollectionYaml() throws Exception {
        String yaml =
                getAsString(
                        "ogc/tiles/collections/"
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testRoadsCollectionJson(json);
    }
}
