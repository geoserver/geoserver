/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.*;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geoserver.wms.dimension.DimensionFilterBuilder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.*;

/**
 * <p>
 * This class represents a dimension providing an abstraction over all types of
 * dimensions and resources types (like raster and vectors).
 * </p>
 * <p>
 * Restrictions can be applied to a dimension and converted into a filter. This
 * makes possible to merge several dimensions restrictions when working with domains.
 * </p>
 */
public abstract class Dimension {

    private final WMS wms;
    private final String dimensionName;
    private final LayerInfo layerInfo;
    private final DimensionInfo dimensionInfo;

    private final ResourceInfo resourceInfo;
    private final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    private ReferencedEnvelope boundingBox;
    private final List<Object> domainRestrictions = new ArrayList<>();

    public Dimension(WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo) {
        this.wms = wms;
        this.dimensionName = dimensionName;
        this.layerInfo = layerInfo;
        this.dimensionInfo = dimensionInfo;
        resourceInfo = layerInfo.getResource();
    }

    /**
     * Returns this dimension domain values filtered with the provided filter.
     * The provided filter can be NULL. Duplicate values may be included if
     * noDuplicates parameter is set to FALSE.
     */
    public abstract Tuple<ReferencedEnvelope, List<Object>> getDomainValues(Filter filter, boolean noDuplicates);


    /**
     * Returns a filter that will contain all the restrictions applied to this dimension.
     */
    public abstract Filter getFilter();

    /**
     * <p>
     * Computes an histogram of this dimension domain values. The provided resolution value can be NULL
     * or AUTO to let the server decide the proper resolution. If a resolution is provided it needs
     * to be a number for numerical domains or a period syntax for time domains. For enumerated domains
     * (i.e. string values) the resolution will be ignored.
     * </p>
     * <p>
     * A filter can be provided to filter the domain values. The provided filter can be NULL.
     * </p>
     * <p>
     * The first element of the returned tuple will contain the description of the histogram domain as
     * start, end and resolution. The second element of the returned tuple will contain a list of the
     * histogram values represented as strings. If no description of the domain can be provided (for
     * example enumerated values) NULL will be returned and the same allies the histogram values.
     * </p>
     */
    public Tuple<String, List<Integer>> getHistogram(Filter filter, String resolution) {
        return HistogramUtils.buildHistogram(getDomainValues(filter, false).second, resolution);
    }

    protected abstract String getDefaultValueFallbackAsString();

    protected WMS getWms() {
        return wms;
    }

    ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    public String getDimensionName() {
        return dimensionName;
    }

    protected DimensionInfo getDimensionInfo() {
        return dimensionInfo;
    }

    public void setBoundingBox(ReferencedEnvelope boundingBox) {
        this.boundingBox = boundingBox;
    }

    public void addDomainRestriction(Object domainRestriction) {
        if (domainRestriction instanceof Collection) {
            domainRestrictions.addAll((Collection) domainRestriction);
        } else {
            domainRestrictions.add(domainRestriction);
        }
    }

    /**
     * Returns this dimension values represented as strings taking in account this
     * dimension representation strategy. The returned values will be sorted. The
     * provided filter will be used to filter the domain values. The provided filter
     * can be NULL.
     */
    public Tuple<ReferencedEnvelope, Tuple<Integer, List<String>>> getDomainValuesAsStrings(Filter filter) {
        Tuple<ReferencedEnvelope, List<Object>> domainValues = getDomainValues(filter, true);
        return Tuple.tuple(domainValues.first,
                Tuple.tuple(domainValues.second.size(), DimensionsUtils.getDomainValuesAsStrings(dimensionInfo, domainValues.second)));
    }


    /**
     * Return this dimension default value as a string taking in account this dimension default strategy.
     */
    public String getDefaultValueAsString() {
        DimensionDefaultValueSelectionStrategy strategy = wms.getDefaultValueStrategy(resourceInfo, dimensionName, dimensionInfo);
        String defaultValue = strategy.getCapabilitiesRepresentation(resourceInfo, dimensionName, dimensionInfo);
        return defaultValue != null ? defaultValue : getDefaultValueFallbackAsString();
    }

    /**
     * Helper method that can be used by vectors types to create a filter with the current restrictions.
     */
    Filter buildVectorFilter() {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        Filter filter = Filter.INCLUDE;
        if (boundingBox != null) {
            // we have a bounding box so lets build a filter for it
            String geometryAttributeName;
            try {
                // let's find out the geometry attribute
                geometryAttributeName = typeInfo.getFeatureSource(null, null).getSchema().getGeometryDescriptor().getLocalName();
            } catch (IOException exception) {
                throw new RuntimeException(String.format("Exception accessing feature source of vector type '%s'.",
                        typeInfo.getName()), exception);
            }
            // creating the bounding box filter and append it to our filter
            filter = appendBoundingBoxFilter(filter, geometryAttributeName);
        }
        if (domainRestrictions != null) {
            // we have a domain filter
            filter = appendDomainRestrictionsFilter(filter, dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        return filter;
    }

    /**
     * Helper method that can be used by raster types to create a filter with the current restrictions.
     */
    Filter buildRasterFilter() {
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

    /**
     * Helper method that extract the geomtry attribute name from the current type info and invoke
     * the method that will actually build the spatial filter.
     */
    private Filter appendBoundingBoxFilter(Filter filter, CoverageInfo typeInfo) throws IOException {
        // getting the geometry attribute name
        CoverageDimensionsReader reader = CoverageDimensionsReader.instantiateFrom(typeInfo);
        String geometryAttributeName = reader.getGeometryAttributeName();
        // checking if we have a valid geometry attribute
        if (geometryAttributeName == null) {
            // this raster doesn't supports spatial filtering
            return filter;
        }
        // creating the filter
        return appendBoundingBoxFilter(filter, geometryAttributeName);
    }

    /**
     * Helper method that will build a bounding box filter using the provided geometry attribute name and
     * the current bounding box restriction. The bounding box filter will be merged with the provided filter.
     */
    private Filter appendBoundingBoxFilter(Filter filter, String geometryAttributeName) {
        CoordinateReferenceSystem coordinateReferenceSystem = boundingBox.getCoordinateReferenceSystem();
        String epsgCode = coordinateReferenceSystem == null ? null : GML2EncodingUtils.toURI(coordinateReferenceSystem);
        Filter spatialFilter = filterFactory.bbox(geometryAttributeName, boundingBox.getMinX(), boundingBox.getMinY(),
                boundingBox.getMaxX(), boundingBox.getMaxY(), epsgCode);
        return filterFactory.and(filter, spatialFilter);
    }

    /**
     * Helper method that will build a dimension domain values filter based on this dimension start and end
     * attributes. The created filter will be merged with the provided filter.
     */
    private Filter appendDomainRestrictionsFilter(Filter filter, String startAttributeName, String endAttributeName) {
        DimensionFilterBuilder dimensionFilterBuilder = new DimensionFilterBuilder(filterFactory);
        dimensionFilterBuilder.appendFilters(startAttributeName, endAttributeName, domainRestrictions);
        return filterFactory.and(filter, dimensionFilterBuilder.getFilter());
    }

    /**
     * Helper method used to get domain values from a raster type.
     */
    Tuple<ReferencedEnvelope, List<Object>> getRasterDomainValues(Filter filter, boolean noDuplicates,
                                       CoverageDimensionsReader.DataType dataType, Comparator<Object> comparator) {
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

    /**
     * Helper method used to get domain values from a vector type.
     */
    Tuple<ReferencedEnvelope, List<Object>> getVectorDomainValues(Filter filter, boolean noDuplicates, Comparator<Object> comparator) {
        FeatureCollection featureCollection = getVectorDomainValues(filter);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = DimensionsUtils.
                    getValuesWithoutDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator);
            List<Object> list = new ArrayList<>(values.size());
            list.addAll(values);
            return Tuple.tuple(featureCollection.getBounds(), list);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return Tuple.tuple(featureCollection.getBounds(),
                DimensionsUtils.getValuesWithDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator));
    }

    /**
     * Helper method used to get domain values from a vector type in the form of a feature collection.
     */
    private FeatureCollection getVectorDomainValues(Filter filter) {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        FeatureSource source;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error getting feature source of vector '%s'.", resourceInfo.getName()), exception);
        }
        Query query = new Query(source.getSchema().getName().getLocalPart(), filter == null ? Filter.INCLUDE : filter);
        try {
            return source.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error reading feature from layer '%s' for dimension '%s'.",
                    resourceInfo.getName(), getDimensionName()), exception);
        }
    }

    /**
     * Return dimension start and end attributes, values may be NULL.
     */
    public Tuple<String, String> getAttributes() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            // for vectors this information easily available
            return Tuple.tuple(dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        if (resourceInfo instanceof CoverageInfo) {
            return CoverageDimensionsReader.instantiateFrom((CoverageInfo) resourceInfo).getDimensionAttributesNames(getDimensionName());
        }
        return Tuple.tuple(null, null);
    }

    @Override
    public String toString() {
        return "Dimension{" + ", name='" + dimensionName + '\'' + ", layer=" + layerInfo.getName() + '}';
    }
}
