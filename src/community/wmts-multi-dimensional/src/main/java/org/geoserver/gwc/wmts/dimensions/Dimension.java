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
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

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
     * The provided filter can be NULL.
     */
    public abstract TreeSet<?> getDomainValues(Filter filter);

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
        return HistogramUtils.buildHistogram(getDomainValues(filter), resolution);
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

    ReferencedEnvelope getBoundingBox() {
        return boundingBox;
    }

    List<Object> getDomainRestrictions() {
        return domainRestrictions;
    }

    FilterFactory getFilterFactory() {
        return filterFactory;
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

    public Tuple<String, String> getAttributes() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            // for vectors this information easily available
            return Tuple.tuple(dimensionInfo.getAttribute(), dimensionInfo.getEndAttribute());
        }
        if (resourceInfo instanceof CoverageInfo) {
            // raster dimensions don't provide start and end attributes so we need the ask the dimension descriptor
            Tuple<String, StructuredGridCoverage2DReader> rasterReader;
            try {
                rasterReader = getRasterReader();
            } catch (IOException exception) {
                throw new RuntimeException(String.format(
                        "Error opening structured reader for raster '%s'.", layerInfo.getName(), exception));
            }
            List<DimensionDescriptor> descriptors;
            try {
                descriptors = rasterReader.second.getDimensionDescriptors(rasterReader.first);
            } catch (IOException exception) {
                throw new RuntimeException(String.format(
                        "Error extracting dimensions descriptors from raster '%s'.", layerInfo.getName(), exception));
            }
            // we have this raster dimension descriptors let's find the descriptor for our dimension
            String startAttributeName = null;
            String endAttributeName = null;
            for (DimensionDescriptor descriptor : descriptors) {
                if (getDimensionName().equalsIgnoreCase(descriptor.getName())) {
                    startAttributeName = descriptor.getStartAttribute();
                    endAttributeName = descriptor.getEndAttribute();
                }
            }
            return Tuple.tuple(startAttributeName, endAttributeName);
        }
        return Tuple.tuple(null, null);
    }

    /**
     * Returns this dimension values represented as strings taking in account this
     * dimension representation strategy. The returned values will be sorted. The
     * provided filter will be used to filter the domain values. The provided filter
     * can be NULL.
     */
    public Tuple<Integer, List<String>> getDomainValuesAsStrings(Filter filter) {
        TreeSet<?> domainValues = getDomainValues(filter);
        return Tuple.tuple(domainValues.size(), DimensionsUtils.getDomainValuesAsStrings(dimensionInfo, domainValues));
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
        if (getBoundingBox() != null) {
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
        if (getDomainRestrictions() != null) {
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
        if (getBoundingBox() != null) {
            // we have a bounding box so lets build a filter for it
            try {
                filter = appendBoundingBoxFilter(filter, typeInfo);
            } catch (IOException exception) {
                throw new RuntimeException(String.format("Exception accessing feature source of raster type '%s'.",
                        typeInfo.getName()), exception);
            }
        }
        if (getDomainRestrictions() != null) {
            Tuple<String, String> attributes = getAttributes();
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
     * Helper method
     */
    private Filter appendBoundingBoxFilter(Filter filter, CoverageInfo typeInfo) throws IOException {
        // let's find the geometry attribute name
        GridCoverage2DReader reader = (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
        if (!(reader instanceof StructuredGridCoverage2DReader)) {
            return filter;
        }
        StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;
        String coverageName = structuredReader.getGridCoverageNames()[0];
        GranuleSource source = structuredReader.getGranules(coverageName, true);
        String geometryAttributeName = source.getSchema().getGeometryDescriptor().getLocalName();
        // creating
        return appendBoundingBoxFilter(filter, geometryAttributeName);
    }

    /**
     * Helper method that will build a bounding box filter using the provided geometry attribute name and
     * the current bounding box restriction. The bounding box filter will be merged with the provided filter.
     */
    private Filter appendBoundingBoxFilter(Filter filter, String geometryAttributeName) {
        CoordinateReferenceSystem coordinateReferenceSystem = getBoundingBox().getCoordinateReferenceSystem();
        String epsgCode = coordinateReferenceSystem == null ? null : GML2EncodingUtils.toURI(coordinateReferenceSystem);
        Filter spatialFilter = getFilterFactory().bbox(geometryAttributeName, getBoundingBox().getMinX(), getBoundingBox().getMinY(),
                getBoundingBox().getMaxX(), getBoundingBox().getMaxY(), epsgCode);
        return getFilterFactory().and(filter, spatialFilter);
    }

    /**
     * Helper method that will build a dimension domain values filter based on this dimension start and end
     * attributes. The created filter will be merged with the provided filter.
     */
    private Filter appendDomainRestrictionsFilter(Filter filter, String startAttributeName, String endAttributeName) {
        DimensionFilterBuilder dimensionFilterBuilder = new DimensionFilterBuilder(getFilterFactory());
        dimensionFilterBuilder.appendFilters(startAttributeName, endAttributeName, getDomainRestrictions());
        return getFilterFactory().and(filter, dimensionFilterBuilder.getFilter());
    }

    /**
     * Helper method that can be used to read the domain values of a dimension from a raster.
     * The provided filter will be used to filter the domain values that should be returned,
     * if the provided filter is NULL nothing will ve filtered.
     */
    TreeSet<?> getRasterDomainValues(Filter filter) throws IOException {
        // preparing to read the dimension domain values
        Tuple<String, StructuredGridCoverage2DReader> rasterReader = getRasterReader();
        GranuleSource source = rasterReader.second.getGranules(rasterReader.first, true);
        List<DimensionDescriptor> descriptors = rasterReader.second.getDimensionDescriptors(rasterReader.first);
        // let's find our dimension
        for (DimensionDescriptor descriptor : descriptors) {
            if (getDimensionName().equalsIgnoreCase(descriptor.getName())) {
                // we found our dimension descriptor, creating a query
                Query query = new Query(source.getSchema().getName().getLocalPart());
                if (filter != null) {
                    query.setFilter(filter);
                }
                // reading the features removing duplicates
                FeatureCollection featureCollection = source.getGranules(query);
                UniqueVisitor uniqueVisitor = new UniqueVisitor(descriptor.getStartAttribute());
                featureCollection.accepts(uniqueVisitor, null);
                return new TreeSet(uniqueVisitor.getUnique());
            }
        }
        // well our dimension was not found
        return new TreeSet();
    }

    /**
     * Helper method that will open a structured grid coverage for the current dimension resource.
     * Returns a tuple that will contain the coverage name and the reader.
     */
    private Tuple<String, StructuredGridCoverage2DReader> getRasterReader() throws IOException {
        CoverageInfo typeInfo = (CoverageInfo) getResourceInfo();
        GridCoverage2DReader reader = (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
        if (!(reader instanceof StructuredGridCoverage2DReader)) {
            throw new RuntimeException("Non structured grid coverages cannot be filtered.");
        }
        StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;
        String coverageName = structuredReader.getGridCoverageNames()[0];
        return Tuple.tuple(coverageName, structuredReader);
    }

    @Override
    public String toString() {
        return "Dimension{" + ", name='" + dimensionName + '\'' + ", layer=" + layerInfo.getName() + '}';
    }
}
