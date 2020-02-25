/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

/**
 * Utility class for aggregating several dimensions. All the dimensions will share the same spatial
 * domain (bounding box), restrictions (filter) and resource.
 */
public class Domains {

    private final List<Dimension> dimensions;
    private final ReferencedEnvelope spatialDomain;
    private final Filter filter;

    private final LayerInfo layerInfo;
    private int expandLimit;
    private int maxReturnedValues;
    private SortOrder sortOrder;

    private String histogram;
    private String resolution;
    private String fromValue;

    public Domains(
            List<Dimension> dimensions,
            LayerInfo layerInfo,
            ReferencedEnvelope boundingBox,
            Filter filter) {
        this.dimensions = dimensions;
        this.layerInfo = layerInfo;
        this.spatialDomain = boundingBox;
        this.filter = filter;
    }

    public Domains withExpandLimit(int expandLimit) {
        this.expandLimit = expandLimit;
        return this;
    }

    public Domains withMaxReturnedValues(int maxReturnedValues) {
        this.maxReturnedValues = maxReturnedValues;
        return this;
    }

    public Domains withSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public Domains withFromValue(String fromValue) {
        this.fromValue = fromValue;
        return this;
    }

    /** The maximum number of returned values in a GetDomainValues request */
    public int getMaxReturnedValues() {
        return maxReturnedValues;
    }

    /** Returns the "fromValue" parameter in a GetDomainValues request */
    public String getFromValue() {
        return fromValue;
    }

    /** The sort direction in a GetDomainValues request */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public List<Dimension> getDimensions() {
        return dimensions;
    }

    ReferencedEnvelope getSpatialDomain() {
        return spatialDomain;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setHistogram(String histogram) {
        this.histogram = histogram;
    }

    void setResolution(String resolution) {
        this.resolution = resolution;
    }

    String getHistogramName() {
        return histogram;
    }

    public int getExpandLimit() {
        return expandLimit;
    }

    Tuple<String, List<Integer>> getHistogramValues() {
        for (Dimension dimension : dimensions) {
            if (dimension.getDimensionName().equalsIgnoreCase(histogram)) {
                return dimension.getHistogram(filter, resolution);
            }
        }
        throw new RuntimeException(String.format("Dimension '%s' could not be found.", histogram));
    }

    /** Returns the feature collection associated with these domains. */
    FeatureCollection getFeatureCollection() {
        ResourceInfo resourceInfo = layerInfo.getResource();
        try {
            if (resourceInfo instanceof FeatureTypeInfo) {
                // accessing the features of a vector
                return new FilteredFeatureType((FeatureTypeInfo) resourceInfo, filter)
                        .getFeatureSource(null, null)
                        .getFeatures();
            }
            // accessing the features of a raster
            return getFeatureCollection((CoverageInfo) resourceInfo);
        } catch (IOException exception) {
            throw new RuntimeException(
                    String.format("Error getting features of layer '%s'.", layerInfo.getName()),
                    exception);
        }
    }

    /** Helper method that just gets a feature collection from a raster. */
    private FeatureCollection getFeatureCollection(CoverageInfo typeInfo) throws IOException {
        GridCoverage2DReader reader =
                (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
        if (!(reader instanceof StructuredGridCoverage2DReader)) {
            throw new RuntimeException(
                    "Is not possible to obtain a feature collection from a non structured reader.");
        }
        StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;
        String coverageName = structuredReader.getGridCoverageNames()[0];
        GranuleSource source = structuredReader.getGranules(coverageName, true);
        Query query = new Query(source.getSchema().getName().getLocalPart());
        if (filter != null) {
            query.setFilter(filter);
        }
        return source.getGranules(query);
    }
}
