/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.GroupByVisitor;
import org.geotools.feature.visitor.GroupByVisitorBuilder;
import org.geotools.util.Converters;
import org.geotools.util.Range;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * This class represents a dimension providing an abstraction over all types of dimensions and
 * resources types (like raster and vectors).
 *
 * <p>Restrictions can be applied to a dimension and converted into a filter. This makes possible to
 * merge several dimensions restrictions when working with domains.
 */
public abstract class Dimension {

    static final FilterFactory2 FILTER_FACTORY = CommonFactoryFinder.getFilterFactory2();

    /** Empty histogram representation */
    public static final Tuple<String, List<Integer>> EMPTY_HISTOGRAM =
            Tuple.tuple("", Collections.emptyList());

    protected final WMS wms;
    protected final String dimensionName;
    protected final LayerInfo layerInfo;
    protected final DimensionInfo dimensionInfo;

    protected final ResourceInfo resourceInfo;

    private static final Logger LOGGER = Logging.getLogger(Dimension.class);

    public Dimension(
            WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        this.wms = wms;
        this.dimensionName = dimensionName;
        this.layerInfo = layerInfo;
        this.dimensionInfo = dimensionInfo;
        resourceInfo = layerInfo.getResource();
    }

    /**
     * Returns this dimension domain values filtered with the provided filter. The provided filter
     * can be NULL. Duplicate values may be included if noDuplicates parameter is set to FALSE.
     */
    public abstract List<Comparable> getDomainValues(Filter filter, boolean noDuplicates);

    /**
     * Returns the domain summary. If the count is lower than <code>expandLimit</code> then only the
     * count will be returned, otherwise min and max will also be returned
     *
     * @param features the featureCollection.
     * @param attribute the dimension attribute name.
     * @param expandLimit value determining if count or also min and max should be returned.
     * @return the DomainSummary of the Resource.
     */
    protected DomainSummary getDomainSummary(
            FeatureCollection features, String attribute, int expandLimit) {
        return getDomainSummary(features, attribute, null, expandLimit);
    }

    /**
     * Returns the domain summary. If the count is lower than <code>expandLimit</code> then only the
     * count will be returned, otherwise min and max will also be returned
     *
     * @param features the featureCollection.
     * @param attribute the dimension attribute name.
     * @param endAttributeName the dimension end attribute name. Can be null.
     * @param expandLimit value determining if count or also min and max should be returned.
     * @return the DomainSummary of the Resource.
     */
    protected DomainSummary getDomainSummary(
            FeatureCollection features,
            String attribute,
            String endAttributeName,
            int expandLimit) {
        // grab domain, but at most expandLimit + 1, to know if there are too many
        if (expandLimit != 0) {
            Set<Comparable> uniqueValues =
                    DimensionsUtils.getUniqueValues(
                            features, attribute, endAttributeName, expandLimit + 1);
            if (uniqueValues.size() <= expandLimit || expandLimit < 0) {
                return new DomainSummary(new TreeSet<>(uniqueValues));
            }
        }
        Map<Aggregate, Comparable> minMax =
                DimensionsUtils.getMinMaxAggregate(attribute, endAttributeName, features);
        // we return only the number of non null mix/max elements, as computing the whole count
        // might take just too much time on large datasets
        return new DomainSummary(
                minMax.get(Aggregate.MIN),
                minMax.get(Aggregate.MAX),
                minMax.values().stream().filter(v -> v != null).count());
    }

    /**
     * Returns the domain summary. If the count is lower than <code>maxValues</code> then only the
     * count will be returned, otherwise min and max will also be returned
     *
     * @param features the featureCollection.
     * @param attribute the dimension attribute name.
     * @param maxValues the limit of values to return.
     * @return the domain summary of the resource.
     */
    protected DomainSummary getPagedDomainValues(
            FeatureCollection features, String attribute, int maxValues) {
        return getPagedDomainValues(features, attribute, null, maxValues, null);
    }

    /**
     * Returns the domain summary. If the count is lower than <code>maxValues</code> then only the
     * count will be returned, otherwise min and max will also be returned
     *
     * @param features the featureCollection.
     * @param attribute the dimension attribute name.
     * @param endAttribute the dimension end attribute name.
     * @param maxValues the limit of values to return.
     * @param sortBy the sortBy query. Used to properly sort ranges.
     * @return the domain summary of the resource.
     */
    protected DomainSummary getPagedDomainValues(
            FeatureCollection features,
            String attribute,
            String endAttribute,
            int maxValues,
            SortBy sortBy) {
        Set<Comparable> uniqueValues =
                DimensionsUtils.getUniqueValues(
                        features, attribute, endAttribute, maxValues, sortBy);
        return new DomainSummary(uniqueValues);
    }

    /** Returns the data type of the dimension */
    public abstract Class<?> getDimensionType();

    /**
     * Computes an histogram of this dimension domain values. The provided resolutionSpec value can
     * be NULL or AUTO to let the server decide the proper resolutionSpec. If a resolutionSpec is
     * provided it needs to be a number for numerical domains or a period syntax for time domains.
     * For enumerated domains (i.e. string values) the resolutionSpec will be ignored.
     *
     * <p>A filter can be provided to filter the domain values. The provided filter can be NULL.
     *
     * <p>The first element of the returned tuple will contain the description of the histogram
     * domain as start, end and resolutionSpec. The second element of the returned tuple will
     * contain a list of the histogram values represented as strings. If no description of the
     * domain can be provided (for example enumerated values) NULL will be returned and the same
     * allies the histogram values.
     */
    public Tuple<String, List<Integer>> getHistogram(Filter filter, String resolutionSpec) {
        if (loadDataInMemory()) {
            boolean isRange = dimensionInfo.getEndAttribute() != null;
            return HistogramUtils.buildHistogram(
                    getDomainValues(filter, false), resolutionSpec, isRange);
        }

        FilterFactory2 ff = DimensionsUtils.FF;
        String dimensionAttributeName = getDimensionAttributeName();
        PropertyName dimensionProperty = ff.property(dimensionAttributeName);
        Query query = new Query(null, filter);
        Class<?> dimType = getDimensionType();
        boolean isNumeric = Number.class.isAssignableFrom(dimType);
        boolean isDate = Date.class.isAssignableFrom(dimType);
        if (!isNumeric && !isDate)
            return getCustomDimHistogram(filter, dimensionProperty, dimensionAttributeName);

        DomainSummary summary = getDomainSummary(query, 0);

        if (summary.getMin() == null || summary.getMax() == null) return EMPTY_HISTOGRAM;

        Tuple<String, List<Range>> specAndBuckets =
                getSpecsBuckets(isDate, summary, resolutionSpec);

        if (hasEndAttribute())
            return getRangeHistogram(
                    specAndBuckets, dimensionAttributeName, dimensionInfo.getEndAttribute(), query);

        if (isNumeric) {

            List<Range> buckets = specAndBuckets.second;
            @SuppressWarnings("unchecked")
            Range<Double> referenceBucket = buckets.get(0);
            double resolution = referenceBucket.getMaxValue() - referenceBucket.getMinValue();
            double min = ((Number) summary.getMin()).doubleValue();
            // the aggregation expression classifies results in buckets numbered from 1 on
            Function classifier =
                    ff.function(
                            "floor",
                            ff.divide(
                                    ff.subtract(dimensionProperty, ff.literal(min)),
                                    ff.literal(resolution)));
            TreeMap<Object, Object> results =
                    groupByDomainOnExpression(
                            filter, classifier, dimensionAttributeName, Integer.class);

            // map out domain representation
            List<Integer> counts = new ArrayList<>(buckets.size());
            for (int i = 0; i < buckets.size(); i++) {
                Number count = Optional.ofNullable((Number) results.get(i)).orElse(0);
                counts.add(count.intValue());
            }
            return Tuple.tuple(specAndBuckets.first, counts);
        } else {

            Date min = (Date) summary.getMin();

            List<Range> buckets = specAndBuckets.second;
            @SuppressWarnings("unchecked")
            Range<Date> referenceBucket = buckets.get(0);
            double resolution =
                    referenceBucket.getMaxValue().getTime()
                            - referenceBucket.getMinValue().getTime();

            // the aggregation expression classifies results in buckets numbered from 1 on
            Function classifier =
                    ff.function(
                            "floor",
                            ff.divide(
                                    ff.function(
                                            "dateDifference", dimensionProperty, ff.literal(min)),
                                    ff.literal(resolution)));
            TreeMap<Object, Object> results =
                    groupByDomainOnExpression(
                            filter, classifier, dimensionAttributeName, Integer.class);

            // map out domain representation
            List<Integer> counts = new ArrayList<>(buckets.size());
            for (int i = 0; i < buckets.size(); i++) {
                Number count = Optional.ofNullable((Number) results.get(i)).orElse(0);
                counts.add(count.intValue());
            }
            return Tuple.tuple(specAndBuckets.first, counts);
        }
    }

    private Tuple<String, List<Range>> getSpecsBuckets(
            boolean isDate, DomainSummary summary, String resolutionSpec) {
        Tuple<String, List<Range>> result;
        if (isDate) {
            Date min = (Date) summary.getMin();
            Date max = (Date) summary.getMax();
            result = HistogramUtils.getTimeBuckets(min, max, resolutionSpec);
        } else {
            double min = ((Number) summary.getMin()).doubleValue();
            double max = ((Number) summary.getMax()).doubleValue();
            result = HistogramUtils.getNumericBuckets(min, max, resolutionSpec);
        }
        return result;
    }

    private Tuple<String, List<Integer>> getCustomDimHistogram(
            Filter filter, PropertyName dimensionProperty, String dimensionAttributeName) {
        // assuming custom dimension, will handle as strings, once there is support
        // for custom dimensions of different type (for structured readers) this will
        // have to be modified
        TreeMap<Object, Object> results =
                groupByDomainOnExpression(
                        filter, dimensionProperty, dimensionAttributeName, String.class);

        // map out domain representation and histogram value representation
        List<Integer> counts =
                results.values().stream()
                        .map(v -> ((Number) v).intValue())
                        .collect(Collectors.toList());
        String domainRepresentation =
                results.keySet().stream().map(v -> v.toString()).collect(Collectors.joining(","));

        return Tuple.tuple(domainRepresentation, counts);
    }

    private Tuple<String, List<Integer>> getRangeHistogram(
            Tuple<String, List<Range>> specAndBuckets,
            String attributeName,
            String endAttributeName,
            Query query) {
        List<Range> buckets = specAndBuckets.second;
        List<Integer> counts = new ArrayList<>(buckets.size());
        CountVisitor visitor = new CountVisitor();
        Filter original = query.getFilter();
        FilterFactory2 ff = DimensionsUtils.FF;
        PropertyName startPn = ff.property(attributeName);
        PropertyName endPn = ff.property(endAttributeName);
        for (Range r : buckets) {
            Filter rangeFilter = getRangeIntersectionFilter(original, r, startPn, endPn);
            Query q = new Query();
            q.setFilter(rangeFilter);
            q.setProperties(Arrays.asList(startPn, endPn));
            FeatureCollection domain = getDomain(q);
            try {
                domain.accepts(visitor, null);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error while counting ranges in bucket.", e);
                throw new RuntimeException(e);
            }
            counts.add(visitor.getResult().toInt());
            visitor.reset();
        }
        return Tuple.tuple(specAndBuckets.first, counts);
    }

    private Filter getRangeIntersectionFilter(
            Filter original, Range bucket, PropertyName startPn, PropertyName endPn) {
        Comparable min = bucket.getMinValue();
        Comparable max = bucket.getMaxValue();
        FilterFactory2 ff = DimensionsUtils.FF;
        Literal maxLiteral = ff.literal(max);
        Literal minLiteral = ff.literal(min);
        Filter low =
                bucket.isMaxIncluded()
                        ? ff.lessOrEqual(startPn, maxLiteral)
                        : ff.less(startPn, maxLiteral);
        Filter upper =
                bucket.isMinIncluded()
                        ? ff.greaterOrEqual(endPn, minLiteral)
                        : ff.greater(endPn, minLiteral);
        Filter and = ff.and(low, upper);
        return ff.and(and, original);
    }

    /**
     * Should we load data in memory to compute histograms (fast for small datasets not having
     * indexes) or do we try to use visitor and perform one or more data scans instead? Small easter
     * egg to allow testing, we might want to extend it to a per layer configuration in case there
     * are large shapefile layers involved in this (computing min/max/groupby is three full data
     * scans in that case). Or have a way to figure out if a collection can optimize out a visit
     * (completely missing right now, that would be a significant API change).
     */
    private boolean loadDataInMemory() {
        return Boolean.getBoolean("WMTS_HISTOGRAM_IN_MEMORY");
    }

    public TreeMap<Object, Object> groupByDomainOnExpression(
            Filter filter,
            Expression classifier,
            String dimensionAttribute,
            Class<?> classifierType) {
        Query query = new Query();
        query.setFilter(filter);
        query.setPropertyNames(new String[] {dimensionAttribute});
        FeatureCollection domain = getDomain(query);
        GroupByVisitorBuilder builder = new GroupByVisitorBuilder();
        builder.withAggregateVisitor(Aggregate.COUNT);
        builder.withGroupByAttribute(classifier);
        builder.withAggregateAttribute(classifier);
        GroupByVisitor visitor = builder.build();

        try {
            domain.accepts(visitor, null);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format(
                            "Error fetching histogram in formation from database for '%s'.",
                            resourceInfo.getName()),
                    e);
        }

        // turn the group result into the expected histogram result
        @SuppressWarnings("unchecked")
        Map<List<Object>, Object> groupResult = visitor.getResult().toMap();
        TreeMap<Object, Object> sortedResults = new TreeMap<>();
        groupResult.forEach(
                (k, v) -> {
                    Object classifierValue = Converters.convert(k.get(0), classifierType);
                    sortedResults.put(classifierValue, v);
                });
        return sortedResults;
    }

    protected boolean hasEndAttribute() {
        return dimensionInfo.getEndAttribute() != null;
    }

    /** Returns the attribute name representing the dimension */
    protected String getDimensionAttributeName() {
        return dimensionInfo.getAttribute();
    }

    /** Returns the domain given a filter */
    protected abstract FeatureCollection getDomain(Query filter);

    protected abstract String getDefaultValueFallbackAsString();

    protected WMS getWms() {
        return wms;
    }

    ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public DimensionInfo getDimensionInfo() {
        return dimensionInfo;
    }

    /** Returns a list of formatted domain values */
    public Tuple<Integer, List<String>> getDomainValuesAsStrings(
            Query query, int maxNumberOfValues) {
        DomainSummary summary = getDomainSummary(query, maxNumberOfValues);
        return Tuple.tuple(summary.getCount(), DimensionsUtils.getDomainValuesAsStrings(summary));
    }

    /**
     * Returns this dimension values represented as strings taking in account this dimension
     * representation strategy. The returned values will be sorted. The provided filter will be used
     * to filter the domain values. The provided filter can be NULL.
     */
    public Tuple<Integer, List<String>> getPagedDomainValuesAsStrings(
            Query query, int maxValues, SortOrder sortOrder) {
        DomainSummary summary = getPagedDomainValues(query, maxValues, sortOrder);
        return Tuple.tuple(summary.getCount(), DimensionsUtils.getDomainValuesAsStrings(summary));
    }

    protected abstract DomainSummary getDomainSummary(Query query, int expandLimit);

    /** Returns a page of domain values */
    protected abstract DomainSummary getPagedDomainValues(
            Query query, int maxNumberOfValues, SortOrder sortOrder);

    /**
     * Return this dimension default value as a string taking in account this dimension default
     * strategy.
     */
    public String getDefaultValueAsString() {
        DimensionDefaultValueSelectionStrategy strategy =
                wms.getDefaultValueStrategy(resourceInfo, dimensionName, dimensionInfo);
        String defaultValue =
                strategy.getCapabilitiesRepresentation(resourceInfo, dimensionName, dimensionInfo);
        return defaultValue != null ? defaultValue : getDefaultValueFallbackAsString();
    }

    /** Return dimension start and end attributes, values may be NULL. */
    public Tuple<String, String> getAttributes() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            // for vectors this information easily available
            return Tuple.tuple(dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        if (resourceInfo instanceof CoverageInfo) {
            return CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo)
                    .getDimensionAttributesNames(getDimensionName());
        }
        return Tuple.tuple(null, null);
    }

    @Override
    public String toString() {
        return "Dimension{"
                + ", name='"
                + dimensionName
                + '\''
                + ", layer="
                + layerInfo.getName()
                + '}';
    }
}
