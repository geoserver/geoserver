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
 * Represents an elevation dimension of a raster.
 */
public class RasterElevationDimension extends Dimension {

    public RasterElevationDimension(WMS wms, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        super(wms, ResourceInfo.ELEVATION, layerInfo, dimensionInfo);
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return DimensionDefaultValueSetting.TIME_CURRENT;
    }

    @Override
    public Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates) {
        return getRasterDomainValues(filter, noDuplicates, CoverageDimensionsReader.DataType.NUMERIC, DimensionsUtils.NUMERICAL_COMPARATOR);
    }

    @Override
    public Filter getFilter() {
        return buildRasterFilter();
    }
}
