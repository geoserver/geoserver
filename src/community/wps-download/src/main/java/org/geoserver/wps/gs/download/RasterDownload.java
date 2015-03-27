/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import it.geosolutions.imageio.stream.output.FileImageOutputStreamExtImpl;
import it.geosolutions.io.output.adapter.OutputStreamAdapter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.ImageOutputStream;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.resource.GridCoverageResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.Parameter;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.raster.CropCoverage;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Implements the download services for raster data. If limits are configured this class will use {@link LimitedImageOutputStream}, which raises an
 * exception when the download size exceeded the limits.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
class RasterDownload {

    private static final Logger LOGGER = Logging.getLogger(RasterDownload.class);

    /** The {@link DownloadServiceConfiguration} object containing the configured limits. */
    private DownloadServiceConfiguration limits;

    /** The resource manager for handling the used resources. */
    private WPSResourceManager resourceManager;

    /**
     * Constructor, takes a {@link DownloadEstimatorProcess}.
     * 
     * @param limits the {@link DownloadEstimatorProcess} to check for not exceeding the download limits.
     * @param resourceManager the {@link WPSResourceManager} to handl generated resources
     */
    public RasterDownload(DownloadServiceConfiguration limits, WPSResourceManager resourceManager) {
        this.limits = limits;
        this.resourceManager = resourceManager;
    }

    /**
     * This method does the following operations:
     * <ul>
     * <li>Reprojection of the coverage (if needed)</li>
     * <li>Clips the coverage (if needed)</li>
     * <li>Writes the result</li>
     * <li>Cleanup the generated coverages</li>
     * </ul>
     * 
     * @param mimeType mimetype of the result
     * @param progressListener listener to use for logging the operations
     * @param coverageInfo resource associated to the Coverage
     * @param roi input ROI object
     * @param targetCRS CRS of the file to write
     * @param clip indicates if the clipping geometry must be exactly that of the ROI or simply its envelope
     * @param filter the {@link Filter} to load the data
     * @return
     * @throws Exception
     */
    public File execute(String mimeType, final ProgressListener progressListener,
            CoverageInfo coverageInfo, Geometry roi, CoordinateReferenceSystem targetCRS,
            boolean clip, Filter filter) throws Exception {

        GridCoverage2D clippedGridCoverage = null, reprojectedGridCoverage = null, originalGridCoverage = null;
        try {

            //
            // look for output extension. Tiff/tif/geotiff will be all threated as GeoTIFF
            //

            //
            // ---> READ FROM NATIVE RESOLUTION <--
            //

            // prepare native CRS
            CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(coverageInfo);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Native CRS is " + nativeCRS.toWKT());
            }

            //
            // STEP 0 - Push ROI back to native CRS (if ROI is provided)
            //
            ROIManager roiManager = null;
            if (roi != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Pushing ROI to native CRS");
                }
                final CoordinateReferenceSystem roiCRS = (CoordinateReferenceSystem) roi
                        .getUserData();
                roiManager = new ROIManager(roi, roiCRS);
            }

            //
            // STEP 1 - Reproject if needed
            //
            boolean reproject = false;
            MathTransform reprojectionTrasform = null;
            if (targetCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, targetCRS)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Checking if reprojection is needed");
                }
                // testing reprojection...
                reprojectionTrasform = CRS.findMathTransform(nativeCRS, targetCRS, true);
                if (!reprojectionTrasform.isIdentity()) {
                    // avoid doing the transform if this is the identity
                    reproject = true;
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Reprojection needed");
                    }
                }
            } else {
                targetCRS = nativeCRS;
            }

            // get a reader for this CoverageInfo
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Getting reader for the coverage");
            }
            final GridCoverage2DReader reader = (GridCoverage2DReader) coverageInfo
                    .getGridCoverageReader(null, null);
            final ParameterValueGroup readParametersDescriptor = reader.getFormat()
                    .getReadParameters();
            final List<GeneralParameterDescriptor> parameterDescriptors = readParametersDescriptor
                    .getDescriptor().descriptors();
            // get the configured metadata for this coverage without
            GeneralParameterValue[] readParameters = CoverageUtils.getParameters(
                    readParametersDescriptor, coverageInfo.getParameters(), false);

            // merge support for filter
            if (filter != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Add the filter");
                }
                readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                        filter, "FILTER", "Filter");
            }
            // read GridGeometry preparation
            if (roi != null) {
                // set crs in roi manager
                roiManager.useNativeCRS(reader.getCoordinateReferenceSystem());
                roiManager.useTargetCRS(targetCRS);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            "Preparing the GridGeometry for cropping input layer with ROI");
                }
                // create GridGeometry
                final ReferencedEnvelope roiEnvelope = new ReferencedEnvelope(roiManager
                        .getSafeRoiInNativeCRS().getEnvelopeInternal(), // safe envelope
                        nativeCRS);
                GridGeometry2D gg2D = new GridGeometry2D(PixelInCell.CELL_CENTER,
                        reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER), roiEnvelope,
                        GeoTools.getDefaultHints());
                // TODO make sure the GridRange is not empty, depending on the resolution it might happen
                readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                        gg2D, AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().getCode());
            }
            // make sure we work in streaming fashion
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                    Boolean.TRUE, AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode());

            // --> READ
            originalGridCoverage = reader.read(readParameters);

            //
            // STEP 1 - Reproject if needed
            //
            if (reproject) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Reprojecting the layer");
                }
                // avoid doing the transform if this is the identity
                reprojectedGridCoverage = (GridCoverage2D) Operations.DEFAULT.resample(
                        originalGridCoverage, targetCRS);

            } else {
                reprojectedGridCoverage = originalGridCoverage;
            }

            //
            // STEP 2 - Clip if needed
            //
            // we need to push the ROI to the final CRS to crop or CLIP
            if (roi != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Cropping the layer");
                }
                // Crop or Clip
                final CropCoverage cropCoverage = new CropCoverage(); // TODO avoid creation
                if (clip) {
                    // clipping means carefully following the ROI shape
                    clippedGridCoverage = cropCoverage.execute(reprojectedGridCoverage,
                            roiManager.getSafeRoiInTargetCRS(), progressListener);
                } else {
                    // use envelope of the ROI to simply crop and not clip the raster. This is important since when
                    // reprojecting we might read a bit more than needed!
                    final Geometry polygon = roiManager.getSafeRoiInTargetCRS();
                    polygon.setUserData(targetCRS);
                    clippedGridCoverage = cropCoverage.execute(reprojectedGridCoverage,
                            roiManager.getSafeRoiInTargetCRS(), progressListener);
                }
            } else {
                // do nothing
                clippedGridCoverage = reprojectedGridCoverage;
            }

            //
            // STEP 3 - Writing
            //
            return writeRaster(mimeType, coverageInfo, clippedGridCoverage);
        } finally {
            if (originalGridCoverage != null) {
                resourceManager.addResource(new GridCoverageResource(originalGridCoverage));
            }
            if (reprojectedGridCoverage != null) {
                resourceManager.addResource(new GridCoverageResource(reprojectedGridCoverage));
            }
            if (clippedGridCoverage != null) {
                resourceManager.addResource(new GridCoverageResource(clippedGridCoverage));
            }
        }
    }

    /**
     * Writes the providede GridCoverage as a GeoTiff file.
     * 
     * @param mimeType result mimetype
     * @param coverageInfo resource associated to the input coverage
     * @param gridCoverage gridcoverage to write
     * @return a {@link File} that points to the GridCoverage we wrote.
     * 
     * @throws Exception
     */
    private File writeRaster(String mimeType, CoverageInfo coverageInfo, GridCoverage2D gridCoverage)
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
        ProcessParameterIO ppio_ = DownloadUtilities.find(new Parameter<GridCoverage2D>(
                "fakeParam", GridCoverage2D.class), null, mimeType, false);
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
        final File output = resourceManager.getTemporaryFile("." + extension);

        // the limit output stream will throw an exception if the process is trying to writer more than the max allowed bytes
        final FileImageOutputStreamExtImpl fileImageOutputStreamExtImpl = new FileImageOutputStreamExtImpl(
                output);
        ImageOutputStream os = null;
        // write
        try {
            // If limit is defined, LimitedImageOutputStream is used
            if (limit > DownloadServiceConfiguration.NO_LIMIT) {
                os = new LimitedImageOutputStream(fileImageOutputStreamExtImpl, limit) {

                    @Override
                    protected void raiseError(long pSizeMax, long pCount) throws IOException {
                        IOException e = new IOException(
                                "Download Exceeded the maximum HARD allowed size!");
                        throw e;
                    }
                };
            } else {
                os = fileImageOutputStreamExtImpl;
            }
            // Encoding the GridCoverage
            complexPPIO.encode(gridCoverage, new OutputStreamAdapter(os));
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