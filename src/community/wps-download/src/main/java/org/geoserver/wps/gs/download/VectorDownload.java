/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.process.ProcessException;
import org.geotools.process.vector.ClipProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * The class that does the real work of checking if we are exceeeding the download limits for vector
 * data. Also this class writes the features in the output file.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class VectorDownload {

    private static final Logger LOGGER = Logging.getLogger(VectorDownload.class);

    /** The {@link DownloadServiceConfiguration} object containing the configured limits. */
    private DownloadServiceConfiguration limits;

    /** The resource manager for handling the used resources. */
    private WPSResourceManager resourceManager;

    private ApplicationContext context;

    /**
     * Constructor, takes a {@link DownloadServiceConfiguration} and a {@link WPSResourceManager}.
     *
     * @param limits the {@link DownloadServiceConfiguration} to check for not exceeding the
     *     download limits.
     * @param resourceManager the {@link WPSResourceManager} to handle generated resources
     */
    public VectorDownload(
            DownloadServiceConfiguration limits,
            WPSResourceManager resourceManager,
            ApplicationContext context) {
        this.limits = limits;
        this.resourceManager = resourceManager;
        this.context = context;
    }

    /**
     * Extract vector data to a file, given the provided mime-type. This method does the following
     * operations:
     *
     * <ul>
     *   <li>Reads and filter the features (if needed)
     *   <li>Reprojects the features (if needed)
     *   <li>Clips the features (if needed)
     *   <li>Writes the result
     *   <li>Cleanup the generated coverages
     * </ul>
     *
     * @param resourceInfo the {@link FeatureTypeInfo} to download from
     * @param mimeType the mme-type for the requested output format
     * @param roi the {@link Geometry} for the clip/intersection
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @param targetCRS the reproject {@link CoordinateReferenceSystem}
     * @return a file, given the provided mime-type.
     */
    public Resource execute(
            FeatureTypeInfo resourceInfo,
            String mimeType,
            Geometry roi,
            boolean clip,
            Filter filter,
            CoordinateReferenceSystem targetCRS,
            final ProgressListener progressListener)
            throws Exception {

        // prepare native CRS
        CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(resourceInfo);
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
            CoordinateReferenceSystem roiCRS = (CoordinateReferenceSystem) roi.getUserData();
            roiManager = new ROIManager(roi, roiCRS);
            // set use nativeCRS
            roiManager.useNativeCRS(nativeCRS);
        }

        //
        // STEP 1 - Read and Filter
        //

        // access feature source and collection of features
        final SimpleFeatureSource featureSource =
                (SimpleFeatureSource)
                        resourceInfo.getFeatureSource(null, GeoTools.getDefaultHints());

        // basic filter preparation
        Filter ra = Filter.INCLUDE;
        if (filter != null) {
            ra = filter;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Using filter " + ra);
            }
        }
        // and with the ROI if we have one
        SimpleFeatureCollection originalFeatures;
        final boolean hasROI = roiManager != null;
        if (hasROI) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Adding Geometry filter with ROI");
            }
            final String dataGeomName =
                    featureSource.getSchema().getGeometryDescriptor().getLocalName();
            final Intersects intersectionFilter =
                    FeatureUtilities.DEFAULT_FILTER_FACTORY.intersects(
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.property(dataGeomName),
                            FeatureUtilities.DEFAULT_FILTER_FACTORY.literal(
                                    roiManager.getSafeRoiInNativeCRS()));
            ra = FeatureUtilities.DEFAULT_FILTER_FACTORY.and(ra, intersectionFilter);
        }

        // simplify filter
        ra = (Filter) ra.accept(new SimplifyingFilterVisitor(), null);
        // read
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Reading the filtered features");
        }
        originalFeatures = featureSource.getFeatures(ra);
        DownloadUtilities.checkIsEmptyFeatureCollection(originalFeatures);

        //
        // STEP 2 - Reproject feature collection
        //
        // do we need to reproject?
        SimpleFeatureCollection reprojectedFeatures;
        if (targetCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, targetCRS)) {
            roiManager.useTargetCRS(targetCRS);
            // testing reprojection...
            final MathTransform targetTX = CRS.findMathTransform(nativeCRS, targetCRS, true);
            if (!targetTX.isIdentity()) {
                // avoid doing the transform if this is the identity
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Reprojecting features");
                }
                reprojectedFeatures =
                        new ReprojectingFeatureCollection(originalFeatures, targetCRS);
            } else {
                reprojectedFeatures = originalFeatures;
                DownloadUtilities.checkIsEmptyFeatureCollection(reprojectedFeatures);
            }
        } else {
            reprojectedFeatures = originalFeatures;
            if (hasROI) {
                roiManager.useTargetCRS(nativeCRS);
            }
        }

        //
        // STEP 3 - Clip in targetCRS
        //
        SimpleFeatureCollection clippedFeatures;
        if (clip && roi != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Clipping features");
            }
            final ClipProcess clipProcess = new ClipProcess(); // TODO avoid unnecessary creation
            clippedFeatures =
                    clipProcess.execute(
                            reprojectedFeatures, roiManager.getSafeRoiInTargetCRS(), true);

            // checks
            DownloadUtilities.checkIsEmptyFeatureCollection(clippedFeatures);
        } else {
            clippedFeatures = reprojectedFeatures;
        }

        //
        // STEP 4 - Write down respecting limits in bytes
        //
        // writing the output, making sure it is a zip
        return writeVectorOutput(clippedFeatures, resourceInfo.getName(), mimeType);
    }

    /**
     * Write vector output with the provided PPIO. It returns the {@link File} it writes to.
     *
     * @param features {@link SimpleFeatureCollection} containing the features to write
     * @param name name of the feature source
     * @param mimeType mimetype of the result
     * @return a {@link File} containing the written features
     */
    private Resource writeVectorOutput(
            final SimpleFeatureCollection features, final String name, final String mimeType)
            throws Exception {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Writing features");
        }
        // Search a proper PPIO
        ProcessParameterIO ppio_ =
                DownloadUtilities.find(
                        new Parameter<SimpleFeatureCollection>(
                                "fakeParam", SimpleFeatureCollection.class),
                        context,
                        mimeType,
                        false);
        if (ppio_ == null) {
            throw new ProcessException("Don't know how to encode in mime type " + mimeType);
        } else if (!(ppio_ instanceof ComplexPPIO)) {
            throw new ProcessException("Invalid PPIO found " + ppio_.getIdentifer());
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

        //
        // Get fileName
        //
        String extension = "";
        if (ppio_ instanceof ComplexPPIO) {
            extension = "." + ((ComplexPPIO) ppio_).getFileExtension();
        }

        // create output file
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Writing file in a temporary folder");
        }
        final Resource output = resourceManager.getTemporaryResource(extension);

        // write checking limits
        try (OutputStream os = getResourceOutputStream(output, limit)) {
            // write with PPIO
            if (ppio_ instanceof ComplexPPIO) {
                ((ComplexPPIO) ppio_).encode(features, os);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Flushing stream");
            }
            os.flush();
        }

        // return
        return output;
    }

    private OutputStream getResourceOutputStream(Resource output, long limit) {
        OutputStream os = null;
        // If limits are configured we must create an OutputStream that checks limits
        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(output.out());
        if (limit > DownloadServiceConfiguration.NO_LIMIT) {
            os =
                    new LimitedOutputStream(bufferedOutputStream, limit) {

                        @Override
                        protected void raiseError(long pSizeMax, long pCount) throws IOException {
                            IOException ioe =
                                    new IOException(
                                            "Download Exceeded the maximum HARD allowed size!");
                            throw ioe;
                        }
                    };

        } else {
            os = bufferedOutputStream;
        }

        return os;
    }
}
