/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.imagen.Interpolation;
import org.geoserver.catalog.Predicates;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.processing.Operation;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterNotFoundException;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.coverage.processing.operation.Scale;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

/**
 * Applies access limits policies around the wrapped reader
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGridCoverage2DReader extends DecoratingGridCoverage2DReader {

    /** Parameters used to control the {@link Crop} operation. */
    private static final ParameterValueGroup cropParams;

    /** Cached crop factory */
    private static final Crop coverageCropFactory = new Crop();

    /** Cached scale factory */
    private static final Scale coverageScaleFactory = new Scale();

    static {
        final CoverageProcessor processor = new CoverageProcessor(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        cropParams = processor.getOperation("CoverageCrop").getParameters();
    }

    WrapperPolicy policy;

    public SecuredGridCoverage2DReader(GridCoverage2DReader delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public Format getFormat() {
        Format format = delegate.getFormat();
        if (format == null) {
            return null;
        } else {
            return SecuredObjects.secure(format, policy);
        }
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue... parameters) throws IllegalArgumentException, IOException {
        return SecuredGridCoverage2DReader.read(delegate, policy, parameters);
    }

    static GridCoverage2D read(GridCoverage2DReader delegate, WrapperPolicy policy, GeneralParameterValue... parameters)
            throws IllegalArgumentException, IOException {
        // Package private static method to share reading code with Structured reader
        MultiPolygon rasterFilter = null;
        if (policy.getLimits() instanceof CoverageAccessLimits) {
            CoverageAccessLimits limits = (CoverageAccessLimits) policy.getLimits();

            // get the crop filter
            rasterFilter = limits.getRasterFilter();
            Filter readFilter = limits.getReadFilter();

            // update the read params
            final GeneralParameterValue[] limitParams = limits.getParams();
            if (parameters == null || parameters.length == 0) { // beware a no-args call means an empty array
                parameters = limitParams;
            } else if (limitParams != null) {
                // scan the input params, add and overwrite with the limits params as needed
                List<GeneralParameterValue> params = new ArrayList<>(Arrays.asList(parameters));
                for (GeneralParameterValue lparam : limitParams) {
                    // remove the overwritten param, if any
                    for (Iterator it = params.iterator(); it.hasNext(); ) {
                        GeneralParameterValue param = (GeneralParameterValue) it.next();
                        if (param.getDescriptor().equals(lparam.getDescriptor())) {
                            it.remove();
                            break;
                        }
                    }
                    // add the overwrite param (will be an overwrite if it was already there, an
                    // addition otherwise)
                    params.add(lparam);
                }

                parameters = params.toArray(new GeneralParameterValue[params.size()]);
            }

            if (readFilter != null && !Filter.INCLUDE.equals(readFilter)) {
                Format format = delegate.getFormat();
                ParameterValueGroup readParameters = format.getReadParameters();
                List<GeneralParameterDescriptor> descriptors =
                        readParameters.getDescriptor().descriptors();

                // scan all the params looking for the one we want to add
                boolean replacedOriginalFilter = false;
                for (GeneralParameterValue pv : parameters) {
                    String pdCode = pv.getDescriptor().getName().getCode();
                    if ("FILTER".equals(pdCode) || "Filter".equals(pdCode)) {
                        replacedOriginalFilter = true;
                        ParameterValue pvalue = (ParameterValue) pv;
                        Filter originalFilter = (Filter) pvalue.getValue();
                        if (originalFilter == null || Filter.INCLUDE.equals(originalFilter)) {
                            pvalue.setValue(readFilter);
                        } else {
                            Filter combined = Predicates.and(originalFilter, readFilter);
                            pvalue.setValue(combined);
                        }
                    }
                }
                if (!replacedOriginalFilter) {
                    parameters = CoverageUtils.mergeParameter(descriptors, parameters, readFilter, "FILTER", "Filter");
                }
            }
        }

        GridCoverage2D grid = delegate.read(parameters);

        // crop if necessary
        if (rasterFilter != null && grid != null) {
            Geometry coverageBounds = JTS.toGeometry((Envelope) new ReferencedEnvelope(grid.getEnvelope2D()));
            if (coverageBounds.intersects(rasterFilter)) {
                Interpolation interpolation = null;
                for (GeneralParameterValue pv : parameters) {
                    String pdCode = pv.getDescriptor().getName().getCode();
                    if ("Interpolation".equals(pdCode)) {
                        ParameterValue pvalue = (ParameterValue) pv;
                        interpolation = (Interpolation) pvalue.getValue();
                        break;
                    }
                }

                // The underlying reader may have returned a coverage with a larger envelope than the one requested
                grid = cropToEnvelope(
                        grid,
                        new ReferencedEnvelope(
                                rasterFilter.getEnvelopeInternal(), grid.getCoordinateReferenceSystem2D()));

                // The underlying reader may have returned a coverage with a different resolution than the one
                // requested. The requested gridGeometry may have been limited too, due to reaching the
                // Max Oversampling Factor.
                //
                // This happens for example when the data resolution is bad and the map is heavily oversampled.
                // We want to scale it to the requested map raster extent before cropping to the geometry.

                grid = scaleToRequestedSize(
                        grid,
                        getRequestedMapArea(),
                        coverageBounds.getEnvelopeInternal().intersection(rasterFilter.getEnvelopeInternal()),
                        interpolation);
                if (grid != null) {
                    grid = cropToGeometry(grid, rasterFilter);
                }
            } else {
                return null;
            }
        }
        return grid;
    }

    private static RequestedMapArea getRequestedMapArea() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || request.getKvp() == null) {
            return null;
        }

        Map<String, Object> kvp = request.getKvp();
        Integer width = (Integer) kvp.get("WIDTH");
        Integer height = (Integer) kvp.get("HEIGHT");
        if (width == null || height == null) {
            return null;
        }
        return new RequestedMapArea(new Rectangle(width, height), getRequestedEnvelope(kvp));
    }

    private static Envelope getRequestedEnvelope(Map<String, Object> kvp) {
        Envelope envelope = (Envelope) kvp.get("BBOX");
        if (envelope == null) {
            return null;
        }

        if (envelope instanceof ReferencedEnvelope referencedEnvelope
                && referencedEnvelope.getCoordinateReferenceSystem() != null) {
            return referencedEnvelope;
        }

        CoordinateReferenceSystem crs = getRequestedCRS(kvp);
        if (crs == null) {
            return envelope;
        }
        return new ReferencedEnvelope(envelope, crs);
    }

    private static CoordinateReferenceSystem getRequestedCRS(Map<String, Object> kvp) {
        Object crs = kvp.get("CRS");
        if (crs == null) {
            crs = kvp.get("SRS");
        }
        if (crs instanceof CoordinateReferenceSystem coordinateReferenceSystem) {
            return coordinateReferenceSystem;
        }
        if (crs instanceof String srs) {
            try {
                return CRS.decode(srs);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static GridCoverage2D scaleToRequestedSize(
            GridCoverage2D grid,
            RequestedMapArea requestedMapArea,
            Envelope scalingEnvelope,
            Interpolation interpolation) {
        if (grid == null || requestedMapArea == null) {
            return grid;
        }

        ReferencedEnvelope coverageRequestedEnvelope = getCoverageRequestedEnvelope(requestedMapArea.envelope, grid);
        if (coverageRequestedEnvelope == null || scalingEnvelope == null) {
            return grid;
        }

        RenderedImage image = grid.getRenderedImage();
        int width = image.getWidth();
        int height = image.getHeight();
        Rectangle requestedGridRange = requestedMapArea.gridRange;
        int requestedWidth = (int) requestedGridRange.getWidth();
        int requestedHeight = (int) requestedGridRange.getHeight();
        Envelope coveredRequestedEnvelope = scalingEnvelope.intersection(coverageRequestedEnvelope);
        // WMS 1.3.0 with geographic CRS (e.g. EPSG:4326): axis 0 = lat = vertical, axis 1 = lon = horizontal.
        // Swap span axes so WIDTH pixels map to lon extent and HEIGHT pixels map to lat extent.
        boolean northEast =
                CRS.getAxisOrder(coverageRequestedEnvelope.getCoordinateReferenceSystem()) == CRS.AxisOrder.NORTH_EAST;
        if (northEast) {
            requestedWidth = getRequestedSize(
                    requestedWidth, coveredRequestedEnvelope.getHeight(), coverageRequestedEnvelope.getHeight());
            requestedHeight = getRequestedSize(
                    requestedHeight, coveredRequestedEnvelope.getWidth(), coverageRequestedEnvelope.getWidth());
        } else {
            requestedWidth = getRequestedSize(
                    requestedWidth, coveredRequestedEnvelope.getWidth(), coverageRequestedEnvelope.getWidth());
            requestedHeight = getRequestedSize(
                    requestedHeight, coveredRequestedEnvelope.getHeight(), coverageRequestedEnvelope.getHeight());
        }
        if (width <= 0 || height <= 0 || requestedWidth <= 0 || requestedHeight <= 0) {
            return grid;
        }

        double xScale = requestedWidth / (double) width;
        double yScale = requestedHeight / (double) height;
        if (xScale == 1d && yScale == 1d) {
            return grid;
        }

        Operation scaleOperation = CoverageProcessor.getInstance().getOperation("Scale");
        ParameterValueGroup param = scaleOperation.getParameters();
        param.parameter("Source").setValue(grid);
        param.parameter("xScale").setValue(xScale);
        param.parameter("yScale").setValue(yScale);
        param.parameter("xTrans").setValue(0.0);
        param.parameter("yTrans").setValue(0.0);
        setScaleInterpolation(param, interpolation);
        GridCoverage2D scaled = (GridCoverage2D) coverageScaleFactory.doOperation(param, null);
        return new GridCoverageFactory()
                .create(grid.getName().toString(), scaled.getRenderedImage(), grid.getEnvelope2D());
    }

    private static ReferencedEnvelope getCoverageRequestedEnvelope(Envelope requestedEnvelope, GridCoverage2D grid) {
        if (!(requestedEnvelope instanceof ReferencedEnvelope referencedRequestedEnvelope)
                || referencedRequestedEnvelope.getCoordinateReferenceSystem() == null) {
            return null;
        }

        CoordinateReferenceSystem coverageCRS = grid.getCoordinateReferenceSystem2D();
        if (coverageCRS == null) {
            return null;
        }

        try {
            CoordinateReferenceSystem requestedCRS = referencedRequestedEnvelope.getCoordinateReferenceSystem();
            if (CRS.equalsIgnoreMetadata(requestedCRS, coverageCRS)) {
                return referencedRequestedEnvelope;
            }
            return referencedRequestedEnvelope.transform(coverageCRS, true);
        } catch (Exception e) {
            return null;
        }
    }

    private static int getRequestedSize(int fullSize, double coverageSpan, double requestedSpan) {
        if (coverageSpan <= 0 || requestedSpan <= 0) {
            return fullSize;
        }
        return Math.max(1, (int) Math.round(fullSize * coverageSpan / requestedSpan));
    }

    private static class RequestedMapArea {
        private final Rectangle gridRange;
        private final Envelope envelope;

        RequestedMapArea(Rectangle gridRange, Envelope envelope) {
            this.gridRange = gridRange;
            this.envelope = envelope;
        }
    }

    private static void setScaleInterpolation(ParameterValueGroup param, Interpolation interpolation) {
        if (interpolation == null) {
            return;
        }
        try {
            param.parameter("Interpolation").setValue(interpolation);
        } catch (ParameterNotFoundException e) {
            param.parameter("InterpolationType").setValue(interpolation);
        }
    }

    private static GridCoverage2D cropToEnvelope(GridCoverage2D grid, ReferencedEnvelope envelope) {
        ReferencedEnvelope coverageBounds = new ReferencedEnvelope(grid.getEnvelope2D());
        ReferencedEnvelope intersection = envelope.intersection(coverageBounds);
        if (intersection.isEmpty()) {
            return null;
        }

        final ParameterValueGroup param = cropParams.clone();
        param.parameter("Source").setValue(grid);
        param.parameter("Envelope").setValue(intersection);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, null);
    }

    private static GridCoverage2D cropToGeometry(GridCoverage2D grid, Geometry rasterFilter) {
        final ParameterValueGroup param = cropParams.clone();
        param.parameter("Source").setValue(grid);
        param.parameter("ROI").setValue(rasterFilter);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, null);
    }

    @Override
    public ServiceInfo getInfo() {
        ServiceInfo info = delegate.getInfo();
        if (info == null) {
            return null;
        } else {
            return SecuredObjects.secure(info, policy);
        }
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        ResourceInfo info = delegate.getInfo(coverageName);
        if (info == null) {
            return null;
        } else {
            return SecuredObjects.secure(info, policy);
        }
    }
}
