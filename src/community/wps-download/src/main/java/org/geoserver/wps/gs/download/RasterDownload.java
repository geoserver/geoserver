/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
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
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.raster.BandSelectProcess;
import org.geotools.process.raster.CropCoverage;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

import it.geosolutions.imageio.stream.output.ImageOutputStreamAdapter;
import it.geosolutions.io.output.adapter.OutputStreamAdapter;

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
     * The application context used to look-up PPIO factories
     */
    private ApplicationContext context;

    /**
     * Constructor, takes a {@link DownloadEstimatorProcess}.
     * 
     * @param limits the {@link DownloadEstimatorProcess} to check for not exceeding the download
     *        limits.
     * @param resourceManager the {@link WPSResourceManager} to handl generated resources
     * @param context
     */
    public RasterDownload(DownloadServiceConfiguration limits, WPSResourceManager resourceManager,
            ApplicationContext context) {
        this.limits = limits;
        this.resourceManager = resourceManager;
        this.context = context;
    }

    /**
     * This method does the following operations:
     * <ul>
     * <li>Uses only those bands specified by indices (if defined)</li>
     * <li>Reprojection of the coverage (if needed)</li>
     * <li>Clips the coverage (if needed)</li>
     * <li>Scales the coverage to match the target size (if needed)</li>
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
     * @param interpolation interpolation method to use when reprojecting / scaling
     * @param targetSizeX the size of the target image along the X axis
     * @param targetSizeY the size of the target image along the Y axis
     * @param bandIndices the indices of the bands used for the final result
     * @param filter the {@link Filter} to load the data
     *
     */
    public Resource execute(String mimeType, final ProgressListener progressListener,
            CoverageInfo coverageInfo, Geometry roi, CoordinateReferenceSystem targetCRS,
            boolean clip, Filter filter, Interpolation interpolation, Integer targetSizeX,
            Integer targetSizeY, int[] bandIndices) throws Exception {

        GridCoverage2D scaledGridCoverage = null, clippedGridCoverage = null, reprojectedGridCoverage = null, bandFilteredCoverage = null, originalGridCoverage = null;
        try {

            //
            // look for output extension. Tiff/tif/geotiff will be all treated as GeoTIFF
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
            // read GridGeometry preparation and scaling setup
            ScaleToTarget scaling = null;
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
                final Polygon originalEnvelopeAsPolygon = FeatureUtilities.getPolygon(reader.getOriginalEnvelope(),
                        new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING)));
                originalEnvelopeAsPolygon.setUserData(nativeCRS);
                final ReferencedEnvelope originalEnvelope = JTS.toEnvelope(originalEnvelopeAsPolygon);
                // calculate intersection between original envelope and ROI, as blindly trusting
                // the ROI may give issues with scaling, if target size is not specified for
                // both X and Y dimensions
                final ReferencedEnvelope intersection = originalEnvelope.intersection(roiEnvelope);
                // take scaling into account
                scaling = new ScaleToTarget(reader, intersection);
                scaling.setTargetSize(targetSizeX, targetSizeY);
                GridGeometry2D gg2D = scaling.getGridGeometry();

                // TODO make sure the GridRange is not empty, depending on the resolution it might happen
                readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                        gg2D, AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().getCode());
            } else {
                // we are reading the full coverage
                scaling = new ScaleToTarget(reader);
                scaling.setTargetSize(targetSizeX, targetSizeY);
            }
            // make sure we work in streaming fashion
            readParameters = CoverageUtils.mergeParameter(parameterDescriptors, readParameters,
                    Boolean.TRUE, AbstractGridFormat.USE_JAI_IMAGEREAD.getName().getCode());

            // --> READ
            originalGridCoverage = reader.read(readParameters);

            // check, the reader might have returned a null coverage
            if(originalGridCoverage == null) {
                throw new WPSException("The reader did not return any data for current input "
                        + "parameters. It normally means there is nothing there, or the data got filtered out by the ROI or filter");
            }
            
            //
            // STEP 0 - Check for bands, select only those specified
            //   
            if (bandIndices!=null && bandIndices.length>0){                
                //check band indices are valid
                int sampleDimensionsNumber = originalGridCoverage.getNumSampleDimensions(); 
                for (int i:bandIndices){
                    if (i<0 || i>=sampleDimensionsNumber){
                        throw new WPSException(
                                "Band index "+i+" is invalid for the current input raster. "
                                + "This raster contains "+sampleDimensionsNumber+" band"
                                + (sampleDimensionsNumber>1?"s":""));            
                    }
                }
                BandSelectProcess bandSelectProcess = new BandSelectProcess();
                
                //using null for the VisibleSampleDimension parameter of BandSelectProcess.execute. 
                //GeoTools BandSelector2D takes care of remapping visible band index
                //or assigns it to first band in order if remapping is not possible
                bandFilteredCoverage = bandSelectProcess.execute(
                        originalGridCoverage, bandIndices, null);
            
            }else{
                bandFilteredCoverage = originalGridCoverage;
            }

            //
            // STEP 1 - Reproject if needed
            //
            if (reproject) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Reprojecting the layer");
                }
                // avoid doing the transform if this is the identity
                reprojectedGridCoverage = (GridCoverage2D) Operations.DEFAULT.resample(
                        bandFilteredCoverage, targetCRS, null, interpolation);

            } else {
                reprojectedGridCoverage = bandFilteredCoverage;
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


                // Get the proper ROI (depending on clip parameter and CRS)
                Geometry croppingRoi = roiManager.getTargetRoi(clip);
                clippedGridCoverage = cropCoverage.execute(reprojectedGridCoverage, croppingRoi, progressListener);

                if (clippedGridCoverage == null) {
                    throw new WPSException("No data left after applying the ROI. This means there "
                            + "is source data, but none matching the requested ROI");
                }
            } else {
                // do nothing
                clippedGridCoverage = reprojectedGridCoverage;
            }

            //
            // STEP 3 - scale to target size, if needed
            //
            if (interpolation != null) {
                scaling.setInterpolation(interpolation);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Scaling the layer");
            }
            // scaling and/or interpolation
            scaledGridCoverage = scaling.scale(clippedGridCoverage);

            //
            // STEP 4 - Writing
            //
            return writeRaster(mimeType, coverageInfo, scaledGridCoverage);
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
            if (scaledGridCoverage != null) {
                resourceManager.addResource(new GridCoverageResource(scaledGridCoverage));
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
     */
    private Resource writeRaster(String mimeType, CoverageInfo coverageInfo, GridCoverage2D gridCoverage)
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
        Parameter<GridCoverage2D> gridParam = new Parameter<GridCoverage2D>("fakeParam",
                GridCoverage2D.class);
        ProcessParameterIO ppio_ = DownloadUtilities.find(gridParam, context, mimeType,
                false);
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

        // the limit output stream will throw an exception if the process is trying to writer more than the max allowed bytes
        final ImageOutputStream fileImageOutputStreamExtImpl = new ImageOutputStreamAdapter(
                output.out());
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