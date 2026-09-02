/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mlt;

import static org.geoserver.wms.mlt.MltTileBuilderFactory.MIME_TYPE;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileBuilder;
import org.geotools.util.Converters;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.maplibre.mlt.converter.ConversionConfig;
import org.maplibre.mlt.converter.MltConverter;
import org.maplibre.mlt.converter.mvt.ColumnMappingConfig;
import org.maplibre.mlt.converter.mvt.MapboxVectorTile;
import org.maplibre.mlt.data.Feature;
import org.maplibre.mlt.data.Layer;
import org.maplibre.mlt.metadata.tileset.MltMetadata;

/**
 * Collects features into a MapLibre Tile.
 *
 * <p>Not thread safe, the vector tile pipeline uses one instance per tile. Encoding follows the MapLibre Tile
 * specification as implemented by the {@code org.maplibre:mlt} library, see the version in the module pom.
 */
public class MltTileBuilder implements VectorTileBuilder {

    private static final ColumnMappingConfig NO_COLUMN_MAPPINGS = new ColumnMappingConfig();

    /**
     * Largest tile extent that can use Morton vertex encoding. The encoder interleaves the two coordinates of a vertex
     * into a single {@code int}, which needs two bits per coordinate bit, so a wider extent overflows and corrupts the
     * geometries without any error.
     */
    private static final int MAX_MORTON_EXTENT = 16384;

    private final int extent;

    private final Map<String, List<Feature>> featuresByLayer = new LinkedHashMap<>();

    public MltTileBuilder(Rectangle mapSize) {
        this.extent = Math.max(mapSize.width, mapSize.height);
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties) {
        List<Feature> features = featuresByLayer.computeIfAbsent(layerName, name -> new ArrayList<>());
        addParts(features, geometry, numericId(featureId), encodableProperties(properties));
    }

    /**
     * Adds one feature per encodable geometry. A general GeometryCollection is the only type the encoder cannot write,
     * so it is the only one split into several features, all sharing attributes and identifier.
     */
    private static void addParts(List<Feature> features, Geometry geometry, Long id, Map<String, Object> properties) {
        if (geometry == null || geometry.isEmpty()) return;
        if (isGeneralCollection(geometry)) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                addParts(features, geometry.getGeometryN(i), id, properties);
            }
            return;
        }
        features.add(id == null ? new Feature(geometry, properties) : new Feature(id, geometry, properties));
    }

    /** True for a GeometryCollection that is not one of the single-type collection subtypes. */
    private static boolean isGeneralCollection(Geometry geometry) {
        return geometry instanceof GeometryCollection
                && !(geometry instanceof MultiPoint
                        || geometry instanceof MultiLineString
                        || geometry instanceof MultiPolygon);
    }

    /**
     * Returns the identifier to encode, or null to leave this feature without one. Features with and without an
     * identifier can be mixed, the encoder writes a nullable identifier column.
     */
    private static Long numericId(String featureId) {
        int start = featureId.lastIndexOf('.') + 1;
        if (start == featureId.length()) return null;
        for (int i = start; i < featureId.length(); i++) {
            // a negative value would be written as a large unsigned one, so digits only
            if (!Character.isDigit(featureId.charAt(i))) return null;
        }
        try {
            return Long.valueOf(featureId.substring(start));
        } catch (NumberFormatException e) {
            // more digits than a long can hold
            return null;
        }
    }

    /**
     * Returns the attributes in a form the encoder accepts. Values of an unsupported type make it fail, and a null
     * value makes it fail too, so both are converted or left out.
     */
    private static Map<String, Object> encodableProperties(Map<String, Object> properties) {
        if (properties.values().stream().allMatch(MltTileBuilder::isEncodableValue)) return properties;
        Map<String, Object> encodable = new LinkedHashMap<>();
        properties.forEach((name, value) -> {
            if (isEncodableValue(value)) encodable.put(name, value);
            else if (value != null) encodable.put(name, convertValue(value));
        });
        return encodable;
    }

    /** Lists the value types the encoder maps to a column type of its own. */
    private static boolean isEncodableValue(Object value) {
        return value instanceof String
                || value instanceof Boolean
                || value instanceof Byte
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double;
    }

    /** Widens any other number to a double, and renders everything else as text, dates included. */
    private static Object convertValue(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        String text = Converters.convert(value, String.class);
        return text != null ? text : value.toString();
    }

    @Override
    public RawMap build(WMSMapContent mapContent) throws IOException {
        List<Layer> layers = featuresByLayer.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new Layer(entry.getKey(), entry.getValue(), extent))
                .collect(Collectors.toList());
        return new RawMap(mapContent, encode(layers), MIME_TYPE);
    }

    private byte[] encode(List<Layer> layers) throws IOException {
        MapboxVectorTile tile = new MapboxVectorTile(layers);
        ConversionConfig config = ConversionConfig.builder()
                .includeIds(true)
                .useFastPFOR(true)
                // the mlt jar ships no FSST native library
                .useFSST(false)
                .mismatchPolicy(ConversionConfig.TypeMismatchPolicy.COERCE)
                // sorting reorders the features, losing the drawing order the style asked for
                .optimizations(Map.of())
                .useMortonEncoding(extent <= MAX_MORTON_EXTENT)
                // pre-tessellation and polygon outlines need client support
                .preTessellatePolygons(false)
                .build();
        MltMetadata.TileSetMetadata metadata =
                MltConverter.createTilesetMetadata(tile, config, NO_COLUMN_MAPPINGS, true);
        try {
            return MltConverter.convertMvt(tile, metadata, config, null);
        } catch (RuntimeException e) {
            List<String> names = layers.stream().map(Layer::name).collect(Collectors.toList());
            throw new ServiceException("Failed to encode MapLibre tile for layers " + names, e);
        }
    }
}
