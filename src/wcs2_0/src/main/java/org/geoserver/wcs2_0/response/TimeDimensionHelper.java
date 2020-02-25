/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Provides support to build the coverage description for time based data
 *
 * @author Andrea Aime - GeoSolutions
 */
class TimeDimensionHelper {

    static final Logger LOGGER = Logging.getLogger(TimeDimensionHelper.class);

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

    /** Labels for teh above time periods */
    static final String[] DURATION_UNITS =
            new String[] {"year", "month", "day", "hour", "minute", "second"};

    DimensionInfo timeDimension;

    ReaderDimensionsAccessor accessor;

    ISO8601Formatter formatter = new ISO8601Formatter();

    String resolutionUnit;

    long resolutionValue;

    String coverageId;

    public TimeDimensionHelper(
            DimensionInfo timeDimension, GridCoverage2DReader reader, String coverageId)
            throws IOException {
        this.timeDimension = timeDimension;
        this.accessor = new ReaderDimensionsAccessor(reader);
        this.coverageId = coverageId;

        if (timeDimension.getResolution() != null) {
            setupResolution(timeDimension.getResolution());
        }
    }

    private void setupResolution(BigDecimal resolution) {
        for (int i = 0; i < DURATIONS.length; i++) {
            BigDecimal duration = DURATIONS[i];
            if (resolution.remainder(duration).longValue() == 0) {
                resolutionValue = resolution.divide(duration).longValue();
                resolutionUnit = DURATION_UNITS[i];
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

    public TreeSet<Object> getTimeDomain() throws IOException {
        return accessor.getTimeDomain();
    }

    /** Returns the minimum time, formatted according to ISO8601 */
    public String getBeginPosition() throws IOException {
        Date minTime = accessor.getMinTime();
        return format(minTime);
    }

    /** Returns the maximum time, formatted according to ISO8601 */
    public String getEndPosition() throws IOException {
        Date maxTime = accessor.getMaxTime();
        return format(maxTime);
    }

    /** Formats a Date into ISO86011 */
    public String format(Date time) {
        if (time != null) {
            return formatter.format(time);
        } else {
            return null;
        }
    }

    /** Returns the type of presentation for the time dimension */
    public DimensionPresentation getPresentation() {
        return timeDimension.getPresentation();
    }

    /**
     * Returns the resolution unit, choosing among "year", "month", "day", "hour", "minute",
     * "second"
     */
    public String getResolutionUnit() {
        return resolutionUnit;
    }

    /** The resolution value, expressed in the unit returned by {@link #getResolutionUnit()} */
    public long getResolutionValue() {
        return resolutionValue;
    }

    /** The coverage identifier */
    public String getCoverageId() {
        return coverageId;
    }
}
