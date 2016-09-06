/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wms.WMS;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.TreeSet;

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
    public TreeSet<?> getDomainValues(Filter filter) {
        try {
            CoverageInfo typeInfo = (CoverageInfo) getResourceInfo();
            GridCoverage2DReader reader = (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
            ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
            if (filter == null || filter.equals(Filter.INCLUDE)) {
                return new TreeSet<>(dimensions.getDomain(getDimensionName()));
            }
            return getRasterDomainValues(filter);
        } catch (IOException exception) {
            throw new RuntimeException(String.format("Error getting domain values for dimension '%s' of coverage '%s'.",
                    getDimensionName(), getResourceInfo().getName()), exception);
        }
    }

    @Override
    public Filter getFilter() {
        return buildRasterFilter();
    }
}
