/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for vector based dimension
 */
public abstract class VectorDimension extends Dimension {

    private final Comparator comparator;

    public VectorDimension(WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo, 
                           Comparator comparator) {
        super(wms, dimensionName, layerInfo, dimensionInfo);
        this.comparator = comparator;
    }

    /**
     * Helper method used to get domain values from a vector type in the form of a feature collection.
     */
    protected FeatureCollection getDomain(Filter filter) {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        FeatureSource source;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error getting feature source of vector '%s'.", resourceInfo.getName()), exception);
        }
        Query query = new Query(source.getSchema().getName().getLocalPart(), filter == null ? Filter.INCLUDE : filter);
        try {
            return source.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error reading feature from layer '%s' for dimension '%s'.",
                    resourceInfo.getName(), getDimensionName()), exception);
        }
    }

    /**
     * Helper method used to get domain values from a vector type.
     */
    Tuple<ReferencedEnvelope, List<Object>> getVectorDomainValues(Filter filter, boolean noDuplicates, Comparator<Object> comparator) {
        FeatureCollection featureCollection = getDomain(filter);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = DimensionsUtils.
                    getValuesWithoutDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator);
            List<Object> list = new ArrayList<>(values.size());
            list.addAll(values);
            return Tuple.tuple(featureCollection.getBounds(), list);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return Tuple.tuple(featureCollection.getBounds(),
                DimensionsUtils.getValuesWithDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator));
    }

    @Override
    public Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates) {
        FeatureCollection featureCollection = getDomain(filter);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = DimensionsUtils.
                    getValuesWithoutDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator);
            List<Object> list = new ArrayList<>(values.size());
            list.addAll(values);
            return Tuple.tuple(featureCollection.getBounds(), list);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return Tuple.tuple(featureCollection.getBounds(),
                DimensionsUtils.getValuesWithDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator));
    }

    protected DomainSummary getDomainSummary(Filter filter, boolean includeCount) {
        FeatureCollection features = getDomain(filter);
        String attribute = dimensionInfo.getAttribute();

        return getDomainSummary(features, attribute, includeCount);
    }

    @Override
    public Filter getFilter() {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        Filter filter = Filter.INCLUDE;
        if (boundingBox != null) {
            // we have a bounding box so lets build a filter for it
            String geometryAttributeName;
            try {
                // let's find out the geometry attribute
                geometryAttributeName = typeInfo.getFeatureSource(null, null).getSchema().getGeometryDescriptor().getLocalName();
            } catch (IOException exception) {
                throw new RuntimeException(String.format("Exception accessing feature source of vector type '%s'.",
                        typeInfo.getName()), exception);
            }
            // creating the bounding box filter and append it to our filter
            filter = appendBoundingBoxFilter(filter, geometryAttributeName);
        }
        if (domainRestrictions != null) {
            // we have a domain filter
            filter = appendDomainRestrictionsFilter(filter, dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        return filter;
    }
}
