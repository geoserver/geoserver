/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mlt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.iterator.CoalescingVTIterator;
import org.geoserver.wms.vector.iterator.VTFeature;
import org.geoserver.wms.vector.iterator.VTIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.maplibre.mlt.data.Feature;
import org.maplibre.mlt.data.Layer;
import org.maplibre.mlt.decoder.MltDecoder;

public class MltTileBuilderTest {

    private static final int TILE_SIZE = 256;

    private final MltTileBuilderFactory factory = new MltTileBuilderFactory();

    private MltTileBuilder builder() {
        return factory.newBuilder(new Rectangle(TILE_SIZE, TILE_SIZE), new ReferencedEnvelope());
    }

    private static Geometry geom(String wkt) throws ParseException {
        return new WKTReader().read(wkt);
    }

    private static byte[] encode(MltTileBuilder builder) throws IOException {
        RawMap map = builder.build(mock(WMSMapContent.class));
        assertEquals(MltTileBuilderFactory.MIME_TYPE, map.getMimeType());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        bos.close();
        return bos.toByteArray();
    }

    /** Decodes the tile the builder produced, indexed by layer name. */
    private static Map<String, Layer> decode(MltTileBuilder builder) throws IOException {
        List<Layer> layers = MltDecoder.decodeMlTile(encode(builder)).layers();
        return layers.stream().collect(Collectors.toMap(Layer::name, Function.identity()));
    }

    /** Finds the single feature carrying the given name attribute. */
    private static Feature byName(Layer layer, String name) {
        List<Feature> matches = layer.features().stream()
                .filter(f -> name.equals(f.properties().get("name")))
                .collect(Collectors.toList());
        assertEquals("features named " + name, 1, matches.size());
        return matches.get(0);
    }

    @Test
    public void testGeometryTypesAndAttributes() throws Exception {
        Geometry point = geom("POINT(1 10)");
        Geometry line = geom("LINESTRING(0 0, 1 1, 2 2)");
        Geometry polygon = geom("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");

        MltTileBuilder builder = builder();
        builder.addFeature("Points", "points.1", "geom", point, Map.of("name", "point1", "rank", 3L));
        builder.addFeature("Lines", "lines.2", "geom", line, Map.of("name", "line1"));
        builder.addFeature("Polygons", "polygons.3", "geom", polygon, Map.of("name", "polygon1"));
        Geometry multiPolygon = geom("MULTIPOLYGON(((0 0, 5 0, 5 5, 0 5, 0 0)), ((6 6, 8 6, 8 8, 6 6)))");
        builder.addFeature("Polygons", "polygons.4", "geom", multiPolygon, Map.of("name", "multipolygon1"));
        Geometry multiLine = geom("MULTILINESTRING((0 0, 1 1), (5 5, 6 6))");
        builder.addFeature("Lines", "lines.5", "geom", multiLine, Map.of("name", "multiline1"));

        Map<String, Layer> layers = decode(builder);
        assertEquals(Set.of("Points", "Lines", "Polygons"), layers.keySet());

        Layer points = layers.get("Points");
        assertEquals(TILE_SIZE, points.tileExtent());
        Feature pointFeature = byName(points, "point1");
        assertTrue(pointFeature.geometry() instanceof Point);
        assertEquals(point, pointFeature.geometry());
        // MLT picks the narrowest physical type that fits the column values
        assertEquals(3, pointFeature.properties().get("rank"));
        assertTrue(pointFeature.hasId());
        assertEquals(1L, pointFeature.id());

        Feature lineFeature = byName(layers.get("Lines"), "line1");
        assertTrue(lineFeature.geometry() instanceof LineString);
        assertEquals(line, lineFeature.geometry());
        assertEquals(2L, lineFeature.id());

        Feature polygonFeature = byName(layers.get("Polygons"), "polygon1");
        assertTrue(polygonFeature.geometry() instanceof Polygon);
        assertEquals(polygon, polygonFeature.geometry());
        assertEquals(3L, polygonFeature.id());

        assertEquals(
                multiPolygon, byName(layers.get("Polygons"), "multipolygon1").geometry());
        assertEquals(multiLine, byName(layers.get("Lines"), "multiline1").geometry());
    }

    /** The MLT encoder has no GeometryCollection support, the builder splits the members out. */
    @Test
    public void testGeometryCollectionSplitIntoParts() throws Exception {
        MltTileBuilder builder = builder();
        builder.addFeature(
                "Mixed",
                "mixed.7",
                "geom",
                geom("GEOMETRYCOLLECTION(POINT(1 1), LINESTRING(2 2, 3 3))"),
                Map.of("name", "mixed1"));

        Layer mixed = decode(builder).get("Mixed");
        assertEquals(2, mixed.features().size());
        Set<String> types = mixed.features().stream()
                .map(f -> f.geometry().getGeometryType())
                .collect(Collectors.toSet());
        assertEquals(Set.of("Point", "LineString"), types);
        assertEquals(List.of(7L, 7L), mixed.features().stream().map(Feature::id).collect(Collectors.toList()));
    }

    /** Nested collections are flattened, a MultiPoint inside one stays whole. */
    @Test
    public void testNestedGeometryCollectionFlattened() throws Exception {
        MltTileBuilder builder = builder();
        builder.addFeature(
                "Mixed",
                "mixed.1",
                "geom",
                geom("GEOMETRYCOLLECTION(GEOMETRYCOLLECTION(MULTIPOINT((1 1), (2 2))), LINESTRING(0 0, 1 1))"),
                Map.of("name", "nested"));

        Layer mixed = decode(builder).get("Mixed");
        assertEquals(2, mixed.features().size());
        Set<String> types = mixed.features().stream()
                .map(f -> f.geometry().getGeometryType())
                .collect(Collectors.toSet());
        assertEquals(Set.of("MultiPoint", "LineString"), types);
    }

    @Test
    public void testEmptyGeometriesSkipped() throws Exception {
        MltTileBuilder builder = builder();
        builder.addFeature("Points", "points.1", "geom", geom("POINT EMPTY"), Map.of("name", "empty"));
        builder.addFeature("Points", "points.2", "geom", geom("POINT(5 5)"), Map.of("name", "kept"));

        Layer points = decode(builder).get("Points");
        assertEquals(1, points.features().size());
        assertEquals("kept", points.features().get(0).properties().get("name"));
    }

    /** A non-numeric id costs that one feature its id, the others keep theirs. */
    @Test
    public void testFeatureIdsMixed() throws Exception {
        MltTileBuilder builder = builder();
        builder.addFeature("Points", "points.a", "geom", geom("POINT(1 1)"), Map.of("name", "noId"));
        builder.addFeature("Points", "points.2", "geom", geom("POINT(2 2)"), Map.of("name", "numeric"));
        builder.addFeature("Points", "17", "geom", geom("POINT(3 3)"), Map.of("name", "noDot"));
        builder.addFeature("Points", "points.-5", "geom", geom("POINT(4 4)"), Map.of("name", "negative"));

        Layer points = decode(builder).get("Points");
        assertEquals(4, points.features().size());
        assertFalse(byName(points, "noId").hasId());
        assertEquals(2L, byName(points, "numeric").id());
        assertEquals(17L, byName(points, "noDot").id());
        // a negative value would be encoded as a large unsigned one
        assertFalse(byName(points, "negative").hasId());
    }

    /** Null values and types the encoder has no column for must not reach it: it throws on both. */
    @Test
    public void testUnsupportedAttributeValues() throws Exception {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", "mixed");
        properties.put("missing", null);
        properties.put("when", new Date(0));
        properties.put("amount", new BigDecimal("1.50"));
        properties.put("count", (short) 7);

        MltTileBuilder builder = builder();
        builder.addFeature("Points", "points.1", "geom", geom("POINT(1 1)"), properties);

        Feature feature = decode(builder).get("Points").features().get(0);
        assertEquals("mixed", feature.properties().get("name"));
        // the null valued attribute is left out rather than encoded
        assertFalse(feature.properties().containsKey("missing"));
        // dates are written as text, in the ISO 8601 form Converters produces (tests run in GMT)
        assertEquals("1970-01-01Z", feature.properties().get("when"));
        assertEquals(1.5d, feature.properties().get("amount"));
        assertEquals(7.0d, feature.properties().get("count"));
    }

    /** An attribute typed differently across features of one layer is coerced, not rejected. */
    @Test
    public void testAttributeTypeMismatchCoerced() throws Exception {
        MltTileBuilder builder = builder();
        builder.addFeature("Points", "points.1", "geom", geom("POINT(1 1)"), Map.of("name", "a", "size", 2));
        builder.addFeature("Points", "points.2", "geom", geom("POINT(2 2)"), Map.of("name", "b", "size", "wide"));

        Layer points = decode(builder).get("Points");
        assertEquals(2, points.features().size());
        assertEquals("2", byName(points, "a").properties().get("size"));
        assertEquals("wide", byName(points, "b").properties().get("size"));
    }

    /** Coalescing merges features that share attributes, and hands over a collection to split. */
    @Test
    public void testCoalescedFeaturesSplitBack() throws Exception {
        Map<String, Object> shared = Map.of("name", "same");
        List<VTFeature> input = List.of(
                new VTFeature("mixed.1", geom("POINT(1 1)"), shared),
                new VTFeature("mixed.2", geom("LINESTRING(2 2, 3 3)"), shared));

        MltTileBuilder builder = builder();
        try (CoalescingVTIterator coalescing = new CoalescingVTIterator(new ListVTIterator(input))) {
            int merged = 0;
            while (coalescing.hasNext()) {
                VTFeature feature = coalescing.next();
                // the coalescer collects the two geometries into a plain collection
                assertEquals("GeometryCollection", feature.getGeometry().getGeometryType());
                builder.addFeature("Mixed", feature.getFeatureId(), "geom", feature.getGeometry(), shared);
                merged++;
            }
            assertEquals(1, merged);
        }

        Layer mixed = decode(builder).get("Mixed");
        assertEquals(2, mixed.features().size());
        Set<String> types = mixed.features().stream()
                .map(f -> f.geometry().getGeometryType())
                .collect(Collectors.toSet());
        assertEquals(Set.of("Point", "LineString"), types);
    }

    /** Feeds a fixed list of features to the coalescing iterator. */
    private static class ListVTIterator implements VTIterator {
        private final Iterator<VTFeature> delegate;

        ListVTIterator(List<VTFeature> features) {
            this.delegate = features.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public VTFeature next() {
            return delegate.next();
        }

        @Override
        public void close() {}
    }

    /** A tile with no feature at all is empty, rather than a failed encode. */
    @Test
    public void testEmptyTile() throws Exception {
        assertEquals(0, encode(builder()).length);
    }
}
