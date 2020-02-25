/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import java.util.Collection;
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
import org.geoserver.catalog.StructuredCoverageViewReader;
import org.geoserver.ows.kvp.TimeParser;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.util.Converters;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.Utilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.PropertyName;

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

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /** Comparator for the TreeSet made either by Date objects, or by DateRange objects */
    private static final Comparator<Object> TEMPORAL_COMPARATOR =
            new Comparator<Object>() {

                @Override
                public int compare(Object o1, Object o2) {
                    // the domain can be a mix of dates and ranges
                    if (o1 instanceof Date) {
                        if (o2 instanceof DateRange) {
                            return ((Date) o1).compareTo(((DateRange) o2).getMinValue());
                        } else {
                            return ((Date) o1).compareTo((Date) o2);
                        }
                    } else if (o1 instanceof DateRange) {
                        if (o2 instanceof Date) {
                            return ((DateRange) o1).getMinValue().compareTo((Date) o2);
                        } else {
                            return ((DateRange) o1)
                                    .getMinValue()
                                    .compareTo(((DateRange) o2).getMinValue());
                        }
                    }
                    throw new IllegalArgumentException(
                            "Unxpected object type found, was expecting date or date range but found "
                                    + o1
                                    + " and "
                                    + o2);
                }
            };

    /** Comparator for TreeSet made either by Double objects, or by NumberRange objects */
    private static final Comparator<Object> ELEVATION_COMPARATOR =
            new Comparator<Object>() {

                @Override
                public int compare(Object o1, Object o2) {
                    if (o1 instanceof Double) {
                        if (o2 instanceof Double) {
                            return ((Double) o1).compareTo((Double) o2);
                        } else if (o2 instanceof NumberRange) {
                            return ((Double) o1)
                                    .compareTo(((NumberRange<Double>) o2).getMinValue());
                        }
                    } else if (o1 instanceof NumberRange) {
                        if (o2 instanceof NumberRange) {
                            return ((NumberRange<Double>) o1)
                                    .getMinValue()
                                    .compareTo(((NumberRange<Double>) o2).getMinValue());
                        } else {
                            return ((NumberRange<Double>) o1).getMinValue().compareTo((Double) o2);
                        }
                    }
                    throw new IllegalArgumentException(
                            "Unxpected object type found, was expecting double or range of doubles but found "
                                    + o1
                                    + " and "
                                    + o2);
                }
            };

    private final GridCoverage2DReader reader;

    private final List<String> metadataNames = new ArrayList<String>();

    public ReaderDimensionsAccessor(GridCoverage2DReader reader) throws IOException {
        Utilities.ensureNonNull("reader", reader);
        this.reader = reader;
        final String[] dimensions = reader.getMetadataNames();
        if (dimensions != null) {
            metadataNames.addAll(Arrays.asList(dimensions));
        }
    }

    /** True if the reader has a time dimension */
    public boolean hasTime() throws IOException {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_TIME_DOMAIN));
    }

    /**
     * Returns the full set of time values supported by the raster, sorted by time. They are either
     * {@link Date} objects, or {@link DateRange} objects, according to what the underlying reader
     * provides.
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
     * Returns the set of time values supported by the raster, sorted by time, in the specified
     * range. They are either {@link Date} objects, or {@link DateRange} objects, according to what
     * the underlying reader provides.
     */
    public TreeSet<Object> getTimeDomain(DateRange range, int maxEntries) throws IOException {
        if (!hasTime()) {
            Collections.emptySet();
        }

        TreeSet<Object> result = null;
        if (reader instanceof StructuredGridCoverage2DReader) {
            StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
            result = getDimensionValuesInRange("time", range, maxEntries, sr);
        }

        // if we got here, the optimization did not work, do the normal path
        if (result == null) {
            result = new TreeSet<Object>();
            TreeSet<Object> fullDomain = getTimeDomain();

            for (Object o : fullDomain) {
                if (o instanceof Date) {
                    if (range.contains((Date) o)) {
                        result.add(o);
                    }
                } else if (o instanceof DateRange) {
                    if (range.intersects((DateRange) o)) {
                        result.add(o);
                    }
                }
            }
        }

        return result;
    }

    /** Parses either a time expression in ISO format, or a time period in start/end format */
    private Object parseTimeOrRange(SimpleDateFormat df, String timeOrRange) throws ParseException {
        if (timeOrRange.contains("/")) {
            String[] splitted = timeOrRange.split("/");
            final String strStart = splitted[0];
            final String strEnd = splitted[1];
            if (strStart == null || strEnd == null) {
                throw new IllegalArgumentException("Invalid date range " + timeOrRange);
            }
            if (strStart != null && strStart.equals(strEnd)) {
                return df.parse(strStart);
            } else {
                Date start = df.parse(strStart);
                Date end = df.parse(strEnd);
                return new DateRange(start, end);
            }
        } else {
            return df.parse(timeOrRange);
        }
    }

    /**
     * Parses the specified value as a NumberRange if it's in the min/max form, as a Double
     * otherwise
     */
    private Object parseNumberOrRange(String val) {
        if (val.contains("/")) {
            String[] splitted = val.split("/");
            final String strStart = splitted[0];
            final String strEnd = splitted[1];
            if (strStart.equals(strEnd)) {
                return Double.parseDouble(strStart);
            }
            double start = Double.parseDouble(strStart);
            double end = Double.parseDouble(strEnd);
            return new NumberRange<Double>(Double.class, start, end);
        } else {
            return Double.parseDouble(val);
        }
    }

    /**
     * Returns the max value for the time, either as a single {@link Date} or {@link DateRange}
     * according to what the underlying reader provides
     */
    public Date getMaxTime() throws IOException {
        if (!hasTime()) {
            return null;
        }
        final String currentTime =
                reader.getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MAXIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get CURRENT time from coverage reader", e);
        }
    }

    /** Returns the min value for the time */
    public Date getMinTime() throws IOException {
        if (!hasTime()) {
            return null;
        }
        final String currentTime =
                reader.getMetadataValue(AbstractGridCoverage2DReader.TIME_DOMAIN_MINIMUM);
        if (currentTime == null) {
            return null;
        }
        try {
            return getTimeFormat().parse(currentTime);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to get minimum time from coverage reader", e);
        }
    }

    /** Returns a {@link SimpleDateFormat} using the UTC_PATTERN and the UTC time zone */
    public SimpleDateFormat getTimeFormat() {
        final SimpleDateFormat df = new SimpleDateFormat(UTC_PATTERN);
        df.setTimeZone(UTC_TIME_ZONE);
        return df;
    }

    /** True if the reader has a elevation dimension */
    public boolean hasElevation() throws IOException {
        return "true".equalsIgnoreCase(reader.getMetadataValue(HAS_ELEVATION_DOMAIN));
    }

    /**
     * Returns the full set of elevation values (either as Double or NumberRange), sorted from
     * smaller to higher
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
     * Returns the set of elevation values supported by the raster, sorted from smaller to bigger,
     * in the specified range. They are either {@link Double} objects, or {@link NumberRange}
     * objects, according to what the underlying reader provides.
     */
    public TreeSet<Object> getElevationDomain(NumberRange range, int maxEntries)
            throws IOException {
        if (!hasElevation()) {
            Collections.emptySet();
        }

        // special optimization for structured coverage readers
        TreeSet<Object> result = null;
        if (reader instanceof StructuredGridCoverage2DReader) {
            StructuredGridCoverage2DReader sr = (StructuredGridCoverage2DReader) reader;
            result = getDimensionValuesInRange("elevation", range, maxEntries, sr);
        }

        // if we got here, the optimization did not work, do the normal path
        if (result == null) {
            result = new TreeSet<Object>();
            TreeSet<Object> fullDomain = getElevationDomain();

            for (Object o : fullDomain) {
                if (o instanceof Double) {
                    if (range.contains((Number) o)) {
                        result.add(o);
                    }
                } else if (o instanceof NumberRange) {
                    if (range.intersects((NumberRange) o)) {
                        result.add(o);
                    }
                }
            }
        }

        return result;
    }

    private TreeSet<Object> getDimensionValuesInRange(
            String dimensionName, Range range, int maxEntries, StructuredGridCoverage2DReader sr)
            throws IOException {
        final String name = sr.getGridCoverageNames()[0];
        List<DimensionDescriptor> descriptors = sr.getDimensionDescriptors(name);
        for (DimensionDescriptor descriptor : descriptors) {
            // do we find the time, and can we optimize?
            if (dimensionName.equalsIgnoreCase(descriptor.getName())
                    && descriptor.getEndAttribute() == null) {
                GranuleSource gs = sr.getGranules(name, true);
                final Query query = new Query(gs.getSchema().getName().getLocalPart());
                // The NetCDF plug-in gets a corrupted cache if we provide a property list
                // query.setPropertyNames(Arrays.asList(descriptor.getStartAttribute()));
                final PropertyName attribute = FF.property(descriptor.getStartAttribute());
                final PropertyIsBetween rangeFilter =
                        FF.between(
                                attribute,
                                FF.literal(range.getMinValue()),
                                FF.literal(range.getMaxValue()));
                query.setFilter(rangeFilter);
                query.setMaxFeatures(maxEntries);
                query.setPropertyNames(new String[] {descriptor.getStartAttribute()});
                query.setHints(new Hints(StructuredCoverageViewReader.QUERY_FIRST_BAND, true));

                FeatureCollection collection = gs.getGranules(query);

                // collect all unique values (can't do ranges now, we don't have a multi-attribute
                // unique visitor)
                UniqueVisitor visitor = new UniqueVisitor(attribute);
                collection.accepts(visitor, null);
                TreeSet<Object> result = new TreeSet<>(visitor.getUnique());
                return result;
            }
        }

        return null;
    }

    /** Returns the max value for the elevation (as a Double, or as a NumberRange) */
    public Double getMaxElevation() throws IOException {
        if (!hasElevation()) {
            return null;
        }
        final String elevation =
                reader.getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MAXIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get maximum elevation from coverage reader", e);
        }
    }

    /** Returns the min value for the elevation (as a Double, or as a NumbeRange) */
    public Double getMinElevation() throws IOException {
        if (!hasElevation()) {
            return null;
        }
        final String elevation =
                reader.getMetadataValue(AbstractGridCoverage2DReader.ELEVATION_DOMAIN_MINIMUM);
        if (elevation == null) {
            return null;
        }
        try {
            return Double.parseDouble(elevation);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to get minimum elevation from coverage reader", e);
        }
    }

    /** Lists the custom domains of a raster data set */
    public List<String> getCustomDomains() {
        if (metadataNames.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> names = new HashSet<String>(metadataNames);
        TreeSet<String> result = new TreeSet<String>();
        for (String name : names) {
            if (name.startsWith("HAS_") && name.endsWith("_DOMAIN")) {
                String dimension = name.substring(4, name.length() - 7);
                if (names.contains(dimension + "_DOMAIN")
                        && !"TIME".equals(dimension)
                        && !"ELEVATION".equals(dimension)) {
                    result.add(dimension);
                }
            }
        }

        return new ArrayList<String>(result);
    }

    /** Return the domain datatype (if available) */
    public String getDomainDatatype(final String domainName) throws IOException {
        return reader.getMetadataValue(domainName.toUpperCase() + "_DOMAIN_DATATYPE");
    }

    /** True if the reader has a dimension with the given name */
    public boolean hasDomain(String name) throws IOException {
        Utilities.ensureNonNull("name", name);
        return "true"
                .equalsIgnoreCase(reader.getMetadataValue("HAS_" + name.toUpperCase() + "_DOMAIN"));
    }

    /** Returns the full set of values for the given dimension */
    public List<String> getDomain(String name) throws IOException {
        String[] values = reader.getMetadataValue(name.toUpperCase() + "_DOMAIN").split(",");
        List<String> valueSet = new ArrayList<String>();
        for (String val : values) {
            valueSet.add(val);
        }
        return valueSet;
    }

    /** Extracts the custom domain lowest value (using String sorting) */
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

    /** Checks if this dimension has a range (min/max) or just a domain */
    public boolean hasRange(String domain) {
        return metadataNames.contains(domain + "_DOMAIN_MAXIMUM")
                && metadataNames.contains(domain + "_DOMAIN_MINIMUM");
    }

    /** Checks if this dimension has a resolution */
    public boolean hasResolution(String domain) {
        Utilities.ensureNonNull("name", domain);
        return metadataNames.contains(domain.toUpperCase() + "_DOMAIN_RESOLUTION");
    }

    public Collection<Object> convertDimensionValue(String name, String value) {
        List<Object> result = new ArrayList<Object>();
        try {
            String typeName = getDomainDatatype(name);
            if (typeName != null) {
                Class<?> type = Class.forName(typeName);
                if (type == java.util.Date.class) {
                    result.addAll(new TimeParser().parse(value));
                } else if (Number.class.isAssignableFrom(type) && !value.contains(",")) {
                    result.add(parseNumberOrRange(value));
                } else {
                    for (String element : value.split(",")) {
                        result.add(Converters.convert(element, type));
                    }
                }
            } else {
                result.add(value);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to convert dimension value: ", e);
            result.add(value);
        }

        return result;
    }

    public List<Object> convertDimensionValue(String name, List<String> value) {
        List<Object> list = new ArrayList<Object>();

        for (String val : value) {
            list.addAll(convertDimensionValue(name, val));
        }

        return list;
    }
}
