/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class represents a dimension providing an abstraction over all types of
 * dimensions and resources types (like raster and vectors).
 * </p>
 * <p>
 * Restrictions can be applied to a dimension and converted into a filter. This
 * makes possible to merge several dimensions restrictions when working with domains.
 * </p>
 */
public abstract class Dimension {

    protected final WMS wms;
    protected final String dimensionName;
    protected final LayerInfo layerInfo;
    protected final DimensionInfo dimensionInfo;

    protected final ResourceInfo resourceInfo;

    public Dimension(WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        this.wms = wms;
        this.dimensionName = dimensionName;
        this.layerInfo = layerInfo;
        this.dimensionInfo = dimensionInfo;
        resourceInfo = layerInfo.getResource();
    }

    /**
     * Returns this dimension domain values filtered with the provided filter.
     * The provided filter can be NULL. Duplicate values may be included if
     * noDuplicates parameter is set to FALSE.
     */
    public abstract List<Object> getDomainValues(Filter filter, boolean noDuplicates);


    protected DomainSummary getDomainSummary(FeatureCollection features, String attribute, boolean includeCount) {
        if (includeCount) {
            Map<Aggregate, Object> aggregates = DimensionsUtils.getAggregates(attribute, features,
                    Aggregate.MIN, Aggregate.MAX, Aggregate.COUNT);
            return new DomainSummary(aggregates.get(Aggregate.MIN), aggregates.get(Aggregate.MAX), (Long)
                    aggregates.get(Aggregate.COUNT));
        } else {
            Map<Aggregate, Object> aggregates = DimensionsUtils.getAggregates(attribute, features,
                    Aggregate.MIN, Aggregate.MAX);
            return new DomainSummary(aggregates.get(Aggregate.MIN), aggregates.get(Aggregate.MAX));
        }
    }

    /**
     * <p>
     * Computes an histogram of this dimension domain values. The provided resolution value can be NULL
     * or AUTO to let the server decide the proper resolution. If a resolution is provided it needs
     * to be a number for numerical domains or a period syntax for time domains. For enumerated domains
     * (i.e. string values) the resolution will be ignored.
     * </p>
     * <p>
     * A filter can be provided to filter the domain values. The provided filter can be NULL.
     * </p>
     * <p>
     * The first element of the returned tuple will contain the description of the histogram domain as
     * start, end and resolution. The second element of the returned tuple will contain a list of the
     * histogram values represented as strings. If no description of the domain can be provided (for
     * example enumerated values) NULL will be returned and the same allies the histogram values.
     * </p>
     */
    public Tuple<String, List<Integer>> getHistogram(Filter filter, String resolution) {
        return HistogramUtils.buildHistogram(getDomainValues(filter, false), resolution);
    }

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

    /**
     * Returns this dimension values represented as strings taking in account this
     * dimension representation strategy. The returned values will be sorted. The
     * provided filter will be used to filter the domain values. The provided filter
     * can be NULL.
     */
    public Tuple<Integer, List<String>> getDomainValuesAsStrings(Filter filter) {
        if (dimensionInfo.getPresentation() == DimensionPresentation.LIST) {
            List<Object> domainValues = getDomainValues(filter, true);
            return Tuple.tuple(domainValues.size(), DimensionsUtils.getDomainValuesAsStrings(dimensionInfo, 
                            domainValues));
        } else {
            // optimize out and get just the min and max values
            DomainSummary summary = getDomainSummary(filter, false);
            List<Object> aggregates = new ArrayList<>();
            if (summary.getMin() != null) {
                aggregates.add(summary.getMin());
            }
            if (summary.getMax() != null && !aggregates.contains(summary.getMax())) {
                aggregates.add(summary.getMax());
            }
            return Tuple.tuple(aggregates.size(), DimensionsUtils
                    .getDomainValuesAsStrings(dimensionInfo, new ArrayList<>(aggregates)));
        }
    }

    protected abstract DomainSummary getDomainSummary(Filter filter, boolean includeCount);

    /**
     * Return this dimension default value as a string taking in account this dimension default strategy.
     */
    public String getDefaultValueAsString() {
        DimensionDefaultValueSelectionStrategy strategy = wms.getDefaultValueStrategy(resourceInfo, dimensionName, dimensionInfo);
        String defaultValue = strategy.getCapabilitiesRepresentation(resourceInfo, dimensionName, dimensionInfo);
        return defaultValue != null ? defaultValue : getDefaultValueFallbackAsString();
    }

    /**
     * Return dimension start and end attributes, values may be NULL.
     */
    public Tuple<String, String> getAttributes() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            // for vectors this information easily available
            return Tuple.tuple(dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        if (resourceInfo instanceof CoverageInfo) {
            return CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo).getDimensionAttributesNames(getDimensionName());
        }
        return Tuple.tuple(null, null);
    }

    @Override
    public String toString() {
        return "Dimension{" + ", name='" + dimensionName + '\'' + ", layer=" + layerInfo.getName() + '}';
    }

}
