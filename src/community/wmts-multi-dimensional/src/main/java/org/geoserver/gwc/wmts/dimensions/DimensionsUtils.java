/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.*;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wms.WMS;
import org.geotools.util.Converters;
import org.geotools.util.Range;

import java.io.Serializable;
import java.util.*;

/**
 * Some utils methods useful to interact with dimensions.
 */
public final class DimensionsUtils {

    /**
     * Helper method that will extract a layer dimensions.
     */
    public static List<Dimension> extractDimensions(WMS wms, LayerInfo layerInfo) {
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            return extractDimensions(wms, layerInfo, (FeatureTypeInfo) resourceInfo);
        }
        if (resourceInfo instanceof CoverageInfo) {
            return extractDimensions(wms, layerInfo, (CoverageInfo) resourceInfo);
        }
        return Collections.emptyList();
    }

    /**
     * Helper method that will extract the dimensions from a feature type info.
     */
    private static List<Dimension> extractDimensions(WMS wms, LayerInfo layerInfo, FeatureTypeInfo typeInfo) {
        List<Dimension> dimensions = new ArrayList<>();
        DimensionInfo timeDimension = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeDimension != null) {
            checkAndAddDimension(dimensions, new VectorTimeDimension(wms, layerInfo, timeDimension));
        }
        DimensionInfo elevationDimension = typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevationDimension != null) {
            checkAndAddDimension(dimensions, new VectorElevationDimension(wms, layerInfo, elevationDimension));
        }
        return dimensions;
    }

    /**
     * Helper method that will extract the dimensions from a coverage type info.
     */
    private static List<Dimension> extractDimensions(WMS wms, LayerInfo layerInfo, CoverageInfo typeInfo) {
        List<Dimension> dimensions = new ArrayList<>();
        for (Map.Entry<String, Serializable> entry : typeInfo.getMetadata().entrySet()) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            if (key.equals(ResourceInfo.TIME)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                checkAndAddDimension(dimensions, new RasterTimeDimension(wms, layerInfo, dimensionInfo));
            } else if (key.equals(ResourceInfo.ELEVATION)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                checkAndAddDimension(dimensions, new RasterElevationDimension(wms, layerInfo, dimensionInfo));
            } else if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                String dimensionName = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                checkAndAddDimension(dimensions, new RasterCustomDimension(wms, layerInfo, dimensionName, dimensionInfo));
            }
        }
        return dimensions;
    }

    /**
     * Helper method that adds a dimension to a list of dimensions if the dimension is enabled.
     */
    private static void checkAndAddDimension(List<Dimension> dimensions, Dimension dimension) {
        // some layers can have a dimension configured but not enable
        if (dimension.getDimensionInfo().isEnabled()) {
            dimensions.add(dimension);
        }
    }

    /**
     * Helper method that simply returns a string representation of the values of a dimension.
     * Dates and ranges will have a special handling. This method will take in account the
     * dimension required presentation.
     */
    static List<String> getDomainValuesAsStrings(DimensionInfo dimension, TreeSet<?> values) {
        if (values == null || values.isEmpty()) {
            // no domain values so he just return an empty collection
            return Collections.emptyList();
        }
        List<String> stringValues = new ArrayList<>();
        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            // the dimension representation for this values requires that all the values are listed
            for (Object value : values) {
                stringValues.add(formatDomainValue(value));
            }
        } else {
            // the dimension representation for this values require a compact representation
            Object minValue = getMinValue(values);
            Object maxValue = getMaxValue(values);
            stringValues.add(formatDomainSimpleValue(minValue) + "--" + formatDomainSimpleValue(maxValue));
        }
        return stringValues;
    }

    /**
     * Helper method that converts a domain value to string, range will be correctly handled.
     */
    public static String formatDomainValue(Object value) {
        if (value instanceof Range) {
            // this domain value is a range, we use the min and max value
            Object minValue = ((Range) value).getMinValue();
            Object maxValue = ((Range) value).getMaxValue();
            return formatDomainSimpleValue(minValue) + "--" + formatDomainSimpleValue(maxValue);
        }
        return formatDomainSimpleValue(value);
    }

    /**
     * Helper method that converts a domain value to string. Date values are formatted using the ISO8601 format.
     */
    public static String formatDomainSimpleValue(Object value) {
        if (value instanceof Date) {
            // FIXME: is the ISO formatter thread safe or can he be reused multiple times ?
            ISO8601Formatter formatter = new ISO8601Formatter();
            return formatter.format(value);
        }
        return value.toString();
    }

    /**
     * Helper method that return the minimum value. If the first value of the tree set
     * is a range the minimum value of the range is returned.
     */
    private static Object getMinValue(TreeSet<?> values) {
        Object minValue = values.first();
        if (minValue instanceof Range) {
            return ((Range) minValue).getMinValue();
        }
        return minValue;
    }

    /**
     * Helper method that return the maximum value. If the first value of the tree set
     * is a range the maximum value of the range is returned.
     */
    private static Object getMaxValue(TreeSet<?> values) {
        Object maxValue = values.last();
        if (maxValue instanceof Range) {
            return ((Range) maxValue).getMaxValue();
        }
        return maxValue;
    }

    /**
     * Return the min a max values of a tree set of values converted to the provided type.
     */
    static <T> Tuple<T, T> getMinMax(TreeSet<?> values, Class<T> type) {
        Object minValue = getMinValue(values);
        Object maxValue = getMaxValue(values);
        return Tuple.tuple(Converters.convert(minValue, type), Converters.convert(maxValue, type));
    }
}
