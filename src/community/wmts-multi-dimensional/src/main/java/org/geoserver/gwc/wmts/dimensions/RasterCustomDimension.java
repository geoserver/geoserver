/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.dimensions.CoverageDimensionsReader.DataType;
import org.geoserver.wms.WMS;

/** Represents a custom dimension of a raster. */
public class RasterCustomDimension extends RasterDimension {

    public RasterCustomDimension(
            WMS wms, LayerInfo layerInfo, String name, DimensionInfo dimensionInfo) {
        super(wms, name, layerInfo, dimensionInfo, DataType.CUSTOM);
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return null;
    }

    @Override
    public String getDefaultValueAsString() {
        return getWms().getDefaultCustomDimensionValue(
                        getDimensionName(), getResourceInfo(), String.class);
    }
}
