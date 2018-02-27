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
import org.geoserver.wms.dimension.DimensionFilterBuilder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    protected  ReferencedEnvelope boundingBox;
    protected  final List<Object> domainRestrictions = new ArrayList<>();

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
    public abstract Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates);


    protected DomainSummary getDomainSummary(FeatureCollection features, String attribute, boolean includeCount) {
        ReferencedEnvelope bounds = features.getBounds();
        if (includeCount) {
            Map<Aggregate, Object> aggregates = DimensionsUtils.getAggregates(attribute, features,
                    Aggregate.MIN, Aggregate.MAX, Aggregate.COUNT);
            return new DomainSummary(bounds, aggregates.get(Aggregate.MIN), aggregates.get(Aggregate.MAX), (Long)
                    aggregates.get(Aggregate.COUNT));
        } else {
            Map<Aggregate, Object> aggregates = DimensionsUtils.getAggregates(attribute, features,
                    Aggregate.MIN, Aggregate.MAX);
            return new DomainSummary(bounds, aggregates.get(Aggregate.MIN), aggregates.get(Aggregate.MAX));
        }
    }

    /**
     * Returns a filter that will contain all the restrictions applied to this dimension.
     */
    public abstract Filter getFilter();

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
        return HistogramUtils.buildHistogram(getDomainValues(filter, false).second, resolution);
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

    protected DimensionInfo getDimensionInfo() {
        return dimensionInfo;
    }

    public void setBoundingBox(ReferencedEnvelope boundingBox) {
        this.boundingBox = boundingBox;
    }

    public void addDomainRestriction(Object domainRestriction) {
        if (domainRestriction instanceof Collection) {
            domainRestrictions.addAll((Collection) domainRestriction);
        } else {
            domainRestrictions.add(domainRestriction);
        }
    }

    /**
     * Returns this dimension values represented as strings taking in account this
     * dimension representation strategy. The returned values will be sorted. The
     * provided filter will be used to filter the domain values. The provided filter
     * can be NULL.
     */
    public Tuple<ReferencedEnvelope, Tuple<Integer, List<String>>> getDomainValuesAsStrings(Filter filter) {
        if (dimensionInfo.getPresentation() == DimensionPresentation.LIST) {
            Tuple<ReferencedEnvelope, List<Object>> domainValues = getDomainValues(filter, true);
            return Tuple.tuple(domainValues.first,
                    Tuple.tuple(domainValues.second.size(), DimensionsUtils.getDomainValuesAsStrings(dimensionInfo, 
                            domainValues.second)));
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
            return Tuple.tuple(summary.getEnvelope(), Tuple.tuple(aggregates.size(), DimensionsUtils
                    .getDomainValuesAsStrings(dimensionInfo, new ArrayList<>(aggregates))));
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
     * Helper method that extract the geomtry attribute name from the current type info and invoke
     * the method that will actually build the spatial filter.
     */
    protected Filter appendBoundingBoxFilter(Filter filter, CoverageInfo typeInfo) throws IOException {
        // getting the geometry attribute name
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom(typeInfo);
        String geometryAttributeName = reader.getGeometryAttributeName();
        // checking if we have a valid geometry attribute
        if (geometryAttributeName == null) {
            // this raster doesn't supports spatial filtering
            return filter;
        }
        // creating the filter
        return appendBoundingBoxFilter(filter, geometryAttributeName);
    }

    /**
     * Helper method that will build a bounding box filter using the provided geometry attribute name and
     * the current bounding box restriction. The bounding box filter will be merged with the provided filter.
     */
    protected Filter appendBoundingBoxFilter(Filter filter, String geometryAttributeName) {
        CoordinateReferenceSystem coordinateReferenceSystem = boundingBox.getCoordinateReferenceSystem();
        String epsgCode = coordinateReferenceSystem == null ? null : GML2EncodingUtils.toURI(coordinateReferenceSystem);
        Filter spatialFilter = filterFactory.bbox(geometryAttributeName, boundingBox.getMinX(), boundingBox.getMinY(),
                boundingBox.getMaxX(), boundingBox.getMaxY(), epsgCode);
        return filterFactory.and(filter, spatialFilter);
    }

    /**
     * Helper method that will build a dimension domain values filter based on this dimension start and end
     * attributes. The created filter will be merged with the provided filter.
     */
    protected Filter appendDomainRestrictionsFilter(Filter filter, String startAttributeName, String endAttributeName) {
        DimensionFilterBuilder dimensionFilterBuilder = new DimensionFilterBuilder(filterFactory);
        dimensionFilterBuilder.appendFilters(startAttributeName, endAttributeName, domainRestrictions);
        return filterFactory.and(filter, dimensionFilterBuilder.getFilter());
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
