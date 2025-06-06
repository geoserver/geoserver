/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.MetadataMap;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * This class check whether or not the provided download request goes beyond the provided limits for raster data or not.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
class RasterEstimator {

    private static final Logger LOGGER = Logging.getLogger(RasterEstimator.class);

    /** The downloadServiceConfiguration object containing the limits to check */
    private DownloadServiceConfiguration downloadServiceConfiguration;

    private Catalog catalog;

    /**
     * Constructor
     *
     * @param limits the parent {@link DownloadEstimatorProcess} that contains the download limits to be enforced.
     */
    public RasterEstimator(DownloadServiceConfiguration limits, Catalog catalog) {
        this.downloadServiceConfiguration = limits;
        this.catalog = catalog;
        if (limits == null) {
            throw new NullPointerException("The provided DownloadEstimatorProcess is null!");
        }
    }

    /**
     * Check the download limits for raster data.
     *
     * @param coverageInfo the {@link CoverageInfo} to estimate the download limits
     * @param roi the {@link Geometry} for the clip/intersection
     * @param targetCRS the reproject {@link CoordinateReferenceSystem} (useless for the moment)
     * @param clip whether or not to clip the resulting data (useless for the moment)
     * @param filter the {@link Filter} to load the data
     * @param targetSizeX the size of the target image along the X axis
     * @param targetSizeY the size of the target image along the Y axis
     * @param bandIndices the band indices selected for output, in case of raster input
     */
    public boolean execute(
            final ProgressListener progressListener,
            CoverageInfo coverageInfo,
            Geometry roi,
            CoordinateReferenceSystem targetCRS,
            boolean clip,
            Filter filter,
            Integer targetSizeX,
            Integer targetSizeY,
            int[] bandIndices)
            throws Exception {

        final long rasterSizeLimits = downloadServiceConfiguration.getRasterSizeLimits();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Raster size limits: " + rasterSizeLimits);
        }

        //
        // ---> READ FROM NATIVE RESOLUTION <--
        //
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Checking download limits for raster request");
        }
        CoordinateReferenceSystem nativeCRS = DownloadUtilities.getNativeCRS(coverageInfo);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Native CRS is " + nativeCRS.toWKT());
        }

        //
        // STEP 0 - Push ROI back to native CRS (if ROI is provided)
        //
        // get a reader for this CoverageInfo
        final GridCoverage2DReader reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        CRSRequestHandler crsRequestHandler = new CRSRequestHandler(reader, catalog, targetCRS, roi);
        crsRequestHandler.setFilter(filter);
        crsRequestHandler.init();

        // Area to read in pixel
        // take scaling into account
        ScaleToTarget scaling = null;
        if (roi != null) {
            // If ROI is present, then the coverage BBOX is cropped with the ROI geometry
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Reprojecting ROI");
            }
            final Geometry safeRoiInNativeCRS =
                    crsRequestHandler.getRoiManager().getSafeRoiInNativeCRS();
            Geometry roiInNativeCRS_ = safeRoiInNativeCRS.intersection(FeatureUtilities.getPolygon(
                    reader.getOriginalEnvelope(), new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING))));
            if (roiInNativeCRS_.isEmpty()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Empty intersection, so the result does not exceed the limits");
                }
                return true; // EMPTY Intersection
            }

            // try to make an estimate of the area we need to read
            // NOTE I use the envelope since in the end I can only pass down
            // a rectangular source region to the ImageIO-Ext reader, but in the end I am only going
            // to read the tile I will need during processing as in this case I am going to perform
            // deferred reads
            ReferencedEnvelope refEnvelope = JTS.toEnvelope(roiInNativeCRS_.getEnvelope());
            scaling = new ScaleToTarget(reader, refEnvelope);

            // TODO investigate on improved precision taking into account tiling on raster geometry
        } else {
            // No ROI, we are trying to read the entire coverage
            scaling = new ScaleToTarget(reader);
        }
        scaling.setTargetSize(targetSizeX, targetSizeY);

        GridGeometry2D gg = null;

        if (targetSizeX == null && targetSizeY == null) {
            // Ask to the GridGeometryProvider
            GridGeometryProvider provider = new GridGeometryProvider(crsRequestHandler);
            gg = provider.getGridGeometry();
        } else {
            gg = scaling.getGridGeometry();
        }

        final long areaRead = (long) gg.getGridRange2D().width * gg.getGridRange2D().height;
        // checks on the area we want to download
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Area to read in pixels: " + areaRead);
        }

        long targetArea;
        Integer[] targetSize = scaling.getTargetSize();
        if (targetSize[0] != null && targetSize[1] != null) {
            targetArea = (long) targetSize[0] * targetSize[1]; // NOPMD for cast to long
        } else {
            targetArea = areaRead;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Area to write in pixels: " + targetArea);
        }

        // Here we used to have a check to refuse processing if the read area or
        // the target area exceeded the Integer.MAX_INT limits. However, this is
        // often not a problem, because we are processing based on tiles.
        // Let the download eventually fail on cases where things are not properly
        // handled, rather than failing all requests because a subset migth.

        // If the area exceeds the limits, false is returned
        if (rasterSizeLimits > DownloadServiceConfiguration.NO_LIMIT
                && (areaRead > rasterSizeLimits || targetArea > rasterSizeLimits)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Area exceeds the limits");
            }
            return false;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Area does not exceed the limits");
        }
        // Try to check write limits, using input's coverageinfo
        int bandsCount = coverageInfo.getDimensions().size();

        // Use sample info type for each output band to estimate size
        List<CoverageDimensionInfo> coverageDimensionInfoList = coverageInfo.getDimensions();

        if (coverageDimensionInfoList.stream().anyMatch(cdi -> cdi.getDimensionType() == null)) {
            LOGGER.log(Level.FINE, "Recalculating Band Dimensions Types");
            coverageDimensionInfoList = getBandDimensionsFromCoverageInfo(coverageInfo);
        }
        int accumulatedPixelSizeInBits = 0;

        // Use only selected bands for output, if specified
        if (bandIndices != null && bandIndices.length > 0) {
            for (int bandIndex : bandIndices) {
                // Use valid indices
                if (bandIndex >= 0 && bandIndex < bandsCount)
                    accumulatedPixelSizeInBits += TypeMap.getSize(
                            coverageDimensionInfoList.get(bandIndex).getDimensionType());
            }
        } else {
            for (int i = 0; i < bandsCount; i++) {
                accumulatedPixelSizeInBits +=
                        TypeMap.getSize(coverageDimensionInfoList.get(i).getDimensionType());
            }
        }

        /// Total size in bytes
        long rasterSizeInBytes = targetArea * accumulatedPixelSizeInBits / 8;

        final long writeLimits = downloadServiceConfiguration.getWriteLimits();
        SpatioTemporalCoverageSlicer slicer = new SpatioTemporalCoverageSlicer(reader, coverageInfo, filter);
        if (slicer.hasMultipleSlices()) {
            // Rough assumption: the size of the raster to be written is multiplied by the number of slices
            rasterSizeInBytes *= slicer.getNumSlices();
        }
        // Let's re-use the slicer to avoid having it re-initialized by the actual download machinery
        SpatioTemporalCoverageSlicer.SpatioTemporalCoverageSlicerHolder.set(slicer);

        // If size exceeds the write limits, false is returned
        if (writeLimits > DownloadServiceConfiguration.NO_LIMIT && rasterSizeInBytes > writeLimits) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Output raw raster size ("
                                + rasterSizeInBytes
                                + ") exceeds"
                                + " the specified write limits ("
                                + writeLimits
                                + ")");
            }
            return false;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Output raw raster size ("
                            + rasterSizeInBytes
                            + ") does not exceed"
                            + " the specified write limits ("
                            + writeLimits
                            + ")");
        }
        return true;
    }

    private List<CoverageDimensionInfo> getBandDimensionsFromCoverageInfo(CoverageInfo ci) throws Exception {
        String nativeName = ci.getNativeCoverageName();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(ci.getStore());
        MetadataMap metadata = ci.getMetadata();
        CoverageInfo rebuilt;
        if (metadata != null && metadata.containsKey(CoverageView.COVERAGE_VIEW)) {
            GridCoverage2DReader reader = (GridCoverage2DReader)
                    catalog.getResourcePool().getGridCoverageReader(ci, nativeName, GeoTools.getDefaultHints());
            rebuilt = cb.buildCoverage(reader, nativeName, null);
        } else {
            rebuilt = cb.buildCoverage(nativeName);
        }
        return rebuilt.getDimensions();
    }
}
