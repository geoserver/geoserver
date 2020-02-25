/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.GridCoverageRequest;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.geometry.Envelope;

/**
 * A class which takes care of handling default values for unspecified dimensions (if needed).
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class WCSDefaultValuesHelper {

    FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private String coverageName;

    private GridCoverage2DReader reader;

    private GetCoverageType request;

    private ReaderDimensionsAccessor accessor;

    private static final WCSDimensionsValueParser PARSER = new WCSDimensionsValueParser();

    public WCSDefaultValuesHelper(
            GridCoverage2DReader reader,
            ReaderDimensionsAccessor accessor,
            GetCoverageType request,
            String coverageName)
            throws IOException {
        super();
        this.accessor =
                accessor == null
                        ? new ReaderDimensionsAccessor(reader)
                        : accessor; // Force the creation of an accessor
        this.reader = reader;
        this.request = request;
        this.coverageName = coverageName;
    }

    /**
     * Check the current request and update default values if needed. Return the updated {@link
     * GridCoverageRequest}
     */
    public void setDefaults(GridCoverageRequest subsettingRequest) throws IOException {
        // Deal with default values
        final String format = request.getFormat();
        if (format != null && !GetCoverage.formatSupportMDOutput(format)) {
            // TODO: Revisit this code and change that Format String.
            // Formats supporting multidimensional output format don't neede to setup default values
            // Therefore, no need to set default values

            // For 2D output format, we can setup default values to reduce the number of results
            if (!(reader instanceof StructuredGridCoverage2DReader)) {
                // use standard code which gets default value from each domain which hasn't be
                // subset
                setStandardReaderDefaults(subsettingRequest);
            } else {
                // Use optimized code for structured grid coverage reader which uses granuleSource
                // queries
                // to determine valid default values
                setDefaultsFromStructuredReader(subsettingRequest);
            }
        }
    }

    /**
     * Set default values by querying a {@link GranuleSource} from {@link
     * StructuredGridCoverage2DReader} in order to update unspecified dimensions values from
     * attributes values obtained from the query.
     */
    private GridCoverageRequest setDefaultsFromStructuredReader(
            GridCoverageRequest subsettingRequest) throws IOException {

        // Get subsetting request
        DateRange temporalSubset = subsettingRequest.getTemporalSubset();
        NumberRange<?> elevationSubset = subsettingRequest.getElevationSubset();
        Map<String, List<Object>> dimensionsSubset = subsettingRequest.getDimensionsSubset();
        Envelope envelopeSubset = subsettingRequest.getSpatialSubset();
        Filter originalFilter = subsettingRequest.getFilter();

        final int specifiedDimensionsSubset =
                dimensionsSubset != null ? dimensionsSubset.size() : 0;

        // Casting to StructuredGridCoverage2DReader
        final StructuredGridCoverage2DReader structuredReader =
                (StructuredGridCoverage2DReader) reader;

        // Getting dimension descriptors
        final List<DimensionDescriptor> dimensionDescriptors =
                structuredReader.getDimensionDescriptors(coverageName);
        DimensionDescriptor timeDimension = null;
        DimensionDescriptor elevationDimension = null;
        final List<DimensionDescriptor> customDimensions = new ArrayList<DimensionDescriptor>();
        int dimensions = 0;

        // Collect dimension Descriptor info
        for (DimensionDescriptor dimensionDescriptor : dimensionDescriptors) {
            if (dimensionDescriptor.getName().equalsIgnoreCase(ResourceInfo.TIME)) {
                timeDimension = dimensionDescriptor;
            } else if (dimensionDescriptor.getName().equalsIgnoreCase(ResourceInfo.ELEVATION)) {
                elevationDimension = dimensionDescriptor;
            } else {
                customDimensions.add(dimensionDescriptor);
                dimensions++;
            }
        }

        final boolean defaultTimeNeeded = temporalSubset == null && timeDimension != null;
        final boolean defaultElevationNeeded =
                elevationSubset == null && elevationDimension != null;
        final boolean defaultCustomDimensionsNeeded = dimensions != specifiedDimensionsSubset;

        // Note that only Slicing is currently supported;
        if (defaultTimeNeeded || defaultElevationNeeded || defaultCustomDimensionsNeeded) {

            // Get granules source
            GranuleSource source = structuredReader.getGranules(coverageName, true);

            // Set filtering query matching the specified subsets.
            Filter finalFilter =
                    setFilters(
                            originalFilter,
                            temporalSubset,
                            elevationSubset,
                            envelopeSubset,
                            dimensionsSubset,
                            structuredReader,
                            timeDimension,
                            elevationDimension,
                            customDimensions);
            Query query = new Query();

            // Set sorting order (default Policy is using Max... therefore Descending order)
            final List<SortBy> requestedSort = subsettingRequest.getSortBy();
            if (requestedSort == null) {
                sortBy(query, timeDimension, elevationDimension);
            } else {
                query.setSortBy(requestedSort.toArray(new SortBy[requestedSort.size()]));
            }
            query.setFilter(finalFilter);

            // Returning a single feature matching the filtering
            query.setMaxFeatures(1);

            // Get granules from query
            SimpleFeatureCollection granulesCollection = source.getGranules(query);
            SimpleFeatureIterator features = granulesCollection.features();
            try {
                if (features.hasNext()) {
                    final SimpleFeature feature = features.next();

                    // Default time
                    if (defaultTimeNeeded && timeDimension != null) {
                        temporalSubset = setDefaultTemporalSubset(timeDimension, feature);
                        subsettingRequest.setTemporalSubset(temporalSubset);
                    }

                    // Default elevation
                    if (defaultElevationNeeded && elevationDimension != null) {
                        elevationSubset = setDefaultElevationSubset(elevationDimension, feature);
                        subsettingRequest.setElevationSubset(elevationSubset);
                    }

                    // Default custom dimensions
                    if (defaultCustomDimensionsNeeded && !customDimensions.isEmpty()) {
                        dimensionsSubset = setDefaultDimensionsSubset(customDimensions, feature);
                        subsettingRequest.setDimensionsSubset(dimensionsSubset);
                    }
                }
            } finally {
                if (features != null) {
                    features.close();
                }
            }
        }
        return subsettingRequest;
    }

    /**
     * Set default for custom dimensions, taking values from the feature resulting from the query.
     */
    private Map<String, List<Object>> setDefaultDimensionsSubset(
            List<DimensionDescriptor> customDimensions, SimpleFeature feature) {
        Map<String, List<Object>> dimensionsSubset = new HashMap<String, List<Object>>();
        for (DimensionDescriptor dimensionDescriptor : customDimensions) {

            // TODO: Add support for ranged additional dimensions
            final String start = dimensionDescriptor.getStartAttribute();
            Object value = feature.getAttribute(start);

            // Replace specified values since they have been anyway set in the filters
            List<Object> dimensionValues = new ArrayList<Object>();
            dimensionValues.add(value);
            dimensionsSubset.put(dimensionDescriptor.getName().toUpperCase(), dimensionValues);
        }
        return dimensionsSubset;
    }

    /** Set default elevation value from the provided feature */
    private NumberRange<?> setDefaultElevationSubset(
            DimensionDescriptor elevationDimension, SimpleFeature f) {
        final String start = elevationDimension.getStartAttribute();
        final String end = elevationDimension.getEndAttribute();
        Number startTime = (Number) f.getAttribute(start);
        Number endTime = startTime;
        if (end != null) {
            endTime = (Number) f.getAttribute(end);
        }
        return new NumberRange(startTime.getClass(), startTime, endTime);
    }

    /** Set default time value from the provided feature */
    private DateRange setDefaultTemporalSubset(DimensionDescriptor timeDimension, SimpleFeature f) {
        final String start = timeDimension.getStartAttribute();
        final String end = timeDimension.getEndAttribute();
        Date startTime = (Date) f.getAttribute(start);
        Date endTime = startTime;
        if (end != null) {
            endTime = (Date) f.getAttribute(end);
        }
        return new DateRange(startTime, endTime);
    }

    /**
     * Current policy is to use the max value as default for time and min value as default for
     * elevation.
     *
     * @param query the originating query
     * @param elevationDimension TODO: Consider also sorting on custom dimensions
     */
    private void sortBy(
            Query query,
            DimensionDescriptor timeDimension,
            DimensionDescriptor elevationDimension) {
        final List<SortBy> clauses = new ArrayList<SortBy>();
        // TODO: Check sortBy clause is supported
        if (timeDimension != null) {
            clauses.add(
                    new SortByImpl(
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.property(
                                    timeDimension.getStartAttribute()),
                            SortOrder.DESCENDING));
        }
        if (elevationDimension != null) {
            clauses.add(
                    new SortByImpl(
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.property(
                                    elevationDimension.getStartAttribute()),
                            SortOrder.ASCENDING));
        }
        final SortBy[] sb = clauses.toArray(new SortBy[] {});
        query.setSortBy(sb);
    }

    /**
     * Setup filter query on top of specified subsets values to return only granules satisfying the
     * specified conditions.
     */
    private Filter setFilters(
            Filter originalFilter,
            DateRange temporalSubset,
            NumberRange<?> elevationSubset,
            Envelope envelopeSubset,
            Map<String, List<Object>> dimensionSubset,
            StructuredGridCoverage2DReader reader,
            DimensionDescriptor timeDimension,
            DimensionDescriptor elevationDimension,
            List<DimensionDescriptor> additionalDimensions)
            throws IOException {
        List<Filter> filters = new ArrayList<Filter>();

        // Setting temporal filter
        Filter timeFilter =
                temporalSubset == null && timeDimension == null
                        ? null
                        : setTimeFilter(
                                temporalSubset,
                                timeDimension.getStartAttribute(),
                                timeDimension.getEndAttribute());

        // Setting elevation filter
        Filter elevationFilter =
                elevationSubset == null && elevationDimension == null
                        ? null
                        : setElevationFilter(
                                elevationSubset,
                                elevationDimension.getStartAttribute(),
                                elevationDimension.getEndAttribute());

        // setting envelope filter
        Filter envelopeFilter = setEnevelopeFilter(envelopeSubset, reader);

        // Setting dimensional filters
        Filter additionalDimensionsFilter =
                setAdditionalDimensionsFilter(dimensionSubset, additionalDimensions);

        // Updating filters
        if (originalFilter != null) {
            filters.add(originalFilter);
        }
        if (elevationFilter != null) {
            filters.add(elevationFilter);
        }
        if (timeFilter != null) {
            filters.add(timeFilter);
        }
        if (envelopeFilter != null) {
            filters.add(envelopeFilter);
        }
        if (additionalDimensionsFilter != null) {
            filters.add(additionalDimensionsFilter);
        }

        // Merging all filters
        Filter finalFilter = FF.and(filters);
        return finalFilter;
    }

    /** Set envelope filter to restrict the results to the specified envelope */
    private Filter setEnevelopeFilter(
            Envelope envelopeSubset, StructuredGridCoverage2DReader reader) throws IOException {
        Filter envelopeFilter = null;
        if (envelopeSubset != null) {
            Polygon polygon = JTS.toGeometry(new ReferencedEnvelope(envelopeSubset));
            GeometryDescriptor geom =
                    reader.getGranules(coverageName, true).getSchema().getGeometryDescriptor();
            PropertyName geometryProperty = FF.property(geom.getLocalName());
            Geometry nativeCRSPolygon;
            try {
                nativeCRSPolygon =
                        JTS.transform(
                                polygon,
                                CRS.findMathTransform(
                                        DefaultGeographicCRS.WGS84,
                                        reader.getCoordinateReferenceSystem()));
                Literal polygonLiteral = FF.literal(nativeCRSPolygon);
                // TODO: Check that geom operation. Should I do intersection or containment check?
                envelopeFilter = FF.intersects(geometryProperty, polygonLiteral);
                //                envelopeFilter = FF.within(geometryProperty, polygonLiteral);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return envelopeFilter;
    }

    /** Set filter to match specified additional dimensions values */
    private Filter setAdditionalDimensionsFilter(
            Map<String, List<Object>> dimensionSubset,
            List<DimensionDescriptor> additionalDimensions) {
        Filter additionalDimensionsFilter = null;

        // Check whether the number of specified additional dimensions values doesn't match the
        // number of available additional dimensions
        if (additionalDimensions != null
                && dimensionSubset != null
                && additionalDimensions.size() != dimensionSubset.size()
                && dimensionSubset.size() > 0) {

            List<Filter> additionalDimensionFilterList = new ArrayList<Filter>();
            Set<String> dimensionKeys = dimensionSubset.keySet();
            for (String dimension : dimensionKeys) {
                // Look for the specified dimension
                Filter dimensionFilter =
                        createCustomDimensionFilter(
                                dimension, dimensionSubset, additionalDimensions);
                if (dimensionFilter != null) {
                    additionalDimensionFilterList.add(dimensionFilter);
                }
            }
            if (!additionalDimensionFilterList.isEmpty()) {
                additionalDimensionsFilter = FF.and(additionalDimensionFilterList);
            }
        }
        return additionalDimensionsFilter;
    }

    /** Create a filter matching the specified additional dimension value */
    private Filter createCustomDimensionFilter(
            String dimension,
            Map<String, List<Object>> dimensionSubset,
            List<DimensionDescriptor> customDimensions) {
        List<Object> dimensionSelection = dimensionSubset.get(dimension);

        // Only supporting slicing right now. Dealing with a single dimension value
        Object dimensionValue = dimensionSelection.get(0);
        for (DimensionDescriptor dimensionDescriptor : customDimensions) {
            if (dimensionDescriptor.getName().equalsIgnoreCase(dimension)) {
                String attribute = dimensionDescriptor.getStartAttribute();
                return FF.equals(FF.property(attribute), FF.literal(dimensionValue));
            }
        }
        return null;
    }

    /** Set a {@link Filter} based on the specified time subset, or null if missing. */
    private Filter setTimeFilter(DateRange timeRange, String start, String end) {
        if (timeRange != null) {
            if (end == null) {
                // single value time
                return betweenFilter(start, timeRange.getMinValue(), timeRange.getMaxValue());
            } else {
                return rangeFilter(start, end, timeRange.getMinValue(), timeRange.getMaxValue());
            }
        }
        return null;
    }

    /** Set a {@link Filter} based on the specified elevation subset, or null if missing. */
    private Filter setElevationFilter(NumberRange elevationSubset, String start, String end) {
        if (elevationSubset != null) {
            if (end == null) {
                // single value elevation
                return betweenFilter(
                        start, elevationSubset.getMinValue(), elevationSubset.getMaxValue());
            } else {
                return rangeFilter(
                        start, end, elevationSubset.getMinValue(), elevationSubset.getMaxValue());
            }
        }
        return null;
    }

    /** A simple filter making sure a property is contained between minValue and maxValue */
    private Filter betweenFilter(String start, Object minValue, Object maxValue) {
        return FF.between(FF.property(start), FF.literal(minValue), FF.literal(maxValue));
    }

    /** A simple filter for range containment */
    private Filter rangeFilter(String start, String end, Object minValue, Object maxValue) {
        Filter f1 = FF.lessOrEqual(FF.property(start), FF.literal(maxValue));
        Filter f2 = FF.greaterOrEqual(FF.property(end), FF.literal(minValue));
        return FF.and(Arrays.asList(f1, f2));

        //        Filter f1 = FF.greaterOrEqual(FF.property(start), FF.literal(minValue));
        //        Filter f2 = FF.lessOrEqual(FF.property(end), FF.literal(maxValue));
        //        return FF.and(Arrays.asList(f1, f2));
    }

    /** Set default values for the standard reader case (no DimensionsDescriptor available) */
    private GridCoverageRequest setStandardReaderDefaults(GridCoverageRequest subsettingRequest)
            throws IOException {
        DateRange temporalSubset = subsettingRequest.getTemporalSubset();
        NumberRange<?> elevationSubset = subsettingRequest.getElevationSubset();
        Map<String, List<Object>> dimensionSubset = subsettingRequest.getDimensionsSubset();

        // Reader is not a StructuredGridCoverage2DReader instance. Set default ones with policy
        // "time = max, elevation = min".

        // Setting default time
        if (temporalSubset == null) {
            // use "max" as the default
            Date maxTime = accessor.getMaxTime();
            if (maxTime != null) {
                temporalSubset = new DateRange(maxTime, maxTime);
            }
        }

        // Setting default elevation
        if (elevationSubset == null) {
            // use "min" as the default
            Number minElevation = accessor.getMinElevation();
            if (minElevation != null) {
                elevationSubset =
                        new NumberRange(minElevation.getClass(), minElevation, minElevation);
            }
        }

        // Setting default custom dimensions
        final List<String> customDomains = accessor.getCustomDomains();
        int availableCustomDimensions = 0;
        int specifiedCustomDimensions = 0;
        if (customDomains != null && !customDomains.isEmpty()) {
            availableCustomDimensions = customDomains.size();
            specifiedCustomDimensions = dimensionSubset != null ? dimensionSubset.size() : 0;
            if (dimensionSubset == null) {
                dimensionSubset = new HashMap<String, List<Object>>();
            }
        }
        if (availableCustomDimensions != specifiedCustomDimensions) {
            setDefaultCustomDimensions(customDomains, dimensionSubset);
        }

        subsettingRequest.setDimensionsSubset(dimensionSubset);
        subsettingRequest.setTemporalSubset(temporalSubset);
        subsettingRequest.setElevationSubset(elevationSubset);
        return subsettingRequest;
    }

    /** Set default custom dimensions */
    private void setDefaultCustomDimensions(
            List<String> customDomains, Map<String, List<Object>> dimensionSubset)
            throws IOException {

        // Scan available custom dimensions
        for (String customDomain : customDomains) {
            if (!dimensionSubset.containsKey(customDomain)) {
                List<Object> dimensionValue = new ArrayList<Object>();

                // set default of the proper datatype (in case of known Domain datatype)
                String defaultValue = accessor.getCustomDomainDefaultValue(customDomain);
                String dataType = reader.getMetadataValue(customDomain + "_DOMAIN_DATATYPE");
                if (dataType != null) {
                    PARSER.setValues(defaultValue, dimensionValue, dataType);
                } else {
                    dimensionValue.add(defaultValue);
                }
                dimensionSubset.put(customDomain, dimensionValue);
            }
        }
    }
}
