/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.DecimationPolicy;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Interpolate;
import org.geotools.coverage.processing.operation.Resample;
import org.geotools.coverage.processing.operation.SelectSampleDimension;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.math.DD;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vfny.geoserver.wcs.WcsException;

/**
 * @author Simone Giannecchini, GeoSolutions
 * @author Alessio Fabiani, GeoSolutions
 */
public class WCSUtils {

    private static final double SHEAR_EPS = 1e-3;

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(WCSUtils.class);

    public static final String ELEVATION = "ELEVATION";

    public static final Hints LENIENT_HINT = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();

    private static final Hints hints = new Hints();

    /**
     * <strong>Reprojecting</strong><br>
     * The new grid geometry can have a different coordinate reference system than the underlying
     * grid geometry. For example, a grid coverage can be reprojected from a geodetic coordinate
     * reference system to Universal Transverse Mercator CRS.
     *
     * @param coverage GridCoverage2D
     * @param sourceCRS CoordinateReferenceSystem
     * @param targetCRS CoordinateReferenceSystem
     * @return GridCoverage2D
     */
    public static GridCoverage2D resample(
            final GridCoverage2D coverage,
            final CoordinateReferenceSystem sourceCRS,
            final CoordinateReferenceSystem targetCRS,
            final GridGeometry2D gridGeometry,
            final Interpolation interpolation)
            throws WcsException {

        final ParameterValueGroup param = PROCESSOR.getOperation("Resample").getParameters();
        param.parameter("Source").setValue(coverage);
        param.parameter("CoordinateReferenceSystem").setValue(targetCRS);
        param.parameter("GridGeometry").setValue(gridGeometry);
        param.parameter("InterpolationType").setValue(interpolation);

        return (GridCoverage2D)
                ((Resample) PROCESSOR.getOperation("Resample")).doOperation(param, hints);
    }

    /** Crops the coverage to the specified bounds. May return null in case of empty intersection */
    public static GridCoverage2D crop(final GridCoverage2D coverage, final Envelope bounds) {

        // checks
        final ReferencedEnvelope cropBounds = new ReferencedEnvelope(bounds);
        final ReferencedEnvelope coverageBounds = new ReferencedEnvelope(coverage.getEnvelope());
        if (cropBounds.contains((org.locationtech.jts.geom.Envelope) coverageBounds)) {
            return coverage;
        }
        // if the intersection is so small that we'll end up reading nothing, return null
        // instead of failing at the JAI level
        ReferencedEnvelope intersection = cropBounds.intersection(coverageBounds);
        if (getEnvelopeInRasterSpace(intersection, coverage.getGridGeometry()).isEmpty())
            return null;
        Polygon polygon = JTS.toGeometry(cropBounds);
        Geometry roi = polygon.getFactory().createMultiPolygon(new Polygon[] {polygon});

        // perform the crops
        final ParameterValueGroup param = PROCESSOR.getOperation("CoverageCrop").getParameters();
        param.parameter("Source").setValue(coverage);
        param.parameter("Envelope").setValue(bounds);
        param.parameter("ROI").setValue(roi);

        return (GridCoverage2D) PROCESSOR.doOperation(param);
    }

    /**
     * Pads the coverage to the specified bounds
     *
     * @param coverage The coverage to be padded
     * @param bounds The bounds to pad to
     * @return The padded covearge, or the original one, if the padding would not add a single pixel
     *     to it. May return null if the padding area is so small it won't contain a single pixel.
     */
    public static GridCoverage2D padToEnvelope(final GridCoverage2D coverage, final Envelope bounds)
            throws TransformException {
        GridGeometry2D gg = coverage.getGridGeometry();
        GridEnvelope2D targetRange = getEnvelopeInRasterSpace(bounds, gg);
        GridEnvelope2D sourceRange = gg.getGridRange2D();
        if (sourceRange.x == targetRange.x
                && sourceRange.y == targetRange.y
                && sourceRange.width == targetRange.width
                && sourceRange.height == targetRange.height) {
            return coverage;
        }
        // in case the target envelope is so tiny that it won't fix a whole pixel on either axis
        if (targetRange.isEmpty()) return null;

        GridGeometry2D target =
                new GridGeometry2D(
                        targetRange, gg.getGridToCRS(), gg.getCoordinateReferenceSystem2D());

        List<GridCoverage2D> sources = new ArrayList<>(2);
        sources.add(coverage);

        // perform the mosaic
        final ParameterValueGroup param = PROCESSOR.getOperation("Mosaic").getParameters();
        param.parameter("Sources").setValue(sources);
        param.parameter("geometry").setValue(target);

        return (GridCoverage2D) PROCESSOR.doOperation(param);
    }

    private static GridEnvelope2D getEnvelopeInRasterSpace(Envelope bounds, GridGeometry2D gg) {
        try {
            // transform to raster space, and snap to the integer grid
            GeneralEnvelope rasterEnvelopeFloat =
                    CRS.transform(gg.getCRSToGrid2D(PixelOrientation.UPPER_LEFT), bounds);
            return new GridEnvelope2D(
                    (int) Math.round(rasterEnvelopeFloat.getMinimum(0)),
                    (int) Math.round(rasterEnvelopeFloat.getMinimum(1)),
                    (int) Math.round(rasterEnvelopeFloat.getSpan(0)),
                    (int) Math.round(rasterEnvelopeFloat.getSpan(1)));
        } catch (TransformException e) {
            throw new ServiceException("Failed to transform envelope to raster space", e);
        }
    }

    /**
     * <strong>Interpolating</strong><br>
     * Specifies the interpolation type to be used to interpolate values for points which fall
     * between grid cells. The default value is nearest neighbor. The new interpolation type
     * operates on all sample dimensions. Possible values for type are: {@code "NearestNeighbor"},
     * {@code "Bilinear"} and {@code "Bicubic"} (the {@code "Optimal"} interpolation type is
     * currently not supported).
     *
     * @param coverage GridCoverage2D
     * @param interpolation Interpolation
     * @return GridCoverage2D
     */
    public static GridCoverage2D interpolate(
            final GridCoverage2D coverage, final Interpolation interpolation) throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // INTERPOLATE
        //
        //
        // ///////////////////////////////////////////////////////////////////
        if (interpolation != null) {
            /* Operations.DEFAULT.interpolate(coverage, interpolation) */
            final ParameterValueGroup param = PROCESSOR.getOperation("Interpolate").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("Type").setValue(interpolation);

            return (GridCoverage2D)
                    ((Interpolate) PROCESSOR.getOperation("Interpolate")).doOperation(param, hints);
        }

        return coverage;
    }

    /**
     * <strong>Band Selecting</strong><br>
     * Chooses <var>N</var> {@linkplain org.geotools.coverage.GridSampleDimension sample dimensions}
     * from a grid coverage and copies their sample data to the destination grid coverage in the
     * order specified. The {@code "SampleDimensions"} parameter specifies the source {@link
     * org.geotools.coverage.GridSampleDimension} indices, and its size ({@code
     * SampleDimensions.length}) determines the number of sample dimensions of the destination grid
     * coverage. The destination coverage may have any number of sample dimensions, and a particular
     * sample dimension of the source coverage may be repeated in the destination coverage by
     * specifying it multiple times in the {@code "SampleDimensions"} parameter.
     *
     * @param params Set
     * @param coverage GridCoverage
     * @return Coverage
     */
    public static Coverage bandSelect(final Map params, final GridCoverage coverage)
            throws WcsException {
        // ///////////////////////////////////////////////////////////////////
        //
        // BAND SELECT
        //
        //
        // ///////////////////////////////////////////////////////////////////
        final int numDimensions = coverage.getNumSampleDimensions();
        final Map<String, Integer> dims = new HashMap<>();
        final List<Integer> selectedBands = new ArrayList<>();

        for (int d = 0; d < numDimensions; d++) {
            dims.put("band" + (d + 1), Integer.valueOf(d));
        }

        if ((params != null) && !params.isEmpty()) {
            for (Object o : params.keySet()) {
                final String param = (String) o;

                if (param.equalsIgnoreCase("BAND")) {
                    try {
                        final String values = (String) params.get(param);

                        if (values.indexOf("/") > 0) {
                            final String[] minMaxRes = values.split("/");
                            final int min = (int) Math.round(Double.parseDouble(minMaxRes[0]));
                            final int max = (int) Math.round(Double.parseDouble(minMaxRes[1]));

                            for (int v = min; v <= max; v++) {
                                final String key = param.toLowerCase() + v;

                                if (dims.containsKey(key)) {
                                    selectedBands.add(dims.get(key));
                                }
                            }
                        } else {
                            final String[] bands = values.split(",");

                            for (String band : bands) {
                                final String key = param.toLowerCase() + band;

                                if (dims.containsKey(key)) {
                                    selectedBands.add(dims.get(key));
                                }
                            }

                            if (selectedBands.isEmpty()) {
                                throw new Exception("WRONG PARAM VALUES.");
                            }
                        }
                    } catch (Exception e) {
                        throw new WcsException(
                                "Band parameters incorrectly specified: "
                                        + e.getLocalizedMessage());
                    }
                }
            }
        }

        final int length = selectedBands.size();
        final int[] bands = new int[length];

        for (int b = 0; b < length; b++) {
            bands[b] = selectedBands.get(b).intValue();
        }

        return bandSelect(coverage, bands);
    }

    public static Coverage bandSelect(final GridCoverage coverage, final int[] bands) {
        Coverage bandSelectedCoverage;

        if ((bands != null) && (bands.length > 0)) {
            /* Operations.DEFAULT.selectSampleDimension(coverage, bands) */
            final ParameterValueGroup param =
                    PROCESSOR.getOperation("SelectSampleDimension").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("SampleDimensions").setValue(bands);
            // param.parameter("VisibleSampleDimension").setValue(bands);
            bandSelectedCoverage =
                    ((SelectSampleDimension) PROCESSOR.getOperation("SelectSampleDimension"))
                            .doOperation(param, hints);
        } else {
            bandSelectedCoverage = coverage;
        }

        return bandSelectedCoverage;
    }

    /**
     * Checks the coverage described by the specified geometry and sample model does not exceeds the
     * output WCS limits
     */
    public static void checkOutputLimits(
            WCSInfo info, GridEnvelope2D gridRange2D, SampleModel sampleModel) {
        // do we have to check a limit at all?
        long limit = info.getMaxOutputMemory() * 1024;
        if (limit <= 0) {
            return;
        }

        // compute the coverage memory usage and compare with limit
        long actual = getCoverageSize(gridRange2D, sampleModel);
        if (actual > limit) {
            throw new WcsException(
                    "This request is trying to generate too much data, "
                            + "the limit is "
                            + formatBytes(limit)
                            + " but the actual amount of bytes to be "
                            + "written in the output is "
                            + formatBytes(actual));
        }
    }

    /**
     * Checks the coverage read is below the input limits. Mind, at this point the reader might have
     * have subsampled the original image in some way so it is expected the coverage is actually
     * smaller than what computed but {@link #checkInputLimits(CoverageInfo, GridCoverage2DReader,
     * GeneralEnvelope)}, however that method might have failed the computation due to lack of
     * metadata (or wrong metadata) so it's safe to double check the actual coverage wit this one.
     * Mind, this method might cause the coverage to be fully read in memory (if that is the case,
     * the actual WCS processing chain would result in the same behavior so this is not causing any
     * extra memory usage, just makes it happen sooner)
     */
    public static void checkInputLimits(WCSInfo info, GridCoverage2D coverage) {
        // do we have to check a limit at all?
        long limit = info.getMaxInputMemory() * 1024;
        if (limit <= 0) {
            return;
        }

        // compute the coverage memory usage and compare with limit
        long actual =
                getCoverageSize(
                        coverage.getGridGeometry().getGridRange2D(),
                        coverage.getRenderedImage().getSampleModel());
        if (actual > limit) {
            throw new WcsException(
                    "This request is trying to read too much data, "
                            + "the limit is "
                            + formatBytes(limit)
                            + " but the actual amount of "
                            + "bytes to be read is "
                            + formatBytes(actual));
        }
    }

    /**
     * Computes the size of a grid coverage in bytes given its grid envelope and the target sample
     * model
     */
    static long getCoverageSize(GridEnvelope2D envelope, SampleModel sm) {
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
     * Utility method to called to check the amount of data to be read does not exceed the WCS read
     * limits. This method has to jump through a few hoops to estimate the size of the data to be
     * read without having to actually read the coverage (which might trigger the loading of the
     * full coverage in memory)
     *
     * @throws WcsException if the coverage size exceeds the configured limits
     */
    public static void checkInputLimits(
            WCSInfo info,
            CoverageInfo meta,
            GridCoverage2DReader reader,
            GridGeometry2D gridGeometry)
            throws WcsException {
        // do we have to check a limit at all?
        long limit = info.getMaxInputMemory() * 1024;
        if (limit <= 0) {
            return;
        }

        // compute the actual amount of data read
        long actual = 0;
        try {
            // if necessary reproject back to the original CRS
            GeneralEnvelope requestedEnvelope = new GeneralEnvelope(gridGeometry.getEnvelope());
            final CoordinateReferenceSystem requestCRS =
                    requestedEnvelope.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem nativeCRS = reader.getCoordinateReferenceSystem();
            if (!CRS.equalsIgnoreMetadata(requestCRS, nativeCRS)) {
                requestedEnvelope = CRS.transform(requestedEnvelope, nativeCRS);
            }
            // intersect with the native envelope, we cannot read outside of it
            requestedEnvelope.intersect(reader.getOriginalEnvelope());

            // check if we are still reading anything
            if (!requestedEnvelope.isEmpty()) {
                MathTransform crsToGrid = meta.getGrid().getGridToCRS().inverse();
                GeneralEnvelope requestedGrid = CRS.transform(crsToGrid, requestedEnvelope);
                double[] spans = new double[requestedGrid.getDimension()];
                double[] resolutions = new double[requestedGrid.getDimension()];
                for (int i = 0; i < spans.length; i++) {
                    spans[i] = requestedGrid.getSpan(i);
                    resolutions[i] = requestedEnvelope.getSpan(i) / spans[i];
                }

                // adjust the spans based on the overview policy
                OverviewPolicy policy = info.getOverviewPolicy();
                double[] readResoutions = reader.getReadingResolutions(policy, resolutions);
                double[] baseResolutions =
                        reader.getReadingResolutions(OverviewPolicy.IGNORE, resolutions);
                for (int i = 0; i < spans.length; i++) {
                    spans[i] *= readResoutions[i] / baseResolutions[i];
                }

                // compute how many pixels we're going to read
                long pixels = 1;
                for (int i = 0; i < requestedGrid.getDimension(); i++) {
                    pixels *= Math.ceil(requestedGrid.getSpan(i));
                }

                // compute the size of a pixel using the coverage metadata (the reader won't give
                // us any information about the bands)
                long pixelSize = 0;
                if (meta.getDimensions() != null) {
                    for (CoverageDimensionInfo dimension : meta.getDimensions()) {
                        int size = guessSizeFromRange(dimension.getRange());
                        if (size == 0) {
                            LOGGER.log(
                                    Level.INFO,
                                    "Failed to guess the size of dimension "
                                            + dimension.getName()
                                            + ", skipping the pre-read check");
                            pixelSize = -1;
                            break;
                        }
                        pixelSize += size;
                    }
                }

                actual = pixels * pixelSize / 8;
            }
        } catch (Throwable t) {
            throw new WcsException("An error occurred while checking serving limits", t);
        }

        if (actual < 0) {
            // TODO: provide some more info about the request? It seems to be we'd have to
            // log again the full request... unless the request logger starts dumping the thread
            // id, in that case we can just refer to that and tell the admin to enable
            // the request logger to debug these issues?
            LOGGER.log(
                    Level.INFO,
                    "Warning, we could not estimate the amount of bytes to be "
                            + "read from the coverage source for the current request");
        }

        if (actual > limit) {
            throw new WcsException(
                    "This request is trying to read too much data, "
                            + "the limit is "
                            + formatBytes(limit)
                            + " but the actual amount of bytes "
                            + "to be read is "
                            + formatBytes(actual));
        }
    }

    /** Guesses the size of the sample able to contain the range fully */
    static int guessSizeFromRange(NumberRange range) {
        double min = range.getMinimum();
        double max = range.getMaximum();
        double diff = max - min;

        if (diff <= ((int) Byte.MAX_VALUE - (int) Byte.MIN_VALUE)) {
            return 8;
        } else if (diff <= ((int) Short.MAX_VALUE - (int) Short.MIN_VALUE)) {
            return 16;
        } else if (diff <= ((double) Integer.MAX_VALUE - (double) Integer.MIN_VALUE)) {
            return 32;
        } else if (diff <= ((double) Float.MAX_VALUE - (double) Float.MIN_VALUE)) {
            return 32;
        } else {
            return 64;
        }
    }

    /** Utility function to format a byte amount into a human readable string */
    static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.##").format(bytes / 1024.0) + "KB";
        } else {
            return new DecimalFormat("#.##").format(bytes / 1024.0 / 1024.0) + "MB";
        }
    }

    /** Returns the reader hints based on the current WCS configuration */
    public static Hints getReaderHints(WCSInfo wcs) {
        Hints hints = new Hints();
        hints.add(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        if (wcs.getOverviewPolicy() == null) {
            hints.add(new Hints(Hints.OVERVIEW_POLICY, OverviewPolicy.IGNORE));
        } else {
            hints.add(new Hints(Hints.OVERVIEW_POLICY, wcs.getOverviewPolicy()));
        }
        hints.put(
                Hints.DECIMATION_POLICY,
                wcs.isSubsamplingEnabled() ? DecimationPolicy.ALLOW : DecimationPolicy.DISALLOW);
        return hints;
    }

    /**
     * Returns an eventual filter included among the parsed kvp map of the current request. Will
     * work for CQL_FILTER, FILTER and FEATURE_ID
     */
    public static Filter getRequestFilter() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return null;
        }
        Object filter = request.getKvp().get("FILTER");
        if (!(filter instanceof Filter)) {
            filter = request.getKvp().get("CQL_FILTER");
            if (filter instanceof List) {
                List list = (List) filter;
                if (!list.isEmpty()) {
                    filter = list.get(0);
                }
            }
        }
        if (!(filter instanceof Filter)) {
            filter = request.getKvp().get("FEATURE_ID");
        }

        if (filter instanceof Filter) {
            return (Filter) filter;
        } else {
            return null;
        }
    }

    /**
     * Checks the coverage described by the specified source coverage and target band names does not
     * exceeds the output
     */
    public static void checkOutputLimits(WCSInfo wcs, GridCoverage2D gc, int[] indexes) {
        // do we have to check a limit at all?
        long limit = wcs.getMaxOutputMemory() * 1024;
        if (limit <= 0) {
            return;
        }

        // === compute the coverage memory usage and compare with limit
        final long pixelsNumber = computePixelsNumber(gc.getGridGeometry().getGridRange2D());

        // bands
        long pixelSize = 0;
        final RenderedImage image = gc.getRenderedImage();
        final SampleModel sm = image.getSampleModel();
        for (int band : indexes) {
            pixelSize += sm.getSampleSize(band);
        }
        long actual = pixelsNumber * pixelSize / 8; // in bytes

        if (actual > limit) {
            throw new WcsException(
                    "This request is trying to generate too much data, "
                            + "the limit is "
                            + formatBytes(limit)
                            + " but the actual amount of bytes to be "
                            + "written in the output is "
                            + formatBytes(actual));
        }
    }

    /**
     * Computes the number of pixels for this {@link GridEnvelope2D}.
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

    /** Replace or add the provided parameter in the read parameters */
    public static <T> GeneralParameterValue[] replaceParameter(
            GeneralParameterValue[] readParameters, Object value, ParameterDescriptor<T> pd) {

        // scan all the params looking for the one we want to add
        for (GeneralParameterValue gpv : readParameters) {
            // in case of match of any alias add a param value to the lot
            if (gpv.getDescriptor().getName().equals(pd.getName())) {
                ((ParameterValue) gpv).setValue(value);
                // leave
                return readParameters;
            }
        }

        // add it to the array
        // add to the list
        GeneralParameterValue[] readParametersClone =
                new GeneralParameterValue[readParameters.length + 1];
        System.arraycopy(readParameters, 0, readParametersClone, 0, readParameters.length);
        final ParameterValue<T> pv = pd.createValue();
        pv.setValue(value);
        readParametersClone[readParameters.length] = pv;
        return readParametersClone;
    }

    /**
     * Maps the declared envelope so that it fits with the native grid geometry, making sure a
     * request without parameter does not result in pixel resampling.
     *
     * @param ci The coverage info with the configured envelope
     * @param reader The reader
     * @return
     */
    public static ReferencedEnvelope fitEnvelope(CoverageInfo ci, GridCoverage2DReader reader) {
        try {
            ReferencedEnvelope bounds = ci.boundingBox();

            return fitEnvelope(bounds, reader);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fit the grid geometry to the declared envelope/crs", e);
        }
    }

    protected static ReferencedEnvelope fitEnvelope(
            ReferencedEnvelope bounds, GridCoverage2DReader reader) {
        if (fitUnecessary(bounds, reader)) {
            return bounds;
        }

        if (!simpleFitSupported(bounds, reader)) {
            return bounds;
        }

        return simpleEnvelopeFit(bounds, reader);
    }

    private static ReferencedEnvelope simpleEnvelopeFit(
            ReferencedEnvelope bounds, GridCoverage2DReader reader) {
        GeneralEnvelope original = reader.getOriginalEnvelope();
        AffineTransform2D at =
                (AffineTransform2D) reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        double scaleX = Math.abs(at.getScaleX());
        double minX = fit(bounds.getMinimum(0), original.getMinimum(0), scaleX);
        double maxX = fit(bounds.getMaximum(0), original.getMaximum(0), scaleX);
        if (maxX <= minX) maxX = minX + scaleX;
        double scaleY = Math.abs(at.getScaleY());
        double minY = fit(bounds.getMinimum(1), original.getMinimum(1), scaleY);
        double maxY = fit(bounds.getMaximum(1), original.getMaximum(1), scaleY);
        if (maxY <= minY) maxY = minY + scaleY;

        return new ReferencedEnvelope(
                minX, maxX, minY, maxY, bounds.getCoordinateReferenceSystem());
    }

    private static boolean simpleFitSupported(
            ReferencedEnvelope bounds, GridCoverage2DReader reader) {
        // in case of reprojection resampling will happen anyways
        if (!CRS.equalsIgnoreMetadata(
                bounds.getCoordinateReferenceSystem(), reader.getCoordinateReferenceSystem())) {
            LOGGER.fine(
                    "Cannot fit the declared envelope to native grid: reprojection is being used");
            return false;
        }

        // same if the transformation is not an affine
        MathTransform tx = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        if (!(tx instanceof AffineTransform2D)) {
            LOGGER.fine(
                    "Cannot fit the declared envelope to native grid: grid to world is not an affine");
            return false;
        }

        AffineTransform2D at = (AffineTransform2D) tx;
        if (Math.abs(at.getShearX()) > SHEAR_EPS || Math.abs(at.getShearY()) > SHEAR_EPS) {
            LOGGER.fine(
                    "Cannot fit the declared envelope to native grid: grid to world affine has shear factors");
            return false;
        }

        return true;
    }

    private static boolean fitUnecessary(ReferencedEnvelope bounds, GridCoverage2DReader reader) {
        return bounds.equals(ReferencedEnvelope.reference(reader.getOriginalEnvelope()));
    }

    /**
     * Fits the a given corner coordinate to a grid identified by origin and pixel size, using
     * {@link DD} for higher precision
     *
     * @param cornerValue The value to fit
     * @param origin The origin of the grid
     * @param scale The size of the pixel
     * @return The corner value snapped to the closest grid position
     */
    private static double fit(double cornerValue, double origin, double scale) {
        // using DoubleDouble to get as much precision as possible (resolutions in degrees
        // tend to be very small numbers)
        DD cv = DD.valueOf(cornerValue);
        DD px = cv.subtract(origin).divide(scale);
        DD roundPx = roundDD(px);
        double fit = cv.subtract(px.subtract(roundPx).multiply(scale)).doubleValue();
        return fit;
    }

    private static DD roundDD(DD x) {
        DD xCeil = x.ceil();
        DD spaceAbove = xCeil.subtract(x);
        DD xFloor = x.floor();
        DD spaceBelow = x.subtract(xFloor);
        DD roundPx = spaceAbove.compareTo(spaceBelow) < 0 ? xCeil : xFloor;
        return roundPx;
    }

    public static GridGeometry2D fitGridGeometry(CoverageInfo ci, GridCoverage2DReader reader) {
        // if envelope has not been remapped, then go with the original grid geometry
        MathTransform gridToWorld = reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER);
        GridGeometry2D nativeGridGeometry =
                new GridGeometry2D(
                        reader.getOriginalGridRange(),
                        gridToWorld,
                        reader.getCoordinateReferenceSystem());

        try {
            ReferencedEnvelope nativeEnvelope = ci.boundingBox();
            if (fitUnecessary(nativeEnvelope, reader)) {
                return nativeGridGeometry;
            }

            if (!simpleFitSupported(nativeEnvelope, reader)) {
                // could make up something more precise for specific rotation transforms
                return reprojectGridGeometryFit(reader, nativeEnvelope);
            }

            return simpleGridGeometryFit(
                    simpleEnvelopeFit(nativeEnvelope, reader),
                    (AffineTransform2D) reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fit the grid geometry to the declared envelope/crs", e);
        }
    }

    /**
     * In case of reprojection, the grid geometry is just loosely fitted by reprojecting a fake,
     * small raster in the center of the area, and picking up scale factors from its grid geometry
     *
     * @param reader
     * @param envelope
     * @return
     */
    private static GridGeometry2D reprojectGridGeometryFit(
            GridCoverage2DReader reader, ReferencedEnvelope envelope) {
        // build a fake coverage in the same area as the original one, with
        // the same pixel size and raster size, but without actually pushing it in memory:
        // ConstantDescriptor is a JAI operation, will produce tiles only on pull.
        AffineTransform2D originalG2W =
                (AffineTransform2D) reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        double scale = XAffineTransform.getScale(originalG2W);
        GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
        AffineTransform2D g2w =
                new AffineTransform2D(
                        scale,
                        0,
                        0,
                        -scale,
                        originalEnvelope.getMinimum(0),
                        originalEnvelope.getMaximum(1));
        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridEnvelope range = reader.getOriginalGridRange();
        RenderedOp image =
                ConstantDescriptor.create(
                        (float) range.getSpan(0), (float) range.getSpan(1), new Byte[] {0}, null);
        GridCoverage2D sampleCoverage =
                factory.create(
                        "sample",
                        image,
                        originalEnvelope.getCoordinateReferenceSystem(),
                        g2w,
                        null,
                        null,
                        null);

        // reproject to target CRS (again does not really compute pixels, just set up a JAI chain)
        CoverageProcessor processor = CoverageProcessor.getInstance();
        final Operation operation = processor.getOperation("Resample");
        final ParameterValueGroup param = operation.getParameters().clone();
        param.parameter("source").setValue(sampleCoverage);
        param.parameter("CoordinateReferenceSystem")
                .setValue(envelope.getCoordinateReferenceSystem());
        GridCoverage2D reprojected = (GridCoverage2D) processor.doOperation(param, hints);
        GridGeometry2D gg = reprojected.getGridGeometry();

        // fit the grid geometry based on the reprojected grid to world
        return simpleGridGeometryFit(
                envelope, ((AffineTransform2D) gg.getGridToCRS(PixelInCell.CELL_CORNER)));
    }

    private static GridGeometry2D simpleGridGeometryFit(
            ReferencedEnvelope envelope, AffineTransform2D g2w) {
        // move the top left corner where the fitted envelope is
        AffineTransform2D fittedG2W =
                new AffineTransform2D(
                        g2w.getScaleX(),
                        g2w.getShearX(),
                        g2w.getShearY(),
                        g2w.getScaleY(),
                        envelope.getMinimum(0),
                        envelope.getMaximum(1));

        try {
            GeneralEnvelope gridEnvelope = CRS.transform(fittedG2W.inverse(), envelope);
            GridEnvelope2D fittedGridRange =
                    new GridEnvelope2D(
                            0,
                            0,
                            (int) Math.round(gridEnvelope.getSpan(0)),
                            (int) Math.round(gridEnvelope.getSpan(1)));
            return new GridGeometry2D(
                    fittedGridRange, fittedG2W, envelope.getCoordinateReferenceSystem());
        } catch (TransformException e) {
            throw new RuntimeException("Failed to invert grid to world", e);
        }
    }
}
