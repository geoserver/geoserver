/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.kvp.ElevationParser;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategy;
import org.geoserver.wms.dimension.DimensionDefaultValueSelectionStrategyFactory;
import org.geoserver.wms.dimension.FixedValueStrategyFactory;
import org.geoserver.wms.dimension.NearestValueStrategyFactory;
import org.geotools.feature.type.DateUtil;

/**
 * The default implementation of the {@link DimensionDefaultValueSelectionStrategyFactory}. Uses
 * strategies and strategy factories injected by the WMS application context. Thus to change the
 * default value selection strategy implementations one typically only needs to inject another
 * strategy of strategy factory (for NEAREST and FIXED strategies).
 *
 * <p>Supports default value selection for TIME, ELEVATION and custom dimensions for both coverage
 * and feature resources.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class DimensionDefaultValueSelectionStrategyFactoryImpl
        implements DimensionDefaultValueSelectionStrategyFactory {

    private static final String VECTOR_CUSTOM_DIMENSION_PREFIX = "dim_";

    // Initialized in the applicationContext:
    private DimensionDefaultValueSelectionStrategy featureTimeMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy featureTimeMaximumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageTimeMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageTimeMaximumStrategy;

    private DimensionDefaultValueSelectionStrategy featureElevationMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy featureElevationMaximumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageElevationMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageElevationMaximumStrategy;

    private DimensionDefaultValueSelectionStrategy featureCustomDimensionMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy featureCustomDimensionMaximumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageCustomDimensionMinimumStrategy;

    private DimensionDefaultValueSelectionStrategy coverageCustomDimensionMaximumStrategy;

    private NearestValueStrategyFactory featureNearestValueStrategyFactory;

    private NearestValueStrategyFactory coverageNearestValueStrategyFactory;

    private FixedValueStrategyFactory fixedValueStrategyFactory;

    private TimeParser timeParser = new TimeParser();

    private ElevationParser elevationParser = new ElevationParser();

    /**
     * Return the default value strategy for the given dimension. If the default value strategy
     * setting is not set for this dimension, returns the default strategy for this resource type.
     */
    @Override
    public DimensionDefaultValueSelectionStrategy getStrategy(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        DimensionDefaultValueSelectionStrategy retval =
                getStrategyFromSetting(resource, dimensionName, dimensionInfo);
        if (retval != null) {
            return retval;
        }
        // Else just select the default strategy based on the dimension name
        if (dimensionName.equals(ResourceInfo.TIME)) {
            if (resource instanceof FeatureTypeInfo) {
                retval =
                        featureNearestValueStrategyFactory.createNearestValueStrategy(
                                new Date(), DimensionDefaultValueSetting.TIME_CURRENT);
            } else if (resource instanceof CoverageInfo) {
                retval =
                        coverageNearestValueStrategyFactory.createNearestValueStrategy(
                                new Date(), DimensionDefaultValueSetting.TIME_CURRENT);
            }
        } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
            if (resource instanceof FeatureTypeInfo) {
                retval = featureElevationMinimumStrategy;
            } else if (resource instanceof CoverageInfo) {
                retval = coverageElevationMinimumStrategy;
            }
        } else if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)
                || dimensionName.startsWith(VECTOR_CUSTOM_DIMENSION_PREFIX)) {
            if (resource instanceof FeatureTypeInfo) {
                retval = featureCustomDimensionMinimumStrategy;
            } else if (resource instanceof CoverageInfo) {
                retval = coverageCustomDimensionMinimumStrategy;
            }
        }
        return retval;
    }

    /** @return the featureTimeMinimumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureTimeMinimumStrategy() {
        return featureTimeMinimumStrategy;
    }

    /** @param featureTimeMinimumStrategy the featureTimeMinimumStrategy to set */
    public void setFeatureTimeMinimumStrategy(
            DimensionDefaultValueSelectionStrategy featureTimeMinimumStrategy) {
        this.featureTimeMinimumStrategy = featureTimeMinimumStrategy;
    }

    /** @return the featureTimeMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureTimeMaximumStrategy() {
        return featureTimeMaximumStrategy;
    }

    /** @param featureTimeMaximumStrategy the featureTimeMaximumStrategy to set */
    public void setFeatureTimeMaximumStrategy(
            DimensionDefaultValueSelectionStrategy featureTimeMaximumStrategy) {
        this.featureTimeMaximumStrategy = featureTimeMaximumStrategy;
    }

    /** @return the coverageTimeMinimumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageTimeMinimumStrategy() {
        return coverageTimeMinimumStrategy;
    }

    /** @param coverageTimeMinimumStrategy the coverageTimeMinimumStrategy to set */
    public void setCoverageTimeMinimumStrategy(
            DimensionDefaultValueSelectionStrategy coverageTimeMinimumStrategy) {
        this.coverageTimeMinimumStrategy = coverageTimeMinimumStrategy;
    }

    /** @return the coverageTimeMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageTimeMaximumStrategy() {
        return coverageTimeMaximumStrategy;
    }

    /** @param coverageTimeMaximumStrategy the coverageTimeMaximumStrategy to set */
    public void setCoverageTimeMaximumStrategy(
            DimensionDefaultValueSelectionStrategy coverageTimeMaximumStrategy) {
        this.coverageTimeMaximumStrategy = coverageTimeMaximumStrategy;
    }

    /** @return the featureElevationMiminumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureElevationMinimumStrategy() {
        return featureElevationMinimumStrategy;
    }

    /** @param featureElevationMinimumStrategy the featureElevationMiminumStrategy to set */
    public void setFeatureElevationMinimumStrategy(
            DimensionDefaultValueSelectionStrategy featureElevationMinimumStrategy) {
        this.featureElevationMinimumStrategy = featureElevationMinimumStrategy;
    }

    /** @return the featureElevationMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureElevationMaximumStrategy() {
        return featureElevationMaximumStrategy;
    }

    /** @param featureElevationMaximumStrategy the featureElevationMaxinumStrategy to set */
    public void setFeatureElevationMaximumStrategy(
            DimensionDefaultValueSelectionStrategy featureElevationMaximumStrategy) {
        this.featureElevationMaximumStrategy = featureElevationMaximumStrategy;
    }

    /** @return the coverageElevationMinimumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageElevationMinimumStrategy() {
        return coverageElevationMinimumStrategy;
    }

    /** @param coverageElevationMinimumStrategy the coverageElevationMinimumStrategy to set */
    public void setCoverageElevationMinimumStrategy(
            DimensionDefaultValueSelectionStrategy coverageElevationMinimumStrategy) {
        this.coverageElevationMinimumStrategy = coverageElevationMinimumStrategy;
    }

    /** @return the coverageElevationMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageElevationMaximumStrategy() {
        return coverageElevationMaximumStrategy;
    }

    /** @param coverageElevationMaximumStrategy the coverageElevationMaximumStrategy to set */
    public void setCoverageElevationMaximumStrategy(
            DimensionDefaultValueSelectionStrategy coverageElevationMaximumStrategy) {
        this.coverageElevationMaximumStrategy = coverageElevationMaximumStrategy;
    }

    /** @return the featureCustomDimensionMinimumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureCustomDimensionMinimumStrategy() {
        return featureCustomDimensionMinimumStrategy;
    }

    /**
     * @param featureCustomDimensionMinimumStrategy the featureCustomDimensionMinimumStrategy to set
     */
    public void setFeatureCustomDimensionMinimumStrategy(
            DimensionDefaultValueSelectionStrategy featureCustomDimensionMinimumStrategy) {
        this.featureCustomDimensionMinimumStrategy = featureCustomDimensionMinimumStrategy;
    }

    /** @return the featureCustomDimensionMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getFeatureCustomDimensionMaximumStrategy() {
        return featureCustomDimensionMaximumStrategy;
    }

    /**
     * @param featureCustomDimensionMaximumStrategy the featureCustomDimensionMaximumStrategy to set
     */
    public void setFeatureCustomDimensionMaximumStrategy(
            DimensionDefaultValueSelectionStrategy featureCustomDimensionMaximumStrategy) {
        this.featureCustomDimensionMaximumStrategy = featureCustomDimensionMaximumStrategy;
    }

    /** @return the coverageCustomDimensionMinimumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageCustomDimensionMinimumStrategy() {
        return coverageCustomDimensionMinimumStrategy;
    }

    /**
     * @param coverageCustomDimensionMinimumStrategy the coverageCustomDimensionMinimumStrategy to
     *     set
     */
    public void setCoverageCustomDimensionMinimumStrategy(
            DimensionDefaultValueSelectionStrategy coverageCustomDimensionMinimumStrategy) {
        this.coverageCustomDimensionMinimumStrategy = coverageCustomDimensionMinimumStrategy;
    }

    /** @return the coverageCustomDimensionMaximumStrategy */
    public DimensionDefaultValueSelectionStrategy getCoverageCustomDimensionMaximumStrategy() {
        return coverageCustomDimensionMaximumStrategy;
    }

    /**
     * @param coverageCustomDimensionMaximumStrategy the coverageCustomDimensionMaximumStrategy to
     *     set
     */
    public void setCoverageCustomDimensionMaximumStrategy(
            DimensionDefaultValueSelectionStrategy coverageCustomDimensionMaximumStrategy) {
        this.coverageCustomDimensionMaximumStrategy = coverageCustomDimensionMaximumStrategy;
    }

    /** @return the featureNearestValueStrategyFactory */
    public NearestValueStrategyFactory getFeatureNearestValueStrategyFactory() {
        return featureNearestValueStrategyFactory;
    }

    /** @param featureNearestValueStrategyFactory the featureNearestValueStrategyFactory to set */
    public void setFeatureNearestValueStrategyFactory(
            NearestValueStrategyFactory featureNearestValueStrategyFactory) {
        this.featureNearestValueStrategyFactory = featureNearestValueStrategyFactory;
    }

    /** @return the coverageNearestValueStrategyFactory */
    public NearestValueStrategyFactory getCoverageNearestValueStrategyFactory() {
        return coverageNearestValueStrategyFactory;
    }

    /** @param coverageNearestValueStrategyFactory the coverageNearestValueStrategyFactory to set */
    public void setCoverageNearestValueStrategyFactory(
            NearestValueStrategyFactory coverageNearestValueStrategyFactory) {
        this.coverageNearestValueStrategyFactory = coverageNearestValueStrategyFactory;
    }

    /** @return the fixedValueStrategyFactory */
    public FixedValueStrategyFactory getFixedValueStrategyFactory() {
        return fixedValueStrategyFactory;
    }

    /** @param fixedValueStrategyFactory the fixedValueStrategyFactory to set */
    public void setFixedValueStrategyFactory(FixedValueStrategyFactory fixedValueStrategyFactory) {
        this.fixedValueStrategyFactory = fixedValueStrategyFactory;
    }

    private DimensionDefaultValueSelectionStrategy getStrategyFromSetting(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        DimensionDefaultValueSelectionStrategy retval = null;
        DimensionDefaultValueSetting setting = dimensionInfo.getDefaultValue();
        if (setting != null && setting.getStrategyType() != null) {
            if (dimensionName.equals(ResourceInfo.TIME)) {
                retval = getDefaultTimeStrategy(resource, setting);
            } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
                retval = getDefaultElevationStrategy(resource, setting);
            } else if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)
                    || dimensionName.startsWith(ResourceInfo.VECTOR_CUSTOM_DIMENSION_PREFIX)) {
                retval = getDefaultCustomDimensionStrategy(resource, setting);
            }
        }
        return retval;
    }

    private DimensionDefaultValueSelectionStrategy getDefaultTimeStrategy(
            ResourceInfo resource, DimensionDefaultValueSetting setting) {
        DimensionDefaultValueSelectionStrategy retval = null;
        Strategy getStrategyType = setting.getStrategyType();
        switch (getStrategyType) {
            case NEAREST:
                {
                    Date refDate;
                    String capabilitiesValue = null;
                    String referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        if (referenceValue.equalsIgnoreCase(
                                DimensionDefaultValueSetting.TIME_CURRENT)) {
                            refDate = new Date();
                            capabilitiesValue = DimensionDefaultValueSetting.TIME_CURRENT;
                        } else {
                            try {
                                refDate = new Date(DateUtil.parseDateTime(referenceValue));
                            } catch (IllegalArgumentException e) {
                                throw new ServiceException(
                                        "Unable to parse time dimension default value reference '"
                                                + referenceValue
                                                + "' as date, an ISO 8601 datetime format is expected",
                                        e);
                            }
                        }
                        if (resource instanceof FeatureTypeInfo) {
                            retval =
                                    featureNearestValueStrategyFactory.createNearestValueStrategy(
                                            refDate, capabilitiesValue);
                        } else if (resource instanceof CoverageInfo) {
                            retval =
                                    coverageNearestValueStrategyFactory.createNearestValueStrategy(
                                            refDate, capabilitiesValue);
                        }
                    } else {
                        throw new ServiceException(
                                "No reference value given for time dimension default value 'nearest' strategy");
                    }
                    break;
                }
            case MINIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureTimeMinimumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageTimeMinimumStrategy;
                    }
                    break;
                }
            case MAXIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureTimeMaximumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageTimeMaximumStrategy;
                    }
                    break;
                }
            case FIXED:
                {
                    Object refDate;
                    String referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        try {
                            refDate = singleValue(timeParser.parse(referenceValue), new Date());
                        } catch (ParseException e) {
                            throw new ServiceException(
                                    "Unable to parse time dimension default value reference '"
                                            + referenceValue
                                            + "' as date or a date range, an ISO 8601 datetime format is expected",
                                    e);
                        }
                        retval =
                                fixedValueStrategyFactory.createFixedValueStrategy(
                                        refDate, referenceValue);
                    } else {
                        throw new ServiceException(
                                "No reference value given for time dimension default value 'fixed' strategy");
                    }
                    break;
                }
        }
        return retval;
    }

    private DimensionDefaultValueSelectionStrategy getDefaultElevationStrategy(
            ResourceInfo resource, DimensionDefaultValueSetting setting) {
        DimensionDefaultValueSelectionStrategy retval = null;
        switch (setting.getStrategyType()) {
            case NEAREST:
                {
                    Number refNumber;
                    String referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        try {
                            refNumber = Long.parseLong(referenceValue);
                        } catch (NumberFormatException fne) {
                            try {
                                refNumber = Double.parseDouble(referenceValue);
                            } catch (NumberFormatException e) {
                                throw new ServiceException(
                                        "Unable to parse elevation dimension default value reference '"
                                                + referenceValue
                                                + "' as long or double",
                                        e);
                            }
                        }
                        if (resource instanceof FeatureTypeInfo) {
                            retval =
                                    featureNearestValueStrategyFactory.createNearestValueStrategy(
                                            refNumber);
                        } else if (resource instanceof CoverageInfo) {
                            retval =
                                    coverageNearestValueStrategyFactory.createNearestValueStrategy(
                                            refNumber);
                        }
                    } else {
                        throw new ServiceException(
                                "No reference value given for elevation dimension default value 'nearest' strategy");
                    }
                    break;
                }
            case MINIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureElevationMinimumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageElevationMinimumStrategy;
                    }
                    break;
                }
            case MAXIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureElevationMaximumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageElevationMaximumStrategy;
                    }
                    break;
                }
            case FIXED:
                {
                    Object refNumber;
                    String referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        try {
                            refNumber =
                                    singleValue(elevationParser.parse(referenceValue), new Date());
                        } catch (ParseException e) {
                            throw new ServiceException(
                                    "Unable to parse elevation dimension default value reference '"
                                            + referenceValue
                                            + "' as long or double",
                                    e);
                        }
                    } else {
                        throw new ServiceException(
                                "No reference value given for elevation dimension default value 'fixed' strategy");
                    }
                    retval =
                            fixedValueStrategyFactory.createFixedValueStrategy(
                                    refNumber, referenceValue);
                    break;
                }
        }
        return retval;
    }

    private Object singleValue(Collection parsed, Object defaultValue) {
        Object result = null;
        if (parsed.size() == 1) {
            result = parsed.iterator().next();
        } else if (parsed.size() > 1) {
            throw new IllegalArgumentException(
                    "Dimension reference value must be a single value or range");
        }
        if (result == null) {
            return defaultValue;
        } else {
            return result;
        }
    }

    private DimensionDefaultValueSelectionStrategy getDefaultCustomDimensionStrategy(
            ResourceInfo resource, DimensionDefaultValueSetting setting) {
        DimensionDefaultValueSelectionStrategy retval = null;
        String referenceValue = null;
        switch (setting.getStrategyType()) {
            case NEAREST:
                {
                    Object refValue;
                    referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        try {
                            refValue = new Date(DateUtil.parseDateTime(referenceValue));
                        } catch (IllegalArgumentException e) {
                            try {
                                refValue = Long.parseLong(referenceValue);
                            } catch (NumberFormatException nfe) {
                                try {
                                    refValue = Double.parseDouble(referenceValue);
                                } catch (NumberFormatException nfe2) {
                                    refValue = referenceValue;
                                }
                            }
                        }
                        if (resource instanceof FeatureTypeInfo) {
                            retval =
                                    featureNearestValueStrategyFactory.createNearestValueStrategy(
                                            refValue);
                        } else if (resource instanceof CoverageInfo) {
                            retval =
                                    coverageNearestValueStrategyFactory.createNearestValueStrategy(
                                            refValue);
                        }
                    } else {
                        throw new ServiceException(
                                "No reference value given for custom dimension default value 'nearest' strategy");
                    }
                    break;
                }
            case MINIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureCustomDimensionMinimumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageCustomDimensionMinimumStrategy;
                    }
                    break;
                }
            case MAXIMUM:
                {
                    if (resource instanceof FeatureTypeInfo) {
                        retval = featureCustomDimensionMaximumStrategy;
                    } else if (resource instanceof CoverageInfo) {
                        retval = coverageCustomDimensionMaximumStrategy;
                    }
                    break;
                }
            case FIXED:
                {
                    Object refValue;
                    referenceValue = setting.getReferenceValue();
                    if (referenceValue != null) {
                        try {
                            refValue = new Date(DateUtil.parseDateTime(referenceValue));
                        } catch (IllegalArgumentException e) {
                            try {
                                refValue = Long.parseLong(referenceValue);
                            } catch (NumberFormatException nfe) {
                                try {
                                    refValue = Double.parseDouble(referenceValue);
                                } catch (NumberFormatException nfe2) {
                                    refValue = referenceValue;
                                }
                            }
                        }
                    } else {
                        throw new ServiceException(
                                "No reference value given for custom dimension default value 'fixed' strategy");
                    }
                    retval = fixedValueStrategyFactory.createFixedValueStrategy(refValue);
                    break;
                }
        }
        return retval;
    }
}
