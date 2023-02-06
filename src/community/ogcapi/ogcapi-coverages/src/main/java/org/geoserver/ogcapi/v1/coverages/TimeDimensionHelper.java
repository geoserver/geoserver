/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.DateRange;
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
    static final BigDecimal[] DURATIONS = {
        new BigDecimal(31536000000L),
        new BigDecimal(86400000L),
        new BigDecimal(3600000L),
        new BigDecimal(60000),
        new BigDecimal(1000L)
    };

    private static final String SECONDS = "s";
    private static final String DAY = "d";
    /** Labels for teh above time periods */
    static final String[] DURATION_UNITS = {"y", DAY, "h", "min", SECONDS};

    DimensionInfo timeDimension;

    ReaderDimensionsAccessor accessor;

    ISO8601Formatter timeStampFormatter = new ISO8601Formatter();
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    String resolutionUnit;

    long resolutionValue;

    public TimeDimensionHelper(DimensionInfo timeDimension, GridCoverage2DReader reader)
            throws IOException {
        this.timeDimension = timeDimension;
        this.accessor = new ReaderDimensionsAccessor(reader);

        if (timeDimension.getResolution() != null) {
            setupResolution(timeDimension.getResolution());
        } else {
            resolutionValue = 1;
            resolutionUnit = SECONDS;
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
                        + "but this cannot be represented in the domain set output");
    }

    public DimensionInfo getTimeDimension() {
        return timeDimension;
    }

    public TreeSet<Object> getTimeDomain() throws IOException {
        return accessor.getTimeDomain();
    }

    public List<String> getFormattedDomain() throws IOException {
        return accessor.getTimeDomain().stream().map(t -> format(t)).collect(Collectors.toList());
    }

    /** Returns the minimum time, formatted according to ISO8601 */
    public String getFormattedBegin() throws IOException {
        Date minTime = getBegin();
        return format(minTime);
    }

    public Date getBegin() throws IOException {
        return accessor.getMinTime();
    }

    /** Returns the maximum time, formatted according to ISO8601 */
    public String getFormattedEnd() throws IOException {
        Date maxTime = getEnd();
        return format(maxTime);
    }

    public Date getEnd() throws IOException {
        return accessor.getMaxTime();
    }

    private String format(Object time) {
        if (time instanceof Date) {
            return format((Date) time);
        } else if (time instanceof DateRange) {
            // hack, we should probably look into description of partitioned coverages?
            // or report the mid time?
            DateRange range = (DateRange) time;
            return timeStampFormatter.format(range.getMinValue())
                    + "/"
                    + timeStampFormatter.format(range.getMaxValue());
        }

        return null;
    }

    /** Formats a Date into ISO86011 */
    public String format(Date time) {
        if (time instanceof java.sql.Date) {
            return dateFormatter.format(time);
        } else if (time != null) {
            return timeStampFormatter.format(time);
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

    public BigDecimal getResolutionMillis() {
        return timeDimension.getResolution();
    }
}
