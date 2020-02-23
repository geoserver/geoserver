/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.*;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.ows.wms.xml.Dimension;
import org.geotools.ows.wms.xml.Extent;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class avoiding to duplicate the time/elevation management code between WMS 1.1 and 1.3
 *
 * @author Andrea Aime - GeoSolutions
 */
abstract class DimensionHelper {

    static final Logger LOGGER = Logging.getLogger(DimensionHelper.class);
    public static final String NEAREST_VALUE = "nearestValue";
    public static final String NAME = "name";
    public static final String DEFAULT = "default";
    public static final String UNITS = "units";
    public static final String TIME = "time";
    public static final String ELEVATION = "elevation";
    public static final String UNIT_SYMBOL = "unitSymbol";

    enum Mode {
        WMS11,
        WMS13
    }

    Mode mode;
    WMS wms;

    public DimensionHelper(Mode mode, WMS wms) {
        this.mode = mode;
        this.wms = wms;
    }

    /** Implement to write out an element */
    protected abstract void element(String element, String content);

    /** Implement to write out an element */
    protected abstract void element(String element, String content, Attributes atts);

    void handleVectorLayerDimensions(LayerInfo layer) {
        // do we have time and elevation?
        final FeatureTypeInfo typeInfo = (FeatureTypeInfo) layer.getResource();
        // do we have custom dimensions?
        final Map<String, DimensionInfo> customDims = getCustomDimensions(typeInfo);

        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        DimensionInfo elevInfo =
                typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        boolean hasTime = timeInfo != null && timeInfo.isEnabled();
        boolean hasElevation = elevInfo != null && elevInfo.isEnabled();

        // skip if no need
        if (!hasTime && !hasElevation && customDims.isEmpty()) {
            return;
        }

        if (mode == Mode.WMS11) {
            String elevUnits = hasElevation ? elevInfo.getUnits() : "";
            String elevUnitSymbol = hasElevation ? elevInfo.getUnitSymbol() : "";
            declareWMS11Dimensions(
                    hasTime,
                    hasElevation,
                    elevUnits,
                    elevUnitSymbol,
                    customDims.isEmpty() ? null : customDims);
        }

        // Time dimension
        if (hasTime) {
            try {
                handleTimeDimensionVector(typeInfo);
            } catch (IOException e) {
                throw new RuntimeException("Failed to handle time attribute for layer", e);
            }
        }
        // elevation dimension
        if (hasElevation) {
            try {
                handleElevationDimensionVector(typeInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // custom dimensions
        if (!customDims.isEmpty()) {
            handleCustomDimensionsVector(typeInfo, customDims);
        }
    }

    void handleCustomDimensionsVector(
            FeatureTypeInfo featureTypeInfo, Map<String, DimensionInfo> customDims) {
        for (Entry<String, DimensionInfo> entry : customDims.entrySet()) {
            handleCustomDimensionVector(featureTypeInfo, entry);
        }
    }

    void handleCustomDimensionVector(
            FeatureTypeInfo featureTypeInfo, Entry<String, DimensionInfo> customDim) {
        try {
            final TreeSet<Object> values =
                    wms.getDimensionValues(featureTypeInfo, customDim.getValue());
            String metadata;
            String units = customDim.getValue().getUnits();
            String unitSymbol = customDim.getValue().getUnitSymbol();
            final Optional<Class> dataTypeOpt = getDataType(values);
            if (dataTypeOpt.isPresent()) {
                final Class<Object> type = dataTypeOpt.get();
                if (Date.class.isAssignableFrom(type)) {
                    metadata = getTemporalDomainRepresentation(customDim.getValue(), values);
                } else if (Number.class.isAssignableFrom(type)) {
                    metadata = getNumberRepresentation(customDim.getValue(), values);
                } else {
                    final List<String> valuesList =
                            values.stream()
                                    .filter(x -> x != null)
                                    .map(x -> x.toString())
                                    .collect(Collectors.toList());
                    metadata = getCustomDomainRepresentation(customDim.getValue(), valuesList);
                }
            } else {
                metadata = "";
            }
            String defaultValue =
                    getDefaultValueRepresentation(featureTypeInfo, "dim_" + customDim.getKey(), "");
            writeCustomDimensionVector(
                    customDim.getKey(), values, metadata, units, unitSymbol, defaultValue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Optional<Class> getDataType(Set<Object> values) {
        return values.stream().filter(x -> x != null).findFirst().map(Object::getClass);
    }

    private Map<String, DimensionInfo> getCustomDimensions(final FeatureTypeInfo typeInfo) {
        return typeInfo.getMetadata()
                .entrySet()
                .stream()
                .filter(
                        e ->
                                e.getValue() instanceof DimensionInfo
                                        && e.getKey() != null
                                        && e.getKey().startsWith("dim_")
                                        && !ResourceInfo.ELEVATION.equals(e.getKey())
                                        && !ResourceInfo.TIME.equals(e.getKey()))
                .map(
                        e ->
                                Pair.of(
                                        e.getKey().replaceFirst("dim_", ""),
                                        (DimensionInfo) e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    void handleWMTSLayerDimensions(LayerInfo layerInfo) {
        try {
            // do we have time?
            WMTSLayerInfo wli = (WMTSLayerInfo) layerInfo.getResource();
            WMTSLayer wl = (WMTSLayer) wli.getWMTSLayer(null);
            for (String dimName : wl.getDimensions().keySet()) {
                if (TIME.equalsIgnoreCase(dimName)) {
                    Dimension timeDimension = wl.getDimension(dimName);

                    if (mode == Mode.WMS11) {
                        declareWMS11Dimensions(true, false, null, null, null);
                    }

                    Extent extent = timeDimension.getExtent();
                    writeTimeDimension(
                            extent.getValue(), extent.getDefaultValue(), extent.getNearestValue());
                } else {
                    // TODO: custom dimension handling
                    LOGGER.log(
                            Level.WARNING,
                            "Skipping custom dimension "
                                    + dimName
                                    + " in layer "
                                    + layerInfo.getName());
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error handling WMTS time dimension", ex);
        }
    }

    /** Writes down the raster layer dimensions, if any */
    void handleRasterLayerDimensions(final LayerInfo layer) throws RuntimeException, IOException {

        // do we have time and elevation?
        CoverageInfo cvInfo = (CoverageInfo) layer.getResource();
        if (cvInfo == null)
            throw new ServiceException(
                    "Unable to acquire coverage resource for layer: " + layer.getName());

        DimensionInfo timeInfo = null;
        DimensionInfo elevInfo = null;
        Map<String, DimensionInfo> customDimensions = new HashMap<String, DimensionInfo>();
        GridCoverage2DReader reader = null;

        for (Map.Entry<String, Serializable> e : cvInfo.getMetadata().entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (key.equals(ResourceInfo.TIME)) {
                timeInfo = Converters.convert(value, DimensionInfo.class);
            } else if (key.equals(ResourceInfo.ELEVATION)) {
                elevInfo = Converters.convert(value, DimensionInfo.class);
            } else if (value instanceof DimensionInfo) {
                DimensionInfo dimInfo = (DimensionInfo) value;
                if (dimInfo.isEnabled()) {
                    if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                        String dimensionName =
                                key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                        customDimensions.put(dimensionName, dimInfo);
                    } else {
                        LOGGER.log(
                                Level.SEVERE,
                                "Skipping custom  dimension with key "
                                        + key
                                        + " since it does not start with "
                                        + ResourceInfo.CUSTOM_DIMENSION_PREFIX);
                    }
                }
            }
        }
        boolean hasTime = timeInfo != null && timeInfo.isEnabled();
        boolean hasElevation = elevInfo != null && elevInfo.isEnabled();
        boolean hasCustomDimensions = !customDimensions.isEmpty();

        // skip if nothing is configured
        if (!hasTime && !hasElevation && !hasCustomDimensions) {
            return;
        }

        Catalog catalog = cvInfo.getCatalog();
        if (catalog == null)
            throw new ServiceException(
                    "Unable to acquire catalog resource for layer: " + layer.getName());

        CoverageStoreInfo csinfo = cvInfo.getStore();
        if (csinfo == null)
            throw new ServiceException(
                    "Unable to acquire coverage store resource for layer: " + layer.getName());

        try {
            reader = (GridCoverage2DReader) cvInfo.getGridCoverageReader(null, null);
        } catch (Throwable t) {
            LOGGER.log(
                    Level.SEVERE,
                    "Unable to acquire a reader for this coverage with format: "
                            + csinfo.getFormat().getName(),
                    t);
        }
        if (reader == null) {
            throw new ServiceException(
                    "Unable to acquire a reader for this coverage with format: "
                            + csinfo.getFormat().getName());
        }
        ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);

        // Process only custom dimensions supported by the reader
        if (hasCustomDimensions) {
            // loop on a new keyset to avoid ConcurrentModificationException
            for (String key : new HashSet<>(customDimensions.keySet())) {
                if (!dimensions.hasDomain(key)) customDimensions.remove(key);
            }
        }

        if (mode == Mode.WMS11) {
            String elevUnits = hasElevation ? elevInfo.getUnits() : "";
            String elevUnitSymbol = hasElevation ? elevInfo.getUnitSymbol() : "";
            declareWMS11Dimensions(
                    hasTime, hasElevation, elevUnits, elevUnitSymbol, customDimensions);
        }

        // timeDimension
        if (hasTime && dimensions.hasTime()) {
            handleTimeDimensionRaster(cvInfo, timeInfo, dimensions);
        }

        // elevationDomain
        if (hasElevation && dimensions.hasElevation()) {
            handleElevationDimensionRaster(cvInfo, elevInfo, dimensions);
        }

        // custom dimensions
        if (hasCustomDimensions) {
            for (String key : customDimensions.keySet()) {
                DimensionInfo dimensionInfo = customDimensions.get(key);
                handleCustomDimensionRaster(cvInfo, key, dimensionInfo, dimensions);
            }
        }
    }

    private void handleElevationDimensionRaster(
            CoverageInfo cvInfo, DimensionInfo elevInfo, ReaderDimensionsAccessor dimensions)
            throws IOException {
        TreeSet<Object> elevations = null;
        try {
            if (elevInfo.getPresentation() != DimensionPresentation.LIST) {
                Double minValue = dimensions.getMinElevation();
                if (minValue != null) {
                    elevations = new TreeSet<>();
                    elevations.add(minValue);
                    elevations.add(dimensions.getMaxElevation());
                }
            }
            if (elevations == null) {
                throw new Exception(
                        "The \"List\" presentation of the elevation dimension has been selected");
            }
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Dimension has not been extracted. The reason: ", ex);
            }
            elevations = dimensions.getElevationDomain();
        }
        String elevationMetadata = getNumberRepresentation(elevInfo, elevations);
        String defaultValue = getDefaultValueRepresentation(cvInfo, ResourceInfo.ELEVATION, "0");
        writeElevationDimension(
                elevations,
                elevationMetadata,
                elevInfo.getUnits(),
                elevInfo.getUnitSymbol(),
                defaultValue);
    }

    private String getDefaultValueRepresentation(
            ResourceInfo resource, String dimensionName, String fallback) {
        DimensionInfo dimensionInfo = wms.getDimensionInfo(resource, dimensionName);
        DimensionDefaultValueSelectionStrategy strategy =
                wms.getDefaultValueStrategy(resource, dimensionName, dimensionInfo);
        String defaultValue =
                strategy.getCapabilitiesRepresentation(resource, dimensionName, dimensionInfo);
        if (defaultValue == null) {
            defaultValue = fallback;
        }
        return defaultValue;
    }

    private void handleTimeDimensionRaster(
            CoverageInfo cvInfo, DimensionInfo timeInfo, ReaderDimensionsAccessor dimension)
            throws IOException {
        TreeSet<Object> temporalDomain = null;
        try {
            if (timeInfo.getPresentation() != DimensionPresentation.LIST) {
                Date minValue = dimension.getMinTime();
                if (minValue != null) {
                    temporalDomain = new TreeSet<>();
                    temporalDomain.add(minValue);
                    temporalDomain.add(dimension.getMaxTime());
                }
            }
            if (temporalDomain == null) {
                throw new Exception(
                        "The \"List\" presentation of the temporal dimension has been selected");
            }
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Dimension has not been extracted. The reason: ", ex);
            }
            temporalDomain = dimension.getTimeDomain();
        }
        String timeMetadata = getTemporalDomainRepresentation(timeInfo, temporalDomain);
        String defaultValue =
                getDefaultValueRepresentation(
                        cvInfo, ResourceInfo.TIME, DimensionDefaultValueSetting.TIME_CURRENT);
        writeTimeDimension(timeMetadata, defaultValue, timeInfo.isNearestMatchEnabled());
    }

    private void handleCustomDimensionRaster(
            CoverageInfo cvInfo,
            String dimName,
            DimensionInfo dimension,
            ReaderDimensionsAccessor dimAccessor)
            throws IOException {
        final List<String> values = dimAccessor.getDomain(dimName);
        String metadata = getCustomDomainRepresentation(dimension, values);
        String defaultValue = wms.getDefaultCustomDimensionValue(dimName, cvInfo, String.class);
        writeCustomDimensionRaster(
                dimName, metadata, defaultValue, dimension.getUnits(), dimension.getUnitSymbol());
    }

    /**
     * Writes WMS 1.1.1 conforming dimensions (WMS 1.3 squashed dimensions and extent in the same
     * tag instead)
     *
     * @param hasTime - <tt>true</tt> if the layer has the time dimension, <tt>false</tt> otherwise
     * @param hasElevation - <tt>true</tt> if the layer has the elevation dimension, <tt>false</tt>
     *     otherwise
     * @param elevUnits - <tt>units</tt> attribute of the elevation dimension
     * @param elevUnitSymbol - <tt>unitSymbol</tt> attribute of the elevation dimension
     */
    private void declareWMS11Dimensions(
            boolean hasTime,
            boolean hasElevation,
            String elevUnits,
            String elevUnitSymbol,
            Map<String, DimensionInfo> customDimensions) {
        // we have to declare time and elevation before the extents
        if (hasTime) {
            AttributesImpl timeDim = new AttributesImpl();
            timeDim.addAttribute("", NAME, NAME, "", TIME);
            timeDim.addAttribute("", UNITS, UNITS, "", "ISO8601");
            element("Dimension", null, timeDim);
        }
        if (hasElevation) {
            // same as WMS 1.3 except no values
            writeElevationDimensionElement(null, null, elevUnits, elevUnitSymbol);
        }
        if (customDimensions != null) {
            for (String dim : customDimensions.keySet()) {
                DimensionInfo di = customDimensions.get(dim);
                AttributesImpl custDim = new AttributesImpl();
                custDim.addAttribute("", NAME, NAME, "", dim);
                String units = di.getUnits();
                String unitSymbol = di.getUnitSymbol();
                custDim.addAttribute("", UNITS, UNITS, "", units != null ? units : "");
                if (unitSymbol != null) {
                    custDim.addAttribute("", UNIT_SYMBOL, UNIT_SYMBOL, "", unitSymbol);
                }
                element("Dimension", null, custDim);
            }
        }
    }

    protected String getNumberRepresentation(
            DimensionInfo dimension, TreeSet<? extends Object> values) {
        String elevationMetadata = null;

        final StringBuilder buff = new StringBuilder();

        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            for (Object val : values) {
                if (val instanceof Number) {
                    buff.append(val);
                } else {
                    NumberRange range = (NumberRange) val;
                    buff.append(range.getMinimum())
                            .append("/")
                            .append(range.getMaximum())
                            .append("/0");
                }
                buff.append(",");
            }
            elevationMetadata =
                    buff.substring(0, buff.length() - 1)
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "")
                            .replaceAll(" ", "");
        } else if (DimensionPresentation.CONTINUOUS_INTERVAL == dimension.getPresentation()) {
            NumberRange range = getMinMaxZInterval(values);
            buff.append(range.getMinimum());
            buff.append("/");
            buff.append(range.getMaximum());
            buff.append("/0");

            elevationMetadata = buff.toString();
        } else if (DimensionPresentation.DISCRETE_INTERVAL == dimension.getPresentation()) {
            final NumberRange range = getMinMaxZInterval(values);
            final Class<?> typeBinding = values.first().getClass();
            final boolean isDecimal = isDecimal(typeBinding);
            String minStr, maxStr;
            if (isDecimal) {
                minStr = String.valueOf(range.getMinimum());
                maxStr = String.valueOf(range.getMaximum());
            } else {
                minStr = String.valueOf(Double.valueOf(range.getMinimum()).longValue());
                maxStr = String.valueOf(Double.valueOf(range.getMaximum()).longValue());
            }
            buff.append(minStr);
            buff.append("/");
            buff.append(maxStr);
            buff.append("/");

            BigDecimal resolution = dimension.getResolution();
            if (resolution != null) {
                buff.append(resolution.doubleValue());
            } else {
                if (values.size() >= 2 && allNumbers(values)) {
                    int count = 2, i = 2;
                    Number[] zPositions = new Number[count];
                    // convert all to double
                    final List<Number> numberValues =
                            values.stream().map(x -> (Number) x).collect(Collectors.toList());
                    for (Object val : numberValues) {
                        zPositions[count - i--] = (Number) val;
                        if (i == 0) break;
                    }
                    if (isDecimal)
                        buff.append(
                                zPositions[count - 1].doubleValue()
                                        - zPositions[count - 2].doubleValue());
                    else
                        buff.append(
                                zPositions[count - 1].longValue()
                                        - zPositions[count - 2].longValue());
                } else {
                    buff.append(0);
                }
            }

            elevationMetadata = buff.toString();
        }

        return elevationMetadata;
    }

    private boolean isDecimal(Class<?> typeBinding) {
        return Float.class.isAssignableFrom(typeBinding)
                || Double.class.isAssignableFrom(typeBinding)
                || BigDecimal.class.isAssignableFrom(typeBinding);
    }

    /** Builds the proper presentation given the current */
    String getTemporalDomainRepresentation(
            DimensionInfo dimension, TreeSet<? extends Object> values) {
        String timeMetadata = null;

        final StringBuilder buff = new StringBuilder();
        final ISO8601Formatter df = new ISO8601Formatter();

        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            for (Object date : values) {
                buff.append(df.format(date));
                buff.append(",");
            }
            timeMetadata =
                    buff.substring(0, buff.length() - 1)
                            .replaceAll("\\[", "")
                            .replaceAll("\\]", "")
                            .replaceAll(" ", "");
        } else if (DimensionPresentation.CONTINUOUS_INTERVAL == dimension.getPresentation()) {
            DateRange interval = getMinMaxTimeInterval(values);
            buff.append(df.format(interval.getMinValue()));
            buff.append("/");
            buff.append(df.format(interval.getMaxValue()));
            buff.append("/PT1S");
            timeMetadata = buff.toString();
        } else if (DimensionPresentation.DISCRETE_INTERVAL == dimension.getPresentation()) {
            DateRange interval = getMinMaxTimeInterval(values);
            buff.append(df.format(interval.getMinValue()));
            buff.append("/");
            buff.append(df.format(interval.getMaxValue()));
            buff.append("/");

            final BigDecimal resolution = dimension.getResolution();
            if (resolution != null) {
                // resolution has been provided
                buff.append(new DefaultPeriodDuration(resolution.longValue()).toString());
            } else {
                if (values.size() >= 2 && allDates(values)) {
                    int count = 2, i = 2;
                    Date[] timePositions = new Date[count];
                    for (Object date : values) {
                        timePositions[count - i--] = (Date) date;
                        if (i == 0) break;
                    }
                    long durationInMilliSeconds =
                            timePositions[count - 1].getTime() - timePositions[count - 2].getTime();
                    buff.append(new DefaultPeriodDuration(durationInMilliSeconds).toString());
                } else {
                    // assume 1 second and be done with it...
                    buff.append("PT1S");
                }
            }

            timeMetadata = buff.toString();
        }

        return timeMetadata;
    }

    /** Builds a single time range from the domain, be it made of Date or TimeRange objects */
    private DateRange getMinMaxTimeInterval(TreeSet<? extends Object> values) {
        Object minValue = values.first();
        Object maxValue = values.last();
        Date min, max;
        if (minValue instanceof DateRange) {
            min = ((DateRange) minValue).getMinValue();
        } else {
            min = (Date) minValue;
        }
        if (maxValue instanceof DateRange) {
            max = ((DateRange) maxValue).getMaxValue();
        } else {
            max = (Date) maxValue;
        }
        return new DateRange(min, max);
    }

    /** Builds a single Z range from the domain, be it made of Number or NumberRange objects */
    private NumberRange<Double> getMinMaxZInterval(TreeSet<? extends Object> values) {
        Object minValue = values.first();
        Object maxValue = values.last();
        Number min, max;
        if (minValue instanceof NumberRange) {
            min = (Number) ((NumberRange) minValue).getMinValue();
        } else {
            min = (Number) minValue;
        }
        if (maxValue instanceof NumberRange) {
            max = (Number) ((NumberRange) maxValue).getMaxValue();
        } else {
            max = (Number) maxValue;
        }
        return new NumberRange(min.getClass(), min, max);
    }

    /** Returns true if all the values in the set are Date instances */
    private boolean allDates(TreeSet<? extends Object> values) {
        for (Object value : values) {
            if (!(value instanceof Date)) {
                return false;
            }
        }

        return true;
    }

    /** Returns true if all the values in the set are Number instances */
    private boolean allNumbers(TreeSet<? extends Object> values) {
        for (Object value : values) {
            if (!(value instanceof Number)) {
                return false;
            }
        }

        return true;
    }

    /** Builds the proper presentation given the specified value domain */
    String getCustomDomainRepresentation(DimensionInfo dimension, List<String> values) {
        String metadata = null;

        final StringBuilder buff = new StringBuilder();

        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            for (String value : values) {
                buff.append(value.trim());
                buff.append(",");
            }
            metadata = buff.substring(0, buff.length() - 1);

        } else if (DimensionPresentation.DISCRETE_INTERVAL == dimension.getPresentation()) {
            buff.append(values.get(0));
            buff.append("/");

            buff.append(values.get(0));
            buff.append("/");

            final BigDecimal resolution = dimension.getResolution();
            if (resolution != null) {
                buff.append(resolution);
            }

            metadata = buff.toString();
        }

        return metadata;
    }

    /** Writes out metadata for the time dimension */
    private void handleTimeDimensionVector(FeatureTypeInfo typeInfo) throws IOException {
        // build the time dim representation
        TreeSet<Date> values = wms.getFeatureTypeTimes(typeInfo);
        String timeMetadata;
        boolean nearest = false;
        if (values != null && !values.isEmpty()) {
            DimensionInfo timeInfo =
                    typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            timeMetadata = getTemporalDomainRepresentation(timeInfo, values);
            nearest = timeInfo.isNearestMatchEnabled();
        } else {
            timeMetadata = "";
        }
        String defaultValue =
                getDefaultValueRepresentation(
                        typeInfo, ResourceInfo.TIME, DimensionDefaultValueSetting.TIME_CURRENT);
        writeTimeDimension(timeMetadata, defaultValue, nearest);
    }

    private void handleElevationDimensionVector(FeatureTypeInfo typeInfo) throws IOException {
        TreeSet<Double> elevations = wms.getFeatureTypeElevations(typeInfo);
        String elevationMetadata;
        DimensionInfo di = typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        String units = di.getUnits();
        String unitSymbol = di.getUnitSymbol();
        if (elevations != null && !elevations.isEmpty()) {
            elevationMetadata = getNumberRepresentation(di, elevations);
        } else {
            elevationMetadata = "";
        }
        String defaultValue = getDefaultValueRepresentation(typeInfo, ResourceInfo.ELEVATION, "0");
        writeElevationDimension(elevations, elevationMetadata, units, unitSymbol, defaultValue);
    }

    private void writeTimeDimension(
            String timeMetadata, String defaultTimeStr, boolean nearestMatch) {
        AttributesImpl timeDim = new AttributesImpl();
        if (defaultTimeStr == null) {
            defaultTimeStr = DimensionDefaultValueSetting.TIME_CURRENT;
        }
        if (mode == Mode.WMS11) {
            timeDim.addAttribute("", NAME, NAME, "", TIME);
            timeDim.addAttribute("", DEFAULT, DEFAULT, "", defaultTimeStr);
            if (nearestMatch) {
                timeDim.addAttribute("", NEAREST_VALUE, NEAREST_VALUE, "", "1");
            }
            element("Extent", timeMetadata, timeDim);
        } else {
            timeDim.addAttribute("", NAME, NAME, "", TIME);
            timeDim.addAttribute("", DEFAULT, DEFAULT, "", defaultTimeStr);
            timeDim.addAttribute("", UNITS, UNITS, "", DimensionInfo.TIME_UNITS);
            if (nearestMatch) {
                timeDim.addAttribute("", NEAREST_VALUE, NEAREST_VALUE, "", "1");
            }
            element("Dimension", timeMetadata, timeDim);
        }
    }

    private void writeElevationDimension(
            TreeSet<? extends Object> elevations,
            final String elevationMetadata,
            final String units,
            final String unitSymbol,
            String defaultValue) {
        if (mode == Mode.WMS11) {
            AttributesImpl elevDim = new AttributesImpl();
            elevDim.addAttribute("", NAME, NAME, "", ELEVATION);
            elevDim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
            element("Extent", elevationMetadata, elevDim);
        } else {
            writeElevationDimensionElement(elevationMetadata, defaultValue, units, unitSymbol);
        }
    }

    private void writeElevationDimensionElement(
            final String elevationMetadata,
            final String defaultValue,
            final String units,
            final String unitSymbol) {
        AttributesImpl elevDim = new AttributesImpl();
        String unitsNotNull = units;
        String unitSymNotNull = (unitSymbol == null) ? "" : unitSymbol;
        if (units == null) {
            unitsNotNull = DimensionInfo.ELEVATION_UNITS;
            unitSymNotNull = DimensionInfo.ELEVATION_UNIT_SYMBOL;
        }
        elevDim.addAttribute("", NAME, NAME, "", ELEVATION);
        if (defaultValue != null) {
            elevDim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
        }
        elevDim.addAttribute("", UNITS, UNITS, "", unitsNotNull);
        if (!"".equals(unitsNotNull) && !"".equals(unitSymNotNull)) {
            elevDim.addAttribute("", UNIT_SYMBOL, UNIT_SYMBOL, "", unitSymNotNull);
        }
        element("Dimension", elevationMetadata, elevDim);
    }

    private void writeCustomDimensionRaster(
            String name, String metadata, String defaultValue, String unit, String unitSymbol) {
        AttributesImpl dim = new AttributesImpl();
        dim.addAttribute("", NAME, NAME, "", name);
        if (mode == Mode.WMS11) {
            if (defaultValue != null) {
                dim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
            }
            element("Extent", metadata, dim);
        } else {
            if (defaultValue != null) {
                dim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
            }
            dim.addAttribute("", UNITS, UNITS, "", unit != null ? unit : "");
            if (unitSymbol != null && !"".equals(unitSymbol)) {
                dim.addAttribute("", UNIT_SYMBOL, UNIT_SYMBOL, "", unitSymbol);
            }

            element("Dimension", metadata, dim);
        }
    }

    private void writeCustomDimensionVector(
            final String name,
            final TreeSet<? extends Object> elevations,
            final String metadata,
            final String units,
            final String unitSymbol,
            final String defaultValue) {
        if (mode == Mode.WMS11) {
            AttributesImpl elevDim = new AttributesImpl();
            elevDim.addAttribute("", NAME, NAME, "", name);
            elevDim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
            element("Extent", metadata, elevDim);
        } else {
            writeCustomDimensionElement(name, metadata, defaultValue, units, unitSymbol);
        }
    }

    private void writeCustomDimensionElement(
            final String name,
            final String metadata,
            final String defaultValue,
            final String units,
            final String unitSymbol) {
        final AttributesImpl elevDim = new AttributesImpl();
        String unitsNotNull = units;
        String unitSymNotNull = (unitSymbol == null) ? "" : unitSymbol;
        if (units == null) {
            unitsNotNull = DimensionInfo.ELEVATION_UNITS;
            unitSymNotNull = DimensionInfo.ELEVATION_UNIT_SYMBOL;
        }
        elevDim.addAttribute("", NAME, NAME, "", name);
        if (defaultValue != null) {
            elevDim.addAttribute("", DEFAULT, DEFAULT, "", defaultValue);
        }
        elevDim.addAttribute("", UNITS, UNITS, "", unitsNotNull);
        if (!"".equals(unitsNotNull) && !"".equals(unitSymNotNull)) {
            elevDim.addAttribute("", UNIT_SYMBOL, UNIT_SYMBOL, "", unitSymNotNull);
        }
        element("Dimension", metadata, elevDim);
    }
}
