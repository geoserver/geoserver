/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Predicates;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.*;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

/**
 * A class delegated to return the proper GridGeometry to be used by Raster Download when target
 * size is not specified.
 */
class GridGeometryProvider {

    private static final Logger LOGGER = Logging.getLogger(GridGeometryProvider.class);

    private static final int PADDING = 50;
    /**
     * Class delegate to extract the resolution from a features collection based on the available
     * resolution related descriptors.
     */
    class ResolutionProvider {

        // Resolution descriptors
        private DimensionDescriptor resDescriptor;

        private DimensionDescriptor resXDescriptor;

        private DimensionDescriptor resYDescriptor;

        // CRS descriptor
        private DimensionDescriptor crsDescriptor;

        private boolean hasBothResolutions;

        private boolean isHeterogeneousCrs;

        private CRSRequestHandler crsRequestHandler;

        public ResolutionProvider(CRSRequestHandler crsRequestHandler) {
            this.crsRequestHandler = crsRequestHandler;
            Map<String, DimensionDescriptor> descriptors = crsRequestHandler.getDescriptors();
            resDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION);
            resXDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION_X);
            resYDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION_Y);
            crsDescriptor = descriptors.get(DimensionDescriptor.CRS);
            hasBothResolutions = resXDescriptor != null && resYDescriptor != null;
            isHeterogeneousCrs = crsDescriptor != null;
        }

        /** No resolution can be provided if there isn't any resolution related descriptor */
        boolean canCompute() {
            return resDescriptor != null || (resXDescriptor != null && resYDescriptor != null);
        }

        /** Get the best resolution from the input {@link SimpleFeatureCollection}. */
        public ReferencedEnvelope getBestResolution(
                SimpleFeatureCollection features, final double[] bestResolution)
                throws FactoryException, TransformException, IOException {

            // Setting up features attributes to be checked
            final String resXAttribute =
                    hasBothResolutions
                            ? resXDescriptor.getStartAttribute()
                            : resDescriptor.getStartAttribute();
            final String resYAttribute =
                    hasBothResolutions
                            ? resYDescriptor.getStartAttribute()
                            : resDescriptor.getStartAttribute();

            String crsAttribute = isHeterogeneousCrs ? crsDescriptor.getStartAttribute() : null;
            SimpleFeatureType schema = features.getSchema();
            CoordinateReferenceSystem schemaCRS =
                    schema.getGeometryDescriptor().getCoordinateReferenceSystem();

            CoordinateReferenceSystem referenceCRS =
                    crsRequestHandler.canUseTargetCRSAsNative()
                            ? crsRequestHandler.getSelectedTargetCRS()
                            : schemaCRS;
            // Iterate over the features to extract the best resolution
            BoundingBox featureBBox = null;
            GeneralEnvelope env = null;
            ReferencedEnvelope envelope = null;
            double[] fallbackResolution =
                    new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};

            // Look for the best resolution available from features matching the targetCRS.
            // Keep also updating a secondary resolution from features not matching
            // the targetCRS to be used as fallback in case that none of the available
            // features is matching the requested CRS.
            try (SimpleFeatureIterator iterator = features.features()) {
                double[] res = new double[2];
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    extractResolution(
                            feature,
                            resXAttribute,
                            resYAttribute,
                            crsAttribute,
                            referenceCRS,
                            res,
                            fallbackResolution,
                            bestResolution);

                    featureBBox = crsRequestHandler.computeBBox(feature, schemaCRS);
                    // Update the accessed envelope
                    if (env == null) {
                        env = new GeneralEnvelope(featureBBox);
                    } else {
                        env.add(featureBBox);
                    }
                }
            }
            if (env != null) {
                envelope = new ReferencedEnvelope(env);
            }
            if (Double.isInfinite(bestResolution[0]) || Double.isInfinite(bestResolution[1])) {
                // There might be the case that no granules have been found having native CRS
                // matching the Target one, so no best resolution has been retrieved on that CRS.
                // Let's use the fallback resolution
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "No granules are matching the targetCRS. Going to use fallback resolution:"
                                    + "\nresX="
                                    + fallbackResolution[0]
                                    + " resY="
                                    + fallbackResolution[1]);
                }
                bestResolution[0] = fallbackResolution[0];
                bestResolution[1] = fallbackResolution[1];
            }
            return envelope;
        }

        /** Extract the resolution from the specified feature via the resolution attributes. */
        private void extractResolution(
                SimpleFeature feature,
                String resXAttribute,
                String resYAttribute,
                String crsAttribute,
                CoordinateReferenceSystem referenceCRS,
                double[] resolution,
                double[] fallbackResolution,
                double[] bestResolution)
                throws FactoryException, TransformException, IOException {
            resolution[0] = (Double) feature.getAttribute(resXAttribute);
            resolution[1] =
                    hasBothResolutions
                            ? (Double) feature.getAttribute(resYAttribute)
                            : resolution[0];
            CoordinateReferenceSystem granuleCRS = null;
            if (isHeterogeneousCrs) {
                String crsId = (String) feature.getAttribute(crsAttribute);
                granuleCRS = crsRequestHandler.getCRS(crsId);
                transformResolution(feature, referenceCRS, granuleCRS, resolution);
            }
            boolean updateBest =
                    !crsRequestHandler.canUseBestResolutionOnMatchingCRS()
                            || CRS.equalsIgnoreMetadata(granuleCRS, referenceCRS);
            updateResolution(resolution, updateBest ? bestResolution : fallbackResolution);
        }

        private void updateResolution(double[] currentResolution, double[] storedResolution) {
            storedResolution[0] =
                    currentResolution[0] < storedResolution[0]
                            ? currentResolution[0]
                            : storedResolution[0];
            storedResolution[1] =
                    currentResolution[1] < storedResolution[1]
                            ? currentResolution[1]
                            : storedResolution[1];
        }

        /**
         * Compute the transformed resolution of the provided feature since we are in the case of
         * heterogeneous CRS.
         */
        private void transformResolution(
                SimpleFeature feature,
                CoordinateReferenceSystem schemaCRS,
                CoordinateReferenceSystem granuleCRS,
                double[] resolution)
                throws FactoryException, TransformException {
            MathTransform transform = CRS.findMathTransform(schemaCRS, granuleCRS);

            // Do nothing if the CRS transformation is the identity
            if (!transform.isIdentity()) {
                BoundingBox bounds = feature.getBounds();
                // Get the center coordinate in the granule's CRS
                double center[] =
                        new double[] {
                            (bounds.getMaxX() + bounds.getMinX()) / 2,
                            (bounds.getMaxY() + bounds.getMinY()) / 2
                        };

                MathTransform inverse = transform.inverse();
                transform.transform(center, 0, center, 0, 1);

                // Setup 2 segments in inputCrs
                double[] coords = new double[6];
                double[] resCoords = new double[6];

                // center
                coords[0] = center[0];
                coords[1] = center[1];

                // DX from center
                coords[2] = center[0] + resolution[0];
                coords[3] = center[1];

                // DY from center
                coords[4] = center[0];
                coords[5] = center[1] + resolution[1];

                // Transform the coordinates back to targetCrs
                inverse.transform(coords, 0, resCoords, 0, 3);

                double dx1 = resCoords[2] - resCoords[0];
                double dx2 = resCoords[3] - resCoords[1];
                double dy1 = resCoords[4] - resCoords[0];
                double dy2 = resCoords[5] - resCoords[1];

                // Computing euclidean distances
                double transformedDX = Math.sqrt(dx1 * dx1 + dx2 * dx2);
                double transformedDY = Math.sqrt(dy1 * dy1 + dy2 * dy2);
                resolution[0] = transformedDX;
                resolution[1] = transformedDY;
            }
        }

        /** Gets granules' resolution if it is equal for all the granules, otherwise returns null */
        public double[] getGranulesNativeResolutionIfSame(GranuleSource granuleSource)
                throws IOException, TransformException, FactoryException {

            Query query = initQuery(granuleSource);
            SimpleFeatureCollection granules = granuleSource.getGranules(query);
            return getGranulesNativeResolutionIfSame(granules);
        }

        /** Gets granules' resolution if it is equal for all the granules, otherwise returns null */
        public double[] getGranulesNativeResolutionIfSame(SimpleFeatureCollection granules) {
            if (granules == null || granules.isEmpty()) return null;

            SimpleFeatureIterator iterator = granules.features();
            TreeSet<Double> resolutionsX = new TreeSet<>();
            TreeSet<Double> resolutionsY = new TreeSet<>();
            Map<String, DimensionDescriptor> descriptors = crsRequestHandler.getDescriptors();
            DimensionDescriptor resDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION);
            DimensionDescriptor resXDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION_X);
            DimensionDescriptor resYDescriptor = descriptors.get(DimensionDescriptor.RESOLUTION_Y);
            final String resXAttribute =
                    hasBothResolutions
                            ? resXDescriptor.getStartAttribute()
                            : resDescriptor.getStartAttribute();
            final String resYAttribute =
                    hasBothResolutions
                            ? resYDescriptor.getStartAttribute()
                            : resDescriptor.getStartAttribute();
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                resolutionsX.add((Double) feature.getAttribute(resXAttribute));
                resolutionsY.add((Double) feature.getAttribute(resYAttribute));
            }
            if (resolutionsX.size() > 1 || resolutionsY.size() > 1) {
                return null;
            } else {
                return new double[] {resolutionsX.first(), resolutionsY.first()};
            }
        }

        public double[] getResolution(GridCoverage2D coverage2D) {
            MathTransform2D gridToCRS2D = coverage2D.getGridGeometry().getGridToCRS2D();
            if (!(gridToCRS2D instanceof AffineTransform2D)) {
                return null;
            }
            AffineTransform2D at = (AffineTransform2D) gridToCRS2D;
            return new double[] {Math.abs(at.getScaleX()), Math.abs(at.getScaleY())};
        }
    }

    private CRSRequestHandler crsRequestHandler;

    public GridGeometryProvider(CRSRequestHandler crsRequestHandler) {
        this.crsRequestHandler = crsRequestHandler;
    }

    /**
     * Compute the requested GridGeometry taking into account resolution dimensions descriptor (if
     * any), specified filter and ROI
     */
    public GridGeometry2D getGridGeometry()
            throws TransformException, IOException, FactoryException {
        if (!crsRequestHandler.hasStructuredReader()) {

            //
            // CASE A: simple readers: return the native resolution gridGeometry
            //
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("The underlying reader is not structured; returning native resolution");
            }
            return getNativeResolutionGridGeometry();
        } else {
            //
            // CASE B: StructuredGridCoverage2DReader
            //
            StructuredGridCoverage2DReader structuredReader =
                    crsRequestHandler.getStructuredReader();
            String coverageName = structuredReader.getGridCoverageNames()[0];

            ResolutionProvider provider = new ResolutionProvider(crsRequestHandler);
            //
            // Do we have any resolution descriptor available?
            // if not, go to standard computation.
            //
            if (!provider.canCompute()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "The underlying reader is structured but no resolution descriptors are available"
                                    + ". Returning native resolution");
                }
                return getNativeResolutionGridGeometry();
            }

            GranuleSource granules = structuredReader.getGranules(coverageName, true);
            // Initialize resolution with infinite numbers
            double[] resolution = new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            // Setup a query on top of ROI and input filter (if any)
            Query query = initQuery(granules);
            SimpleFeatureCollection features = granules.getGranules(query);

            if (features == null || features.isEmpty()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "No features available for the specified query. Returning native resolution");
                }
                return getNativeResolutionGridGeometry();
            }
            ReferencedEnvelope envelope = null;

            double resolutionsDifferenceTolerance =
                    crsRequestHandler.getResolutionsDifferenceTolerance();
            boolean forceResolution = false;
            if (crsRequestHandler.getReferenceFeatureForAlignment() == null
                    && !crsRequestHandler.needsReprojection()
                    && resolutionsDifferenceTolerance != 0d) {
                // no reprojection but has been request to try to preserve native
                // resolution if under tolerance value
                resolution = provider.getGranulesNativeResolutionIfSame(features);
                double[] testResolution =
                        new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
                // get best resolution to have a reference
                envelope = provider.getBestResolution(features, testResolution);
                if (testResolution[0] != resolution[0] || testResolution[1] != resolution[1]) {
                    // comparing resolutions
                    double diffPercentageX =
                            Math.abs((resolution[0] / testResolution[0]) - 1) * 100;
                    double diffPercentageY =
                            Math.abs((resolution[1] / testResolution[1]) - 1) * 100;
                    if (!(diffPercentageX < resolutionsDifferenceTolerance
                            && diffPercentageY < resolutionsDifferenceTolerance)) {
                        // difference is beyond the tolerance limit
                        // setting the best resolution
                        resolution = testResolution;
                    } else {
                        forceResolution = true;
                    }
                }
            }

            if (envelope == null) {
                envelope = provider.getBestResolution(features, resolution);
            }

            AffineTransform at =
                    new AffineTransform(
                            resolution[0],
                            0,
                            0,
                            -resolution[1],
                            envelope.getMinX(),
                            envelope.getMaxY());
            MathTransform tx = ProjectiveTransform.create(at);
            return computeGridGeometry2D(tx, envelope, resolution, forceResolution);
        }
    }

    /**
     * Produces a GridGeometry from a reprojected coverage, having the resolution of the native
     * granules. The conditions the method checks in order to return the coverage are:
     *
     * <ol>
     *   <li>Granules have same resolution
     *   <li>The involved CRSs have the same measurement's unit
     *   <li>The percentage difference between the reprojected grid resolution and the original one
     *       is less then the threshold value
     * </ol>
     *
     * If the above conditions are not matched the method returns null
     */
    public GridGeometry2D getGridGeometryWithNativeResolution(
            GridCoverage2D originalCoverage, GridCoverage2D testCoverage)
            throws TransformException, IOException, FactoryException {
        double resolutionsDifferenceTolerance =
                crsRequestHandler.getResolutionsDifferenceTolerance();
        if (!crsRequestHandler.hasStructuredReader()) return null;
        ResolutionProvider resProvider = new ResolutionProvider(crsRequestHandler);
        StructuredGridCoverage2DReader structured = crsRequestHandler.getStructuredReader();

        GranuleSource source = structured.getGranules(originalCoverage.getName().toString(), true);
        double[] resolution = resProvider.getGranulesNativeResolutionIfSame(source);
        boolean canUseNative = resolution != null;
        if (canUseNative) {
            CoordinateSystem nativeCS =
                    crsRequestHandler.getSelectedNativeCRS().getCoordinateSystem();
            CoordinateSystem targetCS =
                    crsRequestHandler.getSelectedTargetCRS().getCoordinateSystem();
            int dimension = nativeCS.getDimension();
            int targetDim = targetCS.getDimension();
            if (dimension != targetDim) {
                canUseNative = false;
            } else {
                for (int i = dimension; --i >= 0; ) {
                    if (!nativeCS.getAxis(i).getUnit().equals(targetCS.getAxis(i).getUnit())) {
                        canUseNative = false;
                        break;
                    }
                }
            }
        }
        if (canUseNative) {
            double[] resolutionsResampled = resProvider.getResolution(testCoverage);
            double diffPercentageX = Math.abs((resolution[0] / resolutionsResampled[0]) - 1) * 100;
            double diffPercentageY = Math.abs((resolution[1] / resolutionsResampled[1]) - 1) * 100;
            if (diffPercentageX < resolutionsDifferenceTolerance
                    && diffPercentageY < resolutionsDifferenceTolerance) {
                ReferencedEnvelope envelope =
                        new ReferencedEnvelope(testCoverage.getGridGeometry().getEnvelope());
                AffineTransform at =
                        new AffineTransform(
                                resolution[0],
                                0,
                                0,
                                -resolution[1],
                                envelope.getMinX(),
                                envelope.getMaxY());
                MathTransform tx = ProjectiveTransform.create(at);
                return computeGridGeometry2D(tx, envelope, resolution, false);
            }
        }
        return null;
    }

    private GridGeometry2D computeGridGeometry2D(
            MathTransform tx,
            ReferencedEnvelope envelope,
            double[] resolution,
            boolean forceResolution)
            throws FactoryException, IOException, TransformException {
        GridGeometry2D gg2d =
                new GridGeometry2D(
                        PixelInCell.CELL_CORNER, tx, envelope, GeoTools.getDefaultHints());
        AffineTransform tx2 = (AffineTransform) gg2d.getGridToCRS();
        double scaleX = XAffineTransform.getScaleX0(tx2);
        double scaleY = XAffineTransform.getScaleY0(tx2);
        // There might be the case that granules that will be reprojected are affecting the
        // requested gridGeometry such that the resulting scale doesn't perfectly match the
        // requested resolution
        if (Math.abs(scaleX - resolution[0]) > 1E-6 || (Math.abs(scaleY - resolution[1]) > 1E-6)) {
            if (crsRequestHandler != null
                    && crsRequestHandler.getReferenceFeatureForAlignment() != null) {
                SimpleFeature referenceFeature =
                        crsRequestHandler.getReferenceFeatureForAlignment();
                // Tweak the requested envelope for better alignment so that the resolution get
                // matched
                BoundingBox refEnvelope =
                        crsRequestHandler.computeBBox(
                                referenceFeature,
                                referenceFeature.getFeatureType().getCoordinateReferenceSystem());
                double minX =
                        snapCoordinate(
                                refEnvelope.getMinX(), envelope.getMinX(), resolution[0], true);
                double minY =
                        snapCoordinate(
                                refEnvelope.getMinY(), envelope.getMinY(), resolution[1], true);
                double maxX =
                        snapCoordinate(
                                refEnvelope.getMaxX(), envelope.getMaxX(), resolution[0], false);
                double maxY =
                        snapCoordinate(
                                refEnvelope.getMaxY(), envelope.getMaxY(), resolution[1], false);
                envelope =
                        new ReferencedEnvelope(
                                minX, maxX, minY, maxY, envelope.getCoordinateReferenceSystem());
                gg2d =
                        new GridGeometry2D(
                                PixelInCell.CELL_CORNER, tx, envelope, GeoTools.getDefaultHints());
            }
        }

        if (crsRequestHandler.needsReprojection()) {
            // Apply padding to read extra pixels.
            MathTransform2D worldToScreen = gg2d.getCRSToGrid2D(PixelOrientation.UPPER_LEFT);
            GridEnvelope2D gridRange = gg2d.getGridRange2D();
            gridRange.setBounds(
                    gridRange.x - PADDING,
                    gridRange.y - PADDING,
                    gridRange.width + PADDING * 2,
                    gridRange.height + PADDING * 2);
            try {
                gg2d =
                        new GridGeometry2D(
                                gridRange,
                                PixelInCell.CELL_CORNER,
                                worldToScreen.inverse(),
                                gg2d.getCoordinateReferenceSystem2D(),
                                null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (forceResolution) {
            GridEnvelope2D gridRange = gg2d.getGridRange2D();
            try {
                gg2d =
                        new GridGeometry2D(
                                gridRange,
                                PixelInCell.CELL_CORNER,
                                tx,
                                gg2d.getCoordinateReferenceSystem2D(),
                                null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return gg2d;
    }

    /**
     * Initialize the query to get granules, based on provided filter and region of interest (if
     * any)
     */
    private Query initQuery(GranuleSource granules)
            throws TransformException, FactoryException, IOException {
        List<Filter> filters = new ArrayList<Filter>();

        Query query = Query.ALL;

        // Set bbox query if a ROI has been provided
        ROIManager roiManager = crsRequestHandler.getRoiManager();
        if (roiManager != null) {
            CoordinateReferenceSystem targetCRS = roiManager.getTargetCRS();
            GeometryDescriptor geomDescriptor = granules.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem indexCRS = geomDescriptor.getCoordinateReferenceSystem();
            ReferencedEnvelope envelope = null;
            if (targetCRS != null
                    && (!roiManager.isRoiCrsEqualsTargetCrs()
                            || crsRequestHandler.canUseTargetCRSAsNative())) {
                envelope =
                        new ReferencedEnvelope(
                                roiManager.getSafeRoiInTargetCRS().getEnvelopeInternal(),
                                targetCRS);
                MathTransform reprojectionTrasform = null;
                if (!CRS.equalsIgnoreMetadata(targetCRS, indexCRS)) {
                    reprojectionTrasform = CRS.findMathTransform(targetCRS, indexCRS, true);
                    if (!reprojectionTrasform.isIdentity()) {
                        envelope = envelope.transform(indexCRS, true);
                    }
                }
            } else {
                envelope =
                        new ReferencedEnvelope(
                                roiManager.getSafeRoiInNativeCRS().getEnvelopeInternal(), indexCRS);
            }

            final PropertyName geometryProperty =
                    FeatureUtilities.DEFAULT_FILTER_FACTORY.property(geomDescriptor.getName());
            filters.add(FeatureUtilities.DEFAULT_FILTER_FACTORY.bbox(geometryProperty, envelope));
        }

        // Add the filter if specified
        Filter filter = crsRequestHandler.getFilter();
        if (filter != null) {
            filters.add(filter);
        }

        // Set the query filter
        query = new Query();
        query.setFilter(Predicates.and(filters));
        query.setHints(new Hints(GranuleSource.NATIVE_BOUNDS, true));

        return query;
    }

    /** Default GridGeometry retrieval based on native resolution. */
    private GridGeometry2D getNativeResolutionGridGeometry() throws IOException {
        GridCoverage2DReader reader = crsRequestHandler.getReader();
        ROIManager roiManager = crsRequestHandler.getRoiManager();
        final ReferencedEnvelope roiEnvelope =
                roiManager != null
                        ? new ReferencedEnvelope(
                                roiManager.getSafeRoiInNativeCRS().getEnvelopeInternal(),
                                roiManager.getNativeCRS())
                        : null;
        ScaleToTarget scaling = new ScaleToTarget(reader, roiEnvelope);
        return scaling.getGridGeometry();
    }

    private double snapCoordinate(
            double referenceCoordinate,
            double inputCoordinate,
            double resolution,
            boolean isLowerValue) {
        // Check how many pixels at the given resolution exist between the 2 coordinate
        double numPixels = Math.abs(referenceCoordinate - inputCoordinate) / resolution;
        int numIntegerPixels = (int) Math.ceil(numPixels);
        if (numIntegerPixels - numPixels > resolution * 1E-6) {
            // number of Pixels is not an integer value so let's snap the coordinate to the
            // resolution.
            return referenceCoordinate
                    + (numIntegerPixels * (isLowerValue ? -resolution : resolution));
        }
        return inputCoordinate;
    }
}
