/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Function;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.StructuredCoverageViewReader;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.gwc.wmts.Tuple;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * This class allow us to abstract from the type of different raster readers (structured and non
 * structured ones).
 */
abstract class CoverageDimensionsReader {

    private static final FilterFactory2 FILTER_FACTORY = CommonFactoryFinder.getFilterFactory2();

    public enum DataType {
        TEMPORAL,
        NUMERIC,
        CUSTOM
    }

    abstract Tuple<String, String> getDimensionAttributesNames(String dimensionName);

    abstract String getGeometryAttributeName();

    public abstract Tuple<String, FeatureCollection> getValues(
            String dimensionName, Query query, DataType dataType, SortOrder sortOrder);

    List<Object> readWithDuplicates(String dimensionName, Filter filter, DataType dataType) {
        // getting the feature collection with the values and the attribute name
        Query query = new Query(null, filter);
        Tuple<String, FeatureCollection> values =
                getValues(dimensionName, query, dataType, SortOrder.ASCENDING);
        if (values == null) {
            return Collections.emptyList();
        }
        // extracting the values removing the duplicates
        return DimensionsUtils.getValuesWithDuplicates(values.first, values.second);
    }

    Set<Object> readWithoutDuplicates(String dimensionName, Filter filter, DataType dataType) {
        // getting the feature collection with the values and the attribute name
        Query query = new Query(null, filter);
        Tuple<String, FeatureCollection> values =
                getValues(dimensionName, query, dataType, SortOrder.ASCENDING);
        if (values == null) {
            return new TreeSet<>();
        }
        // extracting the values keeping the duplicates
        return DimensionsUtils.getValuesWithoutDuplicates(values.first, values.second);
    }

    /**
     * Instantiate a coverage reader from the provided read. If the reader is a structured one good
     * we can use some optimizations otherwise we will have to really on the layer metadata.
     */
    static CoverageDimensionsReader instantiateFrom(CoverageInfo typeInfo) {
        // let's get this coverage reader
        GridCoverage2DReader reader;
        try {
            reader = (GridCoverage2DReader) typeInfo.getGridCoverageReader(null, null);
        } catch (Exception exception) {
            throw new RuntimeException("Error getting coverage reader.", exception);
        }
        if (reader instanceof StructuredGridCoverage2DReader) {
            // good we have a structured coverage reader
            return new WrapStructuredGridCoverageDimensions2DReader(
                    (StructuredGridCoverage2DReader) reader);
        }
        // non structured reader let's do our best
        return new WrapNonStructuredReader(typeInfo, reader);
    }

    public abstract ReferencedEnvelope getBounds(Filter filter);

    private static final class WrapStructuredGridCoverageDimensions2DReader
            extends CoverageDimensionsReader {

        private final StructuredGridCoverage2DReader reader;

        private WrapStructuredGridCoverageDimensions2DReader(
                StructuredGridCoverage2DReader reader) {
            this.reader = reader;
        }

        @Override
        public Tuple<String, String> getDimensionAttributesNames(String dimensionName) {
            try {
                // raster dimensions don't provide start and end attributes so we need the ask the
                // dimension descriptors
                List<DimensionDescriptor> descriptors =
                        reader.getDimensionDescriptors(reader.getGridCoverageNames()[0]);
                // we have this raster dimension descriptors let's find the descriptor for our
                // dimension
                String startAttributeName = null;
                String endAttributeName = null;
                // let's find the descriptor for our dimension
                for (DimensionDescriptor descriptor : descriptors) {
                    if (dimensionName.equalsIgnoreCase(descriptor.getName())) {
                        // descriptor found
                        startAttributeName = descriptor.getStartAttribute();
                        endAttributeName = descriptor.getEndAttribute();
                    }
                }
                return Tuple.tuple(startAttributeName, endAttributeName);
            } catch (IOException exception) {
                throw new RuntimeException(
                        "Error extracting dimensions descriptors from raster.", exception);
            }
        }

        @Override
        public String getGeometryAttributeName() {
            try {
                // getting the source of our coverage
                GranuleSource source = reader.getGranules(reader.getGridCoverageNames()[0], true);
                // well returning the geometry attribute
                return source.getSchema().getGeometryDescriptor().getLocalName();
            } catch (Exception exception) {
                throw new RuntimeException("Error getting coverage geometry attribute.");
            }
        }

        /**
         * Helper method that can be used to read the domain values of a dimension from a raster.
         * The provided filter will be used to filter the domain values that should be returned, if
         * the provided filter is NULL nothing will be filtered.
         */
        @Override
        public Tuple<String, FeatureCollection> getValues(
                String dimensionName, Query query, DataType dataType, SortOrder sortOrder) {
            try {
                // opening the source and descriptors for our raster
                GranuleSource source = reader.getGranules(reader.getGridCoverageNames()[0], true);
                List<DimensionDescriptor> descriptors =
                        reader.getDimensionDescriptors(reader.getGridCoverageNames()[0]);
                // let's find our dimension and query the data
                for (DimensionDescriptor descriptor : descriptors) {
                    if (dimensionName.equalsIgnoreCase(descriptor.getName())) {
                        // get the features attribute that contain our dimension values
                        String attributeName = descriptor.getStartAttribute();
                        // we found our dimension descriptor, creating a query
                        Query internalQuery = new Query(query);
                        internalQuery.setTypeName(source.getSchema().getName().getLocalPart());
                        internalQuery
                                .getHints()
                                .put(StructuredCoverageViewReader.QUERY_FIRST_BAND, true);
                        internalQuery.setSortBy(
                                new SortBy[] {FILTER_FACTORY.sort(attributeName, sortOrder)});
                        // reading the features using the build query
                        FeatureCollection featureCollection = source.getGranules(internalQuery);

                        return Tuple.tuple(attributeName, featureCollection);
                    }
                }
                // well our dimension was not found
                return null;
            } catch (Exception exception) {
                throw new RuntimeException("Error reading domain values.", exception);
            }
        }

        @Override
        public ReferencedEnvelope getBounds(Filter filter) {
            try {
                GranuleSource source = reader.getGranules(reader.getGridCoverageNames()[0], true);
                Query query = new Query();
                if (filter != null) {
                    query.setFilter(filter);
                }
                query.getHints().put(StructuredCoverageViewReader.QUERY_FIRST_BAND, true);
                // reading the features using the build query
                FeatureCollection featureCollection = source.getGranules(query);
                return featureCollection.getBounds();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to collect bounds", exception);
            }
        }
    }

    private static final class WrapNonStructuredReader extends CoverageDimensionsReader {

        private final CoverageInfo typeInfo;
        private final GridCoverage2DReader reader;

        private WrapNonStructuredReader(CoverageInfo typeInfo, GridCoverage2DReader reader) {
            this.typeInfo = typeInfo;
            this.reader = reader;
        }

        private static final ThreadLocal<DateFormat> DATE_FORMATTER =
                ThreadLocal.withInitial(
                        () -> {
                            SimpleDateFormat dateFormatter =
                                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                            return dateFormatter;
                        });

        private static Date formatDate(String rawValue) {
            try {
                return DATE_FORMATTER.get().parse(rawValue);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format("Error parsing date '%s'.", rawValue), exception);
            }
        }

        private static final Function<String, Object> TEMPORAL_CONVERTER =
                (rawValue) -> {
                    if (rawValue.contains("/")) {
                        String[] parts = rawValue.split("/");
                        return new DateRange(formatDate(parts[0]), formatDate(parts[1]));
                    } else {
                        return formatDate(rawValue);
                    }
                };

        private static final Function<String, Object> NUMERICAL_CONVERTER =
                (rawValue) -> {
                    if (rawValue.contains("/")) {
                        String[] parts = rawValue.split("/");
                        return new NumberRange<>(
                                Double.class,
                                Double.parseDouble(parts[0]),
                                Double.parseDouble(parts[1]));
                    } else {
                        return Double.parseDouble(rawValue);
                    }
                };

        private static final Function<String, Object> STRING_CONVERTER = (rawValue) -> rawValue;

        @Override
        public Tuple<String, String> getDimensionAttributesNames(String dimensionName) {
            // by convention the metadata entry that contains a dimension information follows the
            // pattern
            // [DIMENSION_NAME]_DOMAIN, i.e. TIME_DOMAIN, ELEVATION_DOMAIN or HAS_CUSTOM_DOMAIN
            String attributeName = dimensionName.toUpperCase() + "_DOMAIN";
            // we only have one value no start and end values
            return Tuple.tuple(attributeName, null);
        }

        @Override
        public String getGeometryAttributeName() {
            // spatial filtering is not supported for non structured readers
            return null;
        }

        @Override
        public Tuple<String, FeatureCollection> getValues(
                String dimensionName, Query query, DataType dataType, SortOrder sortOrder) {
            String metaDataValue;
            try {
                metaDataValue = reader.getMetadataValue(dimensionName.toUpperCase() + "_DOMAIN");
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error extract dimension '%s' values from raster '%s'.",
                                dimensionName, typeInfo.getName()),
                        exception);
            }
            if (metaDataValue == null || metaDataValue.isEmpty()) {
                return Tuple.tuple(getDimensionAttributesNames(dimensionName).first, null);
            }
            String[] rawValues = metaDataValue.split(",");
            dataType = normalizeDataType(rawValues[0], dataType);
            Tuple<SimpleFeatureType, Function<String, Object>> featureTypeAndConverter =
                    getFeatureTypeAndConverter(dimensionName, rawValues[0], dataType);
            MemoryFeatureCollection memoryCollection =
                    new MemoryFeatureCollection(featureTypeAndConverter.first);
            for (int i = 0; i < rawValues.length; i++) {
                SimpleFeatureBuilder featureBuilder =
                        new SimpleFeatureBuilder(featureTypeAndConverter.first);
                featureBuilder.add(featureTypeAndConverter.second.apply(rawValues[i]));
                SimpleFeature feature = featureBuilder.buildFeature(String.valueOf(i));
                if (query.getFilter() == null || query.getFilter().evaluate(feature)) {
                    memoryCollection.add(feature);
                }
            }
            AttributeDescriptor dimensionAttribute =
                    featureTypeAndConverter.first.getAttributeDescriptors().get(0);
            SimpleFeatureCollection features =
                    new SortedSimpleFeatureCollection(
                            memoryCollection,
                            new SortBy[] {
                                FILTER_FACTORY.sort(dimensionAttribute.getLocalName(), sortOrder)
                            });
            if (query.getPropertyNames() != Query.ALL_NAMES) {
                SimpleFeatureType target =
                        SimpleFeatureTypeBuilder.retype(
                                memoryCollection.getSchema(), query.getPropertyNames());
                features = new RetypingFeatureCollection(memoryCollection, target);
            }

            return Tuple.tuple(getDimensionAttributesNames(dimensionName).first, features);
        }

        @Override
        public ReferencedEnvelope getBounds(Filter filter) {
            return ReferencedEnvelope.reference(reader.getOriginalEnvelope());
        }

        private DataType normalizeDataType(String rawValue, DataType dataType) {
            if (dataType.equals(DataType.CUSTOM)) {
                try {
                    TEMPORAL_CONVERTER.apply(rawValue);
                    return DataType.TEMPORAL;
                } catch (Exception exception) {
                    // not a temporal value
                }
                try {
                    NUMERICAL_CONVERTER.apply(rawValue);
                    return DataType.NUMERIC;
                } catch (Exception exception) {
                    // not a numerical value
                }
            }
            return dataType;
        }

        private Tuple<SimpleFeatureType, Function<String, Object>> getFeatureTypeAndConverter(
                String dimensionName, String rawValue, DataType dataType) {
            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
            featureTypeBuilder.setName(typeInfo.getName());
            switch (dataType) {
                case TEMPORAL:
                    featureTypeBuilder.add(
                            getDimensionAttributesNames(dimensionName).first,
                            TEMPORAL_CONVERTER.apply(rawValue).getClass());
                    return Tuple.tuple(featureTypeBuilder.buildFeatureType(), TEMPORAL_CONVERTER);
                case NUMERIC:
                    featureTypeBuilder.add(
                            getDimensionAttributesNames(dimensionName).first,
                            NUMERICAL_CONVERTER.apply(rawValue).getClass());
                    return Tuple.tuple(featureTypeBuilder.buildFeatureType(), NUMERICAL_CONVERTER);
            }
            featureTypeBuilder.add(getDimensionAttributesNames(dimensionName).first, String.class);
            return Tuple.tuple(featureTypeBuilder.buildFeatureType(), STRING_CONVERTER);
        }
    }
}
