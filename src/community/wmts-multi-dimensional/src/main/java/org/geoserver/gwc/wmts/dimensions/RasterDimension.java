/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for raster based dimensions
 */
public abstract class RasterDimension extends Dimension {

    private final CoverageDimensionsReader.DataType dataType;
    private final Comparator comparator;

    public RasterDimension(WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo, CoverageDimensionsReader.DataType dataType, Comparator comparator) {
        super(wms, dimensionName, layerInfo, dimensionInfo);
        this.dataType = dataType;
        this.comparator = comparator;
    }

    @Override
    public List<Object> getDomainValues(Filter filter, boolean noDuplicates) {
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = reader.readWithoutDuplicates(getDimensionName(), filter, dataType, comparator);
            return new ArrayList<>(values);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return reader.readWithDuplicates(getDimensionName(), filter, dataType, comparator);
    }

    protected DomainSummary getDomainSummary(Filter filter, boolean includeCount) {
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);

        Tuple<String, FeatureCollection> values = reader.getValues(getDimensionName(), filter, dataType);
        return getDomainSummary(values.second, values.first, includeCount);
    }

}
