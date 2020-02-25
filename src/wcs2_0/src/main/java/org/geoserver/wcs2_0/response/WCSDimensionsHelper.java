/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.Range;
import org.geotools.util.Utilities;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Provides support to build the coverage description for time/elevation/additional dim based data
 *
 * @author Andrea Aime - GeoSolutions
 * @author Daniele Romagnoli - GeoSolutions
 */
public class WCSDimensionsHelper {

    /** Duration in ms of well know time periods */
    static final BigDecimal[] DURATIONS =
            new BigDecimal[] {
                new BigDecimal(31536000000L),
                new BigDecimal(2628000000L),
                new BigDecimal(86400000L),
                new BigDecimal(3600000L),
                new BigDecimal(60000),
                new BigDecimal(1000L)
            };

    /** Labels for the above time periods */
    static final String[] DURATION_UNITS =
            new String[] {"year", "month", "day", "hour", "minute", "second"};

    /** Quick access fields for timeDimension and elevationDimension */
    DimensionInfo timeDimension;

    DimensionInfo elevationDimension;

    /** Additional dimensions map */
    Map<String, DimensionInfo> additionalDimensions;

    ReaderDimensionsAccessor accessor;

    ISO8601Formatter formatter = new ISO8601Formatter();

    String elevationResolutionUnit;

    double elevationResolutionValue;

    String timeResolutionUnit;

    long timeResolutionValue;

    String coverageId;

    /**
     * Base constructor which only deals with timeDimension. It is used by WCS-EO classes which
     * deals with up to timeDimensions
     */
    public WCSDimensionsHelper(CoverageInfo ci) throws IOException {
        this.coverageId = NCNameResourceCodec.encode(ci);
        this.accessor =
                new ReaderDimensionsAccessor(
                        (GridCoverage2DReader) ci.getGridCoverageReader(null, null));

        Map<String, DimensionInfo> dimensions = new HashMap<String, DimensionInfo>();
        for (Map.Entry<String, Serializable> entry : ci.getMetadata().entrySet()) {
            if (entry.getValue() instanceof DimensionInfo) {
                dimensions.put(entry.getKey(), (DimensionInfo) entry.getValue());
            }
        }
        if (!dimensions.isEmpty()) {
            initDimensions(dimensions);
        }
    }

    /**
     * Base constructor which only deals with timeDimension. It is used by WCS-EO classes which
     * deals with up to timeDimensions
     */
    public WCSDimensionsHelper(
            final DimensionInfo timeDimension,
            final GridCoverage2DReader reader,
            final String coverageId)
            throws IOException {
        this(
                new HashMap<String, DimensionInfo>() {
                    {
                        put(ResourceInfo.TIME, timeDimension);
                    }
                },
                reader,
                coverageId);
    }

    public WCSDimensionsHelper(
            final Map<String, DimensionInfo> dimensions,
            final GridCoverage2DReader reader,
            final String coverageId)
            throws IOException {
        this.accessor = new ReaderDimensionsAccessor(reader);
        this.coverageId = coverageId;

        if (dimensions != null && !dimensions.isEmpty()) {
            initDimensions(dimensions);
        }
    }

    /** Initialize dimensions */
    private void initDimensions(Map<String, DimensionInfo> dimensions) {
        Utilities.ensureNonNull("dimensions", dimensions);
        Map<String, DimensionInfo> updatedDimensions = new HashMap<String, DimensionInfo>();
        updatedDimensions.putAll(dimensions);

        // Initialize Time dimensions
        if (updatedDimensions.containsKey(ResourceInfo.TIME)) {
            timeDimension = updatedDimensions.remove(ResourceInfo.TIME);
            if (timeDimension != null) {
                final BigDecimal resolution = timeDimension.getResolution();
                if (resolution != null) {
                    setupTimeResolution(resolution);
                }
            }
        }

        // Initialize Elevation Dimensions
        if (updatedDimensions.containsKey(ResourceInfo.ELEVATION)) {
            elevationDimension = updatedDimensions.remove(ResourceInfo.ELEVATION);
            final BigDecimal resolution = elevationDimension.getResolution();
            if (resolution != null) {
                elevationResolutionValue = resolution.doubleValue();
                elevationResolutionUnit = elevationDimension.getUnitSymbol();
            }
        }

        // Remaining dimensions are custom dimensions
        this.additionalDimensions = updatedDimensions;
    }

    private void setupTimeResolution(BigDecimal resolution) {
        for (int i = 0; i < DURATIONS.length; i++) {
            BigDecimal duration = DURATIONS[i];
            if (resolution.remainder(duration).longValue() == 0) {
                timeResolutionValue = resolution.divide(duration).longValue();
                timeResolutionUnit = DURATION_UNITS[i];
                return;
            }
        }
        // uh oh? it's a value in milliseconds?
        throw new WcsException(
                "Dimension's resolution requires milliseconds for full representation, "
                        + "but this cannot be represented in WCS 2.0 describe coverage output");
    }

    public DimensionInfo getTimeDimension() {
        return timeDimension;
    }

    public DimensionInfo getElevationDimension() {
        return elevationDimension;
    }

    public Map<String, DimensionInfo> getAdditionalDimensions() {
        return additionalDimensions;
    }

    public TreeSet<Object> getTimeDomain() throws IOException {
        return accessor.getTimeDomain();
    }

    public TreeSet<Object> getElevationDomain() throws IOException {
        return accessor.getElevationDomain();
    }

    public List<String> getDomain(final String domainName) throws IOException {
        return accessor.getDomain(domainName);
    }

    /** Returns the minimum time, formatted according to ISO8601 */
    public String getBeginTime() throws IOException {
        Date minTime = accessor.getMinTime();
        return format(minTime);
    }

    /** Returns the maximum time, formatted according to ISO8601 */
    public String getEndTime() throws IOException {
        Date maxTime = accessor.getMaxTime();
        return format(maxTime);
    }

    /** Returns the minimum elevation */
    public String getBeginElevation() throws IOException {
        Double minElevation = accessor.getMinElevation();
        return minElevation.toString();
    }

    /** Returns the maximum elevation */
    public String getEndElevation() throws IOException {
        Double maxElevation = accessor.getMaxElevation();
        return maxElevation.toString();
    }

    /** Return the default value of the specified additional domain */
    public String getDefaultValue(String domainName) throws IOException {
        if (additionalDimensions != null
                && !additionalDimensions.isEmpty()
                && additionalDimensions.containsKey(domainName)) {
            return accessor.getCustomDomainDefaultValue(domainName);
        }
        return null;
    }

    /** Formats a dimension item into a string */
    public String format(Object o) {
        if (o instanceof Range) {
            Range range = (Range) o;
            return format(range.getMinValue()) + "/" + format(range.getMaxValue());
        } else if (o instanceof Date) {
            return format((Date) o);
        } else {
            return String.valueOf(o);
        }
    }

    /** Formats a Date into ISO86011 */
    public String format(Date time) {
        if (time != null) {
            return formatter.format(time);
        } else {
            return null;
        }
    }

    /**
     * Returns the time resolution unit, choosing among "year", "month", "day", "hour", "minute",
     * "second"
     */
    public String getTimeResolutionUnit() {
        return timeResolutionUnit;
    }

    /**
     * The time resolution value, expressed in the unit returned by {@link #getTimeResolutionUnit()}
     */
    public long getTimeResolutionValue() {
        return timeResolutionValue;
    }

    /** Returns the elevation resolution unit */
    public String getElevationResolutionUnit() {
        return elevationResolutionUnit;
    }

    /** The elevation resolution value */
    public double getElevationResolutionValue() {
        return elevationResolutionValue;
    }

    /** The coverage identifier */
    public String getCoverageId() {
        return coverageId;
    }

    /**
     * Scan the metadataMap looking for resources related to {@link DimensionInfo} objects and
     * return a dimensions Map. Return an empty map if no dimensions are found.
     */
    public static Map<String, DimensionInfo> getDimensionsFromMetadata(MetadataMap metadata) {
        Map<String, DimensionInfo> dimensionsMap = new HashMap<String, DimensionInfo>();
        if (metadata != null && !metadata.isEmpty()) {
            final Set<String> metadataKeys = metadata.keySet();
            final Iterator<String> metadataIterator = metadataKeys.iterator();

            // loop over metadata keys
            while (metadataIterator.hasNext()) {
                String key = metadataIterator.next();
                if (isADimension(key)) {
                    // Check whether the specified metadata is related to an enabled Dimension
                    DimensionInfo dimension = metadata.get(key, DimensionInfo.class);
                    if (dimension != null && dimension.isEnabled()) {
                        if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                            key = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                        }

                        dimensionsMap.put(key, dimension);
                    }
                }
            }
        }
        return dimensionsMap;
    }

    public static DimensionDescriptor getDimensionDescriptor(
            final StructuredGridCoverage2DReader reader,
            final String coverageName,
            final String dimensionName) {
        try {
            List<DimensionDescriptor> descriptors = reader.getDimensionDescriptors(coverageName);
            for (DimensionDescriptor dd : descriptors) {
                if (dd.getName().equalsIgnoreCase(dimensionName)) {
                    return dd;
                }
            }

            return null;
        } catch (IOException e) {
            throw new WCS20Exception(
                    "Failed to locate the reader's " + dimensionName + " dimension descriptor", e);
        }
    }

    /** Return {@code true} in case the specified Key refers to a Dimension. */
    private static final boolean isADimension(final String key) {
        return key != null
                && (key.equals(ResourceInfo.TIME)
                        || key.equals(ResourceInfo.ELEVATION)
                        || key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX));
    }

    /**
     * Builds a dimension helper from the CoverageInfo
     *
     * @param encodedId The encoded coverage id
     * @param ci The CoverageInfo
     * @return A WCSDimensionsHelper, or null if there are no extra dimensions to handle
     */
    public static WCSDimensionsHelper getWCSDimensionsHelper(
            String encodedId, CoverageInfo ci, GridCoverage2DReader reader) throws Exception {
        WCSDimensionsHelper dimensionsHelper = null;
        MetadataMap metadata = ci.getMetadata();
        Map<String, DimensionInfo> dimensionsMap =
                WCSDimensionsHelper.getDimensionsFromMetadata(metadata);

        // Setup a dimension helper in case we found some dimensions for that coverage
        if (!dimensionsMap.isEmpty()) {
            dimensionsHelper = new WCSDimensionsHelper(dimensionsMap, reader, encodedId);
        }
        return dimensionsHelper;
    }

    /** Returns the raw dimension accessor */
    public ReaderDimensionsAccessor getDimensionAccessor() {
        return accessor;
    }
}
