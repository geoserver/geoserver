/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.gwc.wmts.dimensions.CoverageDimensionsReader.DataType;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.util.List;

/**
 * Represents a custom dimension of a raster.
 */
public class RasterCustomDimension extends Dimension {

    public RasterCustomDimension(WMS wms, LayerInfo layerInfo, String name, DimensionInfo dimensionInfo) {
        super(wms, name, layerInfo, dimensionInfo);
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return null;
    }

    @Override
    public String getDefaultValueAsString() {
        return getWms().getDefaultCustomDimensionValue(getDimensionName(), getResourceInfo(), String.class);
    }

    @Override
    public Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates) {
        return getRasterDomainValues(filter, noDuplicates, DataType.CUSTOM, DimensionsUtils.CUSTOM_COMPARATOR);
    }

    @Override
    public Filter getFilter() {
        return buildRasterFilter();
    }
}
