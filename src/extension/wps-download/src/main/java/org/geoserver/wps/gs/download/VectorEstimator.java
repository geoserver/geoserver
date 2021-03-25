/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Checks whether or not the provided request exceeds the provided download limits for a vectorial
 * resource.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class VectorEstimator {

    private static final Logger LOGGER = Logging.getLogger(VectorEstimator.class);

    /** The downloadServiceConfiguration object containing the limits to check */
    private DownloadServiceConfiguration downloadServiceConfiguration;

    /**
     * Constructor.
     *
     * @param limits an instance of the {@link DownloadEstimatorProcess} that contains the limits to
     *     enforce
     */
    public VectorEstimator(DownloadServiceConfiguration limits) {
        this.downloadServiceConfiguration = limits;
    }

    /**
     * Checks whether or not the requests exceed download limits for vector data.
     *
     * @param resourceInfo the {@link FeatureTypeInfo} to download from
     * @param roi the {@link Geometry} for the clip/intersection
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @param targetCRS the reproject {@link CoordinateReferenceSystem} (useless for the moment)
     * @return <code>true</code> if we do not exceeds the limits, <code>false</code> otherwise.
     * @throws Exception in case something bad happens.
     */
    public boolean execute(
            FeatureTypeInfo resourceInfo,
            Geometry roi,
            boolean clip,
            Filter filter,
            CoordinateReferenceSystem targetCRS,
            final ProgressListener progressListener)
            throws Exception {

        //
        // Do we need to do anything?
        //
        if (downloadServiceConfiguration.getMaxFeatures() <= 0) {
            return true;
        }

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
        // STEP 1 - Create the Filter
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
        if (roi != null) {
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
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Counting features");
        }
        // read
        int count = featureSource.getCount(new Query("counter", ra));
        if (count < 0) {
            // a value minor than "0" means that the store does not provide any counting feature ...
            // lets proceed using the iterator
            SimpleFeatureCollection features = featureSource.getFeatures(ra);
            count = features.size();
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Feature size is " + count);
        }
        // finally checking the number of features accordingly to the "maxfeatures" limit
        final long maxFeatures = downloadServiceConfiguration.getMaxFeatures();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Max features limit is " + maxFeatures);
        }
        if (maxFeatures > 0 && count > maxFeatures) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(
                        Level.SEVERE, "MaxFeatures limit exceeded. " + count + " > " + maxFeatures);
            }
            return false;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MaxFeatures limit not exceeded.");
        }
        // limits were not exceeded
        return true;
    }
}
