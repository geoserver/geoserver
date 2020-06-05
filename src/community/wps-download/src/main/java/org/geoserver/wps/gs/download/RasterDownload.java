/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;
import it.geosolutions.io.output.adapter.OutputStreamAdapter;
import it.geosolutions.jaiext.utilities.ImageLayout2;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.*;
import javax.media.jai.operator.MosaicDescriptor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.wps.VerticalCRSConfigurationPanel;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.download.vertical.VerticalResampler;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.resource.GridCoverageResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.*;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.Parameter;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.raster.BandSelectProcess;
import org.geotools.process.raster.CropCoverage;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.crs.WrappingProjectionHandler;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageReaderHelper;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRendererUtilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Implements the download services for raster data. If limits are configured this class will use
 * {@link LimitedImageOutputStream}, which raises an exception when the download size exceeded the
 * limits.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class RasterDownload {

    private static final Logger LOGGER = Logging.getLogger(RasterDownload.class);

    /** The {@link DownloadServiceConfiguration} object containing the configured limits. */
    private DownloadServiceConfiguration limits;

    /** The resource manager for handling the used resources. */
    private WPSResourceManager resourceManager;

    private static final GridCoverageFactory GC_FACTORY = new GridCoverageFactory();

    private static final BorderExtender BORDER_EXTENDER_COPY =
            BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    static final RenderingHints BORDER_EXTENDER_HINTS =
            new RenderingHints(JAI.KEY_BORDER_EXTENDER, BORDER_EXTENDER_COPY);

    /** The application context used to look-up PPIO factories */
    private ApplicationContext context;

    private Catalog catalog;

    /**
     * Constructor, takes a {@link DownloadEstimatorProcess}.
     *
     * @param limits the {@link DownloadEstimatorProcess} to check for not exceeding the download
     *     limits.
     * @param resourceManager the {@link WPSResourceManager} to handl generated resources
     */
    public RasterDownload(
            DownloadServiceConfiguration limits,
            WPSResourceManager resourceManager,
            ApplicationContext context,
            Catalog catalog) {
        this.limits = limits;
        this.resourceManager = resourceManager;
        this.context = context;
        this.catalog = catalog;
    }

    /**
     * This method does the following operations:
     *
     * <ul>
     *   <li>Uses only those bands specified by indices (if defined)
     *   <li>Reprojection of the coverage (if needed)
     *   <li>Clips the coverage (if needed)
     *   <li>Scales the coverage to match the target size (if needed)
     *   <li>Writes the result
     *   <li>Cleanup the generated coverages
     * </ul>
     *
     * @param mimeType mimetype of the result
     * @param progressListener listener to use for logging the operations
     * @param coverageInfo resource associated to the Coverage
     * @param roi input ROI object
     * @param targetCRS CRS of the file to write
     * @param clip indicates if the clipping geometry must be exactly that of the ROI or simply its
     *     envelope
     * @param filter the {@link Filter} to load the data
     * @param interpolation interpolation method to use when reprojecting / scaling
     * @param targetSizeX the size of the target image along the X axis
     * @param targetSizeY the size of the target image along the Y axis
     * @param bandIndices the indices of the bands used for the final result
     * @param writeParams optional writing params
     * @param minimizeReprojections When dealing with a Heterogeneous CRS mosaic, avoid
     *     reprojections of the granules within the ROI, having their nativeCRS equal to the
     *     targetCRS
     * @param bestResolutionOnMatchingCRS When dealing with a Heterogeneous CRS mosaic, given a ROI
     *     and a TargetCRS, with no target size being specified, get the best resolution of data
     *     having nativeCrs matching the TargetCRS
     * @param resolutionsDifferenceTolerance a tolerance value to control the use" of native
     *     resolution of the data: if the percentage difference between original and reprojected
     *     coverages resolutions is below the specified value, the reprojected coverage will be
     *     forced to use native resolutions
     * @param targetVerticalCRS
     */
    public Resource execute(
            String mimeType,
            final ProgressListener progressListener,
            CoverageInfo coverageInfo,
            Geometry roi,
            CoordinateReferenceSystem targetCRS,
            boolean clip,
            Filter filter,
            Interpolation interpolation,
            Integer targetSizeX,
            Integer targetSizeY,
            int[] bandIndices,
            Parameters writeParams,
            boolean minimizeReprojections,
            boolean bestResolutionOnMatchingCRS,
            double resolutionsDifferenceTolerance,
            CoordinateReferenceSystem targetVerticalCRS)
            throws Exception {

        List<GridCoverage2D> disposableSources = new ArrayList<GridCoverage2D>();
        GridCoverage2D gridCoverage = null;
        try {
            // get a reader for this CoverageInfo
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Getting reader for the coverage");
            }
            final GridCoverage2DReader reader =
                    (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
            CRSRequestHandler crsRequestHandler =
                    new CRSRequestHandler(reader, catalog, targetCRS, roi);
            crsRequestHandler.setFilter(filter);
            crsRequestHandler.setMinimizeReprojections(minimizeReprojections);
            crsRequestHandler.setUseBestResolutionOnMatchingCRS(bestResolutionOnMatchingCRS);
            crsRequestHandler.setResolutionsDifferenceTolerance(resolutionsDifferenceTolerance);
            crsRequestHandler.init();
            boolean useTargetCrsAsNative = crsRequestHandler.canUseTargetCRSAsNative();
            final ParameterValueGroup readParametersDescriptor =
                    reader.getFormat().getReadParameters();
            final List<GeneralParameterDescriptor> parameterDescriptors =
                    readParametersDescriptor.getDescriptor().descriptors();
            Map<String, Serializable> coverageParameters = coverageInfo.getParameters();
            GeneralParameterValue[] readParameters =
                    CoverageUtils.getParameters(
                            readParametersDescriptor, coverageParameters, false);

            // read GridGeometry preparation and scaling setup if needed
            GridGeometry2D requestedGridGeometry = null;
            boolean isImposedTargetSize = targetSizeX != null || targetSizeY != null;

            if (targetSizeX == null && targetSizeY == null) {
                // No size is specified. Just do a read and reproject (if needed) + a final crop
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "No Target size has been specified. Requested GridGeometry "
                                    + "will be automatically computed");
                }
                GridGeometryProvider provider = new GridGeometryProvider(crsRequestHandler);
                requestedGridGeometry = provider.getGridGeometry();

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Computed requested GridGeometry: " + requestedGridGeometry.toString());
                }
                readParameters =
                        CoverageUtils.mergeParameter(
                                parameterDescriptors,
                                readParameters,
                                requestedGridGeometry,
                                AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().getCode());
            } else {
                if (targetSizeX == null || targetSizeY == null) {
                    // one of the 2 sizes is not specified. Delegate
                    // scaleToTarget to compute the second one.
                    ScaleToTarget scaling = new ScaleToTarget(reader);
                    scaling.setTargetSize(targetSizeX, targetSizeY);
                    Integer[] computedSizes = scaling.getTargetSize();
                    targetSizeX = computedSizes[0];
                    targetSizeY = computedSizes[1];
                }

                // Since we have imposed a target size, delegate GridCoverageRenderer to do all the
                // dirty job
                requestedGridGeometry =
                        new GridGeometry2D(
                                new GridEnvelope2D(0, 0, targetSizeX, targetSizeY),
                                crsRequestHandler.getTargetEnvelope());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Target size has been specified. Setting up requested GridGeometry: "
                                    + requestedGridGeometry.toString());
                }
            }

            if (useTargetCrsAsNative) {
                readParameters =
                        CoverageUtils.mergeParameter(
                                Collections.singletonList(
                                        ImageMosaicFormat.OUTPUT_TO_ALTERNATIVE_CRS),
                                readParameters,
                                true,
                                ImageMosaicFormat.OUTPUT_TO_ALTERNATIVE_CRS.getName().getCode());
            }

            readParameters =
                    updateReadParams(readParameters, parameterDescriptors, bandIndices, filter);

            // Setting background values and color
            double[] backgroundValues = getBackgroundValues(coverageParameters, readParameters);

            //
            // Read and Reproject
            //
            gridCoverage =
                    readAndReproject(
                            readParameters,
                            targetSizeX,
                            targetSizeY,
                            interpolation,
                            backgroundValues,
                            isImposedTargetSize,
                            crsRequestHandler,
                            disposableSources);

            // Add a bandSelectProcess call if the reader doesn't support bands
            gridCoverage =
                    bandSelect(
                            reader, readParameters, bandIndices, gridCoverage, disposableSources);

            //
            // Handle clip/crop/extend to region
            //
            if (roi != null) {
                // ROI requires a crop/clip
                boolean crop = true;

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Handling ROI");
                }

                if (isImposedTargetSize) {
                    crop = false; // There might be the case that GridCoverageRenderer already
                    // provided what requested

                    final RenderedImage rasterData = gridCoverage.getRenderedImage();
                    final GridEnvelope requestedRange =
                            (GridEnvelope) requestedGridGeometry.getGridRange();

                    // Preliminar check between requested imageLayout and coverage imageLayout
                    final int requestedW = requestedRange.getSpan(0);
                    final int requestedH = requestedRange.getSpan(1);
                    final int imageW = rasterData.getWidth();
                    final int imageH = rasterData.getHeight();

                    if ((imageW == requestedW) && (imageH == requestedH) && !clip) {
                        // No refining is needed. Write it as is
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                    Level.FINE,
                                    "No Crop is needed. "
                                            + "Writing the coverage as provided by GridCoverageRenderer");
                        }

                        disposableSources.add(gridCoverage);
                        return writeRaster(mimeType, coverageInfo, gridCoverage, writeParams);

                    } else {
                        // Check if an actual crop is needed
                        crop = cropIsNeeded(rasterData, requestedRange);

                        if (!crop && !clip) {
                            // The extent of the returned image is smaller than the requested
                            // extent.
                            // Let's do a mosaic to return the requested extent instead.
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(
                                        Level.FINE, "Expanding the result to the requested area");
                            }
                            disposableSources.add(gridCoverage);
                            gridCoverage =
                                    extendToRegion(
                                            gridCoverage, requestedGridGeometry, backgroundValues);
                            return writeRaster(mimeType, coverageInfo, gridCoverage, writeParams);
                        }
                    }
                }

                // Do we still need to do some cropping
                if (crop || clip) {
                    // Crop or Clip
                    final CropCoverage cropCoverage = new CropCoverage();

                    // Get the proper ROI (depending on clip parameter and CRS)
                    Geometry croppingRoi = crsRequestHandler.getRoiManager().getTargetRoi(clip);
                    disposableSources.add(gridCoverage);
                    gridCoverage =
                            cropCoverage.execute(gridCoverage, croppingRoi, progressListener);

                    if (gridCoverage == null) {
                        throw new WPSException(
                                "No data left after applying the ROI. This means there "
                                        + "is source data, but none matching the requested ROI");
                    }
                }
            }
            disposableSources.add(gridCoverage);

            CoordinateReferenceSystem sourceVerticalCRS = null;
            if (targetVerticalCRS != null) {
                MetadataMap metadata = coverageInfo.getMetadata();
                if (metadata != null
                        && metadata.containsKey(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY)) {
                    String sourceVerticalCRSvalue =
                            metadata.get(VerticalCRSConfigurationPanel.VERTICAL_CRS_KEY).toString();
                    if (sourceVerticalCRSvalue != null) {
                        sourceVerticalCRS = CRS.decode(sourceVerticalCRSvalue);
                    }
                }
                if (sourceVerticalCRS == null) {
                    throw new WPSException(
                            "A VerticalCRS reprojection has been required but no source"
                                    + " VerticalCRS has been configured in the coverage.");
                }
                if (!CRS.equalsIgnoreMetadata(sourceVerticalCRS, targetVerticalCRS)) {
                    VerticalResampler verticalResampler =
                            new VerticalResampler(
                                    sourceVerticalCRS,
                                    targetVerticalCRS,
                                    GC_FACTORY,
                                    progressListener);
                    gridCoverage = verticalResampler.resample(gridCoverage);
                }
            }

            //
            // Writing
            //
            return writeRaster(mimeType, coverageInfo, gridCoverage, writeParams);

        } finally {
            for (GridCoverage2D disposableCoverage : disposableSources) {
                resourceManager.addResource(new GridCoverageResource(disposableCoverage));
            }
        }
    }

    /** Read and reproject. */
    private GridCoverage2D readAndReproject(
            GeneralParameterValue[] readParameters,
            Integer targetSizeX,
            Integer targetSizeY,
            Interpolation interpolation,
            double[] backgroundValues,
            boolean isImposedTargetSize,
            CRSRequestHandler crsRequestHandler,
            List<GridCoverage2D> disposableSources)
            throws TransformException, NoninvertibleTransformException, FactoryException,
                    IOException {

        GridCoverage2DReader reader = crsRequestHandler.getReader();
        CoordinateReferenceSystem targetCRS = crsRequestHandler.getSelectedTargetCRS();
        if (isImposedTargetSize) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Target Size has been imposed");
            // mimic the GridCoverageRenderer logic without raster symbolization in place
            // and other colorMap related steps
            CoordinateReferenceSystem sourceCRS = crsRequestHandler.getSelectedNativeCRS();
            Rectangle destinationSize = new Rectangle(0, 0, targetSizeX, targetSizeY);

            GeneralEnvelope destinationEnvelope =
                    new GeneralEnvelope(
                            new ReferencedEnvelope(
                                    crsRequestHandler.getTargetEnvelope(), targetCRS));
            AffineTransform finalWorldToGrid =
                    computeWorldToGrid(destinationEnvelope, destinationSize);
            Hints hints = prepareHints(interpolation);

            List<GridCoverage2D> coverages;
            // read all the coverages we need, cut and whatnot
            GridCoverageReaderHelper rh =
                    new GridCoverageReaderHelper(
                            reader,
                            destinationSize,
                            ReferencedEnvelope.reference(destinationEnvelope),
                            interpolation,
                            hints);

            ProjectionHandler handler = null;
            if (!crsRequestHandler.canUseTargetCRSAsNative()) {
                handler = ProjectionHandlerFinder.getHandler(rh.getReadEnvelope(), sourceCRS, true);
                if (handler instanceof WrappingProjectionHandler) {
                    ((WrappingProjectionHandler) handler).setDatelineWrappingCheckEnabled(false);
                }
            }
            coverages = rh.readCoverages(readParameters, handler, GC_FACTORY);
            coverages =
                    GridCoverageRendererUtilities.forceToValidBounds(
                            coverages, handler, backgroundValues, targetCRS, hints);

            // reproject if needed
            coverages =
                    GridCoverageRendererUtilities.reproject(
                            coverages,
                            targetCRS,
                            interpolation,
                            destinationEnvelope,
                            backgroundValues,
                            GC_FACTORY,
                            hints);

            // displace them if needed via a projection handler
            coverages =
                    GridCoverageRendererUtilities.displace(
                            coverages,
                            handler,
                            destinationEnvelope,
                            sourceCRS,
                            targetCRS,
                            GC_FACTORY);

            // remove displaced/reprojected coverages being outside of the destination envelope
            GridCoverageRendererUtilities.removeNotIntersecting(coverages, destinationEnvelope);

            List<GridCoverage2D> transformedCoverages = new ArrayList<>();
            for (GridCoverage2D displaced : coverages) {
                GridCoverage2D transformed =
                        GridCoverageRendererUtilities.affine(
                                displaced,
                                interpolation,
                                finalWorldToGrid,
                                backgroundValues,
                                true,
                                GC_FACTORY,
                                hints);
                if (transformed != null) {
                    transformedCoverages.add(transformed);
                }
            }
            coverages = transformedCoverages;

            GridCoverage2D mosaicked =
                    GridCoverageRendererUtilities.mosaicSorted(
                            coverages, destinationEnvelope, backgroundValues, hints);

            // the mosaicking can cut off images that are just slightly out of the
            // request (effect of the read buffer + a request touching the actual data area)
            if (mosaicked == null) {
                return null;
            }

            // at this point, we might have a coverage that's still slightly larger
            // than the one requested, crop as needed
            GridCoverage2D cropped =
                    GridCoverageRendererUtilities.crop(
                            mosaicked, destinationEnvelope, false, backgroundValues, hints);

            if (cropped == null || cropped.getRenderedImage() == null) {
                throw new WPSException(
                        "The reader did not return anything"
                                + "It normally means there is nothing there, or the data got filtered out by the ROI or filter");
            }
            return cropped;
        } else {

            boolean forceNativeRes = crsRequestHandler.getResolutionsDifferenceTolerance() != 0d;
            // If not, proceed with standard read and reproject
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Reading the coverage");
            }
            GridCoverage2D gridCoverage = reader.read(readParameters);
            // check, the reader might have returned a null coverage
            if (gridCoverage == null) {
                throw new WPSException(
                        "The reader did not return any data for current input "
                                + "parameters. It normally means there is nothing there, or the data got filtered out by the ROI or filter");
            }

            // Reproject if needed
            if (crsRequestHandler.needsReprojection()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Reprojecting the coverage");
                }
                disposableSources.add(gridCoverage);
                GridCoverage2D testCoverage =
                        (GridCoverage2D)
                                Operations.DEFAULT.resample(
                                        gridCoverage,
                                        targetCRS,
                                        null,
                                        interpolation,
                                        backgroundValues);
                // check for native resolution to be applied only if tolerance is not 0
                if (!forceNativeRes) {
                    return testCoverage;
                }
                GridGeometryProvider gridGeometryProvider =
                        new GridGeometryProvider(crsRequestHandler);
                GridGeometry2D gg2D =
                        gridGeometryProvider.getGridGeometryWithNativeResolution(
                                gridCoverage, testCoverage);
                if (gg2D != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Forcing native resolution on reprojected coverage");
                    }
                    gridCoverage =
                            (GridCoverage2D)
                                    Operations.DEFAULT.resample(
                                            gridCoverage,
                                            targetCRS,
                                            gg2D,
                                            interpolation,
                                            backgroundValues);
                } else {
                    gridCoverage = testCoverage;
                }
            }
            return gridCoverage;
        }
    }

    private AffineTransform computeWorldToGrid(
            GeneralEnvelope destinationEnvelope, Rectangle destinationSize)
            throws NoninvertibleTransformException {
        final GridToEnvelopeMapper gridToEnvelopeMapper = new GridToEnvelopeMapper();
        gridToEnvelopeMapper.setPixelAnchor(PixelInCell.CELL_CORNER);
        gridToEnvelopeMapper.setGridRange(new GridEnvelope2D(destinationSize));
        gridToEnvelopeMapper.setEnvelope(destinationEnvelope);
        AffineTransform finalGridToWorld =
                new AffineTransform(gridToEnvelopeMapper.createAffineTransform());
        AffineTransform finalWorldToGrid = finalGridToWorld.createInverse();
        return finalWorldToGrid;
    }

    private Hints prepareHints(Interpolation interpolation) {
        // Interpolation
        Hints hints = new Hints(new RenderingHints(JAI.KEY_INTERPOLATION, interpolation));
        hints.put(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
        hints.add(
                new RenderingHints(
                        JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
        if (interpolation instanceof InterpolationNearest) {
            hints.add(new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE));
            hints.add(new RenderingHints(JAI.KEY_TRANSFORM_ON_COLORMAP, Boolean.TRUE));
        } else {
            hints.add(new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.TRUE));
            hints.add(new RenderingHints(JAI.KEY_TRANSFORM_ON_COLORMAP, Boolean.FALSE));
        }

        // Tile Size
        final ImageLayout layout = new ImageLayout2();
        layout.setTileGridXOffset(0).setTileGridYOffset(0).setTileHeight(512).setTileWidth(512);
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));
        return hints;
    }

    /** Extract the backgroundValues which will be used for mosaicking operations. */
    private double[] getBackgroundValues(
            Map<String, Serializable> coverageParameters, GeneralParameterValue[] readParameters) {
        double[] backgroundValues = null;
        if (coverageParameters != null && coverageParameters.containsKey("BackgroundValues")) {
            for (GeneralParameterValue readParameter : readParameters) {
                if ("BackgroundValues"
                        .equalsIgnoreCase(readParameter.getDescriptor().getName().toString())) {
                    Object bgValue = ((ParameterValue) readParameter).getValue();
                    if (bgValue != null && bgValue instanceof double[]) {
                        backgroundValues = ((double[]) bgValue);
                    }
                    break;
                }
            }
        }
        return backgroundValues;
    }

    /**
     * Check if a crop operation is needed (meaning that the extent of the returned image contains
     * the requested extent).
     */
    private boolean cropIsNeeded(RenderedImage rasterData, GridEnvelope requestedRange) {
        final int requestedW = requestedRange.getSpan(0);
        final int requestedH = requestedRange.getSpan(1);
        final int requestedMinX = requestedRange.getLow(0);
        final int requestedMinY = requestedRange.getLow(1);
        final int requestedMaxX = requestedRange.getHigh(0);
        final int requestedMaxY = requestedRange.getHigh(1);

        final int imageW = rasterData.getWidth();
        final int imageH = rasterData.getHeight();
        final int minX = rasterData.getMinX();
        final int minY = rasterData.getMinY();
        final int maxX = minX + imageW - 1;
        final int maxY = minY + imageH - 1;

        if (((imageW + ((minX >= 0) ? 0 : minX)) >= requestedW)
                && ((imageH + ((minY >= 0) ? 0 : minY)) >= requestedH)
                && ((requestedMinX >= minX)
                        && (requestedMinY >= minY)
                        && (requestedMaxX <= maxX)
                        && (requestedMaxY <= maxY))) {
            // The extent of the returned image contains the requested extent.
            // Let's do a crop
            return true;
        }
        return false;
    }

    /**
     * The requested region is wider than the returned coverage (the reader may have done a crop to
     * minimal extent). Extend the result to the requested region, using background value.
     */
    private GridCoverage2D extendToRegion(
            GridCoverage2D gridCoverage,
            GridGeometry2D requestedGridGeometry,
            double[] backgroundValues)
            throws IllegalStateException, NoninvertibleTransformException,
                    MismatchedDimensionException, TransformException {

        RenderedImage rasterData = gridCoverage.getRenderedImage();
        final GridEnvelope requestedRange = (GridEnvelope) requestedGridGeometry.getGridRange();
        final int requestedW = requestedRange.getSpan(0);
        final int requestedH = requestedRange.getSpan(1);

        ImageLayout layout = new ImageLayout();

        GridToEnvelopeMapper mapper =
                new GridToEnvelopeMapper(
                        new GridEnvelope2D(
                                new Rectangle(
                                        rasterData.getMinX(),
                                        rasterData.getMinY(),
                                        rasterData.getWidth(),
                                        rasterData.getHeight())),
                        gridCoverage.getEnvelope());
        mapper.setPixelAnchor(PixelInCell.CELL_CORNER);

        AffineTransform envToGrid = mapper.createAffineTransform().createInverse();
        MathTransform mt = ProjectiveTransform.create(envToGrid);
        org.locationtech.jts.geom.Envelope transformedEnvelope =
                JTS.transform(new ReferencedEnvelope(requestedGridGeometry.getEnvelope()), mt);

        int minX = (int) transformedEnvelope.getMinX();
        int minY = (int) transformedEnvelope.getMinY();

        layout.setHeight(requestedH).setWidth(requestedW).setMinX(minX).setMinY(minY);
        layout.setSampleModel(
                rasterData.getSampleModel().createCompatibleSampleModel(requestedW, requestedH));
        layout.setColorModel(rasterData.getColorModel());

        final RenderingHints jaiHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        jaiHints.add(BORDER_EXTENDER_HINTS);
        final RenderedImage mosaic =
                MosaicDescriptor.create(
                        new RenderedImage[] {rasterData},
                        MosaicDescriptor.MOSAIC_TYPE_BLEND,
                        null,
                        null,
                        null,
                        backgroundValues,
                        jaiHints);

        // Setting up a gridCoverage on top of the new image
        final Envelope envelope =
                new GeneralEnvelope(
                        new GridEnvelope2D(minX, minY, requestedW, requestedH),
                        PixelInCell.CELL_CENTER,
                        requestedGridGeometry.getGridToCRS(),
                        requestedGridGeometry.getCoordinateReferenceSystem());

        return GC_FACTORY.create("mosaic", mosaic, envelope);
    }

    /**
     * Apply bandSelection operation in case band indices have been specified but underlying reader
     * do not support bands param.
     */
    private GridCoverage2D bandSelect(
            GridCoverage2DReader reader,
            GeneralParameterValue[] readParameters,
            int[] bandIndices,
            GridCoverage2D originalGridCoverage,
            List<GridCoverage2D> disposableSources) {
        GridCoverage2D bandFilteredCoverage = originalGridCoverage;

        // Do the bandIndices has been specified?
        if (bandIndices != null && bandIndices.length > 0) {

            // Do the reader supports BANDS param?
            for (GeneralParameterValue readParameter : readParameters) {
                if (AbstractGridFormat.BANDS
                        .getName()
                        .equals(readParameter.getDescriptor().getName())) {
                    Object bands = ((ParameterValue) readParameter).getValue();
                    if (bands != null
                            && reader.getFormat() != null
                            && reader.getFormat()
                                    .getReadParameters()
                                    .getDescriptor()
                                    .descriptors()
                                    .contains(AbstractGridFormat.BANDS)) {
                        // Band selection has been made at reader's level.
                        // No need to do a bandSelect process.
                        return bandFilteredCoverage;
                    }
                }
            }

            // check band indices are valid
            int sampleDimensionsNumber = originalGridCoverage.getNumSampleDimensions();
            for (int i : bandIndices) {
                if (i < 0 || i >= sampleDimensionsNumber) {
                    throw new WPSException(
                            "Band index "
                                    + i
                                    + " is invalid for the current input raster. "
                                    + "This raster contains "
                                    + sampleDimensionsNumber
                                    + " band"
                                    + (sampleDimensionsNumber > 1 ? "s" : ""));
                }
            }

            // Perform bands selection as a process
            BandSelectProcess bandSelectProcess = new BandSelectProcess();

            // using null for the VisibleSampleDimension parameter of BandSelectProcess.execute.
            // GeoTools BandSelector2D takes care of remapping visible band index
            // or assigns it to first band in order if remapping is not possible
            disposableSources.add(originalGridCoverage);
            bandFilteredCoverage =
                    bandSelectProcess.execute(originalGridCoverage, bandIndices, null);
        }
        return bandFilteredCoverage;
    }

    /**
     * Update read parameters, taking into accounts filter (if specified), band selection parameters
     * as well as forcing deferred execution loading.
     */
    private GeneralParameterValue[] updateReadParams(
            GeneralParameterValue[] readParameters,
            List<GeneralParameterDescriptor> parameterDescriptors,
            int[] bandIndices,
            Filter filter) {

        // merge support for filter
        if (filter != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Add the filter");
            }
            readParameters =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors, readParameters, filter, "FILTER", "Filter");
        }

        // make sure we work in streaming fashion
        boolean replacedJai = false;
        for (GeneralParameterValue pv : readParameters) {
            String pdCode = pv.getDescriptor().getName().getCode();
            if (AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode().equals(pdCode)) {
                replacedJai = true;
                ParameterValue pvalue = (ParameterValue) pv;
                pvalue.setValue(Boolean.TRUE);
                break;
            }
        }

        if (!replacedJai) {
            readParameters =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors,
                            readParameters,
                            Boolean.TRUE,
                            AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode());
        }

        // Setting band selection parameter
        boolean replacedBands = false;
        for (GeneralParameterValue pv : readParameters) {
            String pdCode = pv.getDescriptor().getName().getCode();
            if (AbstractGridFormat.BANDS.getName().getCode().equals(pdCode)) {
                replacedBands = true;
                ParameterValue pvalue = (ParameterValue) pv;
                pvalue.setValue(bandIndices);
                break;
            }
        }
        if (!replacedBands) {
            readParameters =
                    CoverageUtils.mergeParameter(
                            parameterDescriptors,
                            readParameters,
                            bandIndices,
                            AbstractGridFormat.BANDS.getName().getCode());
        }
        return readParameters;
    }

    /**
     * Writes the provided GridCoverage to the requested output format
     *
     * @param mimeType result mimetype
     * @param coverageInfo resource associated to the input coverage
     * @param gridCoverage gridcoverage to write
     * @param writeParams writing parameters
     * @return a {@link File} that points to the GridCoverage we wrote.
     */
    private Resource writeRaster(
            String mimeType,
            CoverageInfo coverageInfo,
            GridCoverage2D gridCoverage,
            Parameters writeParams)
            throws Exception {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Writing raster");
        }
        // limits
        long limit = DownloadServiceConfiguration.NO_LIMIT;
        if (limits.getHardOutputLimit() > 0) {
            limit = limits.getHardOutputLimit();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Hard output limits set to " + limit);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Hard output limit unset");
            }
        }

        // Search a proper PPIO
        Parameter<GridCoverage2D> gridParam =
                new Parameter<GridCoverage2D>("fakeParam", GridCoverage2D.class);
        ProcessParameterIO ppio_ = DownloadUtilities.find(gridParam, context, mimeType, false);
        if (ppio_ == null) {
            throw new ProcessException("Don't know how to encode in mime type " + mimeType);
        } else if (!(ppio_ instanceof ComplexPPIO)) {
            throw new ProcessException("Invalid PPIO found " + ppio_.getIdentifer());
        }
        final ComplexPPIO complexPPIO = (ComplexPPIO) ppio_;
        String extension = complexPPIO.getFileExtension();

        // writing the output to a temporary folder
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Writing file in a temporary folder");
        }
        final Resource output = resourceManager.getTemporaryResource("." + extension);

        // the limit output stream will throw an exception if the process is trying to writer more
        // than the max allowed bytes
        final ImageOutputStream fileImageOutputStreamExtImpl =
                new ImageOutputStreamAdapter(output.out());
        ImageOutputStream os = null;
        // write
        try {
            // If limit is defined, LimitedImageOutputStream is used
            if (limit > DownloadServiceConfiguration.NO_LIMIT) {
                os =
                        new LimitedImageOutputStream(fileImageOutputStreamExtImpl, limit) {

                            @Override
                            protected void raiseError(long pSizeMax, long pCount)
                                    throws IOException {
                                IOException e =
                                        new IOException(
                                                "Download Exceeded the maximum HARD allowed size!");
                                throw e;
                            }
                        };
            } else {
                os = fileImageOutputStreamExtImpl;
            }
            // Encoding the GridCoverage
            Map encodingParams = writeParams != null ? writeParams.getParametersMap() : null;
            complexPPIO.encode(gridCoverage, encodingParams, new OutputStreamAdapter(os));
            os.flush();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
        return output;
    }
}
