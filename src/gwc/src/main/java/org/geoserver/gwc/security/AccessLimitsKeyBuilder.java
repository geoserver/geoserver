/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WMTSAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Range;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.InitializingBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds a stable, human-readable JSON cache key from an {@link AccessLimits}, or {@code null} when the limits impose
 * no content restriction. Never returns an empty string: callers rely on {@code null} vs non-null to tell unrestricted
 * from restricted.
 *
 * <p>Raster read parameter values are serialized by type. Custom {@link ParameterValueKeySerializer} beans are
 * consulted first and take priority over the built-in handling in {@link #serializeValue}; duplicate registrations for
 * one value type fail at startup. A parameter that is neither {@link IgnorableParameterRegistry ignorable} nor
 * serializable fails the build with a message naming the descriptor.
 *
 * <p>Keys over {@value #DEFAULT_MAX_KEY_LENGTH} characters (override via the {@value #MAX_KEY_LENGTH_PROPERTY} system
 * property) are trimmed by truncating the longest field values first, each replaced with its prefix followed by
 * {@code "...too long, sha is <sha256hex>"}. This keeps every field visible in the stored property file while bounding
 * total size.
 */
public class AccessLimitsKeyBuilder implements InitializingBean {

    static final String MAX_KEY_LENGTH_PROPERTY = "gwc.security.maxKeyLength";
    static final int DEFAULT_MAX_KEY_LENGTH = 65536;

    private static final String TRUNCATION_SUFFIX = "...too long, sha is ";
    // TRUNCATION_SUFFIX (20) + sha256 hex (64)
    private static final int SUFFIX_FIXED_LENGTH = TRUNCATION_SUFFIX.length() + 64;
    // minimum chars of the original value kept visible after truncation
    private static final int MIN_FIELD_PREFIX = 50;

    private static final JsonMapper MAPPER = new JsonMapper();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private List<ParameterValueKeySerializer<?>> custom;
    private final IgnorableParameterRegistry ignorable;
    private final int maxKeyLength;

    /** Spring constructor - custom serializers collected from context in {@link #afterPropertiesSet()}. */
    public AccessLimitsKeyBuilder(IgnorableParameterRegistry ignorable) {
        this(List.of(), ignorable, Integer.getInteger(MAX_KEY_LENGTH_PROPERTY, DEFAULT_MAX_KEY_LENGTH));
    }

    /** Test constructor - custom serializers supplied directly; {@link #afterPropertiesSet()} not called. */
    public AccessLimitsKeyBuilder(List<ParameterValueKeySerializer<?>> custom, IgnorableParameterRegistry ignorable) {
        this(custom, ignorable, Integer.getInteger(MAX_KEY_LENGTH_PROPERTY, DEFAULT_MAX_KEY_LENGTH));
    }

    AccessLimitsKeyBuilder(
            List<ParameterValueKeySerializer<?>> customSerializers,
            IgnorableParameterRegistry ignorable,
            int maxKeyLength) {
        checkNoDuplicates(customSerializers);
        this.custom = List.copyOf(customSerializers);
        this.ignorable = ignorable;
        this.maxKeyLength = maxKeyLength;
    }

    /** Collects all {@link ParameterValueKeySerializer} beans from the Spring context. Fails on duplicates. */
    @Override
    public void afterPropertiesSet() {
        @SuppressWarnings("rawtypes")
        List<ParameterValueKeySerializer> discovered =
                GeoServerExtensions.extensions(ParameterValueKeySerializer.class);
        @SuppressWarnings("unchecked")
        List<ParameterValueKeySerializer<?>> cast = (List<ParameterValueKeySerializer<?>>) (List<?>) discovered;
        checkNoDuplicates(cast);
        this.custom = List.copyOf(cast);
    }

    private static void checkNoDuplicates(List<ParameterValueKeySerializer<?>> serializers) {
        Map<Class<?>, String> seen = new LinkedHashMap<>();
        for (ParameterValueKeySerializer<?> s : serializers) {
            String prev = seen.put(s.getValueType(), s.getClass().getName());
            if (prev != null) {
                throw new IllegalStateException(errorDuplicateSerializer(s.getValueType(), prev, s.getClass()));
            }
        }
    }

    /** Cache key for a single layer, or {@code null} for unrestricted access. */
    public String buildKey(AccessLimits limits) {
        ObjectNode node = buildKeyNode(limits);
        if (node == null || node.isEmpty()) return null;
        return buildAndLimit(node);
    }

    private ObjectNode buildKeyNode(AccessLimits limits) {
        if (limits == null) return null;
        if (limits instanceof VectorAccessLimits val) return buildVectorNode(val);
        if (limits instanceof CoverageAccessLimits cal) return buildCoverageNode(cal);
        // WMS/WMTS limits also carry a raster ROI that clips tiles; must be keyed before the DataAccessLimits fallback
        if (limits instanceof WMSAccessLimits wms)
            return buildRasterDataNode(wms.getReadFilter(), wms.getRasterFilter());
        if (limits instanceof WMTSAccessLimits wmts)
            return buildRasterDataNode(wmts.getReadFilter(), wmts.getRasterFilter());
        if (limits instanceof DataAccessLimits dal) return buildDataNode(dal);
        return null; // base AccessLimits has no tile content affecting fields
    }

    private ObjectNode buildRasterDataNode(Filter readFilter, Geometry rasterFilter) {
        ObjectNode node = MAPPER.createObjectNode();
        addReadFilter(node, readFilter);
        addGeometry(node, "rasterFilter", rasterFilter);
        return node;
    }

    private ObjectNode buildVectorNode(VectorAccessLimits limits) {
        ObjectNode node = MAPPER.createObjectNode();
        addReadFilter(node, limits.getReadFilter());
        List<PropertyName> attrs = limits.getReadAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            node.put(
                    "readAttributes",
                    attrs.stream().map(PropertyName::getPropertyName).sorted().collect(Collectors.joining(",")));
        }
        addGeometry(node, "clipVectorFilter", limits.getClipVectorFilter());
        addGeometry(node, "intersectVectorFilter", limits.getIntersectVectorFilter());
        return node;
    }

    private void addReadFilter(ObjectNode node, Filter filter) {
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            node.put("readFilter", serializeValue("readFilter", filter));
        }
    }

    private void addGeometry(ObjectNode node, String field, Geometry geom) {
        if (geom != null) {
            node.put(field, serializeValue(field, geom));
        }
    }

    private ObjectNode buildCoverageNode(CoverageAccessLimits limits) {
        ObjectNode node = MAPPER.createObjectNode();
        addReadFilter(node, limits.getReadFilter());
        addGeometry(node, "rasterFilter", limits.getRasterFilter());
        GeneralParameterValue[] params = limits.getParams();
        if (params != null) {
            for (GeneralParameterValue gpv : params) {
                if (ignorable.isIgnorable(gpv)) continue;
                String code = gpv.getDescriptor().getName().getCode();
                if (!(gpv instanceof ParameterValue<?> pv)) {
                    throw new IllegalArgumentException(errorUnknownParam(code, gpv));
                }
                Object value = pv.getValue();
                if (value == null) continue;
                node.put(code, serializeValue(code, value));
            }
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private String serializeValue(String paramName, Object value) {
        // List and Range recurse into this method, so they can't be ParameterValueKeySerializer instances
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(e -> serializeValue(paramName, e))
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        if (value instanceof Range<?> range) {
            String min = range.getMinValue() != null ? serializeValue(paramName, range.getMinValue()) : "*";
            String max = range.getMaxValue() != null ? serializeValue(paramName, range.getMaxValue()) : "*";
            return min + "/" + max;
        }
        // custom serializers take priority over the built-in types below
        for (ParameterValueKeySerializer<?> s : custom) {
            if (s.getValueType().isAssignableFrom(value.getClass())) {
                return ((ParameterValueKeySerializer<Object>) s).toKey(value);
            }
        }
        if (value instanceof Filter filter) return serializeFilter(filter);
        if (value instanceof Geometry geom) return serializeGeometry(geom);
        if (value instanceof Date date) return DATE_FORMAT.format(date.toInstant());
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value.toString();
        }
        throw new IllegalArgumentException(errorUnknownValue(paramName, value));
    }

    // new visitor per call: SimplifyingFilterVisitor has mutable state
    static String serializeFilter(Filter filter) {
        return ECQL.toCQL((Filter) filter.accept(new NormalizingFilterVisitor(), null));
    }

    // "AUTHORITY:CODE:WKT" when the CRS is known, plain WKT otherwise; CRS comes from the geometry user data
    // (a CoordinateReferenceSystem) or its SRID
    static String serializeGeometry(Geometry geom) {
        String wkt = geom.norm().toText();
        String crsCode = computeCrsCode(geom);
        return crsCode != null ? crsCode + ":" + wkt : wkt;
    }

    private static String computeCrsCode(Geometry geom) {
        Object userData = geom.getUserData();
        if (userData instanceof CoordinateReferenceSystem crs) {
            try {
                // fullScan=false: avoid expensive authority factory scans on the tile request path
                String id = ResourcePool.lookupIdentifier(crs, false);
                if (id != null) return id;
            } catch (FactoryException e) {
                // fall through to SRID
            }
        }
        int srid = geom.getSRID();
        return srid > 0 ? "EPSG:" + srid : null;
    }

    private ObjectNode buildDataNode(DataAccessLimits limits) {
        ObjectNode node = MAPPER.createObjectNode();
        addReadFilter(node, limits.getReadFilter());
        return node;
    }

    /**
     * Composite key for a layer group; {@code layerNames} and {@code limits} must have the same size and be in
     * composition order. {@code null} if no constituent is restricted.
     */
    public String buildLayerGroupKey(List<String> layerNames, List<AccessLimits> limits) {
        if (layerNames.size() != limits.size()) {
            throw new IllegalArgumentException("layerNames and limits must have the same size");
        }
        boolean anyRestricted = false;
        ArrayNode array = MAPPER.createArrayNode();
        for (int i = 0; i < layerNames.size(); i++) {
            ObjectNode layerNode = buildKeyNode(limits.get(i));
            // each entry always has "layer" first, then restriction fields (if any)
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("layer", layerNames.get(i));
            if (layerNode != null && !layerNode.isEmpty()) {
                anyRestricted = true;
                for (String name : layerNode.propertyNames()) {
                    entry.set(name, layerNode.get(name));
                }
            }
            array.add(entry);
        }
        if (!anyRestricted) return null;
        return buildAndLimit(array);
    }

    private String buildAndLimit(ObjectNode node) {
        if (node.toString().length() <= maxKeyLength) return node.toString();
        truncateLongestFields(List.of(node), node::toString);
        return node.toString();
    }

    private String buildAndLimit(ArrayNode array) {
        if (array.toString().length() <= maxKeyLength) return array.toString();
        List<ObjectNode> entries = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) instanceof ObjectNode on) entries.add(on);
        }
        truncateLongestFields(entries, array::toString);
        return array.toString();
    }

    private void truncateLongestFields(List<ObjectNode> nodes, Supplier<String> serialized) {
        record Field(ObjectNode node, String name, String value) {}
        List<Field> fields = new ArrayList<>();
        for (ObjectNode n : nodes) {
            for (String fname : n.propertyNames()) {
                JsonNode v = n.get(fname);
                if (v.isString()) fields.add(new Field(n, fname, v.stringValue()));
            }
        }
        // longest value first - each truncation removes the most bytes
        fields.sort(Comparator.comparingInt(f -> -f.value().length()));

        for (Field f : fields) {
            // measure the real serialized (JSON-escaped) length each step; estimating from raw value lengths
            // mis-counts escaped chars and can leave the key over the limit
            int current = serialized.get().length();
            if (current <= maxKeyLength) break;
            String orig = f.value();
            int length = orig.length();
            // truncation only helps if the result is shorter than the original
            if (length <= MIN_FIELD_PREFIX + SUFFIX_FIXED_LENGTH) continue;
            int excess = current - maxKeyLength;
            // keep as much prefix as possible while achieving at least `excess` reduction;
            // fall back to MIN_FIELD_PREFIX when the ideal prefix would be too small
            int prefix = Math.max(MIN_FIELD_PREFIX, length - SUFFIX_FIXED_LENGTH - excess);
            if (prefix + SUFFIX_FIXED_LENGTH >= length) continue; // no actual savings
            f.node().put(f.name(), orig.substring(0, prefix) + TRUNCATION_SUFFIX + sha256hex(orig));
        }
    }

    private static String sha256hex(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String errorDuplicateSerializer(Class<?> valueType, String first, Class<?> second) {
        return "Duplicate ParameterValueKeySerializer for value type %s: %s and %s"
                .formatted(valueType.getName(), first, second.getName());
    }

    private static String errorUnknownParam(String code, GeneralParameterValue gpv) {
        return "Cannot build security cache key for parameter '%s': GeneralParameterValue type %s is not a ParameterValue and has no contributed serializer. Contribute a ParameterValueKeySerializer bean or add '%s' to the %s system property if it does not affect tile content."
                .formatted(code, gpv.getClass().getName(), code, IgnorableParameterRegistry.SYSTEM_PROPERTY);
    }

    private static String errorUnknownValue(String paramName, Object value) {
        return "Cannot build security cache key for parameter '%s': no ParameterValueKeySerializer found for value type %s. Contribute a ParameterValueKeySerializer bean or add '%s' to the %s system property if it does not affect tile content."
                .formatted(
                        paramName, value.getClass().getName(), paramName, IgnorableParameterRegistry.SYSTEM_PROPERTY);
    }
}
