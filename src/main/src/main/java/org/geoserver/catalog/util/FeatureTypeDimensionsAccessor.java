/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;

/**
 * Centralizes the metadata extraction and parsing used to read dimension information from a
 * FeatureTypeInfo.
 */
public class FeatureTypeDimensionsAccessor {

    public static final String DIMENSION_PREFIX = "dim_";

    private final FeatureTypeInfo typeInfo;

    public FeatureTypeDimensionsAccessor(FeatureTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    /** Retrieves a Map with custom dimensions. */
    public Map<String, DimensionInfo> getCustomDimensions() {
        return getCustomDimensions(true);
    }

    /**
     * Retrieves a Map with custom dimensions.
     *
     * @param removePrefix Removes the dimension name prefix if true.
     */
    public Map<String, DimensionInfo> getCustomDimensions(boolean removePrefix) {
        return typeInfo.getMetadata()
                .entrySet()
                .stream()
                .filter(
                        e ->
                                e.getValue() instanceof DimensionInfo
                                        && e.getKey() != null
                                        && e.getKey().startsWith(DIMENSION_PREFIX)
                                        && !ResourceInfo.ELEVATION.equals(e.getKey())
                                        && !ResourceInfo.TIME.equals(e.getKey()))
                .map(
                        e ->
                                Pair.of(
                                        removePrefix
                                                ? e.getKey().replaceFirst(DIMENSION_PREFIX, "")
                                                : e.getKey(),
                                        (DimensionInfo) e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    /**
     * Returns the type binding for the dimension name provided.
     *
     * @param dimensionName Dimension Name
     * @return type binding class
     */
    public Optional<Class> getBinding(String dimensionName) {
        final Optional<Entry<String, DimensionInfo>> dimEntry =
                getCustomDimensionByName(dimensionName);
        final Optional<String> attributeNameOpt =
                dimEntry.map(x -> x.getValue()).map(x -> x.getAttribute());
        if (!attributeNameOpt.isPresent()) return Optional.empty();
        return typeInfo.getAttributes()
                .stream()
                .filter(a -> Objects.equals(a.getName(), attributeNameOpt.get()))
                .map(a -> (Class) getBinding(a))
                .findFirst();
    }

    private Class<?> getBinding(AttributeTypeInfo attributeTypeInfo) {
        try {
            return attributeTypeInfo.getAttribute().getType().getBinding();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MetadataMap getMetadataMap() {
        MetadataMap metadataMap = typeInfo.getMetadata();
        if (metadataMap == null)
            throw new IllegalStateException(
                    "Unable to get MetadataMap for feature info: " + typeInfo);
        return metadataMap;
    }

    public Optional<Map.Entry<String, DimensionInfo>> getCustomDimensionByName(
            String dimensionName) {
        if (dimensionName == null) return Optional.empty();
        Serializable dimension = getMetadataMap().get(DIMENSION_PREFIX + dimensionName);
        if (!(dimension instanceof DimensionInfo)) return Optional.empty();
        return Optional.of(Pair.of(dimensionName, (DimensionInfo) dimension));
    }
}
