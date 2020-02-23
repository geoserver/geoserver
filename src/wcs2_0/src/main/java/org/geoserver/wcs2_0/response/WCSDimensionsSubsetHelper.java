/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.GetCoverageType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.GridCoverageRequest;
import org.geoserver.wcs2_0.WCSEnvelope;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.DimensionBean.DimensionType;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.TransformException;

/**
 * Provides support to deal with dimensions slicing, trimming, values conversions and default values
 * computations
 *
 * <p>TODO: Port timeSubset and elevationSubset code here too
 *
 * @author Daniele Romagnoli - GeoSolutions
 */
public class WCSDimensionsSubsetHelper {

    public static final Set<String> TIME_NAMES = new HashSet<String>();

    public static final Set<String> ELEVATION_NAMES = new HashSet<String>();

    private static final Logger LOGGER = Logging.getLogger(WCSDimensionsHelper.class);

    private GetCoverageType request;

    private Map<String, DimensionInfo> enabledDimensions;

    private ReaderDimensionsAccessor accessor;

    private DimensionInfo timeDimension;

    private DimensionInfo elevationDimension;

    private CoordinateReferenceSystem subsettingCRS;

    private WCSEnvelope requestedEnvelope;

    private GridCoverage2DReader reader;

    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    private CoverageInfo coverageInfo;

    private GridCoverageRequest gridCoverageRequest;

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private static final WCSDimensionsValueParser PARSER = new WCSDimensionsValueParser();

    public void setGridCoverageRequest(GridCoverageRequest gridCoverageRequest) {
        this.gridCoverageRequest = gridCoverageRequest;
    }

    public GridCoverageRequest getGridCoverageRequest() {
        return gridCoverageRequest;
    }

    public CoverageInfo getCoverageInfo() {
        return coverageInfo;
    }

    public void setCoverageInfo(CoverageInfo coverageInfo) {
        this.coverageInfo = coverageInfo;
    }

    static {
        TIME_NAMES.add("t");
        TIME_NAMES.add("time");
        TIME_NAMES.add("temporal");
        TIME_NAMES.add("phenomenontime");
        ELEVATION_NAMES.add("elevation");
    }

    public WCSDimensionsSubsetHelper(
            GridCoverage2DReader reader,
            GetCoverageType request,
            CoverageInfo ci,
            CoordinateReferenceSystem subsettingCRS,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper)
            throws IOException {
        this.request = request;
        this.coverageInfo = ci;

        // Note that dimensions will be returned if existing and enabled too
        this.enabledDimensions = WCSDimensionsHelper.getDimensionsFromMetadata(ci.getMetadata());
        this.subsettingCRS = subsettingCRS;
        this.reader = reader;
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
        timeDimension = enabledDimensions.get(ResourceInfo.TIME);
        elevationDimension = enabledDimensions.get(ResourceInfo.ELEVATION);

        if (timeDimension != null || elevationDimension != null || !enabledDimensions.isEmpty()) {
            accessor = new ReaderDimensionsAccessor(reader);
        }
    }

    /** Extracts the simplified dimension name, throws exception if the dimension name is empty */
    public static String getDimensionName(DimensionSubsetType dim) {
        // get basic information
        String dimension = dim.getDimension();

        // remove common prefixes
        if (dimension.startsWith("http://www.opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://www.opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/axis/OGC/0/")) {
            dimension = dimension.substring("http://opengis.net/def/axis/OGC/0/".length());
        } else if (dimension.startsWith("http://opengis.net/def/crs/ISO/2004/")) {
            dimension = dimension.substring("http://opengis.net/def/crs/ISO/2004/".length());
        }

        // checks
        // TODO synonyms on axes labels
        if (dimension == null || dimension.length() <= 0) {
            throw new WCS20Exception(
                    "Empty/invalid axis label provided: " + dim.getDimension(),
                    WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                    "subset");
        }
        return dimension;
    }

    /**
     * This method is responsible for extracting the subsettingEvelope from the incoming request.
     */
    private WCSEnvelope extractSubsettingEnvelope() {

        // default envelope in subsettingCRS
        final CoordinateReferenceSystem sourceCRS = reader.getCoordinateReferenceSystem();
        WCSEnvelope sourceEnvelopeInSubsettingCRS = new WCSEnvelope(reader.getOriginalEnvelope());
        if (!(subsettingCRS == null || CRS.equalsIgnoreMetadata(subsettingCRS, sourceCRS))) {
            // reproject source envelope to subsetting crs for initialization
            try {
                sourceEnvelopeInSubsettingCRS =
                        new WCSEnvelope(CRS.transform(reader.getOriginalEnvelope(), subsettingCRS));
            } catch (Exception e) {
                try {
                    // see if we can get a valid restricted area using the projection handlers
                    ProjectionHandler handler =
                            ProjectionHandlerFinder.getHandler(
                                    new ReferencedEnvelope(0, 1, 0, 1, subsettingCRS),
                                    sourceCRS,
                                    true);
                    if (handler != null) {
                        ReferencedEnvelope validArea = handler.getValidAreaBounds();
                        Envelope intersection =
                                validArea.intersection(
                                        ReferencedEnvelope.reference(reader.getOriginalEnvelope()));
                        ReferencedEnvelope re = new ReferencedEnvelope(intersection, sourceCRS);
                        sourceEnvelopeInSubsettingCRS =
                                new WCSEnvelope(re.transform(subsettingCRS, true));
                    } else {
                        throw new WCS20Exception(
                                "Unable to initialize subsetting envelope",
                                WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                                subsettingCRS.toWKT(),
                                e); // TODO extract code
                    }
                } catch (Exception e2) {
                    throw new WCS20Exception(
                            "Unable to initialize subsetting envelope",
                            WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                            subsettingCRS.toWKT(),
                            e2); // TODO extract code
                }
            }
        }

        // check if we have to subset, if not let's send back the basic coverage
        final EList<DimensionSubsetType> requestedDimensions = request.getDimensionSubset();
        if (requestedDimensions == null || requestedDimensions.size() <= 0) {
            return sourceEnvelopeInSubsettingCRS;
        }

        int maxDimensions = 2 + enabledDimensions.size();
        if (requestedDimensions.size() > maxDimensions) {
            throw new WCS20Exception(
                    "Invalid number of dimensions",
                    WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                    Integer.toString(requestedDimensions.size()));
        }

        // put aside the dimensions that we have for double checking
        final List<String> axesNames =
                envelopeDimensionsMapper.getAxesNames(sourceEnvelopeInSubsettingCRS, true);
        final List<String> foundDimensions = new ArrayList<String>();

        // === parse dimensions
        // the subsetting envelope is initialized with the source envelope in subsetting CRS
        WCSEnvelope subsettingEnvelope = new WCSEnvelope(sourceEnvelopeInSubsettingCRS);

        Set<String> dimensionKeys = enabledDimensions.keySet();
        for (DimensionSubsetType dim : requestedDimensions) {
            String dimension = WCSDimensionsSubsetHelper.getDimensionName(dim);
            // skip time support
            if (WCSDimensionsSubsetHelper.TIME_NAMES.contains(dimension.toLowerCase())) {
                if (dimensionKeys.contains(ResourceInfo.TIME)) {
                    // fine, we'll parse it later
                    continue;
                } else {
                    throw new WCS20Exception(
                            "Invalid axis label provided: " + dimension,
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                            null);
                }
            }
            if (WCSDimensionsSubsetHelper.ELEVATION_NAMES.contains(dimension.toLowerCase())) {
                if (dimensionKeys.contains(ResourceInfo.ELEVATION)) {
                    // fine, we'll parse it later
                    continue;
                } else {
                    throw new WCS20Exception(
                            "Invalid axis label provided: " + dimension,
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                            null);
                }
            }

            boolean isCustomDimension = false;
            for (String dimensionKey : dimensionKeys) {
                if (dimensionKey.equalsIgnoreCase(dimension)) {
                    isCustomDimension = true;
                    break;
                }
            }
            if (isCustomDimension) {
                continue;
            }

            if (!axesNames.contains(dimension)) {
                throw new WCS20Exception(
                        "Invalid axis label provided: " + dimension,
                        WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                        dimension == null ? "Null" : dimension);
            }

            // did we already do something with this dimension?
            if (foundDimensions.contains(dimension)) {
                throw new WCS20Exception(
                        "Axis label already used during subsetting",
                        WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                        dimension);
            }
            foundDimensions.add(dimension);

            // now decide what to do
            //            final String CRS= dim.getCRS();// TODO HOW DO WE USE THIS???
            if (dim instanceof DimensionTrimType) {

                // TRIMMING
                final DimensionTrimType trim = (DimensionTrimType) dim;
                final double low = Double.parseDouble(trim.getTrimLow());
                final double high = Double.parseDouble(trim.getTrimHigh());

                final int axisIndex =
                        envelopeDimensionsMapper.getAxisIndex(
                                sourceEnvelopeInSubsettingCRS, dimension);
                if (axisIndex < 0) {
                    throw new WCS20Exception(
                            "Invalid axis provided",
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                            dimension);
                }

                // low > high && not dateline wrapping?
                if (low > high && !subsettingEnvelope.isLongitude(axisIndex)) {
                    throw new WCS20Exception(
                            "Low greater than High",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            trim.getTrimLow());
                }

                // notice how we choose the order of the axes
                subsettingEnvelope.setRange(axisIndex, low, high);
            } else if (dim instanceof DimensionSliceType) {

                // SLICING
                final DimensionSliceType slicing = (DimensionSliceType) dim;
                final String slicePointS = slicing.getSlicePoint();
                final double slicePoint = Double.parseDouble(slicePointS);

                final int axisIndex =
                        envelopeDimensionsMapper.getAxisIndex(
                                sourceEnvelopeInSubsettingCRS, dimension);
                if (axisIndex < 0) {
                    throw new WCS20Exception(
                            "Invalid axis provided",
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                            dimension);
                }
                // notice how we choose the order of the axes
                AffineTransform affineTransform =
                        RequestUtils.getAffineTransform(
                                reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER));
                final double scale =
                        axisIndex == 0 ? affineTransform.getScaleX() : -affineTransform.getScaleY();
                subsettingEnvelope.setRange(axisIndex, slicePoint, slicePoint + scale);

                // slice point outside coverage
                if (sourceEnvelopeInSubsettingCRS.getMinimum(axisIndex) > slicePoint
                        || slicePoint > sourceEnvelopeInSubsettingCRS.getMaximum(axisIndex)) {
                    throw new WCS20Exception(
                            "SlicePoint outside coverage envelope",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            slicePointS);
                }
            } else {
                throw new WCS20Exception(
                        "Invalid element found while attempting to parse dimension subsetting request",
                        WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                        dim.getClass().toString());
            }
        }

        // make sure we have not been requested to subset outside of the source CRS
        requestedEnvelope = new WCSEnvelope(subsettingEnvelope);
        subsettingEnvelope.intersect(new GeneralEnvelope(sourceEnvelopeInSubsettingCRS));

        if (subsettingEnvelope.isEmpty()) {
            throw new WCS20Exception(
                    "Empty intersection after subsetting",
                    WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                    ""); // TODO spit our
            // envelope trimmed
        }

        // return the subsetting envelope in the CRS it was specified into, to
        // allow projection handlers to handle dateline crossing
        return subsettingEnvelope;

        //
        // intersect with original envelope to make sure the subsetting is valid
        //
        // GeneralEnvelope sourceEnvelope = reader.getOriginalEnvelope();

        // reproject envelope  to native crs for cropping
        // try {
        // if(!CRS.equalsIgnoreMetadata(subsettingEnvelope.getCoordinateReferenceSystem(),
        // reader.getOriginalEnvelope())){
        // // look for transform
        // if (!CRS.equalsIgnoreMetadata(subsettingCRS, sourceCRS)) {
        // final GeneralEnvelope subsettingEnvelopeInSourceCRS = CRS.transform(
        // subsettingEnvelope, sourceCRS);
        //
        // // intersect
        // subsettingEnvelopeInSourceCRS.intersect(sourceEnvelope);
        //
        // // provided trim extent does not intersect coverage envelope
        // if (subsettingEnvelopeInSourceCRS.isEmpty()) {
        // throw new WCS20Exception(
        // "Empty intersection after subsetting",
        // WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,"");// TODO spit our envelope trimmed
        // }
        // return new WCSEnvelope(subsettingEnvelopeInSourceCRS);
        // }
        // }
        // // we are reprojecting
        // subsettingEnvelope.intersect(sourceEnvelope);
        //
        // // provided trim extent does not intersect coverage envelope
        // if(subsettingEnvelope.isEmpty()){
        // throw new WCS20Exception(
        // "Empty intersection after subsetting",
        // WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,"");// TODO spit our envelope trimmed
        // }
        // return new WCSEnvelope(subsettingEnvelope);
        // } catch (TransformException e) {
        // final WCS20Exception exception= new WCS20Exception(
        // "Unable to initialize subsetting envelope",
        // WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
        // subsettingCRS.toWKT()); // TODO extract code
        // exception.initCause(e);
        // throw exception;
        // }

    }

    /** Parses a date range out of the dimension subsetting directives */
    private DateRange extractTemporalSubset() throws IOException {
        DateRange timeSubset = null;
        if (timeDimension != null) {
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = WCSDimensionsSubsetHelper.getDimensionName(dim);

                // only care for time dimensions
                if (!TIME_NAMES.contains(dimension.toLowerCase())) {
                    continue;
                }

                // did we parse the range already?
                if (timeSubset != null) {
                    throw new WCS20Exception(
                            "Time dimension trimming/slicing specified twice in the request",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "subset");
                }

                // now decide what to do
                if (dim instanceof DimensionTrimType) {

                    // TRIMMING
                    final DimensionTrimType trim = (DimensionTrimType) dim;
                    final Date low = PARSER.parseDateTime(trim.getTrimLow());
                    final Date high = PARSER.parseDateTime(trim.getTrimHigh());

                    // low > high???
                    if (low.compareTo(high) > 0) {
                        throw new WCS20Exception(
                                "Low greater than High: "
                                        + trim.getTrimLow()
                                        + ", "
                                        + trim.getTrimHigh(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                                "subset");
                    }

                    timeSubset = new DateRange(low, high);
                } else if (dim instanceof DimensionSliceType) {

                    // SLICING
                    final DimensionSliceType slicing = (DimensionSliceType) dim;
                    final String slicePointS = slicing.getSlicePoint();
                    final Date slicePoint = PARSER.parseDateTime(slicePointS);
                    timeSubset = new DateRange(slicePoint, slicePoint);
                } else {
                    throw new WCS20Exception(
                            "Invalid element found while attempting to parse dimension subsetting request: "
                                    + dim.getClass().toString(),
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "subset");
                }
            }

            // right now we don't support trimming
            // TODO: revisit when we have some multidimensional output support
            if (!(reader instanceof StructuredGridCoverage2DReader)
                    && timeSubset != null
                    && !timeSubset.getMinValue().equals(timeSubset.getMaxValue())) {
                throw new WCS20Exception(
                        "Trimming on time is not supported at the moment on not StructuredGridCoverage2DReaders, only slicing is");
            }

            // apply nearest neighbor matching on time
            if (timeSubset != null && timeSubset.getMinValue().equals(timeSubset.getMaxValue())) {
                timeSubset = interpolateTime(timeSubset, accessor);
            }
        }
        return timeSubset;
    }

    /** Nearest interpolation against time */
    private DateRange interpolateTime(DateRange timeSubset, ReaderDimensionsAccessor accessor)
            throws IOException {
        TreeSet<Object> domain = accessor.getTimeDomain();
        Date slicePoint = timeSubset.getMinValue();
        if (!domainContainsPoint(slicePoint, domain)) {
            // look for the closest time
            Date previous = null;
            Date newSlicePoint = null;
            // for NN matching we don't need the ranges, NN against their extrema will be fine
            TreeSet<Date> domainDates = getDomainDates(domain);
            for (Date curr : domainDates) {
                if (curr.compareTo(slicePoint) > 0) {
                    if (previous == null) {
                        newSlicePoint = curr;
                        break;
                    } else {
                        long diffPrevious = slicePoint.getTime() - previous.getTime();
                        long diffCurr = curr.getTime() - slicePoint.getTime();
                        if (diffCurr > diffPrevious) {
                            newSlicePoint = curr;
                            break;
                        } else {
                            newSlicePoint = previous;
                            break;
                        }
                    }
                } else {
                    previous = curr;
                }
            }

            if (newSlicePoint == null) {
                newSlicePoint = previous;
            }
            timeSubset = new DateRange(newSlicePoint, newSlicePoint);
        }
        return timeSubset;
    }

    /** Get the domain set as a set of dates. */
    private TreeSet<Date> getDomainDates(TreeSet<Object> domain) {
        TreeSet<Date> results = new TreeSet<Date>();
        for (Object item : domain) {
            if (item instanceof Date) {
                Date date = (Date) item;
                results.add(date);
            } else if (item instanceof DateRange) {
                DateRange range = (DateRange) item;
                results.add(range.getMinValue());
                results.add(range.getMaxValue());
            }
        }
        return results;
    }

    public WCSEnvelope getRequestedEnvelope() {
        return requestedEnvelope;
    }

    /**
     * Check whether the provided domain contains the specified slicePoint.
     *
     * @param slicePoint the point to be checked (a Date or a Number)
     * @param domain the domain to be scan for containment.
     */
    private boolean domainContainsPoint(final Object slicePoint, final TreeSet<Object> domain) {
        // cannot use this...
        //        if(domain.contains(slicePoint)) {
        //            return true;
        //        }

        // check date ranges for containment
        if (slicePoint instanceof Date) {
            Date sliceDate = (Date) slicePoint;
            for (Object curr : domain) {
                if (curr instanceof Date) {
                    Date date = (Date) curr;
                    int result = date.compareTo(sliceDate);
                    if (result > 0) {
                        return false;
                    } else if (result == 0) {
                        return true;
                    }
                } else if (curr instanceof DateRange) {
                    DateRange range = (DateRange) curr;
                    if (range.contains(sliceDate)) {
                        return true;
                    } else if (range.getMaxValue().compareTo(sliceDate) < 0) {
                        return false;
                    }
                }
            }
        } else if (slicePoint instanceof Number) {
            // TODO: Should we check for other data types?
            Number sliceNumber = (Number) slicePoint;
            for (Object curr : domain) {
                if (curr instanceof Number) {
                    Double num = (Double) curr;
                    int result = num.compareTo((Double) sliceNumber);
                    if (result > 0) {
                        return false;
                    } else if (result == 0) {
                        return true;
                    }
                } else if (curr instanceof NumberRange) {
                    NumberRange range = (NumberRange) curr;
                    if (range.contains(sliceNumber)) {
                        return true;
                    } else if (range.getMaxValue().compareTo(sliceNumber) < 0) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /** Parses a number range out of the dimension subsetting directives */
    private NumberRange extractElevationSubset() throws IOException {
        NumberRange elevationSubset = null;
        if (elevationDimension != null) {
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = WCSDimensionsSubsetHelper.getDimensionName(dim);

                // only care for elevation dimensions
                if (!WCSDimensionsSubsetHelper.ELEVATION_NAMES.contains(dimension.toLowerCase())) {
                    continue;
                }

                // did we parse the range already?
                if (elevationSubset != null) {
                    throw new WCS20Exception(
                            "Elevation dimension trimming/slicing specified twice in the request",
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "subset");
                }

                // now decide what to do
                if (dim instanceof DimensionTrimType) {

                    // TRIMMING
                    final DimensionTrimType trim = (DimensionTrimType) dim;
                    final Double low = PARSER.parseDouble(trim.getTrimLow());
                    final Double high = PARSER.parseDouble(trim.getTrimHigh());

                    // low > high???
                    if (low > high) {
                        throw new WCS20Exception(
                                "Low greater than High: "
                                        + trim.getTrimLow()
                                        + ", "
                                        + trim.getTrimHigh(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                                "subset");
                    }

                    elevationSubset = new NumberRange<Double>(Double.class, low, high);
                } else if (dim instanceof DimensionSliceType) {

                    // SLICING
                    final DimensionSliceType slicing = (DimensionSliceType) dim;
                    final String slicePointS = slicing.getSlicePoint();
                    final Double slicePoint = PARSER.parseDouble(slicePointS);

                    elevationSubset = new NumberRange<Double>(Double.class, slicePoint, slicePoint);
                } else {
                    throw new WCS20Exception(
                            "Invalid element found while attempting to parse dimension subsetting request: "
                                    + dim.getClass().toString(),
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "subset");
                }
            }

            // right now we don't support trimming
            // TODO: revisit when we have some multidimensional output support
            if (!(reader instanceof StructuredGridCoverage2DReader)
                    && elevationSubset != null
                    && !elevationSubset.getMinValue().equals(elevationSubset.getMaxValue())) {
                throw new WCS20Exception(
                        "Trimming on elevation is not supported at the moment on not StructuredGridCoverage2DReaders, only slicing is");
            }

            // apply nearest neighbor matching on elevation
            if (elevationSubset != null
                    && elevationSubset.getMinValue().equals(elevationSubset.getMaxValue())) {
                interpolateElevation(elevationSubset, accessor);
            }
        }
        return elevationSubset;
    }

    /** Nearest interpolation on elevation */
    private NumberRange interpolateElevation(
            NumberRange elevationSubset, ReaderDimensionsAccessor accessor) throws IOException {
        TreeSet<Object> domain = accessor.getElevationDomain();
        Double slicePoint = elevationSubset.getMinimum();
        if (!domainContainsPoint(slicePoint, domain)) {
            // look for the closest elevation
            Double previous = null;
            Double newSlicePoint = null;
            // for NN matching we don't need the range, NN against their extrema will be fine
            TreeSet<Double> domainDates = PARSER.getDomainNumber(domain);
            for (Double curr : domainDates) {
                if (curr.compareTo(slicePoint) > 0) {
                    if (previous == null) {
                        newSlicePoint = curr;
                        break;
                    } else {
                        double diffPrevious = slicePoint - previous;
                        double diffCurr = curr - slicePoint;
                        if (diffCurr > diffPrevious) {
                            newSlicePoint = curr;
                            break;
                        } else {
                            newSlicePoint = previous;
                            break;
                        }
                    }
                } else {
                    previous = curr;
                }
            }
            if (newSlicePoint == null) {
                newSlicePoint = previous;
            }
            elevationSubset = new NumberRange<Double>(Double.class, newSlicePoint, newSlicePoint);
        }
        return elevationSubset;
    }

    /** Extract custom dimension subset from the current helper */
    private Map<String, List<Object>> extractDimensionsSubset() throws IOException {
        Map<String, List<Object>> dimensionSubset = new HashMap<String, List<Object>>();

        if (enabledDimensions != null && !enabledDimensions.isEmpty()) {
            Set<String> dimensionKeys = enabledDimensions.keySet();
            for (DimensionSubsetType dim : request.getDimensionSubset()) {
                String dimension = getDimensionName(dim);

                if (WCSDimensionsSubsetHelper.ELEVATION_NAMES.contains(dimension.toLowerCase())
                        || WCSDimensionsSubsetHelper.TIME_NAMES.contains(dimension.toLowerCase())) {
                    continue;
                }

                // only care for custom dimensions
                if (dimensionKeys.stream().anyMatch(dimension::equalsIgnoreCase)) {
                    dimension = dimension.toUpperCase(); // using uppercase with imagemosaic
                    List<Object> selectedValues = new ArrayList<Object>();

                    // now decide what to do
                    if (dim instanceof DimensionTrimType) {

                        // TRIMMING
                        final DimensionTrimType trim = (DimensionTrimType) dim;
                        setSubsetRangeValue(
                                dimension, trim.getTrimLow(), trim.getTrimHigh(), selectedValues);

                    } else if (dim instanceof DimensionSliceType) {

                        // SLICING
                        final DimensionSliceType slicing = (DimensionSliceType) dim;
                        setSubsetValue(dimension, slicing.getSlicePoint(), selectedValues);

                    } else {
                        throw new WCS20Exception(
                                "Invalid element found while attempting to parse dimension subsetting request: "
                                        + dim.getClass().toString(),
                                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                                "subset");
                    }

                    // TODO: Deal with default values
                    dimensionSubset.put(dimension, selectedValues);
                }
            }
        }
        return dimensionSubset;
    }

    /**
     * Set the trim value as proper object (by checking whether the domainDatatype metadata exists
     * or by try multiple parsing until one is successfull)
     *
     * @param dimensionName the name of the dimension to be set
     */
    private void setSubsetRangeValue(
            String dimensionName, String low, String high, List<Object> selectedValues)
            throws IOException {
        boolean sliceSet = false;

        String domainDatatype = accessor.getDomainDatatype(dimensionName);
        if (domainDatatype != null) {
            PARSER.setRangeValues(low, high, selectedValues, domainDatatype);
        } else {
            // Try with recursive settings

            // Try setting the value as a time
            if (!sliceSet) {
                sliceSet = PARSER.setAsDateRange(low, high, selectedValues);
            }

            // Try setting the value as an integer
            if (!sliceSet) {
                sliceSet = PARSER.setAsIntegerRange(low, high, selectedValues);
            }

            // Try setting the value as a double
            if (!sliceSet) {
                sliceSet = PARSER.setAsDoubleRange(low, high, selectedValues);
            }

            if (!sliceSet) {
                // Setting it as a String
                selectedValues.add(low + "/" + high); // Check That
            }
        }
    }

    /**
     * Set the slice value as proper object (by checking whether the domainDatatype metadata exists
     * or by try multiple parsing until one is successfull)
     *
     * @param dimensionName the name of the dimension to be set
     */
    private void setSubsetValue(
            String dimensionName, String slicePoint, List<Object> selectedValues)
            throws IOException {
        boolean sliceSet = false;

        String domainDatatype = accessor.getDomainDatatype(dimensionName);
        if (domainDatatype != null) {
            PARSER.setValues(slicePoint, selectedValues, domainDatatype);
        } else {
            // Try with recursive settings

            // Try setting the value as a time
            if (!sliceSet) {
                sliceSet = PARSER.setAsDate(slicePoint, selectedValues);
            }

            // Try setting the value as an integer
            if (!sliceSet) {
                sliceSet = PARSER.setAsInteger(slicePoint, selectedValues);
            }

            // Try setting the value as a double
            if (!sliceSet) {
                sliceSet = PARSER.setAsDouble(slicePoint, selectedValues);
            }

            if (!sliceSet) {
                // Setting it as a String
                selectedValues.add(slicePoint);
            }
        }
    }

    /** Return a {@link GridCoverageRequest} instance containing specified subsetting dimensions. */
    public GridCoverageRequest createGridCoverageRequestSubset() throws IOException {
        final WCSEnvelope spatialSubset = extractSubsettingEnvelope();
        assert spatialSubset != null && !spatialSubset.isEmpty();

        Map<String, List<Object>> dimensionsSubset = null;
        DateRange temporalSubset = null;
        NumberRange elevationSubset = null;

        // Parse specified subset values (if any)
        if (enabledDimensions != null && !enabledDimensions.isEmpty()) {
            // extract temporal subsetting
            temporalSubset = extractTemporalSubset();

            // extract elevation subsetting
            elevationSubset = extractElevationSubset();

            // extract dimensions subsetting
            dimensionsSubset = extractDimensionsSubset();
        }

        // Prepare subsetting request
        GridCoverageRequest subsettingRequest = new GridCoverageRequest();
        subsettingRequest.setSpatialSubset(spatialSubset);
        subsettingRequest.setElevationSubset(elevationSubset);
        subsettingRequest.setTemporalSubset(temporalSubset);
        subsettingRequest.setDimensionsSubset(dimensionsSubset);
        subsettingRequest.setFilter(request.getFilter());
        subsettingRequest.setSortBy(request.getSortBy());

        // Handle default values and update subsetting values if needed
        String coverageName = getCoverageName();
        // TODO consider dealing with the Format instance instead of a String parsing or check
        // against WCSUtils.isSupportedMDOutputFormat(String).
        if (!GetCoverage.formatSupportMDOutput(request.getFormat())) {

            // Right now, only a few formats support multidimensional output.
            // Let's use default values for the others.
            WCSDefaultValuesHelper defaultValuesHelper =
                    new WCSDefaultValuesHelper(reader, accessor, request, coverageName);
            defaultValuesHelper.setDefaults(subsettingRequest);
        }

        return subsettingRequest;
    }

    /**
     * Return the coverageName for the underlying {@link CoverageInfo} by accessing the
     * nativeCoverageName if available. Since the nativeCoverageName may be null for single coverage
     * formats we get the first grid coverage name from the reader as backup.
     */
    private String getCoverageName() throws IOException {
        final String nativeName = coverageInfo.getNativeCoverageName();
        return (nativeName != null ? nativeName : reader.getGridCoverageNames()[0]);
    }

    /**
     * Split the current GridCoverageRequest by creating a list of new GridCoverageRequests: A query
     * will be performed with the current specified subsets, returning N granules (if any). Then new
     * N GridCoverageRequests will be created (one for each granule) having subsets setup on top of
     * the specific values of the dimensions for that N-th granule.
     *
     * <p>This method only works for StructuredGridCoverage2DReaders
     *
     * @return a List of new {@link GridCoverageRequest}s
     */
    public List<GridCoverageRequest> splitRequest()
            throws UnsupportedOperationException, IOException, MismatchedDimensionException,
                    TransformException, FactoryException {
        StructuredGridCoverage2DReader structuredReader = null;
        if (reader instanceof StructuredGridCoverage2DReader) {
            structuredReader = (StructuredGridCoverage2DReader) reader;
        } else {
            throw new IllegalArgumentException(
                    "The method is only supported for StructuredGridCoverage2DReaders");
        }

        // Getting the granule source
        final String coverageName = getCoverageName();

        final GranuleSource source = structuredReader.getGranules(coverageName, true);
        if (source == null) {
            throw new IllegalArgumentException("No granule source available for that coverageName");
        }

        // Preparing a query containing all the specified dimensions.
        // This will allow to get back only the granules respecting the specified request
        final Query query =
                prepareDimensionsQuery(structuredReader, coverageName, gridCoverageRequest, source);

        // Getting the granules for that query; Loop over the granules to create subRequest with
        // single elements dimensions sets
        final SimpleFeatureCollection collection = source.getGranules(query);
        final SimpleFeatureIterator iterator = collection.features();
        final List<GridCoverageRequest> requests = new ArrayList<GridCoverageRequest>();
        try {
            while (iterator.hasNext()) {
                final SimpleFeature feature = iterator.next();

                // Prepare subRequest setting up dimensions matching the values of the current
                // granule
                final GridCoverageRequest subRequest = new GridCoverageRequest();

                // Setting up constant elements (outputCRS, spatial subset, interpolation
                subRequest.setOutputCRS(gridCoverageRequest.getOutputCRS());
                subRequest.setSpatialInterpolation(gridCoverageRequest.getSpatialInterpolation());
                subRequest.setSpatialSubset(gridCoverageRequest.getSpatialSubset());
                subRequest.setTemporalInterpolation(gridCoverageRequest.getTemporalInterpolation());

                // Setting up specific dimensions subset
                updateDimensions(subRequest, feature, structuredReader, coverageName);
                requests.add(subRequest);
            }
        } finally {
            iterator.close();
        }
        return requests;
    }

    public Set<GridCoverageRequest> splitRequestToSet()
            throws MismatchedDimensionException, UnsupportedOperationException, IOException,
                    TransformException, FactoryException {
        List<GridCoverageRequest> list = splitRequest();
        Set<GridCoverageRequest> set = new HashSet<GridCoverageRequest>();
        for (GridCoverageRequest request : list) {
            set.add(request);
        }
        return set;
    }

    /**
     * Update the subset (temporal, vertical, custom) of the request by inspecting the reader
     * DimensionsDescriptor and collecting proper values from the current feature.
     *
     * @param subRequest the subRequest to be updated with subsets
     * @param feature the current feature containing dimensions value to be used for the subsetting
     * @param reader the reader to be used for the inspection.
     * @param coverageName the name of the coverage.
     */
    private void updateDimensions(
            final GridCoverageRequest subRequest,
            final SimpleFeature feature,
            final StructuredGridCoverage2DReader reader,
            final String coverageName) {

        // ----------------------------------
        // Updating temporal dimension subset
        // ----------------------------------
        String startTimeAttribute = null;
        String endTimeAttribute = null;
        DimensionDescriptor timeDescriptor =
                WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, "TIME");

        if (timeDescriptor != null) {
            startTimeAttribute = timeDescriptor.getStartAttribute();
            endTimeAttribute = timeDescriptor.getEndAttribute();
            Date startDate = (Date) feature.getAttribute(startTimeAttribute);
            Date endDate =
                    (endTimeAttribute != null)
                            ? (Date) feature.getAttribute(endTimeAttribute)
                            : startDate;
            DateRange range = new DateRange(startDate, endDate);
            subRequest.setTemporalSubset(range);
        }

        // ----------------------------------
        // Updating vertical dimension subset
        // ----------------------------------
        String startElevationAttribute = null;
        String endElevationAttribute = null;
        DimensionDescriptor elevationDescriptor =
                WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, "ELEVATION");
        if (elevationDescriptor != null) {
            startElevationAttribute = elevationDescriptor.getStartAttribute();
            endElevationAttribute = elevationDescriptor.getEndAttribute();
            Number startValue = (Number) feature.getAttribute(startElevationAttribute);
            Number endValue =
                    (endElevationAttribute != null)
                            ? (Number) feature.getAttribute(endElevationAttribute)
                            : startValue;
            NumberRange range = new NumberRange(startValue.getClass(), startValue, endValue);
            subRequest.setElevationSubset(range);
        }

        // ---------------------------------
        // Updating custom dimensions subset
        // ---------------------------------
        List<String> customDomains =
                (List<String>)
                        (accessor != null ? accessor.getCustomDomains() : Collections.emptyList());
        Map<String, List<Object>> dimensionsSubset = new HashMap<String, List<Object>>();
        for (String customDomain : customDomains) {
            String startAttribute = null;
            String endAttribute = null;
            DimensionDescriptor descriptor =
                    WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, customDomain);
            if (descriptor != null) {
                startAttribute = descriptor.getStartAttribute();
                endAttribute = descriptor.getEndAttribute();

                Object value = feature.getAttribute(startAttribute);
                if (endAttribute != null) {
                    Object endValue = feature.getAttribute(endAttribute);
                    Class objectClass = endValue.getClass();
                    String classDataType = objectClass.toString();
                    if (classDataType.endsWith("Timestamp")) {
                        value =
                                new DateRange(
                                        new Date(((Timestamp) value).getTime()),
                                        new Date(((Timestamp) endValue).getTime()));
                    } else if (classDataType.endsWith("Date")) {
                        value = new DateRange((Date) value, (Date) endValue);
                    } else {
                        value = new NumberRange(objectClass, (Number) value, (Number) endValue);
                    }
                }
                List<Object> dimensionValues = new ArrayList<Object>();
                dimensionValues.add(value);
                dimensionsSubset.put(descriptor.getName().toUpperCase(), dimensionValues);
            }
        }
        subRequest.setDimensionsSubset(dimensionsSubset);
    }

    /**
     * Prepare a query by inspecting the specified dimensions and setting the proper attribute
     * values
     */
    private Query prepareDimensionsQuery(
            final StructuredGridCoverage2DReader reader,
            final String coverageName,
            final GridCoverageRequest gcr,
            final GranuleSource source)
            throws UnsupportedOperationException, IOException, MismatchedDimensionException,
                    TransformException, FactoryException {

        // spatial subset
        Filter filter = filterSpatial(gcr, reader, source);

        // temporal subset
        filter = filterTime(filter, gcr, coverageName, reader);

        // elevation subset
        filter = filterElevation(filter, gcr, coverageName, reader);

        // dimensionsSubset
        filter = filterDimensions(filter, gcr, coverageName, reader);

        Query query = new Query(null, filter);
        return query;
    }

    private Filter filterSpatial(
            GridCoverageRequest gcr, StructuredGridCoverage2DReader reader, GranuleSource source)
            throws IOException, MismatchedDimensionException, TransformException, FactoryException {
        WCSEnvelope envelope = gcr.getSpatialSubset();
        Polygon llPolygon = JTS.toGeometry(new ReferencedEnvelope(envelope));
        GeometryDescriptor geom = source.getSchema().getGeometryDescriptor();
        PropertyName geometryProperty = ff.property(geom.getLocalName());
        Geometry nativeCRSPolygon =
                JTS.transform(
                        llPolygon,
                        CRS.findMathTransform(
                                envelope.getCoordinateReferenceSystem(),
                                reader.getCoordinateReferenceSystem()));
        Literal polygonLiteral = ff.literal(nativeCRSPolygon);
        //                    if(overlaps) {
        return ff.intersects(geometryProperty, polygonLiteral);
        //                    } else {
        //                        filter = ff.within(geometryProperty, polygonLiteral);
        //                    }
    }

    /**
     * Update the filter with a vertical Filter in case the current {@link GridCoverageRequest} has
     * an elevation subset.
     */
    private Filter filterElevation(
            Filter filter,
            GridCoverageRequest gcr,
            String coverageName,
            StructuredGridCoverage2DReader reader) {
        NumberRange elevationRange = gcr.getElevationSubset();
        String startElevation = null;
        String endElevation = null;
        DimensionDescriptor elevationDescriptor = null;
        Filter elevationFilter = filter;
        if (elevationRange != null && filter != Filter.EXCLUDE) {
            elevationDescriptor =
                    WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, "ELEVATION");
            startElevation = elevationDescriptor.getStartAttribute();
            endElevation = elevationDescriptor.getEndAttribute();
            elevationFilter =
                    filter(
                            startElevation,
                            endElevation,
                            elevationRange.getMinValue(),
                            elevationRange.getMaxValue(),
                            filter);
        }
        return elevationFilter;
    }

    /**
     * Update the filter with a temporal Filter in case the current {@link GridCoverageRequest} has
     * a temporal subset.
     */
    private Filter filterTime(
            Filter filter,
            GridCoverageRequest gcr,
            String coverageName,
            StructuredGridCoverage2DReader reader) {
        DateRange timeRange = gcr.getTemporalSubset();
        DimensionDescriptor timeDescriptor = null;
        String startTime = null;
        String endTime = null;
        Filter timeFilter = filter;
        if (timeRange != null && filter != Filter.EXCLUDE) {
            timeDescriptor =
                    WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, "TIME");
            startTime = timeDescriptor.getStartAttribute();
            endTime = timeDescriptor.getEndAttribute();
            timeFilter =
                    filter(
                            startTime,
                            endTime,
                            timeRange.getMinValue(),
                            timeRange.getMaxValue(),
                            filter);
        }
        return timeFilter;
    }

    private Filter filterDimensions(
            Filter filter,
            GridCoverageRequest gcr,
            String coverageName,
            StructuredGridCoverage2DReader reader) {
        Map<String, List<Object>> subset = gcr.getDimensionsSubset();
        Filter dimensionsFilter = filter;
        if (subset != null && !subset.isEmpty()) {
            Set<String> dimensions = subset.keySet();
            Iterator<String> dimensionsIt = dimensions.iterator();
            // Filtering over the dimensions
            while (dimensionsIt.hasNext()) {
                final String dimensionName = dimensionsIt.next();
                List<Object> dimensionValues = subset.get(dimensionName);
                if (dimensionValues == null || dimensionValues.isEmpty()) {
                    continue;
                }
                DimensionDescriptor dimensionDescriptor =
                        WCSDimensionsHelper.getDimensionDescriptor(
                                reader, coverageName, dimensionName);
                if (dimensionDescriptor != null) {
                    final String startAttrib = dimensionDescriptor.getStartAttribute();
                    final String endAttrib = dimensionDescriptor.getEndAttribute();
                    dimensionsFilter =
                            filterDimension(
                                    startAttrib, endAttrib, dimensionValues, dimensionsFilter);

                } else {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(
                                "The specified dimension "
                                        + dimensionName
                                        + "has no descriptors in the reader. Skipping it");
                        continue;
                    }
                }
            }
        }
        return dimensionsFilter;
    }

    /** */
    private Filter filterDimension(
            String startAttribute,
            String endAttribute,
            List<Object> dimensionValues,
            Filter filter) {
        Filter localFilter = null;

        if (dimensionValues != null && !dimensionValues.isEmpty()) {
            // Note that currently, dimensionValues only contain one element (slicing specifies a
            // single value for a dimension)
            Object element = dimensionValues.get(0);
            Object min = null;
            Object max = null;
            if (element instanceof DateRange) {
                DateRange dateRange = (DateRange) element;
                min = dateRange.getMinValue();
                max = dateRange.getMaxValue();
            } else if (element instanceof NumberRange) {
                NumberRange numberRange = (NumberRange) element;
                min = numberRange.getMinValue();
                max = numberRange.getMaxValue();
            } else if (element instanceof Date
                    || element instanceof Number
                    || element instanceof String) {
                min = element;
                max = element;
            } else {
                throw new IllegalArgumentException("Unsupported object type");
            }
            if (endAttribute == null) {
                // single value time
                localFilter =
                        ff.between(ff.property(startAttribute), ff.literal(min), ff.literal(max));
            } else {
                // range value, we need to account for containment then
                Filter f1 = ff.lessOrEqual(ff.property(startAttribute), ff.literal(max));
                Filter f2 = ff.greaterOrEqual(ff.property(endAttribute), ff.literal(min));
                localFilter = ff.and(Arrays.asList(f1, f2));
            }
            if (filter == null) {
                filter = localFilter;
            } else {
                filter = ff.and(filter, localFilter);
            }
        }
        return filter;
    }

    /** Setup an intersection filter */
    private Filter filter(
            String startAttribute,
            String endAttribute,
            Comparable minValue,
            Comparable maxValue,
            Filter filter) {
        Filter localFilter = null;
        if (endAttribute == null) {
            // single value time
            localFilter =
                    ff.between(
                            ff.property(startAttribute),
                            ff.literal(minValue),
                            ff.literal(maxValue));
        } else {
            // range value, we need to account for containment then
            Filter f1 = ff.lessOrEqual(ff.property(startAttribute), ff.literal(maxValue));
            Filter f2 = ff.greaterOrEqual(ff.property(endAttribute), ff.literal(minValue));
            localFilter = ff.and(Arrays.asList(f1, f2));
        }
        if (filter == null) {
            filter = localFilter;
        } else {
            filter = ff.and(filter, localFilter);
        }

        return filter;
    }

    /** Prepare the DimensionBean list for this reader */
    public List<DimensionBean> setupDimensions() throws IOException {
        StructuredGridCoverage2DReader structuredReader = null;
        if (reader instanceof StructuredGridCoverage2DReader) {
            structuredReader = (StructuredGridCoverage2DReader) reader;
        } else {
            // TODO: only structuredGridCoverage2DReaders are currently supported.
            throw new UnsupportedOperationException(
                    "Only structuredGridCoverage2DReaders are currently supported");
        }
        List<DimensionBean> dimensions = new ArrayList<DimensionBean>();
        if (accessor == null) {
            return dimensions;
        }
        List<String> customDimensions =
                (List<String>)
                        (accessor != null ? accessor.getCustomDomains() : Collections.emptyList());

        // Put custom dimensions as first
        for (String customDimension : customDimensions) {
            dimensions.add(setupDimensionBean(structuredReader, customDimension));
        }
        // Put known dimensions afterwards similarly to what COARDS convention suggest: 1) Time ->
        // 2) Elevation
        DimensionBean timeD = setupDimensionBean(structuredReader, "TIME");
        if (timeD != null) {
            dimensions.add(timeD);
        }
        DimensionBean elevationD = setupDimensionBean(structuredReader, "ELEVATION");
        if (elevationD != null) {
            dimensions.add(elevationD);
        }

        return dimensions;
    }

    /**
     * Setup a {@link DimensionBean} instance for the specified dimensionID, extracting it from the
     * provided {@link StructuredGridCoverage2DReader}
     *
     * @param structuredReader the reader used to retrieve dimensionDescriptor and metadata
     * @param dimensionID the ID of the dimension to be setup
     */
    private DimensionBean setupDimensionBean(
            StructuredGridCoverage2DReader structuredReader, String dimensionID)
            throws IOException {
        Utilities.ensureNonNull("structuredReader", structuredReader);
        // Retrieve the proper dimension descriptor
        final String coverageName = getCoverageName();
        final DimensionDescriptor descriptor =
                WCSDimensionsHelper.getDimensionDescriptor(
                        structuredReader, coverageName, dimensionID);
        if (descriptor == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Unable to find a valid descriptor for the specified dimension ID: "
                                + dimensionID
                                + " for the specified coverage "
                                + coverageName
                                + "\n Returning no DimensionBean");
            }
            return null;
        }
        final String dimensionName = descriptor.getName();
        final DimensionType dimensionType =
                dimensionID.equalsIgnoreCase("TIME")
                        ? DimensionType.TIME
                        : dimensionID.equalsIgnoreCase("ELEVATION")
                                ? DimensionType.ELEVATION
                                : DimensionType.CUSTOM;
        final DimensionInfo info = enabledDimensions.get(dimensionID);
        String units = null;
        String symbol = null;
        if (info != null) {
            units = info.getUnits();
            symbol = info.getUnitSymbol();
        }
        // Fallback... set units and symbol from descriptor in case dimensions are not available.
        if (units == null) {
            units = descriptor.getUnits();
        }
        if (symbol == null) {
            symbol = descriptor.getUnitSymbol();
        }
        return new DimensionBean(
                dimensionName,
                units,
                symbol,
                accessor.getDomainDatatype(dimensionName),
                dimensionType,
                descriptor.getEndAttribute() != null);
    }

    /**
     * Add an entry in the coverage properties map, containing the value of the specified
     * coverageDimension
     *
     * @param coverageRequest a {@link GridCoverageRequest} containing single subsettings for the
     *     current coverage
     */
    public void setCoverageDimensionProperty(
            Map properties, GridCoverageRequest coverageRequest, DimensionBean coverageDimension) {
        Utilities.ensureNonNull("properties", properties);
        Utilities.ensureNonNull("coverageDimension", coverageDimension);
        final DimensionType dimensionType = coverageDimension.getDimensionType();
        Object value = null;
        switch (dimensionType) {
            case TIME:
                value = coverageRequest.getTemporalSubset();
                break;
            case ELEVATION:
                value = coverageRequest.getElevationSubset();
                break;
            case CUSTOM:
                Map<String, List<Object>> dimensionsSubset = coverageRequest.getDimensionsSubset();
                List<Object> elements =
                        dimensionsSubset == null
                                ? null
                                : dimensionsSubset.get(coverageDimension.getName().toUpperCase());
                if (elements == null) {
                    throw new IllegalArgumentException("No dimension subset has been found");
                }
                if (elements.size() > 1) {
                    throw new UnsupportedOperationException(
                            "Multiple elements in additional dimensions are not supported on splitted requests");
                }
                value = elements.get(0);
                break;
        }
        properties.put(coverageDimension.getName(), value);
    }
}
