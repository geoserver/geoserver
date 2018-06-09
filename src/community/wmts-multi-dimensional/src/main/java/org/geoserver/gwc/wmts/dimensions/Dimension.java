/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.Aggregate;
import org.opengis.filter.Filter;

/**
 * This class represents a dimension providing an abstraction over all types of dimensions and
 * resources types (like raster and vectors).
 *
 * <p>Restrictions can be applied to a dimension and converted into a filter. This makes possible to
 * merge several dimensions restrictions when working with domains.
 */
public abstract class Dimension {

    protected final WMS wms;
    protected final String dimensionName;
    protected final LayerInfo layerInfo;
    protected final DimensionInfo dimensionInfo;

    protected final ResourceInfo resourceInfo;

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
    public abstract List<Object> getDomainValues(Filter filter, boolean noDuplicates);

    /**
     * Returns the domain summary. If the count is lower than <code>expandLimit</code> then only the
     * count will be returned, otherwise min and max will also be returned
     *
     * @param features
     * @param attribute
     * @param expandLimit
     * @return
     */
    protected DomainSummary getDomainSummary(
            FeatureCollection features, String attribute, int expandLimit) {
        // grab domain, but at most expandLimit + 1, to know if there are too many
        if (expandLimit != 0) {
            TreeSet uniqueValues =
                    DimensionsUtils.getUniqueValues(features, attribute, expandLimit + 1);
            if (uniqueValues.size() <= expandLimit || expandLimit < 0) {
                return new DomainSummary(uniqueValues);
            }
        }
        Map<Aggregate, Object> minMax =
                DimensionsUtils.getAggregates(attribute, features, Aggregate.MIN, Aggregate.MAX);
        // size fixed to 2 as doing a full count might require a lot of time on vector data,
        // e.g. we have a time enabled wind layer that takes tens of seconds as it has tens
        // of millions of points
        return new DomainSummary(minMax.get(Aggregate.MIN), minMax.get(Aggregate.MAX), 2);
    }

    /**
     * Computes an histogram of this dimension domain values. The provided resolution value can be
     * NULL or AUTO to let the server decide the proper resolution. If a resolution is provided it
     * needs to be a number for numerical domains or a period syntax for time domains. For
     * enumerated domains (i.e. string values) the resolution will be ignored.
     *
     * <p>A filter can be provided to filter the domain values. The provided filter can be NULL.
     *
     * <p>The first element of the returned tuple will contain the description of the histogram
     * domain as start, end and resolution. The second element of the returned tuple will contain a
     * list of the histogram values represented as strings. If no description of the domain can be
     * provided (for example enumerated values) NULL will be returned and the same allies the
     * histogram values.
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
     * Returns this dimension values represented as strings taking in account this dimension
     * representation strategy. The returned values will be sorted. The provided filter will be used
     * to filter the domain values. The provided filter can be NULL.
     */
    public Tuple<Integer, List<String>> getDomainValuesAsStrings(Filter filter, int expandLimit) {
        DomainSummary summary = getDomainSummary(filter, expandLimit);
        return Tuple.tuple(summary.getCount(), DimensionsUtils.getDomainValuesAsStrings(summary));
    }

    protected abstract DomainSummary getDomainSummary(Filter filter, int expandLimit);

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
