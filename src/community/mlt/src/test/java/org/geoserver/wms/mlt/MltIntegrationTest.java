/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mlt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.maplibre.mlt.data.Feature;
import org.maplibre.mlt.data.Layer;
import org.maplibre.mlt.decoder.MltDecoder;
import org.springframework.mock.web.MockHttpServletResponse;

public class MltIntegrationTest extends WMSTestSupport {

    /** The standard road segments request, in MLT format. */
    private String roadSegments() {
        return roadSegments(MltTileBuilderFactory.MIME_TYPE);
    }

    private String roadSegments(String format) {
        return "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326&format="
                + format;
    }

    /** Runs a GetMap and decodes the MLT response into layers. */
    private List<Layer> getAsMlt(String request) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertEquals(MltTileBuilderFactory.MIME_TYPE, response.getContentType());
        return MltDecoder.decodeMlTile(response.getContentAsByteArray()).layers();
    }

    @Test
    public void testRoadSegments() throws Exception {
        List<Layer> layers = getAsMlt(roadSegments());
        assertEquals(1, layers.size());

        Layer roads = layers.get(0);
        assertEquals(MockData.ROAD_SEGMENTS.getLocalPart(), roads.name());
        assertEquals(5, roads.features().size());
        assertEquals(3, countByName(roads, "Route 5"));
        assertEquals(1, countByName(roads, "Main Street"));
        assertEquals(1, countByName(roads, "Dirt Road by Green Forest"));
        assertEquals(0, countByName(roads, "No Such Road"));

        Set<String> geometryTypes = roads.features().stream()
                .map(f -> f.geometry().getGeometryType())
                .collect(Collectors.toSet());
        assertEquals(Set.of("LineString"), geometryTypes);
    }

    /** Tile coordinates are oversampled 16 times, so a 768 pixel wide request encodes at 12288. */
    @Test
    public void testTileExtent() throws Exception {
        List<Layer> layers = getAsMlt(roadSegments());
        assertEquals(768 * 16, layers.get(0).tileExtent());
    }

    /** The short "mlt" format name selects the same output format. */
    @Test
    public void testShortFormatName() throws Exception {
        List<Layer> layers = getAsMlt(roadSegments("mlt"));
        assertEquals(1, layers.size());
        Layer roads = layers.get(0);
        assertEquals(MockData.ROAD_SEGMENTS.getLocalPart(), roads.name());
        assertEquals(5, roads.features().size());
        assertEquals(3, countByName(roads, "Route 5"));
    }

    /** A filtered request encodes only the matching features. */
    @Test
    public void testCqlFilter() throws Exception {
        List<Layer> layers = getAsMlt(roadSegments() + "&CQL_FILTER=NAME='Main Street'");
        assertEquals(1, layers.size());
        Layer roads = layers.get(0);
        assertEquals(1, roads.features().size());
        assertEquals(1, countByName(roads, "Main Street"));
        assertEquals(0, countByName(roads, "Route 5"));
    }

    /** A filter matching nothing returns an empty tile that still decodes. */
    @Test
    public void testCqlFilterNoMatch() throws Exception {
        assertEquals(List.of(), getAsMlt(roadSegments() + "&CQL_FILTER=1=0"));
    }

    /** The numeric part of the requested feature id is encoded as the MLT feature id. */
    @Test
    public void testFilterById() throws Exception {
        List<Layer> layers = getAsMlt(roadSegments() + "&featureId=RoadSegments.1107532045091");
        assertEquals(1, layers.size());
        List<Feature> features = layers.get(0).features();
        assertEquals(1, features.size());
        assertEquals(1, countByName(layers.get(0), "Dirt Road by Green Forest"));
        assertEquals(1107532045091L, features.get(0).id());
    }

    /** Tiles are encoded in the requested CRS, not only in 4326. */
    @Test
    public void testWebMercator() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-200000,-200000,200000,200000&width=256&height=256&srs=EPSG:3857&format="
                + MltTileBuilderFactory.MIME_TYPE;
        List<Layer> layers = getAsMlt(request);
        assertEquals(1, layers.size());
        assertEquals(5, layers.get(0).features().size());
        assertEquals(256 * 16, layers.get(0).tileExtent());
    }

    /** Polygon layers round trip through the pipeline as polygons. */
    @Test
    public void testPolygonLayer() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&bbox=-2,-1,2,6&width=256&height=256&srs=EPSG:4326&format="
                + MltTileBuilderFactory.MIME_TYPE;
        List<Layer> layers = getAsMlt(request);
        assertEquals(1, layers.size());
        Layer polygons = layers.get(0);
        assertEquals(MockData.BASIC_POLYGONS.getLocalPart(), polygons.name());
        assertEquals(3, polygons.features().size());
        Set<String> types = polygons.features().stream()
                .map(f -> f.geometry().getGeometryType())
                .collect(Collectors.toSet());
        assertEquals(Set.of("Polygon"), types);
        for (Feature feature : polygons.features()) {
            assertTrue("polygon area", feature.geometry().getArea() > 0);
        }
    }

    private static long countByName(Layer layer, String name) {
        return layer.features().stream()
                .filter(f -> name.equals(f.properties().get("NAME")))
                .count();
    }
}
