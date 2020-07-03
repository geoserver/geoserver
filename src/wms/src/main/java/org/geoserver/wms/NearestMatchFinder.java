/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geoserver.wms.NearestMatchFinder.FilterDirection.HIGHEST_AMONG_LOWERS;
import static org.geoserver.wms.NearestMatchFinder.FilterDirection.LOWEST_AMONG_HIGHER;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.TreeSet;
import org.geoserver.catalog.AcceptableRange;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StructuredCoverageViewReader;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.NearestVisitor;
import org.geotools.util.Range;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/** Support class to find the nearest match to a given dimension value */
public abstract class NearestMatchFinder {

    /**
     * Made available only for testing purposes, as there is no simple (non structured) reader
     * supporting time but better to work off the "reference" one (the image mosaic)
     */
    public static boolean ENABLE_STRUCTURED_READER_SUPPORT = true;

    /**
     * Returns an implementation of {@link NearestMatchFinder} optimized for the given resource
     * type, or throws an {@link IllegalArgumentException} in case the resource type is not
     * supported
     */
    public static NearestMatchFinder get(
            ResourceInfo info, DimensionInfo dimensionInfo, String dimensionName)
            throws IOException {
        Class dataType = getDataTypeFromDimension(info, dimensionName);
        try {
            AcceptableRange acceptableRange =
                    AcceptableRange.getAcceptableRange(
                            dimensionInfo.getAcceptableInterval(), dataType);
            if (info instanceof FeatureTypeInfo) {
                FeatureTypeInfo featureType = (FeatureTypeInfo) info;
                return new Vector(
                        featureType,
                        dimensionInfo.getAttribute(),
                        dimensionInfo.getEndAttribute(),
                        acceptableRange,
                        dataType);
            } else if (info instanceof CoverageInfo) {
                GridCoverageReader reader = ((CoverageInfo) info).getGridCoverageReader(null, null);
                if (reader instanceof StructuredGridCoverage2DReader
                        && ENABLE_STRUCTURED_READER_SUPPORT) {
                    StructuredGridCoverage2DReader structured =
                            (StructuredGridCoverage2DReader) reader;
                    DimensionDescriptor dd = getDimensionDescriptor(structured, dimensionName);
                    return new StructuredReader(
                            structured,
                            dd.getStartAttribute(),
                            dd.getEndAttribute(),
                            acceptableRange,
                            dataType);
                } else if (reader instanceof GridCoverage2DReader) {
                    return new Reader(
                            (GridCoverage2DReader) reader,
                            acceptableRange,
                            dimensionName,
                            dataType);
                }
            }
        } catch (ParseException e) {
            throw new ServiceException(
                    "Failed to apply nearest match search on " + info.prefixedName(), e);
        }

        throw new IllegalArgumentException("No nearest match support for " + info);
    }

    private static DimensionDescriptor getDimensionDescriptor(
            StructuredGridCoverage2DReader structured, String dimensionName) throws IOException {
        String coverageName = structured.getGridCoverageNames()[0];
        return structured
                .getDimensionDescriptors(coverageName)
                .stream()
                .filter(dd -> dimensionName.equalsIgnoreCase(dd.getName()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Could not find dimension"
                                                + dimensionName
                                                + "in grid coverage reader"));
    }

    private static Class getDataTypeFromDimension(ResourceInfo info, String dimensionName) {
        if (dimensionName.equalsIgnoreCase(ResourceInfo.TIME)) {
            return Date.class;
        } else if (dimensionName.equalsIgnoreCase(ResourceInfo.ELEVATION)) {
            return Double.class;
        }

        // if it's a custome dimension do custom logic based on the resourceinfo, e.g. pick the
        // attributes
        // from featuretype/structured readers and use strings for anything else
        throw new IllegalArgumentException(
                "Dimension " + dimensionName + " not supported for nearest match yet");
    }

    enum FilterDirection {
        HIGHEST_AMONG_LOWERS,
        LOWEST_AMONG_HIGHER
    };

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    PropertyName attribute;
    PropertyName endAttribute;
    AcceptableRange acceptableRange;
    Class dataType;

    public NearestMatchFinder(
            String startAttribute,
            String endAttribute,
            AcceptableRange acceptableRange,
            Class dataType) {
        this.attribute = FF.property(startAttribute);
        this.endAttribute = endAttribute == null ? null : FF.property(endAttribute);
        this.acceptableRange = acceptableRange;
        this.dataType = dataType;
    }

    /**
     * Finds the nearest available domain value to the give reference value. The result is always a
     * "point" type, not a range, even if the domain is made of ranges (the WMS spec seems to
     * indicate a instant value needs to be used for the used time in warning
     *
     * @param value The reference value
     * @return The nearest value, or null if the domain was empty. If the nearest value
     *     matches/overlaps the original value, then the original one is returned instead (this
     *     allows to tell apart no match vs exact match vs nearest match and eventually set the WMS
     *     HTTP warning head)
     */
    public Object getNearest(Object value) throws IOException {
        if (value == null) return null;
        // simple point vs point comparison?
        if (endAttribute == null
                && (!(value instanceof Range)
                        || ((Range) value).getMinValue().equals(((Range) value).getMaxValue()))) {
            Date date = (Date) (value instanceof Range ? ((Range) value).getMinValue() : value);
            NearestVisitor visitor = new NearestVisitor(attribute, date);
            Filter filter = Filter.INCLUDE;
            if (acceptableRange != null) {
                Range searchRange = acceptableRange.getSearchRange(date);
                filter =
                        FF.between(
                                attribute,
                                FF.literal(searchRange.getMinValue()),
                                FF.literal(searchRange.getMaxValue()));
            }
            FeatureCollection features = getMatches(filter);
            features.accepts(visitor, null);
            Object result = visitor.getResult().getValue();
            if (date.equals(result)) {
                return value;
            } else {
                return result;
            }
        } else {
            // find the highest among the lower values
            Filter lowerFilter = buildComparisonFilter(value, HIGHEST_AMONG_LOWERS);
            FeatureCollection lowers = getMatches(lowerFilter);
            MaxVisitor lowersVisitor =
                    new MaxVisitor(endAttribute == null ? attribute : endAttribute);
            lowers.accepts(lowersVisitor, null);
            Comparable maxOfSmallers = (Comparable) lowersVisitor.getResult().getValue();

            // find the lowest among the higher values
            Filter higherFilter = buildComparisonFilter(value, LOWEST_AMONG_HIGHER);
            FeatureCollection highers = getMatches(higherFilter);
            MinVisitor highersVisitor = new MinVisitor(attribute);
            highers.accepts(highersVisitor, null);
            Comparable minOfGreater = (Comparable) highersVisitor.getResult().getValue();
            return closest(value, maxOfSmallers, minOfGreater);
        }
    }

    protected Object closest(Object value, Object maxOfSmallers, Object minOfGreater) {
        // normalize ranges to significant instants
        if (maxOfSmallers instanceof Range) {
            maxOfSmallers = ((Range) maxOfSmallers).getMaxValue();
        }
        if (minOfGreater instanceof Range) {
            minOfGreater = ((Range) minOfGreater).getMinValue();
        }

        Object result;
        // now find the closest
        if (maxOfSmallers == null) {
            if (minOfGreater == null) {
                // there is no match
                result = null;
            } else {
                result = minOfGreater;
            }
        } else {
            if (minOfGreater == null) {
                result = maxOfSmallers;
            } else {
                if (value instanceof Range) {
                    Range range = (Range) value;
                    Object min = range.getMinValue();
                    Object max = range.getMaxValue();
                    double distanceBelow = distance(min, maxOfSmallers);
                    double distanceAbove = distance(max, minOfGreater);
                    result = distanceBelow < distanceAbove ? maxOfSmallers : minOfGreater;
                } else {
                    double distanceBelow = distance(value, minOfGreater);
                    double distanceAbove = distance(value, maxOfSmallers);
                    result = distanceBelow < distanceAbove ? minOfGreater : maxOfSmallers;
                }
            }
        }

        if (result instanceof Range) {
            if (result == minOfGreater) {
                return ((Range) result).getMaxValue();
            } else {
                return ((Range) result).getMinValue();
            }
        } else {
            return result;
        }
    }

    protected double distance(Object a, Object b) {
        if (Number.class.isAssignableFrom(dataType)) {
            Number na = (Number) a;
            Number nb = (Number) b;
            return Math.abs(na.doubleValue() - nb.doubleValue());
        } else if (Date.class.isAssignableFrom(dataType)) {
            Date da = (Date) a;
            Date db = (Date) b;
            return Math.abs(da.getTime() - db.getTime());
        } else {
            throw new IllegalArgumentException(
                    "Nearest calculations on data type " + dataType + " are not supported");
        }
    }

    protected Filter buildComparisonFilter(Object value, FilterDirection direction) {
        if (value instanceof Range) {
            Range range = (Range) value;
            Literal qlower = FF.literal(range.getMinValue());
            Literal qupper = FF.literal(range.getMaxValue());
            return buildComparisonFilter(direction, qlower, qupper);
        } else {
            Literal valueReference = FF.literal(value);
            return buildComparisonFilter(direction, valueReference, valueReference);
        }
    }

    private Filter buildComparisonFilter(
            FilterDirection direction, Literal qlower, Literal qupper) {
        PropertyName comparisonAttribute = getComparisonAttribute(direction);
        if (direction == HIGHEST_AMONG_LOWERS) {
            if (acceptableRange != null) {
                Range searchRange = acceptableRange.getSearchRange(qlower.getValue());
                return FF.between(
                        comparisonAttribute, FF.literal(searchRange.getMinValue()), qlower);
            } else {
                return FF.lessOrEqual(comparisonAttribute, qlower);
            }
        } else {
            if (acceptableRange != null) {
                Range searchRange = acceptableRange.getSearchRange(qupper.getValue());
                return FF.between(
                        comparisonAttribute, qupper, FF.literal(searchRange.getMaxValue()));
            } else {
                return FF.greaterOrEqual(comparisonAttribute, qupper);
            }
        }
    }

    private PropertyName getComparisonAttribute(FilterDirection direction) {
        if (endAttribute != null && direction == HIGHEST_AMONG_LOWERS) {
            return endAttribute;
        } else {
            return attribute;
        }
    }

    /** Returns a feature collection matching the */
    protected abstract FeatureCollection getMatches(Filter filter) throws IOException;

    /** Nearest matcher for vector data */
    private static class Vector extends NearestMatchFinder {
        private final FeatureSource featureSource;

        public Vector(
                FeatureTypeInfo ftInfo,
                String attribute,
                String endAttribute,
                AcceptableRange acceptableRange,
                Class dataType)
                throws IOException {
            super(attribute, endAttribute, acceptableRange, dataType);
            this.featureSource = ftInfo.getFeatureSource(null, null);
        }

        @Override
        protected FeatureCollection getMatches(Filter filter) throws IOException {
            return featureSource.getFeatures(filter);
        }
    }

    /**
     * Nearest match for {@link StructuredGridCoverage2DReader} leveraging the {@link GranuleSource}
     */
    private static class StructuredReader extends NearestMatchFinder {
        private final StructuredGridCoverage2DReader reader;

        public StructuredReader(
                StructuredGridCoverage2DReader reader,
                String startAttribute,
                String endAttribute,
                AcceptableRange acceptableRange,
                Class dataType) {
            super(startAttribute, endAttribute, acceptableRange, dataType);
            this.reader = reader;
        }

        @Override
        protected FeatureCollection getMatches(Filter filter) throws IOException {
            GranuleSource granules = reader.getGranules(null, true);
            Query q = new Query(null, filter);
            q.setHints(new Hints(StructuredCoverageViewReader.QUERY_FIRST_BAND, true));
            return granules.getGranules(q);
        }
    }

    /** Nearest match for generic {@link GridCoverage2DReader} leveraging the metadata */
    private static class Reader extends NearestMatchFinder {

        private final GridCoverage2DReader reader;
        private final String dimensionName;

        public Reader(
                GridCoverage2DReader reader,
                AcceptableRange acceptableRange,
                String dimensionName,
                Class dataType) {
            super(null, null, acceptableRange, dataType);
            this.reader = reader;
            this.dimensionName = dimensionName;
        }

        @Override
        public Object getNearest(Object value) throws IOException {
            TreeSet<Object> domain = getDimensionDomain();
            if (domain.isEmpty()) {
                return null;
            }

            // find the two closest to the specified object
            Object maxOfSmallers = null;
            Object minOfGreater = null;

            Range rangeFilter =
                    this.acceptableRange != null
                            ? this.acceptableRange.getSearchRange(value)
                            : null;

            for (Object d : domain) {
                // skip undesired values
                if (!rangeFilterAccepts(rangeFilter, d)) {
                    continue;
                }
                int result = compare(d, value);
                if (result < 0) {
                    maxOfSmallers = d;
                } else if (result == 0) {
                    // straight match, use the original value
                    return value;
                } else {
                    // we switched to higher, end of search
                    minOfGreater = d;
                    break;
                }
            }

            return closest(value, maxOfSmallers, minOfGreater);
        }

        private boolean rangeFilterAccepts(Range rangeFilter, Object domainValue) {
            if (rangeFilter == null) {
                return true;
            }
            if (domainValue instanceof Range) {
                return rangeFilter.intersects((Range) domainValue);
            } else {
                return rangeFilter.contains((Comparable) domainValue);
            }
        }

        /** Compares two object, they can be either instants/ranges or a mix of them */
        private int compare(Object a, Object b) {
            if (!(a instanceof Range)) {
                if (!(b instanceof Range)) {
                    return ((Comparable) a).compareTo(b);
                } else {
                    // reverse comparison
                    return compare((Range) b, a) * -1;
                }
            } else if (a instanceof Range) {
                if (b instanceof Range) {
                    Range ra = (Range) a;
                    Range rb = (Range) b;

                    if (ra.intersects(rb)) {
                        return 0;
                    } else if (ra.getMinValue().compareTo(rb.getMaxValue()) >= 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    return compare((Range) a, b);
                }
            }

            throw new IllegalArgumentException("boo");
        }

        private int compare(Range a, Object b) {
            Range ra = a;
            if (ra.getMinValue().compareTo(b) > 0) {
                // a is greater than b
                return 1;
            } else if (ra.getMaxValue().compareTo(b) < 0) {
                // a is lower than b
                return -1;
            } else {
                // a contains b then?
                return 0;
            }
        }

        private TreeSet<Object> getDimensionDomain() throws IOException {
            ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
            if (ResourceInfo.TIME.equals(dimensionName)) {
                return accessor.getTimeDomain();
            } else {
                throw new IllegalArgumentException(
                        "Nearest match support on simple grid readers is supported only "
                                + "for time at the moment");
            }
        }

        @Override
        protected FeatureCollection getMatches(Filter filter) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
