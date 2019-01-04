/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.net.URLEncoder;
import java.util.List;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileDecoder.Feature;
import no.ecc.vectortile.VectorTileDecoder.FeatureIterable;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs3.response.RFCGeoJSONFeaturesResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Test cases for GetTile requests */
public class GetTileTest extends WFS3TestSupport {

    /** Tests getTile with MVT format */
    @Test
    public void testGetTileMTV() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        String path =
                "wfs3/collections/" + roadSegments + "/tiles/GlobalCRS84Geometric/12/2047/4095";
        MockHttpServletResponse response = getAsServletResponse(path);
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        FeatureIterable fiter = decoder.decode(responseBytes);
        List<Feature> featuresList = fiter.asList();
        assertEquals(2, featuresList.size());
        assertEquals("cite__RoadSegments", featuresList.get(0).getLayerName());
        assertEquals(
                "LINESTRING (232 259, 241 257, 248 255, 257 252)",
                featuresList.get(0).getGeometry().toText());
        assertEquals("cite__RoadSegments", featuresList.get(1).getLayerName());
        assertEquals("LINESTRING (248 270, 248 255)", featuresList.get(1).getGeometry().toText());
    }

    /** Tests getTile with MVT format, Web Mercator projection */
    @Test
    public void testGetTileMTVMercator() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        String path =
                "wfs3/collections/" + roadSegments + "/tiles/GoogleMapsCompatible/16/32768/32767";
        MockHttpServletResponse response = getAsServletResponse(path);
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        FeatureIterable fiter = decoder.decode(responseBytes);
        List<Feature> featuresList = fiter.asList();
        assertEquals(2, featuresList.size());
        assertEquals("cite__RoadSegments", featuresList.get(0).getLayerName());
        assertEquals(
                "LINESTRING (60 28, 107 14, 135 5, 191 -9, 225 -20)",
                featuresList.get(0).getGeometry().toText());
        assertEquals("cite__RoadSegments", featuresList.get(1).getLayerName());
        assertEquals("LINESTRING (191 112, 191 -9)", featuresList.get(1).getGeometry().toText());
    }

    /** Tests getTile with GeoJSON format */
    @Test
    public void testGetTileGeoJSON() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        String path =
                "wfs3/collections/"
                        + roadSegments
                        + "/tiles/GlobalCRS84Geometric/12/2047/4095"
                        + "?f="
                        + URLEncoder.encode(RFCGeoJSONFeaturesResponse.MIME, "UTF-8");
        DocumentContext jsdoc = getAsJSONPath(path, 200);
        // features[0].geometry.type = "MultiLineString"
        assertEquals(jsdoc.read("features[0].geometry.type", String.class), "MultiLineString");
        List<Double> list = jsdoc.read("features[0].geometry.coordinates[0][0]", List.class);
        assertEquals(-0.0042d, list.get(0), 0.0001d);
        assertEquals(-6.0E-4d, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][1]", List.class);
        assertEquals(-0.0032, list.get(0), 0.0001d);
        assertEquals(-3.0E-4, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][2]", List.class);
        assertEquals(-0.0026, list.get(0), 0.0001d);
        assertEquals(-1.0E-4, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][3]", List.class);
        assertEquals(-0.0014, list.get(0), 0.0001d);
        assertEquals(2.0E-4, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][4]", List.class);
        assertEquals(2.0E-4, list.get(0), 0.0001d);
        assertEquals(7.0E-4, list.get(1), 0.0001d);
        // "numberReturned":2
        assertEquals(jsdoc.read("numberReturned", Integer.class).intValue(), 2);
    }

    /** Tests getTile with GeoJSON format, Web Mercator tilingSchema */
    @Test
    public void testGetTileGeoJSONMercator() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        String path =
                "wfs3/collections/"
                        + roadSegments
                        + "/tiles/GoogleMapsCompatible/16/32768/32767"
                        + "?f="
                        + URLEncoder.encode(RFCGeoJSONFeaturesResponse.MIME, "UTF-8");
        ;
        DocumentContext jsdoc = getAsJSONPath(path, 200);
        assertEquals(jsdoc.read("features[0].geometry.type", String.class), "MultiLineString");
        List<Double> list = jsdoc.read("features[0].geometry.coordinates[0][0]", List.class);
        assertEquals(-467.54, list.get(0), 0.0001d);
        assertEquals(-66.79, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][1]", List.class);
        assertEquals(-356.22, list.get(0), 0.0001d);
        assertEquals(-33.4, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][2]", List.class);
        assertEquals(-289.43, list.get(0), 0.0001d);
        assertEquals(-11.13, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][3]", List.class);
        assertEquals(-155.85, list.get(0), 0.0001d);
        assertEquals(22.26, list.get(1), 0.0001d);
        list = jsdoc.read("features[0].geometry.coordinates[0][4]", List.class);
        assertEquals(22.26, list.get(0), 0.0001d);
        assertEquals(77.92, list.get(1), 0.0001d);
        // "numberReturned":2
        assertEquals(jsdoc.read("numberReturned", Integer.class).intValue(), 2);
    }
}
