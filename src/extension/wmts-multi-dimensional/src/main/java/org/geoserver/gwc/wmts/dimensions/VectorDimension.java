/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.MultiDimensionalExtension;
import org.geoserver.wms.WMS;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.feature.FeatureCollection;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Base class for vector based dimension */
public abstract class VectorDimension extends Dimension {

    public VectorDimension(
            WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        super(wms, dimensionName, layerInfo, dimensionInfo);
    }

    /**
     * Helper method used to get domain values from a vector type in the form of a feature
     * collection.
     */
    @Override
    protected FeatureCollection getDomain(Query query) {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        FeatureSource source;
        try {

            source = DimensionsUtils.getFeatures(typeInfo);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error getting feature source of vector '%s'.", resourceInfo.getName()),
                    exception);
        }
        // fix type name
        query = new Query(query);
        query.setTypeName(source.getSchema().getName().getLocalPart());
        try {
            return source.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error reading feature from layer '%s' for dimension '%s'.",
                            resourceInfo.getName(), getDimensionName()),
                    exception);
        }
    }

    @Override
    public List<Comparable> getDomainValues(Filter filter, boolean noDuplicates) {
        FeatureCollection featureCollection = getDomain(new Query(null, filter));
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Comparable> values =
                    DimensionsUtils.getValuesWithoutDuplicates(
                            dimensionInfo.getAttribute(),
                            dimensionInfo.getEndAttribute(),
                            featureCollection);
            return new ArrayList<>(values);
        }
        // we need the duplicate values (this is useful for some operations like get histogram
        // operation)
        return DimensionsUtils.getValuesWithDuplicates(
                dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute(), featureCollection);
    }

    @Override
    protected DomainSummary getDomainSummary(Query query, int expandLimit) {
        FeatureCollection features = getDomain(query);
        String attribute = dimensionInfo.getAttribute();
        String endAttribute = dimensionInfo.getEndAttribute();
        return getDomainSummary(features, attribute, endAttribute, expandLimit);
    }

    @Override
    protected DomainSummary getPagedDomainValues(
            Query query, int maxNumberOfValues, SortOrder sortOrder) {
        String attribute = dimensionInfo.getAttribute();
        String endAttribute = dimensionInfo.getEndAttribute();
        Query sortedQuery = new Query(query);
        boolean sortByEnd = sortByEnd();
        SortBy sortByDim = FILTER_FACTORY.sort(sortByEnd ? endAttribute : attribute, sortOrder);
        sortedQuery.setSortBy(new SortBy[] {sortByDim});
        FeatureCollection features = getDomain(sortedQuery);
        return getPagedDomainValues(
                features, attribute, endAttribute, maxNumberOfValues, sortByDim);
    }

    private boolean sortByEnd() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            Object o = attrs.getAttribute(MultiDimensionalExtension.SORT_BY_END, 0);
            if (o instanceof Boolean) {
                return (Boolean) o;
            }
        }
        return false;
    }
}
