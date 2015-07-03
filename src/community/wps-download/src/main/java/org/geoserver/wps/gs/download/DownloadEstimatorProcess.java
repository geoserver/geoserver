/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The DownloadEstimatorProcess is used for checking if the download request does not exceeds the defined limits.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
@DescribeProcess(title = "Estimator Process", description = "Checks if the input file does not exceed the limits")
public class DownloadEstimatorProcess implements GSProcess {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(DownloadEstimatorProcess.class);

    private DownloadServiceConfigurationGenerator downloadServiceConfigurationGenerator;

    /** The catalog. */
    private final Catalog catalog;

    /**
     * @param readLimits
     * @param writeLimits
     * @param hardOutputLimit
     * @param geoserver
     */
    public DownloadEstimatorProcess(
            DownloadServiceConfigurationGenerator downloadServiceConfigurationGenerator,
            GeoServer geoserver) {
        this.catalog = geoserver.getCatalog();
        this.downloadServiceConfigurationGenerator = downloadServiceConfigurationGenerator;
    }

    /**
     * This process returns a boolean value which indicates if the requested download does not exceed the imposed limits, if present
     * 
     * @param layerName the layer name
     * @param filter the filter
     * @param email the email
     * @param outputFormat the output format
     * @param targetCRS the target crs
     * @param roiCRS the roi crs
     * @param roi the roi
     * @param clip the crop to geometry
     * @param progressListener the progress listener
     * @return the boolean
     * @throws Exception
     */
    @DescribeResult(name = "result", description = "Download Limits are respected or not!")
    public Boolean execute(
            @DescribeParameter(name = "layerName", min = 1, description = "Original layer to download") String layerName,
            @DescribeParameter(name = "filter", min = 0, description = "Optional Vectorial Filter") Filter filter,
            @DescribeParameter(name = "targetCRS", min = 0, description = "Target CRS") CoordinateReferenceSystem targetCRS,
            @DescribeParameter(name = "RoiCRS", min = 0, description = "Region Of Interest CRS") CoordinateReferenceSystem roiCRS,
            @DescribeParameter(name = "ROI", min = 0, description = "Region Of Interest") Geometry roi,
            @DescribeParameter(name = "cropToROI", min = 0, description = "Crop to ROI") Boolean clip,
            ProgressListener progressListener) throws Exception {

        //
        // initial checks on mandatory params
        //
        // layer name
        if (layerName == null || layerName.length() <= 0) {
            throw new IllegalArgumentException("Empty or null layerName provided!");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Estimator process called on resource: " + layerName);
        }
        if (clip == null) {
            clip = false;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Clipping disabled");
            }
        }
        if (roi != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "ROI present");
            }
            DownloadUtilities.checkPolygonROI(roi);
            if (roiCRS == null) {
                throw new IllegalArgumentException("ROI without a CRS is not usable!");
            }
            roi.setUserData(roiCRS);
        }

        //
        // Move on with the real code
        //
        // checking for the resources on the GeoServer catalog
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            // could not find any layer ... abruptly interrupt the process
            throw new IllegalArgumentException("Unable to locate layer: " + layerName);

        }
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo == null) {
            // could not find any data store associated to the specified layer ... abruptly interrupt the process
            throw new IllegalArgumentException("Unable to locate ResourceInfo for layer:"
                    + layerName);

        }

        //
        // Get curent limits
        //
        DownloadServiceConfiguration limits = downloadServiceConfigurationGenerator
                .getConfiguration();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Getting configuration limits");
        }

        // ////
        // 1. DataStore -> look for vectorial data download
        // 2. CoverageStore -> look for raster data download
        // ////
        if (resourceInfo instanceof FeatureTypeInfo) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Working with Vectorial dataset");
            }
            final FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) resourceInfo;

            return new VectorEstimator(limits).execute(featureTypeInfo, roi, clip, filter,
                    targetCRS, progressListener);

        } else if (resourceInfo instanceof CoverageInfo) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Working with Raster dataset");
            }
            final CoverageInfo coverage = (CoverageInfo) resourceInfo;
            return new RasterEstimator(limits).execute(progressListener, coverage, roi, targetCRS,
                    clip, filter);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Working with a wrong Resource");
        }

        // the requeste layer is neither a featuretype nor a coverage --> error
        final ProcessException ex = new ProcessException(
                "Could not complete the Download Process: target resource is of Illegal type --> "
                        + resourceInfo != null ? resourceInfo.getClass().getCanonicalName()
                        : "null");

        // Notify the listener if present
        if (progressListener != null) {
            progressListener.exceptionOccurred(ex);
        }
        throw ex;

    }

    /**
     * @return the {@link DownloadServiceConfiguration} containing the limits to check
     */
    public DownloadServiceConfiguration getDownloadServiceConfiguration() {
        return downloadServiceConfigurationGenerator.getConfiguration();
    }
}