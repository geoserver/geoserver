/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import java.util.stream.Collectors;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.WarpAffine;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.InterpolationAxesType;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationMethodType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.RangeIntervalType;
import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import net.opengis.wcs20.ScalingType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionCustomizerReader.GridCoverageWrapper;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.GranuleStackImpl;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.response.WCSDimensionsHelper;
import org.geoserver.wcs2_0.response.WCSDimensionsSubsetHelper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Mosaic;
import org.geotools.coverage.processing.operation.Mosaic.GridGeometryPolicy;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.geotools.referencing.operation.projection.Mercator;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.geotools.util.Utilities;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Implementation of the WCS 2.0.1 GetCoverage request
 *
 * @author Andrea Aime - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 * @author Daniele Romagnoli, GeoSolutions
 */
public class GetCoverage {

    private static final Hints HINTS = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    private static final Set<String> mdFormats;

    private static final CoverageProcessor processor = CoverageProcessor.getInstance(HINTS);

    static {
        // TODO: This one should be pluggable through Extensions
        mdFormats = new HashSet<String>();
        mdFormats.add("application/x-netcdf");
        mdFormats.add("application/x-netcdf4");
    }

    /** Logger. */
    private static Logger LOGGER = Logging.getLogger(GetCoverage.class);

    private WCSInfo wcs;

    private Catalog catalog;

    /** Utility class to map envelope dimension */
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    /** Factory used to create new coverages */
    private GridCoverageFactory gridCoverageFactory;

    private MIMETypeMapper mimeMapper;

    public static final String SRS_STARTER = "http://www.opengis.net/def/crs/EPSG/0/";

    /** Hints to indicate that a scale has been pre-applied, reporting the scaling factors */
    public static Hints.Key PRE_APPLIED_SCALE = new Hints.Key(Double[].class);

    private static final double EPS = 1e-6;

    public GetCoverage(
            WCSInfo serviceInfo,
            Catalog catalog,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper,
            MIMETypeMapper mimeMapper) {
        this.wcs = serviceInfo;
        this.catalog = catalog;
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
        this.mimeMapper = mimeMapper;
        this.gridCoverageFactory =
                CoverageFactoryFinder.getGridCoverageFactory(GeoTools.getDefaultHints());
    }

    /**
     * Return true in case the specified format supports Multidimensional Output TODO: Consider
     * adding a method to CoverageResponseDelegate returning this information
     */
    public static boolean formatSupportMDOutput(String format) {
        return mdFormats.contains(format);
    }

    /**
     * Executes the provided {@link GetCoverageType}.
     *
     * @param request the {@link GetCoverageType} to be executed.
     * @return the {@link GridCoverage} produced by the chain of operations specified by the
     *     provided {@link GetCoverageType}.
     */
    public GridCoverage run(GetCoverageType request) {

        // get same support filter as in WCS 1.0 and WCS 1.1
        Filter filter = WCSUtils.getRequestFilter();
        if (filter != null) {
            request.setFilter(filter);
        }

        //
        // get the coverage info from the catalog or throw an exception if we don't find it
        //
        final LayerInfo linfo = NCNameResourceCodec.getCoverage(catalog, request.getCoverageId());
        if (linfo == null) {
            throw new WCS20Exception(
                    "Could not locate coverage " + request.getCoverageId(),
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage,
                    "coverageId");
        }
        final CoverageInfo cinfo = (CoverageInfo) linfo.getResource();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Executing GetCoverage request on coverage :" + linfo.toString());
        }

        // prepare the default format
        if (request.getFormat() == null) {
            try {
                String nativeFormat = mimeMapper.mapNativeFormat(cinfo);
                request.setFormat(nativeFormat);
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not compute the native type of the coverage, defaulting to image/tiff",
                        e);
            }
        }

        // === k, now start the execution
        GridCoverage coverage = null;
        try {

            // === extract all extensions for later usage
            Map<String, ExtensionItemType> extensions = extractExtensions(request);

            // === prepare the hints to use
            // here I find if I can use overviews and do subsampling
            final Hints hints = GeoTools.getDefaultHints();
            hints.add(WCSUtils.getReaderHints(wcs));
            hints.add(
                    new RenderingHints(
                            JAI.KEY_BORDER_EXTENDER,
                            BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
            //            hints.add(new
            // RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,Boolean.FALSE));// TODO check
            // interpolation

            // get a reader for this coverage
            final GridCoverage2DReader reader =
                    (GridCoverage2DReader)
                            cinfo.getGridCoverageReader(new DefaultProgressListener(), hints);

            WCSDimensionsSubsetHelper helper =
                    parseGridCoverageRequest(cinfo, reader, request, extensions);
            GridCoverageRequest gcr = helper.getGridCoverageRequest();

            // TODO consider dealing with the Format instance instead of a String parsing or check
            // against WCSUtils.isSupportedMDOutputFormat(String).
            final GridCoverageFactory coverageFactory =
                    CoverageFactoryFinder.getGridCoverageFactory(hints);
            if (reader instanceof StructuredGridCoverage2DReader
                    && formatSupportMDOutput(request.getFormat())) {
                // Split the main request into a List of requests in order to read more coverages to
                // be stacked
                final Set<GridCoverageRequest> requests = helper.splitRequestToSet();
                if (requests == null || requests.isEmpty()) {
                    throw new IllegalArgumentException("Splitting requests returned nothing");
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "Splitting request generated " + requests.size() + " sub requests");
                    }
                }
                final List<DimensionBean> dimensions = helper.setupDimensions();
                final String nativeName = cinfo.getNativeCoverageName();
                final String coverageName =
                        nativeName != null ? nativeName : reader.getGridCoverageNames()[0];
                final GranuleStackImpl stack =
                        new GranuleStackImpl(
                                coverageName, reader.getCoordinateReferenceSystem(), dimensions);
                // Geoserver max memory limit definition
                long outputLimit = wcs.getMaxOutputMemory() * 1024;
                long inputLimit = wcs.getMaxInputMemory() * 1024;
                // Object value used for storing the sum of the output size of each internal
                // coverage
                ImageSizeRecorder incrementalOutputSize = new ImageSizeRecorder(outputLimit, false);
                // Object used for storing the sum of the output size of each internal coverage
                ImageSizeRecorder incrementalInputSize = new ImageSizeRecorder(inputLimit, true);
                // Image size estimation
                final int numRequests = requests.size();
                final Iterator<GridCoverageRequest> requestsIterator = requests.iterator();
                GridCoverageRequest firstRequest = requestsIterator.next();
                GridCoverage2D firstCoverage =
                        setupCoverage(
                                helper,
                                firstRequest,
                                request,
                                reader,
                                hints,
                                extensions,
                                dimensions,
                                incrementalOutputSize,
                                incrementalInputSize,
                                coverageFactory);
                // check the first coverage memory usage
                long actual = incrementalInputSize.finalSize();
                // Estimated size
                long estimatedSize = actual * numRequests;
                // Check if the estimated size is greater than that of the maximum output memory
                // Limit check is performed only when the limit is defined
                if (outputLimit > 0 && estimatedSize > outputLimit) {
                    throw new WcsException(
                            "This request is trying to generate too much data, "
                                    + "the limit is "
                                    + formatBytes(outputLimit)
                                    + " but the estimated amount of bytes to be "
                                    + "written in the output is "
                                    + formatBytes(estimatedSize));
                }
                // If the estimated size does not exceed the limit, the first coverage is added to
                // the GranuleStack
                stack.addCoverage(firstCoverage);

                // Get a coverage for each subrequest
                while (requestsIterator.hasNext()) {
                    GridCoverageRequest subRequest = requestsIterator.next();
                    GridCoverage2D singleCoverage =
                            setupCoverage(
                                    helper,
                                    subRequest,
                                    request,
                                    reader,
                                    hints,
                                    extensions,
                                    dimensions,
                                    incrementalOutputSize,
                                    incrementalInputSize,
                                    coverageFactory);
                    stack.addCoverage(singleCoverage);
                }
                coverage = stack;
            } else {
                // IncrementalSize not used
                coverage =
                        setupCoverage(
                                helper,
                                gcr,
                                request,
                                reader,
                                hints,
                                extensions,
                                null,
                                null,
                                null,
                                coverageFactory);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new WCS20Exception("Failed to read the coverage " + request.getCoverageId(), e);
        } finally {
            // make sure the coverage will get cleaned at the end of the processing
            if (coverage != null) {
                CoverageCleanerCallback.addCoverages(coverage);
            }
        }

        return coverage;
    }

    /**
     * Setup a coverage on top of the specified gridCoverageRequest
     *
     * @param helper a {@link CoverageInfo} instance
     * @param gridCoverageRequest the gridCoverageRequest specifying interpolation, subsettings,
     *     filters, ...
     * @param coverageType the getCoverage
     * @param reader the Reader to be used to perform the read operation
     * @param hints hints to be used by the involved operations
     */
    private GridCoverage2D setupCoverage(
            final WCSDimensionsSubsetHelper helper,
            final GridCoverageRequest gridCoverageRequest,
            final GetCoverageType coverageType,
            final GridCoverage2DReader reader,
            final Hints hints,
            final Map<String, ExtensionItemType> extensions,
            final List<DimensionBean> coverageDimensions,
            ImageSizeRecorder incrementalOutputSize,
            ImageSizeRecorder incrementalInputSize,
            final GridCoverageFactory coverageFactory)
            throws Exception {
        List<GridCoverage2D> coverages = null;
        double[] preAppliedScale = new double[] {Double.NaN, Double.NaN};
        //
        // we setup the params to force the usage of imageread and to make it use
        // the right overview and so on
        // we really try to subset before reading with a grid geometry
        // we specify to work in streaming fashion
        // TODO elevation
        ScalingType scaling = extractScaling(extensions);
        coverages =
                readCoverage(
                        helper,
                        gridCoverageRequest,
                        reader,
                        hints,
                        incrementalInputSize,
                        scaling,
                        preAppliedScale);
        GridSampleDimension[] sampleDimensions = collectDimensions(coverages);
        if (coverages == null || coverages.isEmpty()) {
            throwFailedReadException(
                    coverageType.getCoverageId(),
                    reader,
                    helper.getGridCoverageRequest(),
                    helper.getCoverageInfo());
        }

        //
        // handle range subsetting
        //
        for (int i = 0; i < coverages.size(); i++) {
            GridCoverage2D rangeSubsetted =
                    handleRangeSubsettingExtension(coverages.get(i), extensions, hints);
            coverages.set(i, rangeSubsetted);
        }

        //
        // subsetting, is not really an extension
        //
        List<GridCoverage2D> temp = new ArrayList<>();
        for (int i = 0; i < coverages.size(); i++) {
            List<GridCoverage2D> subsetted =
                    handleSubsettingExtension(
                            coverages.get(i), gridCoverageRequest.getSpatialSubset(), hints);
            temp.addAll(subsetted);
        }
        coverages = temp;

        //
        // scaling extension
        //
        // scaling is done in raster space with eventual interpolation
        for (int i = 0; i < coverages.size(); i++) {
            GridCoverage2D scaled =
                    handleScaling(
                            coverages.get(i),
                            scaling,
                            gridCoverageRequest.getSpatialInterpolation(),
                            preAppliedScale,
                            hints);
            coverages.set(i, scaled);
        }

        //
        // reprojection
        //
        // reproject the output coverage to an eventual outputCrs
        for (int i = 0; i < coverages.size(); i++) {
            GridCoverage2D reprojected =
                    handleReprojection(
                            coverages.get(i),
                            gridCoverageRequest.getOutputCRS(),
                            gridCoverageRequest.getSpatialInterpolation(),
                            hints);
            coverages.set(i, reprojected);
        }

        // after reprojection we can re-unite the coverages into one
        GridCoverage2D coverage = mosaicCoverages(coverages, hints);

        //
        // axes swap management
        //
        final boolean enforceLatLonAxesOrder =
                requestingLatLonAxesOrder(gridCoverageRequest.getOutputCRS());
        if (wcs.isLatLon() && enforceLatLonAxesOrder) {
            coverage = enforceLatLongOrder(coverage, hints, gridCoverageRequest.getOutputCRS());
        }

        //
        // Output limits checks
        // We need to enforce them once again as it might be that no scaling or rangesubsetting is
        // requested
        // Direct check is made only for single dimensional coverages
        if (incrementalOutputSize == null) {
            WCSUtils.checkOutputLimits(
                    wcs,
                    coverage.getGridGeometry().getGridRange2D(),
                    coverage.getRenderedImage().getSampleModel());
        } else {
            // Check for each coverage added if the total coverage dimension exceeds the maximum
            // limit
            // If the size is exceeded an exception is thrown
            incrementalOutputSize.addSize(coverage);
        }

        //        // add the originator -- FOR THE MOMENT DON'T, NOT CLEAR WHAT EO METADATA WE
        // SHOULD ADD TO THE OUTPUT
        //        Map<String, Object> properties = new HashMap<String,
        // Object>(coverage.getProperties());
        //        properties.put(WebCoverageService20.ORIGINATING_COVERAGE_INFO, cinfo);
        //        GridCoverage2D [] sources = (GridCoverage2D[]) coverage.getSources().toArray(new
        // GridCoverage2D[coverage.getSources().size()]);
        //        coverage = new GridCoverageFactory().create(coverage.getName().toString(),
        // coverage.getRenderedImage(),
        //                coverage.getGridGeometry(), coverage.getSampleDimensions(), sources,
        // properties);
        if (reader instanceof StructuredGridCoverage2DReader && coverageDimensions != null) {
            // Setting dimensions as properties
            Map map = coverage.getProperties();
            if (map == null) {
                map = new HashMap();
            }
            for (DimensionBean coverageDimension : coverageDimensions) {
                helper.setCoverageDimensionProperty(map, gridCoverageRequest, coverageDimension);
            }
            // Need to recreate the coverage in order to update the properties since the
            // getProperties method returns a copy
            coverage =
                    coverageFactory.create(
                            coverage.getName(),
                            coverage.getRenderedImage(),
                            coverage.getEnvelope(),
                            coverage.getSampleDimensions(),
                            null,
                            map);
        }
        if (sampleDimensions != null && sampleDimensions.length > 0) {
            coverage =
                    GridCoverageWrapper.wrapCoverage(
                            coverage, coverage, sampleDimensions, null, true);
        }
        return coverage;
    }

    private void throwFailedReadException(
            String coverageId,
            GridCoverage2DReader reader,
            GridCoverageRequest request,
            CoverageInfo coverageInfo)
            throws Exception {
        // how did we get here? space filtering should have been checked already, but maybe
        // it was due to another dimension filter
        WCSDimensionsHelper helper =
                WCSDimensionsHelper.getWCSDimensionsHelper(coverageId, coverageInfo, reader);

        // no dimensions, go for the easy case
        if (helper != null) {
            ReaderDimensionsAccessor accessor = helper.getDimensionAccessor();

            // do we have a time in the request and a domain to compare with?
            DateRange requestedTimeSubset = request.getTemporalSubset();
            DimensionInfo timeDimension = helper.getTimeDimension();
            if (requestedTimeSubset != null && timeDimension != null && timeDimension.isEnabled()) {
                checkTimeDomainIntersection(helper, accessor, requestedTimeSubset, timeDimension);
            }

            // do we have an elevation?
            NumberRange<?> requestedElevationRange = request.getElevationSubset();
            DimensionInfo elevationDimension = helper.getElevationDimension();
            if (requestedElevationRange != null
                    && elevationDimension != null
                    && elevationDimension.isEnabled()) {
                checkElevationDomainIntersection(
                        helper, accessor, requestedElevationRange, elevationDimension);
            }

            // custom dimension checks
            if (request.getDimensionsSubset() != null && !request.getDimensionsSubset().isEmpty()) {
                checkCustomDomainIntersection(reader, request, accessor);
            }
        }

        // nothing? go generic
        throw new WCS20Exception(
                "Unable to read a coverage for the current request (could be due to filtering or subsetting): "
                        + request,
                WCS20ExceptionCode.NoApplicableCode,
                null);
    }

    private void checkCustomDomainIntersection(
            GridCoverage2DReader reader,
            GridCoverageRequest request,
            ReaderDimensionsAccessor accessor)
            throws IOException {
        Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();

        for (ParameterDescriptor<List> dynamicParameter : dynamicParameters) {
            String name = dynamicParameter.getName().getCode();
            List<Object> requestedValues = request.getDimensionsSubset().get(name);
            if (requestedValues != null && !requestedValues.isEmpty()) {
                List<String> actualValues = accessor.getDomain(name);
                if (Collections.disjoint(actualValues, requestedValues)) {
                    throw new WCS20Exception(
                            "Requested "
                                    + name
                                    + " subset does not intersect the available values "
                                    + actualValues,
                            WCS20ExceptionCode.InvalidSubsetting,
                            "subset");
                }
            }
        }
    }

    private void checkElevationDomainIntersection(
            WCSDimensionsHelper helper,
            ReaderDimensionsAccessor accessor,
            NumberRange<?> requestedElevationRange,
            DimensionInfo elevationDimension)
            throws IOException {
        NumberRange actualElevationSubset =
                new NumberRange(
                        Double.class, accessor.getMinElevation(), accessor.getMaxElevation());
        if (!requestedElevationRange.intersects(actualElevationSubset)) {
            throw new WCS20Exception(
                    "Requested elevation subset does not intersect the declared range "
                            + helper.getBeginElevation()
                            + "/"
                            + helper.getEndElevation(),
                    WCS20ExceptionCode.InvalidSubsetting,
                    "subset");
        }
        // deeper check, did we skip value interpolation and provided users with an actual
        // list of values?
        DimensionPresentation presentation = elevationDimension.getPresentation();
        if (requestedElevationRange.getMinimum() < requestedElevationRange.getMaximum()
                && (presentation == DimensionPresentation.LIST
                        || presentation == DimensionPresentation.CONTINUOUS_INTERVAL)) {
            TreeSet<Object> elevationDomain = accessor.getElevationDomain();
            boolean intersectionFound = false;
            for (Object o : elevationDomain) {
                if (o instanceof Number) {
                    intersectionFound |= requestedElevationRange.contains((Comparable<?>) o);
                } else if (o instanceof NumberRange) {
                    intersectionFound |= requestedElevationRange.intersects((Range<?>) o);
                }
                if (intersectionFound) {
                    break;
                }
            }

            if (!intersectionFound) {
                throw new WCS20Exception(
                        "Requested elevation subset does not intersect available values "
                                + elevationDomain,
                        WCS20ExceptionCode.InvalidSubsetting,
                        "subset");
            }
        }
    }

    private void checkTimeDomainIntersection(
            WCSDimensionsHelper helper,
            ReaderDimensionsAccessor accessor,
            DateRange requestedTimeSubset,
            DimensionInfo timeDimension)
            throws IOException {
        DateRange actualTimeSubset = new DateRange(accessor.getMinTime(), accessor.getMaxTime());
        if (!requestedTimeSubset.intersects(actualTimeSubset)) {
            throw new WCS20Exception(
                    "Requested time subset does not intersect the declared range "
                            + helper.getBeginTime()
                            + "/"
                            + helper.getEndTime(),
                    WCS20ExceptionCode.InvalidSubsetting,
                    "subset");
        }
        // deeper check, did we skip value interpolation and provided users with an actual
        // list of values?
        DimensionPresentation presentation = timeDimension.getPresentation();
        if (!requestedTimeSubset.getMinValue().equals(requestedTimeSubset.getMaxValue())
                && (presentation == DimensionPresentation.LIST
                        || presentation == DimensionPresentation.CONTINUOUS_INTERVAL)) {
            TreeSet<Object> timeDomain = accessor.getTimeDomain();
            boolean intersectionFound = false;
            for (Object o : timeDomain) {
                if (o instanceof Date) {
                    intersectionFound |= requestedTimeSubset.contains((Date) o);
                } else if (o instanceof DateRange) {
                    intersectionFound |= requestedTimeSubset.intersects((Range<?>) o);
                }
                if (intersectionFound) {
                    break;
                }
            }

            if (!intersectionFound) {
                List<String> formattedDomain =
                        timeDomain.stream().map(o -> helper.format(o)).collect(Collectors.toList());
                throw new WCS20Exception(
                        "Requested time subset does not intersect available values "
                                + formattedDomain,
                        WCS20ExceptionCode.InvalidSubsetting,
                        "subset");
            }
        }
    }

    private ScalingType extractScaling(Map<String, ExtensionItemType> extensions) {
        ScalingType scaling = null;
        // look for a scaling extension
        if (!(extensions == null || extensions.size() == 0 || !extensions.containsKey("Scaling"))) {
            final ExtensionItemType extensionItem = extensions.get("Scaling");
            assert extensionItem != null;

            // get scaling
            scaling = (ScalingType) extensionItem.getObjectContent();
            if (scaling == null) {
                throw new IllegalStateException("Scaling extension contained a null ScalingType");
            }
        }
        return scaling;
    }

    private GridSampleDimension[] collectDimensions(List<GridCoverage2D> coverages) {
        List<GridSampleDimension> dimensions = new ArrayList<GridSampleDimension>();
        for (GridCoverage2D coverage : coverages) {
            if (coverage instanceof GridCoverageWrapper) {
                for (GridSampleDimension dimension : coverage.getSampleDimensions()) {
                    dimensions.add(dimension);
                }
            }
        }
        return dimensions.toArray(new GridSampleDimension[dimensions.size()]);
    }

    private GridCoverage2D mosaicCoverages(final List<GridCoverage2D> coverages, final Hints hints)
            throws FactoryException, TransformException {
        GridCoverage2D first = coverages.get(0);
        if (coverages.size() == 1) {
            return first;
        }

        // special case for crs that do wrap, we have to roll one of the coverages
        CoordinateReferenceSystem crs = first.getCoordinateReferenceSystem2D();
        MapProjection mapProjection = CRS.getMapProjection(crs);
        if (crs instanceof GeographicCRS || mapProjection instanceof Mercator) {
            double offset;
            if (crs instanceof GeographicCRS) {
                offset = 360;
            } else {
                offset = computeMercatorWorldSpan(crs, mapProjection);
            }
            for (int i = 1; i < coverages.size(); i++) {
                GridCoverage2D c = coverages.get(i);
                if (Math.abs(
                                c.getEnvelope().getMinimum(0)
                                        + offset
                                        - first.getEnvelope().getMaximum(0))
                        < EPS) {
                    GridCoverage2D displaced = displaceCoverage(coverages.get(1), offset);
                    coverages.set(i, displaced);
                }
            }
        }

        // mosaic
        try {
            final ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();
            param.parameter("sources").setValue(coverages);
            param.parameter("policy").setValue(GridGeometryPolicy.FIRST.name());
            return (GridCoverage2D)
                    ((Mosaic) processor.getOperation("Mosaic")).doOperation(param, hints);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mosaic the input coverages", e);
        }
    }

    private GridCoverage2D displaceCoverage(GridCoverage2D coverage, double offset) {
        // let's compute the new grid geometry
        GridGeometry2D originalGG = coverage.getGridGeometry();
        GridEnvelope gridRange = originalGG.getGridRange();
        Envelope2D envelope = originalGG.getEnvelope2D();

        double minx = envelope.getMinX() + offset;
        double miny = envelope.getMinY();
        double maxx = envelope.getMaxX() + offset;
        double maxy = envelope.getMaxY();
        ReferencedEnvelope translatedEnvelope =
                new ReferencedEnvelope(
                        minx, maxx, miny, maxy, envelope.getCoordinateReferenceSystem());

        GridGeometry2D translatedGG = new GridGeometry2D(gridRange, translatedEnvelope);

        GridCoverage2D translatedCoverage =
                gridCoverageFactory.create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        translatedGG,
                        coverage.getSampleDimensions(),
                        new GridCoverage2D[] {coverage},
                        coverage.getProperties());
        return translatedCoverage;
    }

    private double computeMercatorWorldSpan(
            CoordinateReferenceSystem crs, MapProjection mapProjection)
            throws FactoryException, TransformException {
        double centralMeridian =
                mapProjection
                        .getParameterValues()
                        .parameter(AbstractProvider.CENTRAL_MERIDIAN.getName().getCode())
                        .doubleValue();
        double[] src = new double[] {centralMeridian, 0, 180 + centralMeridian, 0};
        double[] dst = new double[4];
        MathTransform mt = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs);
        mt.transform(src, 0, dst, 0, 2);
        double worldSpan = Math.abs(dst[2] - dst[0]);
        return worldSpan;
    }

    private WCSDimensionsSubsetHelper parseGridCoverageRequest(
            CoverageInfo ci,
            GridCoverage2DReader reader,
            GetCoverageType request,
            Map<String, ExtensionItemType> extensions)
            throws IOException {
        //
        // Extract CRS values for relative extension
        //
        final CoordinateReferenceSystem subsettingCRS = extractSubsettingCRS(reader, extensions);
        final CoordinateReferenceSystem outputCRS =
                extractOutputCRS(reader, extensions, subsettingCRS);

        WCSDimensionsSubsetHelper subsetHelper =
                new WCSDimensionsSubsetHelper(
                        reader, request, ci, subsettingCRS, envelopeDimensionsMapper);

        // extract dimensions subsetting
        GridCoverageRequest requestSubset = subsetHelper.createGridCoverageRequestSubset();

        //
        // Handle interpolation extension
        //
        // notice that for the moment we support only homogeneous interpolation on the 2D axis
        final Map<String, InterpolationPolicy> axesInterpolations =
                extractInterpolation(reader, extensions);
        final Interpolation spatialInterpolation =
                extractSpatialInterpolation(axesInterpolations, reader.getOriginalEnvelope());
        final OverviewPolicy overviewPolicy = extractOverviewPolicy(extensions);
        // TODO time interpolation
        assert spatialInterpolation != null;

        // build the grid coverage request
        GridCoverageRequest gcr = new GridCoverageRequest();
        gcr.setOutputCRS(outputCRS);
        gcr.setSpatialInterpolation(spatialInterpolation);
        gcr.setSpatialSubset(requestSubset.getSpatialSubset());
        gcr.setTemporalSubset(requestSubset.getTemporalSubset());
        gcr.setElevationSubset(requestSubset.getElevationSubset());
        gcr.setDimensionsSubset(requestSubset.getDimensionsSubset());
        gcr.setFilter(request.getFilter());
        gcr.setSortBy(request.getSortBy());
        gcr.setOverviewPolicy(overviewPolicy);
        subsetHelper.setGridCoverageRequest(gcr);
        return subsetHelper;
    }

    private OverviewPolicy extractOverviewPolicy(Map<String, ExtensionItemType> extensions) {
        if (extensions == null
                || extensions.size() == 0
                || !extensions.containsKey(WCS20Const.OVERVIEW_POLICY_EXTENSION)) {
            // NO extension at hand
            return null;
        }

        // look for an overviewPolicy extension
        final ExtensionItemType extensionItem =
                extensions.get(WCS20Const.OVERVIEW_POLICY_EXTENSION);
        if (extensionItem.getName().equals(WCS20Const.OVERVIEW_POLICY_EXTENSION)) {
            String overviewPolicy = extensionItem.getSimpleContent();

            // checks
            if (overviewPolicy == null) {
                throw new WCS20Exception(
                        WCS20Const.OVERVIEW_POLICY_EXTENSION + " was null",
                        WCS20ExceptionCode.MissingParameterValue,
                        "null");
            }

            // instantiate the OverviewPolicy
            try {
                return OverviewPolicy.valueOf(overviewPolicy);
            } catch (Exception e) {
                final WCS20Exception exception =
                        new WCS20Exception(
                                "Invalid " + WCS20Const.OVERVIEW_POLICY_EXTENSION,
                                WCS20Exception.WCS20ExceptionCode.InvalidParameterValue,
                                overviewPolicy);
                exception.initCause(e);
                throw exception;
            }
        }
        return null;
    }

    /** */
    private GridCoverage2D enforceLatLongOrder(
            GridCoverage2D coverage, final Hints hints, final CoordinateReferenceSystem outputCRS)
            throws Exception {
        final Integer epsgCode = CRS.lookupEpsgCode(outputCRS, false);
        if (epsgCode != null && epsgCode > 0) {
            // final CRS
            CoordinateReferenceSystem finalCRS = CRS.decode(SRS_STARTER + epsgCode);
            if (CRS.getAxisOrder(outputCRS).equals(CRS.getAxisOrder(finalCRS))) {
                return coverage;
            }

            // get g2w and swap axes
            final AffineTransform g2w =
                    new AffineTransform(
                            (AffineTransform2D) coverage.getGridGeometry().getGridToCRS2D());
            g2w.preConcatenate(CoverageUtilities.AXES_SWAP);

            // rework the transformation
            final GridGeometry2D finalGG =
                    new GridGeometry2D(
                            coverage.getGridGeometry().getGridRange(),
                            PixelInCell.CELL_CENTER,
                            new AffineTransform2D(g2w),
                            finalCRS,
                            hints);

            // recreate the coverage
            coverage =
                    CoverageFactoryFinder.getGridCoverageFactory(hints)
                            .create(
                                    coverage.getName(),
                                    coverage.getRenderedImage(),
                                    finalGG,
                                    coverage.getSampleDimensions(),
                                    new GridCoverage[] {coverage},
                                    coverage.getProperties());
        }
        return coverage;
    }

    /**
     * This utility method tells me whether or not we should do a final reverse on the axis of the
     * data.
     *
     * @param outputCRS the final {@link CoordinateReferenceSystem} for the data as per the request
     * @return <code>true</code> in case we need to swap axes, <code>false</code> otherwise.
     */
    private boolean requestingLatLonAxesOrder(CoordinateReferenceSystem outputCRS) {

        try {
            final Integer epsgCode = CRS.lookupEpsgCode(outputCRS, false);
            if (epsgCode != null && epsgCode > 0) {
                CoordinateReferenceSystem originalCRS = CRS.decode(SRS_STARTER + epsgCode);
                return !CRS.getAxisOrder(originalCRS).equals(CRS.getAxisOrder(outputCRS));
            }
        } catch (FactoryException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        }
        return false;
    }

    /**
     * This method is responsible for extracting the spatial interpolation from the provided {@link
     * GetCoverageType} request.
     *
     * <p>We don't support mixed interpolation at this time and we will never support it for grid
     * axes.
     *
     * @param axesInterpolations the association between axes URIs and the requested interpolations.
     * @param envelope the original envelope for the source {@link GridCoverage}.
     * @return the requested {@link Interpolation}
     */
    private Interpolation extractSpatialInterpolation(
            Map<String, InterpolationPolicy> axesInterpolations, Envelope envelope) {
        // extract interpolation
        //
        // we assume that we are going to support the same interpolation ONLY on the i and j axes
        // therefore we extract the first one of the two we find. We implicitly assume we already
        // checked that we don't have mixed interpolation types for lat,lon
        Interpolation interpolation = InterpolationPolicy.getDefaultPolicy().getInterpolation();
        for (String axisLabel : axesInterpolations.keySet()) {
            // check if this is an axis we like
            final int index = envelopeDimensionsMapper.getAxisIndex(envelope, axisLabel);
            if (index == 0 || index == 1) {
                // found it!
                interpolation = axesInterpolations.get(axisLabel).getInterpolation();
                break;
            }
        }
        return interpolation;
    }

    /**
     * This method is responsible for reading the data based on the specified request. It might
     * return a single coverage, but if the request is a dateline crossing one, it will return two
     * instead
     */
    private List<GridCoverage2D> readCoverage(
            WCSDimensionsSubsetHelper helper,
            GridCoverageRequest request,
            GridCoverage2DReader reader,
            Hints hints,
            ImageSizeRecorder incrementalInputSize,
            final ScalingType scaling,
            final double[] preAppliedScale)
            throws Exception {

        CoverageInfo cinfo = helper.getCoverageInfo();
        WCSEnvelope requestedEnvelope = helper.getRequestedEnvelope();
        // checks
        Interpolation spatialInterpolation = request.getSpatialInterpolation();
        Utilities.ensureNonNull("interpolation", spatialInterpolation);

        //
        // check if we need to reproject the subset envelope back to coverageCRS
        //
        // this does not mean we need to reproject the coverage at the end
        // as the outputCrs can be different from the subsetCrs
        //
        // get source crs
        final CoordinateReferenceSystem coverageCRS = reader.getCoordinateReferenceSystem();
        WCSEnvelope subset = request.getSpatialSubset();
        List<GridCoverage2D> result = new ArrayList<GridCoverage2D>();
        List<GeneralEnvelope> readEnvelopes = new ArrayList<GeneralEnvelope>();
        if (subset.isCrossingDateline()) {
            GeneralEnvelope[] envelopes = subset.getNormalizedEnvelopes();
            addEnvelopes(envelopes[0], readEnvelopes, coverageCRS);
            addEnvelopes(envelopes[1], readEnvelopes, coverageCRS);
        } else {
            addEnvelopes(subset, readEnvelopes, coverageCRS);
        }

        List<GridCoverage2D> readCoverages = new ArrayList<>();
        for (GeneralEnvelope readEnvelope : readEnvelopes) {
            // according to spec we need to return pixel in the intersection between
            // the requested area and the declared bounds, readers might return less
            GeneralEnvelope padEnvelope = computePadEnvelope(readEnvelope, reader);

            // check if a previous read already covered this envelope, readers
            // can return more than we asked
            GridCoverage2D cov = null;
            BoundingBox readBoundingBox = new Envelope2D(readEnvelope);
            for (GridCoverage2D gc : readCoverages) {
                Envelope2D gce = gc.getEnvelope2D();
                if (gce.contains(readBoundingBox)) {
                    cov = gc;
                    break;
                }
            }
            if (cov == null) {
                cov =
                        readCoverage(
                                cinfo,
                                request,
                                reader,
                                hints,
                                incrementalInputSize,
                                spatialInterpolation,
                                coverageCRS,
                                readEnvelope,
                                requestedEnvelope,
                                scaling,
                                preAppliedScale);
                if (cov == null) {
                    continue;
                }
                readCoverages.add(cov);
            }
            // do we have more than requested?
            Envelope2D covEnvelope = cov.getEnvelope2D();
            GridCoverage2D cropped = cov;
            if (covEnvelope.contains(readBoundingBox)
                    && (covEnvelope.getWidth() > readBoundingBox.getWidth()
                            || covEnvelope.getHeight() > readBoundingBox.getHeight())) {
                cropped = cropOnEnvelope(cov, readEnvelope);
            }

            // do we have less than expected?
            GridCoverage2D padded = cropped;
            Envelope croppedEnvelope = cropped.getEnvelope();
            if (!new GeneralEnvelope(croppedEnvelope).contains(padEnvelope, true)) {
                padded = padOnEnvelope(cropped, padEnvelope);
            }

            result.add(padded);
        }

        return result;
    }

    /**
     * Computes the envelope that GetCoveage should be returning given a reading envelope and the
     * reader own native envelope (which is also the envelope we are declaring in output)
     */
    private GeneralEnvelope computePadEnvelope(
            GeneralEnvelope readEnvelope, GridCoverage2DReader reader) {
        CoordinateReferenceSystem sourceCRS = reader.getCoordinateReferenceSystem();
        CoordinateReferenceSystem subsettingCRS = readEnvelope.getCoordinateReferenceSystem();
        try {
            if (!CRS.equalsIgnoreMetadata(subsettingCRS, sourceCRS)) {
                readEnvelope = CRS.transform(readEnvelope, sourceCRS);
            }
        } catch (TransformException e) {
            throw new WCS20Exception(
                    "Unable to initialize subsetting envelope",
                    WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                    subsettingCRS.toWKT(),
                    e);
        }
        GeneralEnvelope padEnvelope = new GeneralEnvelope(readEnvelope);
        padEnvelope.intersect(reader.getOriginalEnvelope());

        return padEnvelope;
    }

    private void addEnvelopes(
            Envelope envelope,
            List<GeneralEnvelope> readEnvelopes,
            CoordinateReferenceSystem readerCRS)
            throws TransformException, FactoryException {
        // leverage GeoTools projection handlers to figure out exactly which areas we should be
        // reading
        ProjectionHandler handler =
                ProjectionHandlerFinder.getHandler(
                        new ReferencedEnvelope(envelope), readerCRS, true);
        if (handler == null) {
            readEnvelopes.add(new GeneralEnvelope(envelope));
        } else {
            List<ReferencedEnvelope> queryEnvelopes = handler.getQueryEnvelopes();
            for (ReferencedEnvelope qe : queryEnvelopes) {
                readEnvelopes.add(new GeneralEnvelope(qe));
            }
        }
    }

    private GridCoverage2D readCoverage(
            CoverageInfo cinfo,
            GridCoverageRequest request,
            GridCoverage2DReader reader,
            Hints hints,
            ImageSizeRecorder incrementalInputSize,
            Interpolation spatialInterpolation,
            final CoordinateReferenceSystem coverageCRS,
            Envelope subset,
            WCSEnvelope requestedEnvelope,
            ScalingType scaling,
            double[] preAppliedScale)
            throws TransformException, IOException, NoninvertibleTransformException {
        if (!CRS.equalsIgnoreMetadata(subset.getCoordinateReferenceSystem(), coverageCRS)) {
            subset = CRS.transform(subset, coverageCRS);
        }
        // k, now subset is in the CRS of the source coverage

        //
        // read best available coverage and render it
        //
        final GridGeometry2D readGG;

        // do we need to reproject the coverage to a different crs?
        // this would force us to enlarge the read area
        CoordinateReferenceSystem outputCRS = request.getOutputCRS();
        final boolean equalsMetadata = CRS.equalsIgnoreMetadata(outputCRS, coverageCRS);
        boolean sameCRS;
        try {
            sameCRS =
                    equalsMetadata
                            ? true
                            : CRS.findMathTransform(outputCRS, coverageCRS, true).isIdentity();
        } catch (FactoryException e1) {
            final IOException ioe = new IOException();
            ioe.initCause(e1);
            throw ioe;
        }

        //
        // instantiate basic params for reading
        //
        //
        final ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
        GeneralParameterValue[] readParameters =
                CoverageUtils.getParameters(readParametersDescriptor, cinfo.getParameters());
        readParameters = (readParameters != null ? readParameters : new GeneralParameterValue[0]);
        // work in streaming fashion when JAI is involved
        readParameters =
                WCSUtils.replaceParameter(
                        readParameters, Boolean.TRUE, AbstractGridFormat.USE_JAI_IMAGEREAD);

        // handle "time"
        if (request.getTemporalSubset() != null) {
            List<GeneralParameterDescriptor> descriptors =
                    readParametersDescriptor.getDescriptor().descriptors();
            List<Object> times = new ArrayList<Object>();
            times.add(request.getTemporalSubset());
            readParameters =
                    CoverageUtils.mergeParameter(
                            descriptors, readParameters, times, "TIME", "Time");
        }

        // handle "elevation"
        if (request.getElevationSubset() != null) {
            List<GeneralParameterDescriptor> descriptors =
                    readParametersDescriptor.getDescriptor().descriptors();
            List<Object> elevations = new ArrayList<Object>();
            elevations.add(request.getElevationSubset());
            readParameters =
                    CoverageUtils.mergeParameter(
                            descriptors, readParameters, elevations, "ELEVATION", "Elevation");
        }

        // handle filter
        if (request.getFilter() != null) {
            List<GeneralParameterDescriptor> descriptors =
                    readParametersDescriptor.getDescriptor().descriptors();
            readParameters =
                    CoverageUtils.mergeParameter(
                            descriptors, readParameters, request.getFilter(), "Filter");
        }

        // handle sorting
        if (request.getSortBy() != null) {
            List<GeneralParameterDescriptor> descriptors =
                    readParametersDescriptor.getDescriptor().descriptors();
            String sortBySpec =
                    request.getSortBy()
                            .stream()
                            .map(
                                    sb ->
                                            sb.getPropertyName().getPropertyName()
                                                    + " "
                                                    + sb.getSortOrder().name().charAt(0))
                            .collect(Collectors.joining(","));

            readParameters =
                    CoverageUtils.mergeParameter(
                            descriptors, readParameters, sortBySpec, "SORTING");
        }

        // handle additional dimensions through dynamic parameters
        // TODO: When dealing with StructuredGridCoverage2DReader we may consider parsing
        // Dimension descriptors and set filter queries
        if (request.getDimensionsSubset() != null && !request.getDimensionsSubset().isEmpty()) {
            final List<GeneralParameterDescriptor> descriptors =
                    new ArrayList<GeneralParameterDescriptor>(
                            readParametersDescriptor.getDescriptor().descriptors());
            Set<ParameterDescriptor<List>> dynamicParameters = reader.getDynamicParameters();
            descriptors.addAll(dynamicParameters);

            Map<String, List<Object>> dimensionsSubset = request.getDimensionsSubset();
            Set<String> dimensionKeys = dimensionsSubset.keySet();
            for (String key : dimensionKeys) {
                List<Object> dimValues = dimensionsSubset.get(key);
                readParameters =
                        CoverageUtils.mergeParameter(descriptors, readParameters, dimValues, key);
            }
        }

        GridCoverage2D coverage = null;
        //
        // kk, now build a good GG to read the smallest available area for the following operations
        //
        // hints
        if (sameCRS) {
            // we should not be reprojecting
            // let's create a subsetting GG2D (Taking overviews and requested scaling into account)
            MathTransform transform =
                    getMathTransform(
                            reader,
                            requestedEnvelope != null ? requestedEnvelope : subset,
                            request,
                            PixelInCell.CELL_CENTER,
                            scaling);
            readGG = new GridGeometry2D(PixelInCell.CELL_CENTER, transform, subset, hints);

        } else {

            // we are reprojecting, let's add a gutter in raster space.
            //
            // We need to investigate much more and also we need
            // to do this only when needed
            //
            //
            // add gutter by increasing size of 10 pixels each side
            Rectangle rasterRange =
                    CRS.transform(
                                    reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER)
                                            .inverse(),
                                    subset)
                            .toRectangle2D()
                            .getBounds();
            rasterRange.setBounds(
                    rasterRange.x - 10,
                    rasterRange.y - 10,
                    rasterRange.width + 20,
                    rasterRange.height + 20);
            rasterRange =
                    rasterRange.intersection(
                            (GridEnvelope2D)
                                    reader.getOriginalGridRange()); // make sure we are in it

            // read
            readGG =
                    new GridGeometry2D(
                            new GridEnvelope2D(rasterRange),
                            PixelInCell.CELL_CENTER,
                            reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                            coverageCRS,
                            hints);
        }

        // === read
        // check limits
        WCSUtils.checkInputLimits(wcs, cinfo, reader, readGG);
        Hints readHints = new Hints();
        if (hints != null) {
            readHints.putAll(hints);
        }
        if (request.getOverviewPolicy() != null) {
            readHints.add(new Hints(Hints.OVERVIEW_POLICY, request.getOverviewPolicy()));
        }
        coverage =
                RequestUtils.readBestCoverage(
                        reader,
                        readParameters,
                        readGG,
                        spatialInterpolation,
                        request.getOverviewPolicy(),
                        readHints);
        if (coverage != null) {
            // check limits again
            if (incrementalInputSize == null) {
                WCSUtils.checkInputLimits(wcs, coverage);
            } else {
                // Check for each coverage added if the total coverage dimension exceeds the maximum
                // limit
                // If the size is exceeded an exception is thrown
                incrementalInputSize.addSize(coverage);
            }

            // see what scaling factors the reader actually applied
            if (scaling != null) {
                MathTransform cmt = coverage.getGridGeometry().getGridToCRS();
                MathTransform rmt = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
                if (!(cmt instanceof AffineTransform2D) || !(rmt instanceof AffineTransform2D)) {
                    LOGGER.log(
                            Level.FINE,
                            "Cannot check if the returned coverage "
                                    + "matched the requested resolution due to a non affine "
                                    + "grid to world backing it");
                } else {
                    AffineTransform2D cat = (AffineTransform2D) cmt;
                    AffineTransform2D rat = (AffineTransform2D) rmt;
                    preAppliedScale[0] = cat.getScaleX() / rat.getScaleX();
                    preAppliedScale[1] = cat.getScaleY() / rat.getScaleY();
                }
            }
        }

        // return
        return coverage;
    }

    MathTransform getMathTransform(
            GridCoverage2DReader reader,
            Envelope subset,
            GridCoverageRequest request,
            PixelInCell pixelInCell,
            ScalingType scaling)
            throws IOException {
        // return the original grid to world only if there is no scaling, the overview policy
        // is going to be taken care of when sending data to the image reader (failing to do
        // so will cause OOM or get the processing thread blocked for a long time because
        // the reader is no more allowed to use subsampling)
        ScalingPolicy scalingPolicy = scaling == null ? null : ScalingPolicy.getPolicy(scaling);
        if (scalingPolicy == null || scalingPolicy == ScalingPolicy.DoNothing) {
            return reader.getOriginalGridToWorld(pixelInCell);
        }

        // here we are already assuming to work off an affine transform
        MathTransform transform = reader.getOriginalGridToWorld(pixelInCell);
        AffineTransform af = (AffineTransform) transform;

        // Getting the native resolution
        final double nativeResX = XAffineTransform.getScaleX0(af);
        final double nativeResY = XAffineTransform.getScaleY0(af);

        // Getting the requested resolution, taking the requested scaling into account
        final double[] requestedResolution =
                computeRequestedResolution(scaling, subset, nativeResX, nativeResY);

        // setup a scaling to get the desired resolution while allowing the reader to apply
        // subsampling
        AffineTransform scale = new AffineTransform();
        scale.scale(requestedResolution[0] / nativeResX, requestedResolution[1] / nativeResY);
        AffineTransform finalTransform = new AffineTransform(af);
        finalTransform.concatenate(scale);
        return ProjectiveTransform.create(finalTransform);
    }

    /**
     * Parse the scaling type applied to that request and return a resolution satisfying that
     * scaling.
     */
    private double[] computeRequestedResolution(
            ScalingType scaling, Envelope subset, double nativeResX, double nativeResY) {
        ScalingPolicy policy = ScalingPolicy.getPolicy(scaling);
        double[] requestedResolution = new double[2];
        if (policy == ScalingPolicy.ScaleToSize || policy == ScalingPolicy.ScaleToExtent) {
            int[] scalingSize = ScalingPolicy.getTargetSize(scaling);

            // Getting the requested resolution (using envelope and requested scaleSize)
            final GridToEnvelopeMapper mapper =
                    new GridToEnvelopeMapper(
                            new GridEnvelope2D(0, 0, scalingSize[0], scalingSize[1]), subset);
            AffineTransform scalingTransform = mapper.createAffineTransform();
            requestedResolution[0] = XAffineTransform.getScaleX0(scalingTransform);
            requestedResolution[1] = XAffineTransform.getScaleY0(scalingTransform);
        } else {
            // Only scaleFactors based will be handled here
            double[] scalingFactors = ScalingPolicy.getScaleFactors(scaling);
            requestedResolution[0] = nativeResX / scalingFactors[0];
            requestedResolution[1] = nativeResY / scalingFactors[1];
        }
        return requestedResolution;
    }

    /**
     * This method is responsible for etracting the outputCRS.
     *
     * <p>In case it is not provided the subsettingCRS falls back on the subsettingCRS.
     *
     * @param reader the {@link GridCoverage2DReader} to be used
     * @param extensions the {@link Map} of extension for this request.
     * @param subsettingCRS the subsettingCRS as a {@link CoordinateReferenceSystem}
     * @return the outputCRS as a {@link CoordinateReferenceSystem}
     */
    private CoordinateReferenceSystem extractOutputCRS(
            GridCoverage2DReader reader,
            Map<String, ExtensionItemType> extensions,
            CoordinateReferenceSystem subsettingCRS) {
        return extractCRSInternal(extensions, subsettingCRS, true);
    }

    /**
     * Extract the specified crs being it subsetting or output with proper defaults.
     *
     * @param extensions the {@link Map}of extensions for this request.
     * @param defaultCRS the defaultCRS as a {@link CoordinateReferenceSystem} for this extraction
     * @param isOutputCRS a <code>boolean</code> which tells me whether the CRS we are looking for
     *     is a subsetting or an OutputCRS
     * @return a {@link CoordinateReferenceSystem}.
     */
    private CoordinateReferenceSystem extractCRSInternal(
            Map<String, ExtensionItemType> extensions,
            CoordinateReferenceSystem defaultCRS,
            boolean isOutputCRS)
            throws WCS20Exception {
        Utilities.ensureNonNull("defaultCRS", defaultCRS);
        final String identifier = isOutputCRS ? "outputCrs" : "subsettingCrs";
        // look for subsettingCRS Extension extension
        if (extensions == null || extensions.size() == 0 || !extensions.containsKey(identifier)) {
            // NO extension at hand
            return defaultCRS;
        }

        // look for an crs extension
        final ExtensionItemType extensionItem = extensions.get(identifier);
        if (extensionItem.getName().equals(identifier)) {
            // get URI
            String crsName = extensionItem.getSimpleContent();

            // checks
            if (crsName == null) {
                throw new WCS20Exception(
                        identifier + " was null", WCS20ExceptionCode.NotACrs, "null");
            }

            // instantiate and make it go lon/lat order if possible
            try {
                CoordinateReferenceSystem crs = CRS.decode(crsName);
                final Integer epsgCode = CRS.lookupEpsgCode(crs, false);
                if (epsgCode != null && epsgCode > 0) {
                    return CRS.decode("EPSG:" + epsgCode);
                } else {
                    return crs;
                }
            } catch (Exception e) {
                final WCS20Exception exception =
                        new WCS20Exception(
                                "Invalid " + identifier,
                                isOutputCRS
                                        ? WCS20Exception.WCS20ExceptionCode.OutputCrsNotSupported
                                        : WCS20Exception.WCS20ExceptionCode
                                                .SubsettingCrsNotSupported,
                                crsName);
                exception.initCause(e);
                throw exception;
            }
        }

        // should not happen
        return defaultCRS;
    }

    /**
     * This method is responsible for extracting the subsettingCRS.
     *
     * <p>In case it is not provided the subsettingCRS falls back on the nativeCRS.
     *
     * @param reader the {@link GridCoverage2DReader} to be used
     * @param extensions the {@link Map} of extension for this request.
     * @return the subsettingCRS as a {@link CoordinateReferenceSystem}
     */
    private CoordinateReferenceSystem extractSubsettingCRS(
            GridCoverage2DReader reader, Map<String, ExtensionItemType> extensions) {
        Utilities.ensureNonNull("reader", reader);
        return extractCRSInternal(extensions, reader.getCoordinateReferenceSystem(), false);
    }

    /**
     * This method id responsible for extracting the extensions from the incoming request to
     * facilitate the work of successive methods.
     *
     * @param request the {@link GetCoverageType} request to execute.
     * @return a {@link Map} that maps extension names to {@link ExtensionType}s.
     */
    private Map<String, ExtensionItemType> extractExtensions(GetCoverageType request) {
        // === checks
        Utilities.ensureNonNull("request", request);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Extracting extensions from provided request");
        }

        // look for subsettingCRS Extension extension
        final ExtensionType extension = request.getExtension();

        // look for the various extensions
        final Map<String, ExtensionItemType> parsedExtensions =
                new HashMap<String, ExtensionItemType>();
        // no extensions?
        if (extension != null) {
            final EList<ExtensionItemType> extensions = extension.getContents();
            for (final ExtensionItemType extensionItem : extensions) {
                final String extensionName = extensionItem.getName();
                if (extensionName == null || extensionName.length() <= 0) {
                    throw new WCS20Exception("Null extension");
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Parsing extension " + extensionName);
                }
                if (extensionName.equals("subsettingCrs")) {
                    parsedExtensions.put("subsettingCrs", extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension subsettingCrs");
                    }
                } else if (extensionName.equals("outputCrs")) {
                    parsedExtensions.put("outputCrs", extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension outputCrs");
                    }
                } else if (extensionName.equals("Scaling")) {
                    parsedExtensions.put("Scaling", extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension Scaling");
                    }
                } else if (extensionName.equals("Interpolation")) {
                    parsedExtensions.put("Interpolation", extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension Interpolation");
                    }
                } else if (extensionName.equals("rangeSubset")
                        || extensionName.equals("RangeSubset")) {
                    parsedExtensions.put("rangeSubset", extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension rangeSubset");
                    }
                } else if (extensionName.equals(WCS20Const.OVERVIEW_POLICY_EXTENSION)) {
                    parsedExtensions.put(WCS20Const.OVERVIEW_POLICY_EXTENSION, extensionItem);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added extension overviewPolicy ");
                    }
                }
            }
        } else if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("No extensions found in provided request");
        }
        return parsedExtensions;
    }

    /** */
    private Map<String, InterpolationPolicy> extractInterpolation(
            GridCoverage2DReader reader, Map<String, ExtensionItemType> extensions) {
        // preparation
        final Map<String, InterpolationPolicy> returnValue =
                new HashMap<String, InterpolationPolicy>();
        final Envelope envelope = reader.getOriginalEnvelope();
        final List<String> axesNames = envelopeDimensionsMapper.getAxesNames(envelope, true);
        for (String axisName : axesNames) {
            returnValue.put(
                    axisName,
                    InterpolationPolicy.getDefaultPolicy()); // use defaults if no specified
        }

        // look for scaling extension
        if (extensions == null
                || extensions.size() == 0
                || !extensions.containsKey("Interpolation")) {
            // NO INTERPOLATION
            return returnValue;
        }

        // look for an interpolation extension
        final ExtensionItemType extensionItem = extensions.get("Interpolation");
        // get interpolationType
        InterpolationType interpolationType = (InterpolationType) extensionItem.getObjectContent();
        // which type
        if (interpolationType.getInterpolationMethod() != null) {
            InterpolationMethodType method = interpolationType.getInterpolationMethod();
            InterpolationPolicy policy = InterpolationPolicy.getPolicy(method);
            for (String axisName : axesNames) {
                returnValue.put(axisName, policy);
            }

        } else if (interpolationType.getInterpolationAxes() != null) {
            // make sure we don't set things twice
            final List<String> foundAxes = new ArrayList<String>();

            final InterpolationAxesType axes = interpolationType.getInterpolationAxes();
            for (InterpolationAxisType axisInterpolation : axes.getInterpolationAxis()) {

                // parse interpolation
                final String method = axisInterpolation.getInterpolationMethod();
                final InterpolationPolicy policy = InterpolationPolicy.getPolicy(method);

                // parse axis
                final String axis = axisInterpolation.getAxis();
                // get label from axis
                // TODO synonyms reduction
                int index = axis.lastIndexOf("/");
                final String axisLabel =
                        (index >= 0 ? axis.substring(index + 1, axis.length()) : axis);

                // did we already set this interpolation?
                if (foundAxes.contains(axisLabel)) {
                    throw new WCS20Exception(
                            "Duplicated axis",
                            WCS20Exception.WCS20ExceptionCode.InvalidAxisLabel,
                            axisLabel);
                }
                foundAxes.add(axisLabel);

                // do we have this axis?
                if (!returnValue.containsKey(axisLabel)) {
                    throw new WCS20Exception(
                            "Invalid axes URI",
                            WCS20Exception.WCS20ExceptionCode.NoSuchAxis,
                            axisLabel);
                }
                returnValue.put(axisLabel, policy);
            }
        }

        // final checks, we dont' supported different interpolations on Long and Lat
        InterpolationPolicy lat = null, lon = null;
        if (returnValue.containsKey("Long")) {
            lon = returnValue.get("Long");
        }
        if (returnValue.containsKey("Lat")) {
            lat = returnValue.get("Lat");
        }
        if (lat != lon) {
            throw new WCS20Exception(
                    "We don't support different interpolations on Lat,Lon",
                    WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                    "");
        }
        returnValue.get("Lat");
        return returnValue;
    }

    /**
     * This method is responsible for handling reprojection of the source coverage to a certain CRS.
     *
     * @param coverage the {@link GridCoverage} to reproject.
     * @param targetCRS the target {@link CoordinateReferenceSystem}
     * @param spatialInterpolation the {@link Interpolation} to adopt.
     * @param hints {@link Hints} to control the process.
     * @return a new instance of {@link GridCoverage} reprojeted to the targetCRS.
     */
    private GridCoverage2D handleReprojection(
            GridCoverage2D coverage,
            CoordinateReferenceSystem targetCRS,
            Interpolation spatialInterpolation,
            Hints hints) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);
        // check the two crs tosee if we really need to do anything
        if (CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem2D(), targetCRS)) {
            return coverage;
        }

        // reproject
        final CoverageProcessor processor =
                hints == null
                        ? CoverageProcessor.getInstance()
                        : CoverageProcessor.getInstance(hints);
        final Operation operation = processor.getOperation("Resample");
        final ParameterValueGroup parameters = operation.getParameters();
        parameters.parameter("Source").setValue(coverage);
        parameters.parameter("CoordinateReferenceSystem").setValue(targetCRS);
        parameters.parameter("GridGeometry").setValue(null);
        parameters.parameter("InterpolationType").setValue(spatialInterpolation);
        return (GridCoverage2D) processor.doOperation(parameters);
    }

    /**
     * This method is responsible for performing the RangeSubsetting operation which can be used to
     * subset of actually remix or even duplicate bands from the input source coverage.
     *
     * <p>The method tries to enforce the WCS Resource Limits specified at config time.
     *
     * @param coverage the {@link GridCoverage2D} to work on
     * @param extensions the list of WCS extension to look for the the RangeSubset one
     * @param hints an instance of {@link Hints} to use for the operations.
     * @return a new instance of {@link GridCoverage2D} or the source one in case no operation was
     *     needed.
     */
    private GridCoverage2D handleRangeSubsettingExtension(
            GridCoverage2D coverage, Map<String, ExtensionItemType> extensions, Hints hints) {
        // preparation
        final List<String> returnValue = new ArrayList<String>();

        // look for rangeSubset extension
        if (extensions == null
                || extensions.size() == 0
                || !extensions.containsKey("rangeSubset")) {
            // NO subsetting
            return coverage;
        }
        // get original bands
        final GridSampleDimension[] bands = coverage.getSampleDimensions();
        final List<String> bandsNames = new ArrayList<String>();
        for (GridSampleDimension band : bands) {
            bandsNames.add(band.getDescription().toString());
        }

        // look for an interpolation extension
        final ExtensionItemType extensionItem = extensions.get("rangeSubset");
        assert extensionItem != null; // should not happen
        final RangeSubsetType range = (RangeSubsetType) extensionItem.getObjectContent();
        for (RangeItemType rangeItem : range.getRangeItems()) {
            // there you go the range item

            // single element
            final String rangeComponent = rangeItem.getRangeComponent();

            // range?
            if (rangeComponent == null) {
                final RangeIntervalType rangeInterval = rangeItem.getRangeInterval();
                final String startRangeComponent = rangeInterval.getStartComponent();
                final String endRangeComponent = rangeInterval.getEndComponent();
                if (!bandsNames.contains(startRangeComponent)) {
                    throw new WCS20Exception(
                            "Invalid Band Name",
                            WCS20Exception.WCS20ExceptionCode.NoSuchField,
                            rangeComponent);
                }
                if (!bandsNames.contains(endRangeComponent)) {
                    throw new WCS20Exception(
                            "Invalid Band Name",
                            WCS20Exception.WCS20ExceptionCode.NoSuchField,
                            rangeComponent);
                }

                // loop
                boolean add = false;
                for (SampleDimension sd : bands) {
                    if (sd instanceof GridSampleDimension) {
                        final GridSampleDimension band = (GridSampleDimension) sd;
                        final String name = band.getDescription().toString();
                        if (name.equals(startRangeComponent)) {
                            returnValue.add(startRangeComponent);
                            add = true;
                        } else if (name.equals(endRangeComponent)) {
                            returnValue.add(endRangeComponent);
                            add = false;
                        } else if (add) {
                            returnValue.add(name);
                        }
                    }
                }
                // paranoiac check add a false
                if (add) {
                    throw new IllegalStateException("Unable to close range in band identifiers");
                }
            } else {
                if (bandsNames.contains(rangeComponent)) {
                    returnValue.add(rangeComponent);
                } else {
                    throw new WCS20Exception(
                            "Invalid Band Name",
                            WCS20Exception.WCS20ExceptionCode.NoSuchField,
                            rangeComponent);
                }
            }
        }

        // kk now let's see what we got to do
        if (returnValue.isEmpty()) {
            return coverage;
        }
        // houston we got a list of dimensions
        // create a list of indexes to select
        final int indexes[] = new int[returnValue.size()];
        int i = 0;
        for (String bandName : returnValue) {
            indexes[i++] =
                    bandsNames.indexOf(
                            bandName); // I am assuming there is no duplication in band names which
            // is ok I believe
        }

        // === enforce limits
        if (coverage.getNumSampleDimensions() < indexes.length) {
            // ok we are enlarging the number of bands, let's check the final size
            WCSUtils.checkOutputLimits(wcs, coverage, indexes);
        }

        // create output
        return (GridCoverage2D) WCSUtils.bandSelect(coverage, indexes);
    }

    /**
     * This method is reponsible for cropping the provided {@link GridCoverage} using the provided
     * subset envelope.
     *
     * <p>The subset envelope at this stage should be in the native crs.
     *
     * @param coverage the source {@link GridCoverage}
     * @param subset an instance of {@link GeneralEnvelope} that drives the crop operation.
     * @return a cropped version of the source {@link GridCoverage}
     */
    private List<GridCoverage2D> handleSubsettingExtension(
            GridCoverage2D coverage, WCSEnvelope subset, Hints hints) {

        List<GridCoverage2D> result = new ArrayList<GridCoverage2D>();
        if (subset != null) {
            if (subset.isCrossingDateline()) {
                Envelope2D coverageEnvelope = coverage.getEnvelope2D();
                GeneralEnvelope[] normalizedEnvelopes = subset.getNormalizedEnvelopes();
                for (int i = 0; i < normalizedEnvelopes.length; i++) {
                    GeneralEnvelope ge = normalizedEnvelopes[i];
                    if (ge.intersects(coverageEnvelope, false)) {
                        GridCoverage2D cropped = cropOnEnvelope(coverage, ge);
                        result.add(cropped);
                    }
                }
            } else {
                GridCoverage2D cropped = cropOnEnvelope(coverage, subset);
                result.add(cropped);
            }
        }
        return result;
    }

    private GridCoverage2D cropOnEnvelope(GridCoverage2D coverage, Envelope cropEnvelope) {
        CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem();
        CoordinateReferenceSystem subsettingCRS = cropEnvelope.getCoordinateReferenceSystem();
        try {
            if (!CRS.equalsIgnoreMetadata(subsettingCRS, sourceCRS)) {
                cropEnvelope = CRS.transform(cropEnvelope, sourceCRS);
            }
        } catch (TransformException e) {
            throw new WCS20Exception(
                    "Unable to initialize subsetting envelope",
                    WCS20Exception.WCS20ExceptionCode.SubsettingCrsNotSupported,
                    subsettingCRS.toWKT(),
                    e);
        }

        GridCoverage2D cropped = WCSUtils.crop(coverage, cropEnvelope);
        cropped = GridCoverageWrapper.wrapCoverage(cropped, coverage, null, null, false);
        return cropped;
    }

    private GridCoverage2D padOnEnvelope(GridCoverage2D coverage, GeneralEnvelope padEnvelope)
            throws TransformException {
        GridCoverage2D padded = WCSUtils.padToEnvelope(coverage, padEnvelope);
        // in case of no padding just return the original coverage without wrapping
        if (padded == coverage) {
            return coverage;
        }
        padded = GridCoverageWrapper.wrapCoverage(padded, coverage, null, null, false);
        return padded;
    }

    /**
     * This method is responsible for handling the scaling WCS extension.
     *
     * <p>Scaling can be used to scale a {@link GridCoverage2D} in different ways. An user can
     * decide to use either an uniform scale factor on each axes or by specifying a specific one on
     * each of them. Alternatively, an user can decide to specify the target size on each axes.
     *
     * <p>In case no scaling is in place but an higher order interpolation is required, scale is
     * performed anyway to respect such interpolation.
     *
     * <p>This method tries to enforce the WCS resource limits if they are set
     *
     * @param coverage the input {@link GridCoverage2D}
     * @param spatialInterpolation the requested {@link Interpolation}
     * @param hints an instance of {@link Hints} to apply
     * @return a scaled version of the input {@link GridCoverage2D} according to what is specified
     *     in the list of extensions. It might be the source coverage itself if no operations where
     *     to be applied.
     */
    private GridCoverage2D handleScaling(
            GridCoverage2D coverage,
            ScalingType scaling,
            Interpolation spatialInterpolation,
            double preAppliedScale[],
            Hints hints) {
        // checks
        Utilities.ensureNonNull("interpolation", spatialInterpolation);

        // look for scaling extension
        if (scaling == null) {
            // NO SCALING do we need interpolation?
            if (spatialInterpolation instanceof InterpolationNearest) {
                return coverage;
            } else {
                // interpolate coverage if requested and not nearest!!!!
                final Operation operation = CoverageProcessor.getInstance().getOperation("Warp");
                final ParameterValueGroup parameters = operation.getParameters();
                parameters.parameter("Source").setValue(coverage);
                parameters
                        .parameter("warp")
                        .setValue(
                                new WarpAffine(AffineTransform.getScaleInstance(1, 1))); // identity
                parameters
                        .parameter("interpolation")
                        .setValue(
                                spatialInterpolation != null
                                        ? spatialInterpolation
                                        : InterpolationPolicy.getDefaultPolicy()
                                                .getInterpolation());
                parameters
                        .parameter("backgroundValues")
                        .setValue(
                                CoverageUtilities.getBackgroundValues(
                                        coverage)); // TODO check and improve
                return (GridCoverage2D)
                        CoverageProcessor.getInstance().doOperation(parameters, hints);
            }
        }

        // instantiate enum
        final ScalingPolicy scalingPolicy = ScalingPolicy.getPolicy(scaling);
        // Before doing the scaling, check if some preScaling as been applied
        // This may occur when dealing with overviews
        if (!Double.isNaN(preAppliedScale[0]) && !Double.isNaN(preAppliedScale[1])) {
            final Double[] scale = new Double[] {preAppliedScale[0], preAppliedScale[1]};
            hints.add(new Hints(GetCoverage.PRE_APPLIED_SCALE, scale));
        }
        return scalingPolicy.scale(coverage, scaling, spatialInterpolation, hints, wcs);
    }

    /**
     * Helper class used for storing and checking the size of each image
     *
     * @author Nicola Lagomarsini
     */
    static class ImageSizeRecorder {
        /** Incremental value for the size */
        private long incrementalSize = 0;

        private final long limit;

        private final boolean input;

        ImageSizeRecorder(long limit, boolean input) {
            this.limit = limit;
            this.input = input;
        }

        /** Increment the total size value if not disabled */
        public void addSize(GridCoverage2D coverage) {
            incrementalSize +=
                    getCoverageSize(
                            coverage.getGridGeometry().getGridRange2D(),
                            coverage.getRenderedImage().getSampleModel());
            isSizeExceeded();
        }

        /** Return the total size accumulated */
        public long finalSize() {
            return incrementalSize;
        }

        private void isSizeExceeded() {
            if (limit > 0 && incrementalSize > limit) {
                throw new WcsException(
                        "This request is trying to "
                                + (input ? "read" : "generate")
                                + " too much data, the limit is "
                                + formatBytes(limit)
                                + " but the actual amount of bytes to be "
                                + (input ? "read" : "written")
                                + " is "
                                + formatBytes(incrementalSize));
            }
        }

        /** Reset the total size stored to 0 */
        public void reset() {
            incrementalSize = 0;
        }

        /**
         * Computes the size of a grid coverage in bytes given its grid envelope and the target
         * sample model (code from WCSUtils)
         */
        private static long getCoverageSize(GridEnvelope2D envelope, SampleModel sm) {
            // === compute the coverage memory usage and compare with limit
            final long pixelsNumber = computePixelsNumber(envelope);

            long pixelSize = 0;
            final int numBands = sm.getNumBands();
            for (int i = 0; i < numBands; i++) {
                pixelSize += sm.getSampleSize(i);
            }
            return pixelsNumber * pixelSize / 8;
        }

        /**
         * Computes the number of pixels for this {@link GridEnvelope2D}. (code from WCSUtils)
         *
         * @param rasterEnvelope the {@link GridEnvelope2D} to compute the number of pixels for
         * @return the number of pixels for the provided {@link GridEnvelope2D}
         */
        private static long computePixelsNumber(GridEnvelope2D rasterEnvelope) {
            // pixels
            long pixelsNumber = 1;
            final int dimensions = rasterEnvelope.getDimension();
            for (int i = 0; i < dimensions; i++) {
                pixelsNumber *= rasterEnvelope.getSpan(i);
            }
            return pixelsNumber;
        }
    }

    /**
     * Utility function to format a byte amount into a human readable string (code from WCSUtils)
     *
     * @return a formatted string
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.##").format(bytes / 1024.0) + "KB";
        } else {
            return new DecimalFormat("#.##").format(bytes / 1024.0 / 1024.0) + "MB";
        }
    }
}
