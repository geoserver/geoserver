package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;

import java.util.List;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileDecoder.Feature;
import no.ecc.vectortile.VectorTileDecoder.FeatureIterable;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Mapbox protobuf encoding test cases */
public class GetFeatureMapboxOutputFormatTest extends WFS3TestSupport {

    /** Tests mapbox protobuf encoding for features in items */
    @Test
    public void testItemsFeatureBuildingsEncode() throws Exception {
        // get ptb inputstream
        String roadSegments = getEncodedName(MockData.BUILDINGS);
        String path =
                "wfs3/collections/"
                        + roadSegments
                        + "/items?f=application%2Fx-protobuf%3Btype%3Dmapbox-vector&resolution=10000";
        MockHttpServletResponse response = getAsServletResponse(path);
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        FeatureIterable fiter = decoder.decode(responseBytes);
        List<Feature> featuresList = fiter.asList();
        assertEquals(2, featuresList.size());
        assertEquals("cite__Buildings", featuresList.get(0).getLayerName());
        assertEquals(
                "POLYGON ((0 256, 0 153.6, 64 153.6, 64 256, 0 256))",
                featuresList.get(0).getGeometry().toText());
        assertEquals("cite__Buildings", featuresList.get(1).getLayerName());
        assertEquals(
                "POLYGON ((192 102.4, 192 0, 256 0, 256 102.4, 192 102.4))",
                featuresList.get(1).getGeometry().toText());
    }

    /** Tests mapbox protobuf encoding for features in items */
    @Test
    public void testItemsFeatureRoadsEncode() throws Exception {
        // get ptb inputstream
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        String path =
                "wfs3/collections/"
                        + roadSegments
                        + "/items?f=application%2Fx-protobuf%3Btype%3Dmapbox-vector&resolution=10000";
        MockHttpServletResponse response = getAsServletResponse(path);
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        FeatureIterable fiter = decoder.decode(responseBytes);
        List<Feature> featuresList = fiter.asList();
        assertEquals(5, featuresList.size());
        assertEquals("cite__RoadSegments", featuresList.get(0).getLayerName());
        assertEquals(
                "LINESTRING (0 160, 30.464 144, 48.768 133.3248, 85.3248 117.3248, 134.0928 90.6752)",
                featuresList.get(0).getGeometry().toText());
        assertEquals("cite__RoadSegments", featuresList.get(1).getLayerName());
        assertEquals(
                "LINESTRING (134.0928 90.6752, 170.6752 74.6752, 213.3248 53.3248)",
                featuresList.get(1).getGeometry().toText());
    }

    /** Tests mapbox protobuf encoding for features in items, with bbox filter */
    @Test
    public void testItemsFeatureBuildingsBBOX() throws Exception {
        // get ptb inputstream
        String roadSegments = getEncodedName(MockData.BUILDINGS);
        // real building bbox is BBOX=0.002,0.001,0.0024,0.0008
        // we will crop it to BBOX=0.002,0.001,0.0024,0.0003
        String path =
                "wfs3/collections/"
                        + roadSegments
                        + "/items?f=application%2Fx-protobuf%3Btype%3Dmapbox-vector&resolution=10000"
                        + "&BBOX=0.002,0.001,0.0024,0.0003";
        MockHttpServletResponse response = getAsServletResponse(path);
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        FeatureIterable fiter = decoder.decode(responseBytes);
        List<Feature> featuresList = fiter.asList();
        assertEquals(1, featuresList.size());
        assertEquals("cite__Buildings", featuresList.get(0).getLayerName());
        assertEquals(
                "POLYGON ((0 73.1392, 0 0, 256 0, 256 73.1392, 0 73.1392))",
                featuresList.get(0).getGeometry().toText());
    }
}
