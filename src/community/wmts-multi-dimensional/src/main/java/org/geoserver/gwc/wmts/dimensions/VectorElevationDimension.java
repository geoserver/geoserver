/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.FilteredFeatureType;
import org.geoserver.wms.WMS;
import org.opengis.filter.Filter;

import java.util.TreeSet;

/**
 * Represents an elevation dimension of a vector (feature type).
 */
public class VectorElevationDimension extends Dimension {

    public VectorElevationDimension(WMS wms, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        super(wms, ResourceInfo.ELEVATION, layerInfo, dimensionInfo);
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return "0";
    }

    @Override
    public TreeSet<?> getDomainValues(Filter filter) {
        try {
            FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
            TreeSet<?> fullDomainValues = getWms().getFeatureTypeElevations(typeInfo);
            fullDomainValues = fullDomainValues == null ? new TreeSet<>() : fullDomainValues;
            if (filter == null || filter.equals(Filter.INCLUDE)) {
                return fullDomainValues;
            }
            TreeSet<?> restrictedValues = getWms().getFeatureTypeElevations(new FilteredFeatureType(typeInfo, filter));
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
