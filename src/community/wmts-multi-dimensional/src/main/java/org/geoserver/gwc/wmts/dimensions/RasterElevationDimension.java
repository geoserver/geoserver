/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.opengis.filter.sort.SortOrder;

/** Represents an elevation dimension of a raster. */
public class RasterElevationDimension extends RasterDimension {

    public RasterElevationDimension(WMS wms, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        super(
                wms,
                ResourceInfo.ELEVATION,
                layerInfo,
                dimensionInfo,
                CoverageDimensionsReader.DataType.NUMERIC);
    }

    @Override
    public Class getDimensionType() {
        return Number.class;
    }

    @Override
    protected FeatureCollection getDomain(Query query) {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        Tuple<String, FeatureCollection> values =
                reader.getValues(
                        this.dimensionName,
                        query,
                        CoverageDimensionsReader.DataType.NUMERIC,
                        SortOrder.ASCENDING);

        return values.second;
    }

    @Override
    protected String getDefaultValueFallbackAsString() {
        return "0";
    }
}
