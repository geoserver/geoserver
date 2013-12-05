/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import static org.geotools.coverage.grid.io.GridCoverage2DReader.ELEVATION_DOMAIN;
import static org.geotools.coverage.grid.io.GridCoverage2DReader.HAS_ELEVATION_DOMAIN;
import static org.geotools.coverage.grid.io.GridCoverage2DReader.HAS_TIME_DOMAIN;
import static org.geotools.coverage.grid.io.GridCoverage2DReader.TIME_DOMAIN;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

/**
 * Centralizes the metadata extraction and parsing used to read dimension informations out of a
 * coverage reader
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ReaderDimensionsAccessor {

    /** UTC_TIME_ZONE */
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final Logger LOGGER = Logging.getLogger(ReaderDimensionsAccessor.class);

    private static final String UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    /**
     * Comparator for the TreeSet made either by Date objects, or by DateRange objects
     */
    private static final Comparator<Object> TEMPORAL_COMPARATOR = new Comparator<Object>() {

        @Override
        public int compare(Object o1, Object o2) {
            if(o1 instanceof Date && o2 instanceof Date) {
                return ((Date) o1).compareTo((Date) o2);
            } else if(o1 instanceof DateRange && o2 instanceof DateRange) {
                return ((DateRange) o1).getMinValue().compareTo(((DateRange) o2).getMinValue());
            } else {
                throw new IllegalArgumentException("Unexpected, values to be ordered have to " +
                        "be either all Date objects, or all DateRange objects, instead they are: " 
                        + o1 + ", " + o2);
            }
        }
        
    };
    
    /**
     * Comparator for TreeSet made either by Double objects, or by NumberRange objects
     * public instead of private for use in WMS unit test.
     */
    public static final Comparator<Object> ELEVATION_COMPARATOR = new Comparator<Object>() {

        @Override
        public int compare(Object o1, Object o2) {
            if(o1 instanceof Double && o2 instanceof Double) {
                return ((Double) o1).compareTo((Double) o2);
            } else if(o1 instanceof NumberRange && o2 instanceof NumberRange) {
                return ((NumberRange<Double>) o1).getMinValue().compareTo(((NumberRange<Double>) o2).getMinValue());
            } else {
                throw new IllegalArgumentException("Unexpected, values to be ordered have to " +
                        "be either all Double objects, or all NumberRange objects");
            }
        }
        
    };

    private final GridCoverage2DReader reader;

    private final List<String> metadataNames= new ArrayList<String>();

    public ReaderDimensionsAccessor(GridCoverage2DReader reader) throws IOException {
        Utilities.ensureNonNull("reader", reader);
        this.reader = reader;
        final String[] dimensions = reader.getMetadataNames();
        if (dimensions != null) {
            metadataNames.addAll(Arrays.asList(dimensions));
        }
    }

    /**
     * True if the reader has a time dimension
     * 
     * @return
     * @throws IOException 
     */
    public boolean hasTime() throws IOException {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_TIME_DOMAIN));
    }

    /**
     * Returns the full set of time values supported by the raster, sorted by time.
     * They are either {@link Date} objects, or {@link DateRange} objects, according to what
     * the underlying reader provides.
     * 
     * @return
     * @throws IOException 
     */
    public TreeSet<Object> getTimeDomain() throws IOException {
        if (!hasTime()) {
            Collections.emptySet();
        }
        final SimpleDateFormat df = getTimeFormat();
        String domain = reader.getMetadataValue(TIME_DOMAIN);
        String[] timeInstants = domain.split("\\s*,\\s*");
        TreeSet<Object> values = new TreeSet<Object>(TEMPORAL_COMPARATOR);
        for (String tp : timeInstants) {
            try {
                values.add(parseTimeOrRange(df, tp));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return values;
    }
    
    /**
     * Parses either a time expression in ISO format, or a time period in start/end format
     * @param df
     * @param timeOrRange
     * @return
     * @throws ParseException
     */
    private Object parseTimeOrRange(SimpleDateFormat df, String timeOrRange) throws ParseException {
        if(timeOrRange.contains("/")) {
            String[] splitted = timeOrRange.split("/");
            Date start = df.parse(splitted[0]);
            Date end = df.parse(splitted[1]);
            return new DateRange(start, end);
        } else {
            return df.parse(timeOrRange);
        }
    }
    
    /**
     * Parses the specified value as a NumberRange if it's in the min/max form, as a Double otherwise
     * @param val
     * @return
     */
    private Object parseNumberOrRange(String val) {
        if(val.contains("/")) {
            String[] splitted = val.split("/");
            double start = Double.parseDouble(splitted[0]);
            double end = Double.parseDouble(splitted[1]);
            return new NumberRange<Double>(Double.class, start, end);
        } else {
            return Double.parseDouble(val);
        }
    }

    /**
     * Returns the max value for the time, either as a single {@link Date} or {@link DateRange} 
     * according to what the underlying reader provides
     * 
     * @return
     * @throws IOException 
     */
    public Date getMaxTime() throws IOException {
        if (!hasTime()) {
            return null;
        }
        final String currentTime = reader
                .getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MAXIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get CURRENT time from coverage reader", e);
        }
    }

    /**
     * Returns the min value for the time
     * 
     * @return
     * @throws IOException 
     */
    public Date getMinTime() throws IOException {
        if (!hasTime()) {
            return null;
        }
        final String currentTime = reader
                .getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MINIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get minimum time from coverage reader", e);
        }
    }

    /**
     * Returns a {@link SimpleDateFormat} using the UTC_PATTERN and the UTC time zone
     * 
     * @return
     */
    public SimpleDateFormat getTimeFormat() {
        final SimpleDateFormat df = new SimpleDateFormat(UTC_PATTERN);
        df.setTimeZone(UTC_TIME_ZONE);
        return df;
    }

    /**
     * True if the reader has a elevation dimension
     * 
     * @return
     * @throws IOException 
     */
    public boolean hasElevation() throws IOException {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_ELEVATION_DOMAIN));
    }

    /**
     * Returns the full set of elevation values (either as Double or NumberRange), sorted from smaller to higher
     * 
     * @return
     * @throws IOException 
     */
    public TreeSet<Object> getElevationDomain() throws IOException {
        if (!hasElevation()) {
            return null;
        }
        // parse the values from the reader, they are exposed as strings...
        String[] elevationValues = reader.getMetadataValue(ELEVATION_DOMAIN).split(",");
        TreeSet<Object> elevations = new TreeSet<Object>(ELEVATION_COMPARATOR);
        for (String val : elevationValues) {
            try {
                elevations.add(parseNumberOrRange(val));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }

        return elevations;
    }

    
    
    /**
     * Returns the max value for the elevation (as a Double, or as a NumberRange)
     * 
     * @return
     * @throws IOException 
     */
    public Double getMaxElevation() throws IOException {
        if (!hasElevation()) {
            return null;
        }
        final String elevation = reader
                .getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MAXIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get maximum elevation from coverage reader", e);
        }
    }

    /**
     * Returns the min value for the elevation (as a Double, or as a NumbeRange)
     * 
     * @return
     * @throws IOException 
     */
    public Double getMinElevation() throws IOException {
        if (!hasElevation()) {
            return null;
        }
        final String elevation = reader
                .getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MINIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get minimum elevation from coverage reader", e);
        }
    }
    
    /**
     * Lists the custom domains of a raster data set
     * @return
     */
    public List<String> getCustomDomains() {
        if (metadataNames.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> names = new HashSet<String>(metadataNames);
        TreeSet<String> result = new TreeSet<String>();
        for (String name : names) {
            if(name.startsWith("HAS_") && name.endsWith("_DOMAIN")) {
                String dimension = name.substring(4, name.length() - 7);
                if(names.contains(dimension + "_DOMAIN") 
                        && !"TIME".equals(dimension) && !"ELEVATION".equals(dimension)) {
                    result.add(dimension);
                }
            }
        }

        return new ArrayList<String>(result);
    }

    /**
     * Return the domain datatype (if available)
     * @param domainName
     * @return
     * @throws IOException 
     */
    public String getDomainDatatype(final String domainName) throws IOException {
        return reader.getMetadataValue(domainName.toUpperCase() + "_DOMAIN_DATATYPE");
    }
    
    /**
     * True if the reader has a dimension with the given name
     * @throws IOException 
     */
    public boolean hasDomain(String name) throws IOException {
        Utilities.ensureNonNull("name", name);
        return "true".equalsIgnoreCase(reader.getMetadataValue("HAS_" + name.toUpperCase() + "_DOMAIN"));
    }

    /**
     * Returns the full set of values for the given dimension
     * @throws IOException 
     */
    public List<String> getDomain(String name) throws IOException {
        String[] values = reader.getMetadataValue(name.toUpperCase() + "_DOMAIN").split(",");
        List<String> valueSet = new ArrayList<String>();
        for (String val : values) {
            valueSet.add(val);
        }
        return valueSet;
    }

    /**
     * Extracts the custom domain lowest value (using String sorting)
     * @return
     * @throws IOException 
     */
    public String getCustomDomainDefaultValue(String name) throws IOException {
        Utilities.ensureNonNull("name", name);

        // see if we have an optimize way to get the minimum
        String minimum = reader.getMetadataValue(name.toUpperCase() + "_DOMAIN_MINIMUM");
        if (minimum != null) {
            return minimum;
        }

        // ok, get the full domain then
        List<String> domain = getDomain(name);
        if (domain.isEmpty()) {
            return null;
        } else {
            return domain.get(0);
        }
    }

    /**
     * Checks if this dimension has a range (min/max) or just a domain
     * @param domain
     * @return
     */
    public boolean hasRange(String domain) {
        return metadataNames.contains(domain + "_DOMAIN_MAXIMUM") && metadataNames.contains(domain + "_DOMAIN_MINIMUM");
    }

    /**
     * Checks if this dimension has a resolution
     * @param domain
     * @return
     */
    public boolean hasResolution(String domain) {
        Utilities.ensureNonNull("name", domain);
        return metadataNames.contains(domain.toUpperCase() + "_DOMAIN_RESOLUTION");
    }

    
}
