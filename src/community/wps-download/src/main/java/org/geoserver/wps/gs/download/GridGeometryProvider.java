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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A class delegated to return the proper GridGeometry to be used by Raster Download when target
 * size is not specified.
 */
class GridGeometryProvider {

    private static final Logger LOGGER = Logging.getLogger(GridGeometryProvider.class);

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

        public ResolutionProvider(Map<String, DimensionDescriptor> descriptors) {
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

        /**
         * Get the best resolution from the input {@link SimpleFeatureCollection}.
         *
         * @param features
         * @param resolutions
         * @return
         * @throws FactoryException
         * @throws TransformException
         * @throws IOException
         */
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
            CoordinateReferenceSystem schemaCrs =
                    schema.getGeometryDescriptor().getCoordinateReferenceSystem();

            // Iterate over the features to extract the best resolution
            BoundingBox featureBBox = null;
            GeneralEnvelope env = null;
            ReferencedEnvelope envelope = null;
            try (SimpleFeatureIterator iterator = features.features()) {
                double[] res = new double[2];
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    extractResolution(
                            feature, resXAttribute, resYAttribute, crsAttribute, schemaCrs, res);

                    // Update bestResolution x and y
                    bestResolution[0] = res[0] < bestResolution[0] ? res[0] : bestResolution[0];
                    bestResolution[1] = res[1] < bestResolution[1] ? res[1] : bestResolution[1];
                    featureBBox = feature.getBounds();

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
            return envelope;
        }

        /**
         * Extract the resolution from the specified feature via the resolution attributes.
         *
         * @param feature
         * @param resXAttribute
         * @param resYAttribute
         * @param crsAttribute
         * @param schemaCrs
         * @param resolution
         * @throws FactoryException
         * @throws TransformException
         * @throws IOException
         */
        private void extractResolution(
                SimpleFeature feature,
                String resXAttribute,
                String resYAttribute,
                String crsAttribute,
                CoordinateReferenceSystem schemaCrs,
                double[] resolution)
                throws FactoryException, TransformException, IOException {
            resolution[0] = (Double) feature.getAttribute(resXAttribute);
            resolution[1] =
                    hasBothResolutions
                            ? (Double) feature.getAttribute(resYAttribute)
                            : resolution[0];
            if (isHeterogeneousCrs) {
                String crsId = (String) feature.getAttribute(crsAttribute);
                CoordinateReferenceSystem granuleCrs = catalog.getResourcePool().getCRS(crsId);
                transformResolution(feature, schemaCrs, granuleCrs, resolution);
            }
        }

        /**
         * Compute the transformed resolution of the provided feature since we are in the case of
         * heterogeneous CRS.
         *
         * @param feature
         * @param schemaCrs
         * @param granuleCrs
         * @param resolution
         * @throws FactoryException
         * @throws TransformException
         */
        private void transformResolution(
                SimpleFeature feature,
                CoordinateReferenceSystem schemaCrs,
                CoordinateReferenceSystem granuleCrs,
                double[] resolution)
                throws FactoryException, TransformException {
            MathTransform transform = CRS.findMathTransform(schemaCrs, granuleCrs);

            // Do nothing if the CRS transformation is the identity
            if (!transform.isIdentity()) {
                BoundingBox bounds = feature.getBounds();
                MathTransform inverse = transform.inverse();

                // Get the center coordinate in the granule's CRS
                double center[] =
                        new double[] {
                            (bounds.getMaxX() + bounds.getMinX()) / 2,
                            (bounds.getMaxY() + bounds.getMinY()) / 2
                        };
                transform.transform(center, 0, center, 0, 1);

                // Setup 2 segments in granule's CRS
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

                // Transform the coordinates back to schemaCrs
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
    }

    /** The underlying reader */
    private GridCoverage2DReader reader;

    /** The ROIManager instance */
    private ROIManager roiManager;

    /** The specified filter (may be null) */
    private Filter filter;

    /** A reference to the geoserver catalog */
    private Catalog catalog;

    public GridGeometryProvider(
            GridCoverage2DReader reader, ROIManager roiManager, Filter filter, Catalog catalog) {
        this.reader = reader;
        this.roiManager = roiManager;
        this.filter = filter;
        this.catalog = catalog;
    }

    /**
     * Compute the requested GridGeometry taking into account resolution dimensions descriptor (if
     * any), specified filter and ROI
     */
    public GridGeometry2D getGridGeometry()
            throws TransformException, IOException, FactoryException {
        if (!StructuredGridCoverage2DReader.class.isAssignableFrom(reader.getClass())) {

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
                    (StructuredGridCoverage2DReader) reader;
            String coverageName = reader.getGridCoverageNames()[0];

            Map<String, DimensionDescriptor> descriptors =
                    structuredReader
                            .getDimensionDescriptors(coverageName)
                            .stream()
                            .collect(Collectors.toMap(dd -> dd.getName(), dd -> dd));

            ResolutionProvider provider = new ResolutionProvider(descriptors);

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
            // Initialize resolution with infinite numbers
            double[] resolution = new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            ReferencedEnvelope envelope = provider.getBestResolution(features, resolution);
            AffineTransform at =
                    new AffineTransform(
                            resolution[0],
                            0,
                            0,
                            -resolution[1],
                            envelope.getMinX(),
                            envelope.getMaxY());
            MathTransform tx = ProjectiveTransform.create(at);

            return new GridGeometry2D(
                    PixelInCell.CELL_CORNER, tx, envelope, GeoTools.getDefaultHints());
        }
    }

    /**
     * Initialize the query to get granules, based on provided filter and region of interest (if
     * any)
     *
     * @param granules
     * @return
     * @throws TransformException
     * @throws FactoryException
     * @throws IOException
     */
    private Query initQuery(GranuleSource granules)
            throws TransformException, FactoryException, IOException {
        List<Filter> filters = new ArrayList<Filter>();

        Query query = Query.ALL;

        // Set bbox query if a ROI has been provided
        if (roiManager != null) {
            CoordinateReferenceSystem targetCRS = roiManager.getTargetCRS();
            GeometryDescriptor geomDescriptor = granules.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem indexCRS = geomDescriptor.getCoordinateReferenceSystem();
            ReferencedEnvelope envelope = null;
            if (targetCRS != null && !roiManager.isRoiCrsEqualsTargetCrs()) {
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
        if (filter != null) {
            filters.add(filter);
        }

        // Set the query filter
        query = new Query();
        query.setFilter(Predicates.and(filters));

        return query;
    }

    /**
     * Default GridGeometry retrieval based on native resolution.
     *
     * @return
     * @throws TransformException
     * @throws IOException
     */
    private GridGeometry2D getNativeResolutionGridGeometry()
            throws TransformException, IOException {
        final ReferencedEnvelope roiEnvelope =
                roiManager != null
                        ? new ReferencedEnvelope(
                                roiManager.getSafeRoiInNativeCRS().getEnvelopeInternal(),
                                roiManager.getNativeCRS())
                        : null;
        ScaleToTarget scaling = new ScaleToTarget(reader, roiEnvelope);
        return scaling.getGridGeometry();
    }
}
