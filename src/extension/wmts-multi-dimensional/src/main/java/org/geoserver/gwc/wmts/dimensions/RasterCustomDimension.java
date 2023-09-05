/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.gwc.wmts.dimensions.CoverageDimensionsReader.DataType;
import org.geoserver.wms.WMS;
import org.geotools.api.data.Query;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.feature.FeatureCollection;

/** Represents a custom dimension of a raster. */
public class RasterCustomDimension extends RasterDimension {

    public RasterCustomDimension(
            WMS wms, LayerInfo layerInfo, String name, DimensionInfo dimensionInfo) {
        super(wms, name, layerInfo, dimensionInfo, DataType.CUSTOM);
    }

    @Override
    public Class<String> getDimensionType() {
        return String.class;
    }

    @Override
    protected FeatureCollection getDomain(Query query) {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        Tuple<String, FeatureCollection> values =
                reader.getValues(this.dimensionName, query, DataType.CUSTOM, SortOrder.ASCENDING);

        return values.second;
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
