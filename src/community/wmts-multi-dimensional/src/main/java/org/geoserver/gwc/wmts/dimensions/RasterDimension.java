/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

/** Base class for raster based dimensions */
public abstract class RasterDimension extends Dimension {

    private final CoverageDimensionsReader.DataType dataType;

    public RasterDimension(
            WMS wms,
            String dimensionName,
            LayerInfo layerInfo,
            DimensionInfo dimensionInfo,
            CoverageDimensionsReader.DataType dataType) {
        super(wms, dimensionName, layerInfo, dimensionInfo);
        this.dataType = dataType;
    }

    @Override
    public List<Object> getDomainValues(Filter filter, boolean noDuplicates) {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = reader.readWithoutDuplicates(getDimensionName(), filter, dataType);
            return new ArrayList<>(values);
        }
        // we need the duplicate values (this is useful for some operations like get histogram
        // operation)
        return reader.readWithDuplicates(getDimensionName(), filter, dataType);
    }

    @Override
    protected DomainSummary getDomainSummary(Query query, int expandLimit) {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);

        Tuple<String, FeatureCollection> values =
                reader.getValues(getDimensionName(), query, dataType, SortOrder.ASCENDING);
        return getDomainSummary(values.second, values.first, expandLimit);
    }

    @Override
    protected DomainSummary getPagedDomainValues(
            Query query, int maxNumberOfValues, SortOrder sortOrder) {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);

        Tuple<String, FeatureCollection> values =
                reader.getValues(getDimensionName(), query, dataType, sortOrder);
        return getPagedDomainValues(values.second, values.first, maxNumberOfValues);
    }

    @Override
    protected String getDimensionAttributeName() {
        CoverageDimensionsReader reader =
                CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        return reader.getDimensionAttributesNames(getDimensionName()).first;
    }
}
