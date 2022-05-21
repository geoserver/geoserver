/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.SIDECAR_TYPE;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
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
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.CalcResult;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.Converters;
import org.geotools.util.Range;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geowebcache.service.OWSException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.comparator.ComparableComparator;

/** Some utils methods useful to interact with dimensions. */
public final class DimensionsUtils {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /** No expansion limit provided */
    public static final int NO_LIMIT = Integer.MIN_VALUE;

    public static final Logger LOGGER = Logging.getLogger(DimensionsUtils.class);

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
                        unknownDimensions.stream()
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
            return formatDomainSimpleValue(minValue) + "/" + formatDomainSimpleValue(maxValue);
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
    private static Object getMinValue(List<Comparable> values) {
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
    private static Object getMaxValue(List<Comparable> values) {
        int last = values.size() - 1;
        Object maxValue = values.get(last);
        if (maxValue instanceof Range) {
            values =
                    values.stream()
                            .map(c -> ((Range) c).getMaxValue())
                            .sorted()
                            .collect(Collectors.toList());
            maxValue = values.get(last);
        }
        return maxValue;
    }

    /** Return the min a max values of a tree set of values converted to the provided type. */
    static <T> Tuple<T, T> getMinMax(List<Comparable> values, Class<T> type) {
        Object minValue = getMinValue(values);
        Object maxValue = getMaxValue(values);
        return Tuple.tuple(Converters.convert(minValue, type), Converters.convert(maxValue, type));
    }

    /**
     * Helper method that simply extract from a feature collection the values of a specific
     * attribute removing duplicate values.
     */
    static Set<Comparable> getValuesWithoutDuplicates(
            String attributeName, String endAttribute, FeatureCollection featureCollection) {
        Set<Comparable> uniques =
                getUniqueValues(featureCollection, attributeName, endAttribute, NO_LIMIT);

        // dimension values are dates/numbers/strings, all comparable, native sorting is fine
        Set<Comparable> values = new TreeSet<>(uniques);
        return values;
    }

    static Set<Comparable> getUniqueValues(
            FeatureCollection featureCollection,
            String attributeName,
            String endAttributeName,
            int limit) {
        return getUniqueValues(featureCollection, attributeName, endAttributeName, limit, null);
    }

    @SuppressWarnings("unchecked")
    static Set<Comparable> getUniqueValues(
            FeatureCollection featureCollection,
            String attributeName,
            String endAttributeName,
            int limit,
            SortBy sortBy) {
        // using the unique visitor to remove duplicate values
        UniqueVisitor uniqueVisitor =
                endAttributeName != null
                        ? new UniqueVisitor(attributeName, endAttributeName)
                        : new UniqueVisitor(attributeName);
        uniqueVisitor.setPreserveOrder(true);
        if (limit > 0 && limit < Integer.MAX_VALUE) {
            uniqueVisitor.setMaxFeatures(limit);
        }
        try {
            featureCollection.accepts(uniqueVisitor, null);
        } catch (Exception exception) {
            throw new RuntimeException("Error visiting collection with unique visitor.");
        }
        if (uniqueVisitor.getAttrNames().size() > 1)
            return resultToComparableSet(uniqueVisitor.getResult(), endAttributeName, sortBy);
        // all dimension values are comparable
        return uniqueVisitor.getUnique();
    }

    private static Set<Comparable> resultToComparableSet(
            CalcResult result, String endAttributeName, SortBy sortBy) {
        if (result == CalcResult.NULL_RESULT) return new HashSet<>();
        @SuppressWarnings("unchecked")
        Set<Object> values = result.toSet();
        if (values.isEmpty()) return Collections.emptySet();
        else return toComparableSet(values, endAttributeName, sortBy);
    }

    private static Set<Comparable> toComparableSet(
            Set<Object> set, String endAttribute, SortBy sortBy) {
        Set<Comparable> resultSet = getTreeSet(sortBy, endAttribute);
        Iterator<Object> objects = set.iterator();

        while (objects.hasNext()) {
            Object val = objects.next();
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) val;
            Object start = values.get(0);
            Object end = values.get(1);
            Comparable range = toRange(start, end);
            if (range != null) resultSet.add(range);
        }
        return resultSet;
    }

    private static TreeSet<Comparable> getTreeSet(SortBy sortBy, String endAttribute) {
        TreeSet<Comparable> resultSet;

        if (sortBy != null) {
            Comparator<Comparable> comparator;
            if (isSortByEnd(endAttribute, sortBy)) comparator = new SortByEndComparator();
            else comparator = new SortByStartComparator();

            if (sortBy.getSortOrder().equals(SortOrder.DESCENDING))
                comparator = comparator.reversed();

            resultSet = new TreeSet<>(comparator);
        } else {
            resultSet = new TreeSet<>();
        }
        return resultSet;
    }

    private static boolean isSortByEnd(String endAttribute, SortBy sortBy) {
        if (endAttribute == null) return false;
        return sortBy.getPropertyName().getPropertyName().equals(endAttribute);
    }

    private static ComparableRange toRange(Object start, Object end) {
        if (start == null && end == null) return null;

        // if one of the two values is null
        // we set the other right?
        if (start == null) start = end;
        else if (end == null) end = start;
        @SuppressWarnings("unchecked")
        ComparableRange result =
                new ComparableRange(Comparable.class, (Comparable) start, (Comparable) end);
        return result;
    }

    static Map<Aggregate, Comparable> getMinMaxAggregate(
            String attributeName, String endAttribute, FeatureCollection featureCollection) {
        Map<Aggregate, Comparable> result = new HashMap<>();
        PropertyName minProp = FF.property(attributeName);
        PropertyName maxProp =
                endAttribute != null ? FF.property(endAttribute) : FF.property(attributeName);
        Aggregate min = Aggregate.MIN;
        Aggregate max = Aggregate.MAX;
        FeatureCalc minCalc = min.create(minProp);
        FeatureCalc maxCalc = max.create(maxProp);
        try {
            featureCollection.accepts(minCalc, null);
            Comparable minVal = (Comparable) minCalc.getResult().getValue();
            featureCollection.accepts(maxCalc, null);
            Comparable maxVal = (Comparable) maxCalc.getResult().getValue();
            result.put(min, minVal);
            result.put(max, maxVal);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to collect summary aggregates on attribute " + attributeName, e);
        }
        return result;
    }

    /**
     * Helper method that extracts a set of aggregates on the given collection and attribute and
     * returns the results
     */
    static Map<Aggregate, Comparable> getAggregates(
            String attributeName, FeatureCollection featureCollection, Aggregate... aggregates) {
        Map<Aggregate, Comparable> result = new HashMap<>();
        PropertyName property = FF.property(attributeName);
        for (Aggregate aggregate : aggregates) {
            FeatureCalc featureCalc = aggregate.create(property);
            try {
                featureCollection.accepts(featureCalc, null);
                Comparable value = (Comparable) featureCalc.getResult().getValue();
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
    @SuppressWarnings("unchecked")
    static List<Comparable> getValuesWithDuplicates(
            String attributeName, FeatureCollection featureCollection) {
        return getValuesWithDuplicates(attributeName, null, featureCollection);
    }

    @SuppressWarnings("unchecked")
    static List<Comparable> getValuesWithDuplicates(
            String attributeName, String endAttributeName, FeatureCollection featureCollection) {
        // full data values are returned including duplicate values
        List<Comparable> values = new ArrayList<>();
        try (FeatureIterator featuresIterator = featureCollection.features()) {
            while (featuresIterator.hasNext()) {
                // extracting the feature attribute that contain our dimension value
                SimpleFeature feature = (SimpleFeature) featuresIterator.next();
                Object attr = feature.getAttribute(attributeName);
                if (endAttributeName != null) {
                    Object endAttribute = feature.getAttribute(endAttributeName);
                    Comparable comparableRange = toRange(attr, endAttribute);
                    if (comparableRange != null) values.add(comparableRange);
                } else if (attr != null) {
                    values.add((Comparable) attr);
                }
            }
            Collections.sort(values, new ComparableComparator());
            return values;
        }
    }

    /**
     * Returns the first qualified name matching the given simple name, or throws an exception in
     * case it's not found
     */
    public static Name getFullName(String name, DataStoreInfo dsi) throws IOException {
        DataAccess<?, ?> da = dsi.getDataStore(null);
        return da.getNames().stream()
                .filter(n -> name.equals(n.getLocalPart()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Could not find type "
                                                + name
                                                + " inside store "
                                                + dsi.getName()));
    }

    /**
     * Returns the features for the given resource info, taking into account the eventual sidecar
     * feature type for fast dimensional queries
     */
    public static FeatureSource getFeatures(ResourceInfo resource) throws IOException {
        String sidecar = resource.getMetadata().get(SIDECAR_TYPE, String.class);
        // sidecar table available?
        if (sidecar != null) {
            DataStoreInfo dsi = (DataStoreInfo) resource.getStore();
            Name name = getFullName(sidecar, dsi);
            return dsi.getDataStore(null).getFeatureSource(name);
        }
        // simple case
        return ((FeatureTypeInfo) resource).getFeatureSource(null, GeoTools.getDefaultHints());
    }

    /** Compute the resource bounds based on the provided filter */
    public static ReferencedEnvelope getBounds(ResourceInfo resource, Filter filter) {
        try {
            if (resource instanceof FeatureTypeInfo) {
                return getFeatures(resource).getFeatures(filter).getBounds();
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

    private static class SortByEndComparator implements Comparator<Comparable> {

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Comparable o1, Comparable o2) {
            if (o1 instanceof ComparableRange && o2 instanceof ComparableRange) {
                ComparableRange comp1 = (ComparableRange) o1;
                ComparableRange comp2 = (ComparableRange) o2;
                int result = comp1.getMaxValue().compareTo(comp2.getMaxValue());
                if (result == 0) result = comp1.getMinValue().compareTo(comp2.getMinValue());
                return result;
            }
            return o1.compareTo(o2);
        }
    }

    private static class SortByStartComparator implements Comparator<Comparable> {

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Comparable o1, Comparable o2) {
            if (o1 instanceof ComparableRange && o2 instanceof ComparableRange) {
                ComparableRange comp1 = (ComparableRange) o1;
                ComparableRange comp2 = (ComparableRange) o2;
                int result = comp1.getMinValue().compareTo(comp2.getMinValue());
                if (result == 0) result = comp1.getMaxValue().compareTo(comp2.getMaxValue());
                return result;
            }
            return o1.compareTo(o2);
        }
    }
}
