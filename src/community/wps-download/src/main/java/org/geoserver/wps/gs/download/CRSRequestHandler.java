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
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
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
                        && !CRS.equalsIgnoreMetadata(originalNativeCRS, originalTargetCRS)
                        && Utils.isSupportedCRS(reader, originalTargetCRS)
                        && haveGranulesMatchingTargetCRS(reader, originalTargetCRS, roi, filter);

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

    /** Checking if at least a granule in the ROI has the same CRS as targetCRS */
    private boolean haveGranulesMatchingTargetCRS(
            GridCoverage2DReader reader,
            CoordinateReferenceSystem targetCRS,
            Geometry roi,
            Filter filter)
            throws IOException, FactoryException {
        if (structuredReader == null) {
            // only StructuredGridCoverage2DReader can support requests in targetCRS,
            // since they can expose a crs attribute
            return false;
        }
        Integer crsCode = CRS.lookupEpsgCode(targetCRS, false);
        if (crsCode == null) {
            return false;
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
        SimpleFeatureCollection features = granules.getGranules(query);

        if (features == null || features.isEmpty()) {
            return false;
        }
        return true;
    }

    public CoordinateReferenceSystem getCRS(String granuleCrsCode) throws IOException {
        return catalog.getResourcePool().getCRS(granuleCrsCode);
    }
}
