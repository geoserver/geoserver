/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.*;
import org.geoserver.gwc.wmts.FilteredFeatureType;
import org.geoserver.wms.WMS;
import org.opengis.filter.Filter;

import java.util.TreeSet;

/**
 * Represents a time dimension of a vector (feature type).
 */
public class VectorTimeDimension extends Dimension {

    public VectorTimeDimension(WMS wms, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        super(wms, ResourceInfo.TIME, layerInfo, dimensionInfo);
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return DimensionDefaultValueSetting.TIME_CURRENT;
    }

    @Override
    public TreeSet<?> getDomainValues(Filter filter) {
        try {
            FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
            TreeSet<?> fullDomainValues = getWms().getFeatureTypeTimes(typeInfo);
            fullDomainValues = fullDomainValues == null ? new TreeSet<>() : fullDomainValues;
            if (filter == null || filter.equals(Filter.INCLUDE)) {
                return fullDomainValues;
            }
            TreeSet<?> restrictedValues = getWms().getFeatureTypeTimes(new FilteredFeatureType(typeInfo, filter));
            if (restrictedValues == null) {
                restrictedValues = new TreeSet<>();
            }
            return restrictedValues;
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error getting domain values for dimension '%s' of vector '%s'.",
                    getDimensionName(), getResourceInfo().getName()), exception);
        }
    }

    @Override
    public Filter getFilter() {
        return buildVectorFilter();
    }
}
