/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.util.List;

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
    public Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates) {
        return getVectorDomainValues(filter, noDuplicates, DimensionsUtils.TEMPORAL_COMPARATOR);
    }

    @Override
    public Filter getFilter() {
        return buildVectorFilter();
    }
}
