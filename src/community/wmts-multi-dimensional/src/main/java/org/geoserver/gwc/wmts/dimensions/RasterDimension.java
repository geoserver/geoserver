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
    public Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates) {
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);
        if (noDuplicates) {
            // no duplicate values should be included
            Tuple<ReferencedEnvelope, Set<Object>> values = reader.readWithoutDuplicates(getDimensionName(), filter, dataType, comparator);
            List<Object> list = new ArrayList<>(values.second.size());
            list.addAll(values.second);
            return Tuple.tuple(values.first, list);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return reader.readWithDuplicates(getDimensionName(), filter, dataType, comparator);
    }

    protected DomainSummary getDomainSummary(Filter filter, boolean includeCount) {
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo);

        Tuple<String, FeatureCollection> values = reader.getValues(getDimensionName(), filter, dataType);
        return getDomainSummary(values.second, values.first, includeCount);
    }

    @Override
    public Filter getFilter() {
        CoverageInfo typeInfo = (CoverageInfo) getResourceInfo();
        Filter filter = Filter.INCLUDE;
        if (boundingBox != null) {
            // we have a bounding box so lets build a filter for it
            try {
                filter = appendBoundingBoxFilter(filter, typeInfo);
            } catch (IOException exception) {
                throw new RuntimeException(String.format("Exception accessing feature source of raster type '%s'.",
                        typeInfo.getName()), exception);
            }
        }
        if (domainRestrictions != null) {
            CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom(typeInfo);
            Tuple<String, String> attributes = reader.getDimensionAttributesNames(getDimensionName());
            if (attributes.first == null) {
                throw new RuntimeException(String.format(
                        "Could not found start attribute name for dimension '%s' in raster '%s'.", getDimensionName(), typeInfo.getName()));
            }
            // ok time to build the domain values filter
            filter = appendDomainRestrictionsFilter(filter, attributes.first, attributes.second);
        }
        return filter;
    }
}
