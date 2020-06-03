/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Class to handle requested CRSs and ROI in order to determine if the request is involving any
 * granule with nativeCRS matching the targetCRS. Once the required flags and properties have been
 * set, it need to be initialized through the init method before any of its methods get used.
 */
class CRSRequestHandler {
    private static final Logger LOGGER = Logging.getLogger(CRSRequestHandler.class);

    private static final FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;

    // ----------------
    // Input Parameters
    // ----------------
    private final GridCoverage2DReader reader;

    private StructuredGridCoverage2DReader structuredReader;
    private final Geometry roi;
    private Filter filter;
    private CoordinateReferenceSystem originalTargetCRS;
    private CoordinateReferenceSystem originalNativeCRS;
    private Catalog catalog;

    /**
     * Requested flag to use the best available resolution of granules in ROI, having their CRS
     * matching the targetCRS
     */
    private boolean useBestResolutionOnMatchingCRS;

    /** Requested flag to minimizeReprojections on granules having CRS matching the Target one */
    private boolean minimizeReprojections;

    // ------------------------------------------------
    // Computed parameters once initialization occurred
    // ------------------------------------------------
    private boolean initialized = false;

    private boolean needsReprojection = false;

    /** reporting if the request can actually use targetCRS as native */
    private boolean canUseTargetCRSAsNative = false;

    /** The targetCRS actually selected for the processing. */
    private CoordinateReferenceSystem selectedTargetCRS;

    private Map<String, DimensionDescriptor> descriptors = null;

    private ROIManager roiManager;

    /** The computed targetEnvelope after initialization */
    private ReferencedEnvelope targetEnvelope;

    /**
     * When minimizing the reprojections, there might be the case that the requested area covers
     * granules in different CRSs. Take note of a reference feature from the requested CRS for
     * better gridGeometry alignment.
     */
    private SimpleFeature referenceFeatureForAlignment;

    private String crsAttribute;

    private double resolutionsDifferenceTolerance;

    public CRSRequestHandler(
            GridCoverage2DReader reader,
            Catalog catalog,
            CoordinateReferenceSystem originalTargetCRS,
            Geometry roi) {
        this.reader = reader;
        this.catalog = catalog;
        this.originalTargetCRS = originalTargetCRS;
        this.selectedTargetCRS = originalTargetCRS;
        this.roi = roi;
        if (StructuredGridCoverage2DReader.class.isAssignableFrom(reader.getClass())) {
            structuredReader = (StructuredGridCoverage2DReader) reader;
        }
        originalNativeCRS = reader.getCoordinateReferenceSystem();
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public CoordinateReferenceSystem getSelectedNativeCRS() {
        return canUseTargetCRSAsNative ? originalTargetCRS : originalNativeCRS;
    }

    public void setMinimizeReprojections(boolean minimizeReprojections) {
        this.minimizeReprojections = minimizeReprojections;
    }

    public boolean canUseBestResolutionOnMatchingCRS() {
        return useBestResolutionOnMatchingCRS && canUseTargetCRSAsNative;
    }

    public void setUseBestResolutionOnMatchingCRS(boolean bestResolutionOnMatchingCrs) {
        this.useBestResolutionOnMatchingCRS = bestResolutionOnMatchingCrs;
    }

    public SimpleFeature getReferenceFeatureForAlignment() {
        return referenceFeatureForAlignment;
    }

    public Map<String, DimensionDescriptor> getDescriptors() {
        return descriptors;
    }

    public boolean needsReprojection() {
        return needsReprojection;
    }

    public boolean hasStructuredReader() {
        return (structuredReader != null);
    }

    public GridCoverage2DReader getReader() {
        return reader;
    }

    public ROIManager getRoiManager() {
        return roiManager;
    }

    public boolean canUseTargetCRSAsNative() {
        return canUseTargetCRSAsNative;
    }

    public Filter getFilter() {
        return filter;
    }

    public CoordinateReferenceSystem getSelectedTargetCRS() {
        return selectedTargetCRS;
    }

    public ReferencedEnvelope getTargetEnvelope() {
        return targetEnvelope;
    }

    public StructuredGridCoverage2DReader getStructuredReader() {
        return structuredReader;
    }

    public void init() throws IOException, FactoryException, TransformException {
        if (initialized) return;

        // Initialize dimension descriptors
        if (structuredReader != null) {
            String coverageName = structuredReader.getGridCoverageNames()[0];
            descriptors =
                    structuredReader
                            .getDimensionDescriptors(coverageName)
                            .stream()
                            .collect(Collectors.toMap(dd -> dd.getName(), dd -> dd));
            DimensionDescriptor crsDescriptor = descriptors.get(DimensionDescriptor.CRS);
            crsAttribute = crsDescriptor != null ? crsDescriptor.getStartAttribute() : null;
        } else {
            descriptors = Collections.emptyMap();
        }

        // Check if the request can actually use the TargetCRS as if it would be the
        // native one of the reader (so no reprojection in the mix).
        canUseTargetCRSAsNative =
                minimizeReprojections
                        && roi != null
                        && originalTargetCRS != null
                        && descriptors.containsKey(DimensionDescriptor.CRS)
                        && (referenceFeatureForAlignment =
                                        getFirstGranuleMatchingCRS(
                                                reader, originalTargetCRS, roi, filter))
                                != null
                        && !CRS.equalsIgnoreMetadata(originalNativeCRS, originalTargetCRS)
                        && Utils.isSupportedCRS(reader, originalTargetCRS);

        MathTransform reprojectionTransform = null;
        CoordinateReferenceSystem nativeCRS = getSelectedNativeCRS();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Native CRS is " + nativeCRS.toWKT());
        }

        // Initialize reprojection flag and
        if (originalTargetCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, originalTargetCRS)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Checking if reprojection is needed");
            }
            // testing reprojection...
            reprojectionTransform = CRS.findMathTransform(nativeCRS, originalTargetCRS, true);
            if (!reprojectionTransform.isIdentity()) {
                // avoid doing the transform if this is the identity
                needsReprojection = true;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Reprojection needed");
                }
            }
        } else {
            selectedTargetCRS = nativeCRS;
        }

        // Initialize ROI management machinery and target envelope
        if (roi != null) {
            this.roiManager = new ROIManager(roi, (CoordinateReferenceSystem) roi.getUserData());
            this.roiManager.useNativeCRS(getSelectedNativeCRS());
            this.roiManager.useTargetCRS(getSelectedTargetCRS());
            targetEnvelope =
                    new ReferencedEnvelope(
                            roiManager.getRoiInTargetCRS().getEnvelopeInternal(),
                            selectedTargetCRS);
        } else {
            targetEnvelope = new ReferencedEnvelope(reader.getOriginalEnvelope());
            if (needsReprojection) {
                targetEnvelope = targetEnvelope.transform(selectedTargetCRS, true);
            }
        }
        initialized = true;
    }

    /**
     * Checking if at least a granule in the ROI has the same CRS as targetCRS and return the
     * related feature (returning null if no granules are found)
     *
     * @return
     */
    private SimpleFeature getFirstGranuleMatchingCRS(
            GridCoverage2DReader reader,
            CoordinateReferenceSystem targetCRS,
            Geometry roi,
            Filter filter)
            throws IOException, FactoryException {
        if (structuredReader == null) {
            // only StructuredGridCoverage2DReader can support requests in targetCRS,
            // since they can expose a crs attribute
            return null;
        }
        Integer crsCode = CRS.lookupEpsgCode(targetCRS, false);
        if (crsCode == null) {
            return null;
        }
        String coverageName = reader.getGridCoverageNames()[0];
        GranuleSource granules = structuredReader.getGranules(coverageName, true);
        List<Filter> filters = new ArrayList<Filter>();
        Query query = Query.ALL;

        GeometryDescriptor geomDescriptor = granules.getSchema().getGeometryDescriptor();
        CoordinateReferenceSystem indexCRS = geomDescriptor.getCoordinateReferenceSystem();
        DimensionDescriptor crsDescriptor = descriptors.get(DimensionDescriptor.CRS);
        Geometry queryGeometry = DownloadUtilities.transformGeometry(roi, indexCRS);

        final PropertyName geometryProperty = FF.property(geomDescriptor.getName());
        filters.add(FF.intersects(geometryProperty, FF.literal(queryGeometry)));
        filters.add(
                FF.equals(
                        FF.property(crsDescriptor.getStartAttribute()),
                        FF.literal("EPSG:" + crsCode)));
        // Add the filter if specified
        if (filter != null) {
            filters.add(filter);
        }
        // Set the query filter
        query = new Query();
        query.setFilter(Predicates.and(filters));
        query.setHints(new Hints(GranuleSource.NATIVE_BOUNDS, true));
        SimpleFeatureCollection features = granules.getGranules(query);
        if (features != null && !features.isEmpty()) {
            try (SimpleFeatureIterator iterator = features.features()) {
                return iterator.next();
            }
        }
        return null;
    }

    public CoordinateReferenceSystem getCRS(String granuleCrsCode) throws IOException {
        return catalog.getResourcePool().getCRS(granuleCrsCode);
    }

    /** Compute the bbox of the provided feature, taking into account its native CRS if available */
    public BoundingBox computeBBox(SimpleFeature feature, CoordinateReferenceSystem schemaCRS)
            throws FactoryException, TransformException, IOException {
        ROIManager roiManager = getRoiManager();
        if (roiManager != null && canUseTargetCRSAsNative()) {
            Object nativeBoundsTest = feature.getUserData().get(GranuleSource.NATIVE_BOUNDS_KEY);
            if (nativeBoundsTest instanceof ReferencedEnvelope) {
                ReferencedEnvelope re = (ReferencedEnvelope) nativeBoundsTest;
                return re.transform(getSelectedTargetCRS(), true);
            } else {
                return computeBBoxReproject(feature, schemaCRS);
            }
        } else {
            // Classic behaviour
            return feature.getBounds();
        }
    }

    public BoundingBox computeBBoxReproject(
            SimpleFeature feature, CoordinateReferenceSystem schemaCRS)
            throws IOException, FactoryException, TransformException {
        String granuleCrsCode = (String) feature.getAttribute(crsAttribute);
        CoordinateReferenceSystem granuleCRS = getCRS(granuleCrsCode);
        CoordinateReferenceSystem targetCRS = getSelectedTargetCRS();
        Geometry geom = (Geometry) feature.getDefaultGeometry();
        MathTransform transform = CRS.findMathTransform(schemaCRS, targetCRS);
        if (CRS.equalsIgnoreMetadata(targetCRS, granuleCRS)) {
            // The granule has same CRS as TargetCRS
            // Do not reproject the boundingBox itself but let's
            // reproject the geometry to get the native bbox
            if (!transform.isIdentity()) {
                geom = JTS.transform(geom, transform);
            }
            return JTS.bounds(geom, targetCRS);
        } else {
            // Reproject the granule geometry to the requested CRS
            return JTS.bounds(geom, schemaCRS).transform(targetCRS, true);
        }
    }

    public double getResolutionsDifferenceTolerance() {
        return resolutionsDifferenceTolerance;
    }

    public void setResolutionsDifferenceTolerance(double resolutionsDifferenceTolerance) {
        this.resolutionsDifferenceTolerance = resolutionsDifferenceTolerance;
    }
}
