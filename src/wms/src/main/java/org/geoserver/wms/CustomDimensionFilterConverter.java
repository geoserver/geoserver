/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geoserver.wms.dimension.DimensionFilterBuilder;
import org.geotools.util.Converters;
import org.geotools.util.Range;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Centralizes filter convert operations for custom dimensions.
 *
 * @author Fernando Mino - Geosolutions
 */
class CustomDimensionFilterConverter {

    private static final Logger LOGGER = Logging.getLogger(CustomDimensionFilterConverter.class);

    private DefaultValueStrategyFactory defaultValueStrategyFactory;
    private final FilterFactory ff;

    private static Collection<ValueConverter> converters;

    CustomDimensionFilterConverter(
            DefaultValueStrategyFactory defaultValueStrategyFactory, FilterFactory ff) {
        this.ff = ff;
        this.defaultValueStrategyFactory = defaultValueStrategyFactory;
    }

    /**
     * Builds a filter in base to KVP map and Feature type info provided parameters.
     *
     * @param rawKVP Request KVP values
     * @param typeInfo Feature type info
     * @return builded filter
     */
    public Filter getDimensionsToFilter(
            final Map<String, String> rawKVP, final FeatureTypeInfo typeInfo) {
        try {
            // get the list of configured custom dimensions for this Feature Type
            final MetadataMap metadataMap = typeInfo.getMetadata();
            final FeatureType featureType = typeInfo.getFeatureType();
            final Map<String, Pair<DimensionInfo, String>> rawValuesMap =
                    metadataMap
                            .entrySet()
                            .stream()
                            .filter(
                                    e ->
                                            e.getValue() instanceof DimensionInfo
                                                    && e.getKey().startsWith("dim_"))
                            .collect(
                                    Collectors.toMap(
                                            e -> unrollDimPrefix(e.getKey()),
                                            e ->
                                                    Pair.of(
                                                            (DimensionInfo) e.getValue(),
                                                            rawKVP.get(e.getKey().toUpperCase()))));
            final List<Filter> filters = new ArrayList<>();
            // convert raw value strings to proper types
            for (Map.Entry<String, Pair<DimensionInfo, String>> entry : rawValuesMap.entrySet()) {
                final String dimensionName = entry.getKey();
                final String attributeName = entry.getValue().getKey().getAttribute();
                final PropertyDescriptor descriptor = featureType.getDescriptor(attributeName);
                if (descriptor == null) {
                    throw new IllegalArgumentException(
                            "Attribute Name '" + attributeName + "' not found.");
                }
                final Class<?> binding = descriptor.getType().getBinding();
                if (StringUtils.isBlank(attributeName)) {
                    LOGGER.severe(
                            "Required attribute name is empty for dimension='"
                                    + dimensionName
                                    + "'");
                    continue;
                }
                final String endAttributeName = entry.getValue().getKey().getEndAttribute();
                final String rawValue =
                        entry.getValue().getRight() != null ? entry.getValue().getRight() : null;
                // convert values
                final List<Object> convertedValues =
                        convertValues(
                                rawValue,
                                binding,
                                () ->
                                        getDefaultCustomDimension(
                                                rollDimPrefix(dimensionName), typeInfo, binding));
                // generate a Filter for every value in convertedValues
                filters.add(buildFilter(convertedValues, attributeName, endAttributeName));
            }
            return ff.and(filters);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<Object> convertValues(
            String rawValue, Class<?> binding, Supplier<Object> defaultSupplier) {
        if (StringUtils.isBlank(rawValue)) {
            return Arrays.asList(defaultSupplier.get());
        } else {
            // convert values
            return convertValues(splitStringValue(rawValue), binding);
        }
    }

    private String unrollDimPrefix(String key) {
        return key.replaceFirst("dim_", "");
    }

    private String rollDimPrefix(String name) {
        return "dim_" + name;
    }

    private Filter buildFilter(List<Object> values, String attributeName, String endAttributeName) {
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
        builder.appendFilters(attributeName, endAttributeName, values);
        return builder.getFilter();
    }

    private List<String> splitStringValue(String value) {
        String[] strings = value.split(",");
        return Arrays.asList(strings);
    }

    private Object getDefaultCustomDimension(
            String name, ResourceInfo resourceInfo, Class<?> binding) {
        // check the time metadata
        final DimensionInfo dimensionInfo =
                resourceInfo.getMetadata().get(name, DimensionInfo.class);
        if (dimensionInfo == null || !dimensionInfo.isEnabled()) {
            throw new ServiceException(
                    "Layer "
                            + resourceInfo.prefixedName()
                            + " does not have custom dimension support enabled");
        }
        DimensionDefaultValueSelectionStrategy strategy =
                defaultValueStrategyFactory.getDefaultValueStrategy(
                        resourceInfo, name, dimensionInfo);
        return strategy.getDefaultValue(resourceInfo, name, dimensionInfo, binding);
    }

    private List<Object> convertValues(List<String> rawValues, Class<?> binding) {
        final Collection<ValueConverter> valueConverters = valueConverters();
        Optional<ValueConverter> converter =
                valueConverters.stream().filter(c -> c.canProcess(binding)).findFirst();
        if (converter.isPresent()) {
            return converter.get().convert(rawValues, binding);
        } else {
            throw new ServiceException("Binding unsuported: " + binding);
        }
    }

    static interface DefaultValueStrategyFactory {
        DimensionDefaultValueSelectionStrategy getDefaultValueStrategy(
                ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo);
    }

    static Collection<ValueConverter> valueConverters() {
        if (converters != null) {
            return converters;
        } else {
            return initValueConverters();
        }
    }

    static synchronized Collection<ValueConverter> initValueConverters() {
        if (converters == null) {
            converters =
                    Arrays.asList(
                            new NumberConverter(),
                            new StringConverter(),
                            new BooleanConverter(),
                            new DateConverter());
        }
        return converters;
    }

    static interface ValueConverter {

        boolean canProcess(Class<?> binding);

        List<Object> convert(List<String> rawValues, Class<?> binding);
    }

    abstract static class ValueConverterImpl implements ValueConverter {

        protected boolean isRange(String value) {
            return value.contains("/");
        }

        protected Optional<String> getRangeValue(List<String> values) {
            return values.stream().filter(v -> isRange(v)).findFirst();
        }

        protected Collection<String> splitRangeValues(String values) {
            return Arrays.asList(values.split(Pattern.quote("/")));
        }

        protected List<String> cleanBlankValues(List<String> values) {
            return values.stream()
                    .filter(v -> StringUtils.isNotBlank(v))
                    .collect(Collectors.toList());
        }
    }

    static class NumberConverter extends ValueConverterImpl {

        @Override
        public boolean canProcess(Class<?> binding) {
            return Number.class.isAssignableFrom(binding);
        }

        @Override
        public List<Object> convert(List<String> rawValues, Class<?> binding) {
            // remove blank values
            rawValues = cleanBlankValues(rawValues);
            // there is at least one range?
            Optional<String> rangeValue = getRangeValue(rawValues);
            if (rangeValue.isPresent()) {
                // range value present, only process it as unique value
                final List<Object> rangeValues =
                        splitRangeValues(rangeValue.get())
                                .stream()
                                .map(v -> Converters.convert(v, binding))
                                .collect(Collectors.toList());
                final Range<? extends Comparable> range =
                        new Range<>(
                                (Class<Comparable>) binding,
                                (Comparable) rangeValues.get(0),
                                (Comparable) rangeValues.get(1));
                return Arrays.asList(range);
            } else {
                // no range value present, convert all values
                // filter null results
                return rawValues
                        .stream()
                        .map(v -> Converters.convert(v, binding))
                        .filter(v -> v != null)
                        .collect(Collectors.toList());
            }
        }
    }

    static class StringConverter extends ValueConverterImpl {

        @Override
        public boolean canProcess(Class<?> binding) {
            return String.class.isAssignableFrom(binding);
        }

        @Override
        public List<Object> convert(List<String> rawValues, Class<?> binding) {
            // remove blank values
            rawValues = cleanBlankValues(rawValues);
            return (List) rawValues;
        }
    }

    static class BooleanConverter extends ValueConverterImpl {

        @Override
        public boolean canProcess(Class<?> binding) {
            return Boolean.class.isAssignableFrom(binding);
        }

        @Override
        public List<Object> convert(List<String> rawValues, Class<?> binding) {
            // remove blank values
            rawValues = cleanBlankValues(rawValues);
            // no range supported
            // filter unsupported values
            // convert values
            return rawValues
                    .stream()
                    .filter(v -> !isRange(v) && isValidBoolean(v))
                    .map(v -> Converters.convert(v.trim(), Boolean.class))
                    .collect(Collectors.toList());
        }

        private boolean isValidBoolean(String value) {
            if (value == null) return false;
            value = value.trim().toLowerCase();
            return value.equals("true") || value.equals("false");
        }
    }

    static class DateConverter extends ValueConverterImpl {

        @Override
        public boolean canProcess(Class<?> binding) {
            return Date.class.isAssignableFrom(binding);
        }

        @Override
        public List<Object> convert(List<String> rawValues, Class<?> binding) {
            // remove blank values
            rawValues = cleanBlankValues(rawValues);
            // there is at least one range?
            Optional<String> rangeValue = getRangeValue(rawValues);
            if (rangeValue.isPresent()) {
                // range value present, only process it as unique value
                final List<Object> rangeValues =
                        splitRangeValues(rangeValue.get())
                                .stream()
                                .map(v -> Converters.convert(v, binding))
                                .collect(Collectors.toList());
                final Range<? extends Comparable> range =
                        new Range<>(
                                (Class<Comparable>) binding,
                                (Comparable) rangeValues.get(0),
                                (Comparable) rangeValues.get(1));
                return Arrays.asList(range);
            } else {
                return rawValues
                        .stream()
                        .map(v -> Converters.convert(v, binding))
                        .filter(v -> v != null)
                        .collect(Collectors.toList());
            }
        }
    }
}
