/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wms.WMS;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.Converters;
import org.geotools.util.Range;
import org.geowebcache.service.OWSException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.comparator.ComparableComparator;

/** Some utils methods useful to interact with dimensions. */
public final class DimensionsUtils {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /** No expansion limit provided */
    public static final int NO_LIMIT = Integer.MIN_VALUE;

    /** Helper method that will extract a layer dimensions. */
    public static List<Dimension> extractDimensions(
            WMS wms, LayerInfo layerInfo, Set<String> requestedDimensions) throws OWSException {
        ResourceInfo resourceInfo = layerInfo.getResource();
        List<Dimension> result = new ArrayList<>();
        if (resourceInfo instanceof FeatureTypeInfo) {
            result = extractDimensions(wms, layerInfo, (FeatureTypeInfo) resourceInfo);
        }
        if (resourceInfo instanceof CoverageInfo) {
            result = extractDimensions(wms, layerInfo, (CoverageInfo) resourceInfo);
        }
        if (requestedDimensions != MultiDimensionalExtension.ALL_DOMAINS) {
            Set<String> availableDimensions =
                    result.stream().map(d -> d.getDimensionName()).collect(Collectors.toSet());
            HashSet<String> unknownDimensions = new HashSet<>(requestedDimensions);
            unknownDimensions.removeAll(availableDimensions);
            unknownDimensions.remove(MultiDimensionalExtension.SPACE_DIMENSION);
            if (!unknownDimensions.isEmpty()) {
                String dimensionList =
                        unknownDimensions
                                .stream()
                                .map(s -> "'" + s + "'")
                                .collect(Collectors.joining(", "));
                throw new OWSException(
                        400,
                        "InvalidParameterValue",
                        "Domains",
                        "Unknown dimensions requested " + dimensionList);
            } else {
                result =
                        result.stream()
                                .filter(d -> requestedDimensions.contains(d.getDimensionName()))
                                .collect(Collectors.toList());
            }
        }

        return result;
    }

    /** Helper method that will extract the dimensions from a feature type info. */
    private static List<Dimension> extractDimensions(
            WMS wms, LayerInfo layerInfo, FeatureTypeInfo typeInfo) {
        List<Dimension> dimensions = new ArrayList<>();
        DimensionInfo timeDimension =
                typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeDimension != null) {
            checkAndAddDimension(
                    dimensions, new VectorTimeDimension(wms, layerInfo, timeDimension));
        }
        DimensionInfo elevationDimension =
                typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevationDimension != null) {
            checkAndAddDimension(
                    dimensions, new VectorElevationDimension(wms, layerInfo, elevationDimension));
        }
        return dimensions;
    }

    /** Helper method that will extract the dimensions from a coverage type info. */
    private static List<Dimension> extractDimensions(
            WMS wms, LayerInfo layerInfo, CoverageInfo typeInfo) {
        List<Dimension> dimensions = new ArrayList<>();
        for (Map.Entry<String, Serializable> entry : typeInfo.getMetadata().entrySet()) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            if (key.equals(ResourceInfo.TIME)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                checkAndAddDimension(
                        dimensions, new RasterTimeDimension(wms, layerInfo, dimensionInfo));
            } else if (key.equals(ResourceInfo.ELEVATION)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                checkAndAddDimension(
                        dimensions, new RasterElevationDimension(wms, layerInfo, dimensionInfo));
            } else if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                DimensionInfo dimensionInfo = Converters.convert(value, DimensionInfo.class);
                String dimensionName = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                checkAndAddDimension(
                        dimensions,
                        new RasterCustomDimension(wms, layerInfo, dimensionName, dimensionInfo));
            }
        }
        return dimensions;
    }

    /** Helper method that adds a dimension to a list of dimensions if the dimension is enabled. */
    private static void checkAndAddDimension(List<Dimension> dimensions, Dimension dimension) {
        // some layers can have a dimension configured but not enable
        if (dimension.getDimensionInfo().isEnabled()) {
            dimensions.add(dimension);
        }
    }

    /**
     * Helper method that simply returns a string representation of the values of a dimension. Dates
     * and ranges will have a special handling. This method will take in account the dimension
     * required presentation.
     */
    static List<String> getDomainValuesAsStrings(DomainSummary summary) {
        if (summary.getMin() == null
                && (summary.getUniqueValues() == null || summary.getUniqueValues().isEmpty())) {
            // no domain values so he just return an empty collection
            return Collections.emptyList();
        }
        List<String> stringValues = new ArrayList<>();
        // did we get a list of unique values?
        if (summary.getUniqueValues() != null) {
            // the dimension representation for this values requires that all the values are listed
            for (Object value : summary.getUniqueValues()) {
                stringValues.add(formatDomainValue(value));
            }
        } else {
            // the dimension representation for this values require a compact representation
            Object minValue = summary.getMin();
            Object maxValue = summary.getMax();
            stringValues.add(
                    formatDomainSimpleValue(minValue) + "--" + formatDomainSimpleValue(maxValue));
        }
        return stringValues;
    }

    /** Helper method that converts a domain value to string, range will be correctly handled. */
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
     * Helper method that converts a domain value to string. Date values are formatted using the
     * ISO8601 format.
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
     * Helper method that return the minimum value. If the first value of the tree set is a range
     * the minimum value of the range is returned.
     */
    private static Object getMinValue(List<Object> values) {
        Object minValue = values.get(0);
        if (minValue instanceof Range) {
            return ((Range) minValue).getMinValue();
        }
        return minValue;
    }

    /**
     * Helper method that return the maximum value. If the first value of the tree set is a range
     * the maximum value of the range is returned.
     */
    private static Object getMaxValue(List<Object> values) {
        Object maxValue = values.get(values.size() - 1);
        if (maxValue instanceof Range) {
            return ((Range) maxValue).getMaxValue();
        }
        return maxValue;
    }

    /** Return the min a max values of a tree set of values converted to the provided type. */
    static <T> Tuple<T, T> getMinMax(List<Object> values, Class<T> type) {
        Object minValue = getMinValue(values);
        Object maxValue = getMaxValue(values);
        return Tuple.tuple(Converters.convert(minValue, type), Converters.convert(maxValue, type));
    }

    /**
     * Helper method that simply extract from a feature collection the values of a specific
     * attribute removing duplicate values.
     */
    static Set<Object> getValuesWithoutDuplicates(
            String attributeName, FeatureCollection featureCollection) {
        Set uniques = getUniqueValues(featureCollection, attributeName, NO_LIMIT);

        // dimension values are dates/numbers/strings, all comparable, native sorting is fine
        Set<Object> values = new TreeSet<>(uniques);
        return values;
    }

    static Set getUniqueValues(
            FeatureCollection featureCollection, String attributeName, int limit) {
        // using the unique visitor to remove duplicate values
        UniqueVisitor uniqueVisitor = new UniqueVisitor(attributeName);
        if (limit > 0 && limit < Integer.MAX_VALUE) {
            uniqueVisitor.setMaxFeatures(limit);
        }
        uniqueVisitor.setPreserveOrder(true);
        try {
            featureCollection.accepts(uniqueVisitor, null);
        } catch (Exception exception) {
            throw new RuntimeException("Error visiting collection with unique visitor.");
        }
        // all dimension values are comparable
        return uniqueVisitor.getUnique();
    }

    /**
     * Helper method that extracts a set of aggregates on the given collection and attribute and
     * returns the results
     */
    static Map<Aggregate, Object> getAggregates(
            String attributeName, FeatureCollection featureCollection, Aggregate... aggregates) {
        Map<Aggregate, Object> result = new HashMap<>();
        PropertyName property = FF.property(attributeName);
        for (Aggregate aggregate : aggregates) {
            FeatureCalc featureCalc = aggregate.create(property);
            try {
                featureCollection.accepts(featureCalc, null);
                Object value = featureCalc.getResult().getValue();
                result.put(aggregate, value);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to collect summary aggregates on attribute " + attributeName, e);
            }
        }
        return result;
    }

    /**
     * Helper method that simply extract from a feature collection the values of a specific
     * attribute keeping duplicate values.
     */
    static List<Object> getValuesWithDuplicates(
            String attributeName, FeatureCollection featureCollection) {
        // full data values are returned including duplicate values
        List<Object> values = new ArrayList<>();
        FeatureIterator featuresIterator = featureCollection.features();
        while (featuresIterator.hasNext()) {
            // extracting the feature attribute that contain our dimension value
            SimpleFeature feature = (SimpleFeature) featuresIterator.next();
            values.add(feature.getAttribute(attributeName));
        }
        Collections.sort(values, new ComparableComparator());
        return values;
    }

    /** Compute the resource bounds based on the provided filter */
    public static ReferencedEnvelope getBounds(ResourceInfo resource, Filter filter) {
        try {
            if (resource instanceof FeatureTypeInfo) {
                FeatureSource featureSource =
                        ((FeatureTypeInfo) resource).getFeatureSource(null, null);
                FeatureCollection features = featureSource.getFeatures(filter);
                return features.getBounds();
            } else if (resource instanceof CoverageInfo) {
                CoverageDimensionsReader reader =
                        CoverageDimensionsReader.instantiateFrom((CoverageInfo) resource);
                return reader.getBounds(filter);
            } else {
                // for all other resource types (WMS/WMTS cascading) we cannot do anything
                // intelligent
                return resource.getNativeBoundingBox();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to compute bounds for " + resource, e);
        }
    }

    /**
     * Builds a bounding box filter, or returns {@link Filter#INCLUDE} if the bounding box is null
     */
    public static Filter getBoundingBoxFilter(
            ResourceInfo resource, ReferencedEnvelope boundingBox, FilterFactory filterFactory)
            throws TransformException, IOException, SchemaException, FactoryException {
        String geometryName = getGeometryPropertyName(resource);
        if (boundingBox == null || geometryName == null) {
            return Filter.INCLUDE;
        }

        // do we need to query multiple areas?
        ProjectionHandler handler =
                ProjectionHandlerFinder.getHandler(
                        boundingBox,
                        getSchemaForResource(resource).getCoordinateReferenceSystem(),
                        true);
        if (handler == null) {
            return toBoundingBoxFilter(boundingBox, filterFactory, geometryName);
        }

        List<ReferencedEnvelope> boxes = handler.getQueryEnvelopes();
        List<Filter> filters =
                boxes.stream()
                        .map(re -> toBoundingBoxFilter(re, filterFactory, geometryName))
                        .collect(Collectors.toList());
        if (filters.size() == 1) {
            return filters.get(0);
        } else if (filters.size() > 1) {
            return filterFactory.or(filters);
        } else {
            return Filter.INCLUDE;
        }
    }

    protected static double rollLongitude(final double x) {
        return x - 360 * Math.floor(x / 360 + 0.5);
    }

    private static FeatureType getSchemaForResource(ResourceInfo resource)
            throws IOException, TransformException, SchemaException {
        FeatureType schema;
        if (resource instanceof FeatureTypeInfo) {
            schema = ((FeatureTypeInfo) resource).getFeatureType();
        } else if (resource instanceof CoverageInfo) {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader)
                            ((CoverageInfo) resource).getGridCoverageReader(null, null);
            schema = FeatureUtilities.wrapGridCoverageReader(reader, null).getSchema();
        } else {
            throw new IllegalArgumentException(
                    "Did not expect this resource, only vector and raster layers are supported: "
                            + resource);
        }

        return schema;
    }

    private static Filter toBoundingBoxFilter(
            ReferencedEnvelope boundingBox, FilterFactory filterFactory, String geometryName) {
        CoordinateReferenceSystem crs = boundingBox.getCoordinateReferenceSystem();
        String epsgCode = crs == null ? null : GML2EncodingUtils.toURI(crs);
        return filterFactory.bbox(
                geometryName,
                boundingBox.getMinX(),
                boundingBox.getMinY(),
                boundingBox.getMaxX(),
                boundingBox.getMaxY(),
                epsgCode);
    }

    private static String getGeometryPropertyName(ResourceInfo resource) {
        try {
            String geometryName =
                    ""; // the default geometry, unfortunately does not work in some cases
            if (resource instanceof FeatureTypeInfo) {
                geometryName =
                        ((FeatureTypeInfo) resource)
                                .getFeatureType()
                                .getGeometryDescriptor()
                                .getLocalName();
            } else if (resource instanceof CoverageInfo) {
                return "";
            }
            return geometryName;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to gather feature type information for " + resource, e);
        }
    }

    public static Tuple<String, String> getAttributes(ResourceInfo resource, Dimension dimension) {
        if (resource instanceof FeatureTypeInfo) {
            DimensionInfo di = dimension.getDimensionInfo();
            return Tuple.tuple(di.getAttribute(), di.getEndAttribute());
        } else if (resource instanceof CoverageInfo) {
            CoverageDimensionsReader reader =
                    CoverageDimensionsReader.instantiateFrom((CoverageInfo) resource);
            String dimensionName = dimension.getDimensionName();
            Tuple<String, String> attributes = reader.getDimensionAttributesNames(dimensionName);
            if (attributes.first == null) {
                throw new RuntimeException(
                        String.format(
                                "Could not found start attribute name for dimension '%s' in raster '%s'.",
                                dimensionName, resource.prefixedName()));
            }
            return attributes;
        } else {
            throw new RuntimeException(
                    "Cannot get restriction attributes on this resource: " + resource);
        }
    }
}
