/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.dimension.AbstractDefaultValueSelectionStrategy;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.feature.type.DateUtil;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;

/**
 * Default implementation for selecting the default values for dimensions of coverage (raster)
 * resources using the nearest-domain-value-to-the-reference-value strategy.
 *
 * @author Ilkka Rinne / Spatineo Inc for the Finnish Meteorological Institute
 */
public class CoverageNearestValueSelectionStrategyImpl
        extends AbstractDefaultValueSelectionStrategy {

    private static Logger LOGGER =
            Logging.getLogger(CoverageNearestValueSelectionStrategyImpl.class);

    private Object toMatch;
    private String fixedCapabilitiesValue;

    /** Default constructor. */
    public CoverageNearestValueSelectionStrategyImpl(Object toMatch) {
        this(toMatch, null);
    }

    public CoverageNearestValueSelectionStrategyImpl(Object toMatch, String capabilitiesValue) {
        this.toMatch = toMatch;
        this.fixedCapabilitiesValue = capabilitiesValue;
    }

    @Override
    public Object getDefaultValue(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension, Class clz) {
        Object retval = null;
        try {
            GridCoverage2DReader reader =
                    (GridCoverage2DReader)
                            ((CoverageInfo) resource).getGridCoverageReader(null, null);
            ReaderDimensionsAccessor dimAccessor = new ReaderDimensionsAccessor(reader);

            if (dimensionName.equals(ResourceInfo.TIME)) {
                Date dateToMatch = null;
                if (this.toMatch instanceof Date) {
                    dateToMatch = (Date) this.toMatch;
                } else if (this.toMatch instanceof Long) {
                    // Assume millis time if reference value is given as Long:
                    dateToMatch = new Date(((Long) this.toMatch).longValue());
                } else {
                    try {
                        dateToMatch = new Date(DateUtil.parseDateTime(this.toMatch.toString()));
                    } catch (IllegalArgumentException e) {
                        throw new ServiceException(
                                "Error parsing value to match against while trying to find the default time value for the layer "
                                        + resource.getName(),
                                e);
                    }
                }
                retval = findNearestTime(dimAccessor, dateToMatch);
            } else if (dimensionName.equals(ResourceInfo.ELEVATION)) {
                if (this.toMatch instanceof Number) {
                    Double doubleToMatch = ((Number) this.toMatch).doubleValue();
                    retval = findNearestElevation(dimAccessor, doubleToMatch);
                } else {
                    throw new ServiceException(
                            "The default value for elevation dimension is not a number. Cannot find a default elevation value for the layer "
                                    + resource.getName());
                }
            } else if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                retval =
                        findNearestCustomDimensionValue(
                                dimensionName.substring(
                                        ResourceInfo.CUSTOM_DIMENSION_PREFIX.length()),
                                dimAccessor,
                                this.toMatch.toString());
            }

        } catch (IOException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return Converters.convert(retval, clz);
    }

    private Date findNearestTime(ReaderDimensionsAccessor dimAccessor, Date toMatch)
            throws IOException {
        Date candidate = null;
        TreeSet<Object> timeDomain = dimAccessor.getTimeDomain();
        long shortestDistance = Long.MAX_VALUE;
        long currentDistance = 0;
        for (Object dateOrRange : timeDomain) {
            if (dateOrRange instanceof Date) {
                Date d = (Date) dateOrRange;
                if (d.before(toMatch)) {
                    currentDistance = toMatch.getTime() - d.getTime();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d;
                    }
                } else if (d.after(toMatch)) {
                    currentDistance = d.getTime() - toMatch.getTime();
                    if (currentDistance < shortestDistance) {
                        candidate = d;
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    break;
                } else if (d.equals(toMatch)) {
                    candidate = d;
                    break;
                }
            } else if (dateOrRange instanceof DateRange) {
                DateRange d = (DateRange) dateOrRange;
                if (d.getMaxValue().before(toMatch)) {
                    currentDistance = toMatch.getTime() - d.getMaxValue().getTime();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d.getMaxValue();
                    }
                } else if (d.getMinValue().after(toMatch)) {
                    currentDistance = d.getMinValue().getTime() - toMatch.getTime();
                    if (currentDistance < shortestDistance) {
                        candidate = d.getMinValue();
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    break;
                } else {
                    // we are within this range, "match" will do:
                    candidate = toMatch;
                    break;
                }
            }
        }
        return candidate;
    }

    @SuppressWarnings("unchecked")
    private Double findNearestElevation(ReaderDimensionsAccessor dimAccessor, Double toMatch)
            throws IOException {
        Double candidate = null;
        TreeSet<Object> elevDomain = dimAccessor.getElevationDomain();
        double shortestDistance = Double.MAX_VALUE;
        double currentDistance = 0d;
        for (Object doubleOrRange : elevDomain) {
            if (doubleOrRange instanceof Double) {
                Double d = (Double) doubleOrRange;
                int comp = d.compareTo(toMatch);
                if (comp < 0) {
                    currentDistance = toMatch.doubleValue() - d.doubleValue();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d;
                    }
                } else if (comp > 0) {
                    currentDistance = d.doubleValue() - toMatch.doubleValue();
                    if (currentDistance < shortestDistance) {
                        candidate = d;
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    break;
                } else {
                    candidate = d;
                    break;
                }
            } else if (doubleOrRange instanceof NumberRange<?>) {
                NumberRange<Double> d = null;
                NumberRange<?> maybeD = (NumberRange<?>) doubleOrRange;
                if (maybeD.getElementClass().equals(Double.class)) {
                    d = (NumberRange<Double>) maybeD;
                } else {
                    d = maybeD.castTo(Double.class);
                }
                if (d.getMaxValue().doubleValue() < toMatch.doubleValue()) {
                    currentDistance = toMatch.doubleValue() - d.getMaxValue().doubleValue();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d.getMaxValue();
                    }
                } else if (d.getMinValue().doubleValue() > toMatch.doubleValue()) {
                    currentDistance = d.getMinValue().doubleValue() - toMatch.doubleValue();
                    if (currentDistance < shortestDistance) {
                        candidate = d.getMinValue();
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    break;
                } else {
                    // we are within this range, "match" will do:
                    candidate = toMatch;
                    break;
                }
            }
        }
        return candidate;
    }

    private String findNearestCustomDimensionValue(
            String dimensionName, ReaderDimensionsAccessor dimAccessor, String toMatch)
            throws IOException {
        String candidate = null;
        List<String> domain = dimAccessor.getDomain(dimensionName);

        // TODO: decide comparison strategy based on domain data type.
        // Does any coverage actually return anything else that null for this:
        // String type = dimAccessor.getDomainDatatype(dimensionName);

        // Just use a case insensitive lexical string comparison for now:
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        Collections.sort(domain, comp);
        long shortestDistance = Long.MAX_VALUE;
        long currentDistance = 0;

        for (String toCompare : domain) {
            int compValue = comp.compare(toCompare, toMatch);
            if (compValue < 0) {
                currentDistance = -compValue;
                if (currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                    candidate = toCompare;
                }
            } else {
                currentDistance = compValue;
                if (currentDistance < shortestDistance) {
                    candidate = toCompare;
                    // the distance can only grow after this
                    // assuming the values are in ascending order,
                    // so stop iterating at this point for efficiency:
                    break;
                }
            }
        }
        return candidate;
    }

    @Override
    public String getCapabilitiesRepresentation(
            ResourceInfo resource, String dimensionName, DimensionInfo dimensionInfo) {
        if (fixedCapabilitiesValue != null) {
            return this.fixedCapabilitiesValue;
        } else {
            return super.getCapabilitiesRepresentation(resource, dimensionName, dimensionInfo);
        }
    }

    public Object getTargetValue() {
        return toMatch;
    }
}
