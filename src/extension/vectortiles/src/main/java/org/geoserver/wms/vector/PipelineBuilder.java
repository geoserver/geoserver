/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.geotools.renderer.lite.VectorMapRenderUtils.buildTransform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.geotools.data.util.ScreenMap;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class PipelineBuilder {

    // The base simplification tolerance for screen coordinates.
    private static final double PIXEL_BASE_SAMPLE_SIZE = 0.25;

    static class Context {

        @Nullable ProjectionHandler projectionHandler;

        MathTransform sourceToTargetCrs;

        MathTransform targetToScreen;

        MathTransform sourceToScreen;

        ReferencedEnvelope
                renderingArea; // WMS request; bounding box - in final map (target) CRS (BBOX from
        // WMS)

        Rectangle paintArea; // WMS request; rectangle of the image (width and height from WMS)

        public ScreenMap screenMap;

        public CoordinateReferenceSystem sourceCrs; // data's CRS

        public AffineTransform worldToScreen;

        public double targetCRSSimplificationDistance;

        public double screenSimplificationDistance;

        public double pixelSizeInTargetCRS; // approximate size of a pixel in the Target CRS

        public int queryBuffer;
    }

    Context context;

    // When clipping, we want to expand the clipping box by a bit so that
    // the client (i.e. OpenLayers) doesn't draw the clip lines created when
    // the polygon is clipped to the request BBOX.
    // 12 is what the current streaming renderer (for WMS images) uses
    final int clipBBOXSizeIncreasePixels = 12;

    private Pipeline first = Pipeline.END, last = Pipeline.END;

    private PipelineBuilder(Context context) {
        this.context = context;
    }

    /**
     * @param renderingArea The extent of the tile in target CRS
     * @param paintArea The extent of the tile in screen/pixel coordinates
     * @param sourceCrs The CRS of the features
     * @param overSampleFactor Divisor for simplification tolerance.
     */
    public static PipelineBuilder newBuilder(
            ReferencedEnvelope renderingArea,
            Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs,
            double overSampleFactor,
            int queryBuffer)
            throws FactoryException {

        Context context =
                createContext(renderingArea, paintArea, sourceCrs, overSampleFactor, queryBuffer);
        return new PipelineBuilder(context);
    }

    private static Context createContext(
            ReferencedEnvelope mapArea,
            Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs,
            double overSampleFactor,
            int queryBuffer)
            throws FactoryException {

        Context context = new Context();
        context.renderingArea = mapArea;
        context.paintArea = paintArea;
        context.sourceCrs = sourceCrs;
        context.worldToScreen = RendererUtilities.worldToScreenTransform(mapArea, paintArea);
        context.queryBuffer = queryBuffer;

        final boolean wrap = false;
        context.projectionHandler = ProjectionHandlerFinder.getHandler(mapArea, sourceCrs, wrap);

        CoordinateReferenceSystem mapCrs = context.renderingArea.getCoordinateReferenceSystem();
        context.sourceToTargetCrs = buildTransform(sourceCrs, mapCrs);
        context.targetToScreen = ProjectiveTransform.create(context.worldToScreen);
        context.sourceToScreen =
                ConcatenatedTransform.create(context.sourceToTargetCrs, context.targetToScreen);

        double[] spans_sourceCRS;
        double[] spans_targetCRS;
        try {
            MathTransform screenToWorld = context.sourceToScreen.inverse();

            // 0.8px is used to make sure the generalization isn't too much (doesn't make visible
            // changes)
            spans_sourceCRS =
                    Decimator.computeGeneralizationDistances(screenToWorld, context.paintArea, 0.8);

            spans_targetCRS =
                    Decimator.computeGeneralizationDistances(
                            context.targetToScreen.inverse(), context.paintArea, 1.0);
            // this is used for clipping the data to A pixels around request BBOX, so we want this
            // to be the larger of the two spans
            // so we are getting at least A pixels around.
            context.pixelSizeInTargetCRS = Math.max(spans_targetCRS[0], spans_targetCRS[1]);

        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

        context.screenSimplificationDistance = PIXEL_BASE_SAMPLE_SIZE / overSampleFactor;
        // use min so generalize "less" (if pixel is different size in X and Y)
        context.targetCRSSimplificationDistance =
                Math.min(spans_targetCRS[0], spans_targetCRS[1]) / overSampleFactor;

        context.screenMap = new ScreenMap(0, 0, paintArea.width, paintArea.height);
        context.screenMap.setSpans(
                spans_sourceCRS[0] / overSampleFactor, spans_sourceCRS[1] / overSampleFactor);
        context.screenMap.setTransform(context.sourceToScreen);

        return context;
    }

    /** Prepares features for subsequent manipulation */
    public PipelineBuilder preprocess() {
        addLast(new PreProcess(context.projectionHandler, context.screenMap));
        return this;
    }

    /** Flatten singleton feature collections */
    public PipelineBuilder collapseCollections() {
        addLast(new CollapseCollections());
        return this;
    }

    /** @return the completed pipeline */
    public Pipeline build() {
        return first;
    }

    private void addLast(Pipeline step) {
        if (first == Pipeline.END) {
            first = step;
            last = first;
        } else {
            last.setNext(step);
            last = step;
        }
    }

    private static final class CollapseCollections extends Pipeline {

        @Override
        protected Geometry _run(Geometry geom) throws Exception {

            if (geom instanceof GeometryCollection && geom.getNumGeometries() == 1) {
                return geom.getGeometryN(0);
            }
            return geom;
        }
    }

    private static final class PreProcess extends Pipeline {

        private final ProjectionHandler projectionHandler;

        private final ScreenMap screenMap;

        PreProcess(@Nullable ProjectionHandler projectionHandler, ScreenMap screenMap) {
            this.projectionHandler = projectionHandler;
            this.screenMap = screenMap;
        }

        @Override
        protected Geometry _run(Geometry geom) throws TransformException, FactoryException {

            Geometry preProcessed = geom;
            if (this.projectionHandler != null) {
                preProcessed = projectionHandler.preProcess(geom);
            }

            if (preProcessed == null || preProcessed.isEmpty()) {
                return EMPTY;
            }

            if (preProcessed.getDimension() > 0) {
                Envelope env = preProcessed.getEnvelopeInternal();
                if (screenMap.canSimplify(env))
                    if (screenMap.checkAndSet(env)) {
                        return EMPTY;
                    } else {
                        preProcessed =
                                screenMap.getSimplifiedShape(
                                        env.getMinX(),
                                        env.getMinY(),
                                        env.getMaxX(),
                                        env.getMaxY(),
                                        preProcessed.getFactory(),
                                        preProcessed.getClass());
                    }
            }
            return preProcessed;
        }
    }

    /**
     * Transform from source CRS to target.
     *
     * @param transformToScreenCoordinates If true, further transfrorm from target to screen
     *     coordinates
     */
    public PipelineBuilder transform(final boolean transformToScreenCoordinates) {
        final MathTransform sourceToScreen = context.sourceToScreen;
        final MathTransform sourceToTargetCrs = context.sourceToTargetCrs;

        final MathTransform tx = transformToScreenCoordinates ? sourceToScreen : sourceToTargetCrs;

        addLast(new Transform(tx));
        return this;
    }

    /**
     * Simplify the geometry
     *
     * @param isTransformToScreenCoordinates Use screen coordinate space simplification tolerance
     */
    public PipelineBuilder simplify(boolean isTransformToScreenCoordinates) {

        double pixelDistance = context.screenSimplificationDistance;
        double simplificationDistance = context.targetCRSSimplificationDistance;

        double distanceTolerance =
                isTransformToScreenCoordinates ? pixelDistance : simplificationDistance;

        addLast(new Simplify(distanceTolerance));
        return this;
    }

    /**
     * Clip to the area of the tile plus its gutter
     *
     * @param clipToMapBounds Do we actually want to clip. Does nothing if false.
     * @param transformToScreenCoordinates is the pipeline working in screen coordinates
     */
    public PipelineBuilder clip(boolean clipToMapBounds, boolean transformToScreenCoordinates) {
        if (clipToMapBounds) {

            Envelope clippingEnvelope;

            if (transformToScreenCoordinates) {
                Rectangle screen = context.paintArea;

                Envelope paintArea = new Envelope(0, screen.getWidth(), 0, screen.getHeight());
                paintArea.expandBy(clipBBOXSizeIncreasePixels + context.queryBuffer);

                clippingEnvelope = paintArea;
            } else {
                ReferencedEnvelope renderingArea = context.renderingArea;
                renderingArea.expandBy(
                        (clipBBOXSizeIncreasePixels + context.queryBuffer)
                                * context.pixelSizeInTargetCRS);
                clippingEnvelope = renderingArea;
            }

            addLast(new ClipRemoveDegenerateGeometries(clippingEnvelope));
        }
        return this;
    }

    private static final class Transform extends Pipeline {

        private final MathTransform tx;

        Transform(MathTransform tx) {
            this.tx = tx;
        }

        @Override
        protected Geometry _run(Geometry geom) throws Exception {
            Geometry transformed = JTS.transform(geom, this.tx);
            return transformed;
        }
    }

    private static final class Simplify extends Pipeline {

        private final double distanceTolerance;

        Simplify(double distanceTolerance) {
            this.distanceTolerance = distanceTolerance;
        }

        @Override
        protected Geometry _run(Geometry geom) throws Exception {
            switch (geom.getDimension()) {
                case 2:
                    return TopologyPreservingSimplifier.simplify(geom, this.distanceTolerance);
                case 1:
                    return DouglasPeuckerSimplifier.simplify(geom, this.distanceTolerance);
                default:
                    return geom;
            }
        }
    }

    protected static class Clip extends Pipeline {

        private final Envelope clippingEnvelope;

        Clip(Envelope clippingEnvelope) {
            this.clippingEnvelope = clippingEnvelope;
        }

        @Override
        protected Geometry _run(Geometry geom) throws Exception {
            GeometryClipper clipper = new GeometryClipper(clippingEnvelope);
            try {
                return clipper.clip(geom, true);
            } catch (Exception e) {
                return clipper.clip(geom, false); // use non-robust clipper
            }
        }
    }

    /**
     * Does the normal clipping, but removes degenerative geometries. For example, a polygon-polygon
     * intersection can result in polygons (normal), but also points and line (degenerative).
     *
     * <p>This will remove the degenerative geometries from the result. ie. input is polygon(s),
     * only polygons are returned input is line(s), only lines are returned input is point(s), only
     * points returned
     *
     * <p>For mixed input (GeometryCollection), we do the above for each component in the
     * GeometryCollection. i.e. for GeometryCollection( POLYGON(...), LINESTRING(...) ) it would
     * ensure that the POLYGON(...) only adds Polygons and that the LINESTRING() only adds Lines
     */
    public static final class ClipRemoveDegenerateGeometries extends Clip {

        ClipRemoveDegenerateGeometries(Envelope clippingEnvelope) {
            super(clippingEnvelope);
        }

        @Override
        protected Geometry _run(Geometry geom) throws Exception {
            // protect against empty input geometry
            if ((geom == null) || (geom.isEmpty())) {
                return null;
            }

            // if its a geometrycollection we do each piece individually
            if (geom.getGeometryType() == "GeometryCollection") {
                return collectionClip((GeometryCollection) geom);
            }

            // normal clipping done here
            Geometry result = super._run(geom);

            // if there's no resulting geometry, don't need to deal with it
            if ((result == null) || (result.isEmpty())) {
                return null;
            }

            // make sure the resulting geometry matches the input geometry

            if ((geom instanceof Point) || (geom instanceof MultiPoint)) {
                return onlyPoints(result);
            } else if ((geom instanceof LineString) || (geom instanceof MultiLineString)) {
                return onlyLines(result);
            } else if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) {
                return onlyPolygon(result);
            }
            return result;
        }

        /*
         * We do each geometry in sequence, and remove degenerative geometries generated at each step. For example, if the input is a
         * GeometryCollection(POLYGON(...), LINESTRING(...)) the POLYGON(...) will be clipped (and only polygons preserved) the LINESTRING(..) will be
         * clipped (and only lines preserved)
         *
         * There are some edge cases unhandled here - if the input contains multiple polygons, the result might be better reduced to a multipolygon
         * instead of a GeometryCollect with multiple polygons in it. However, this would be computationally expensive and unlikely to make any
         * difference.
         */
        private Geometry collectionClip(GeometryCollection geom) throws Exception {
            ArrayList<Geometry> result = new ArrayList<Geometry>();
            for (int t = 0; t < geom.getNumGeometries(); t++) {
                Geometry g = geom.getGeometryN(0);
                Geometry clipped = _run(g); // gets the non-degenerative of the result
                if ((clipped != null) && (!clipped.isEmpty())) {
                    result.add(clipped);
                }
            }
            if (result.size() == 0) {
                return null;
            }
            return new GeometryCollection(
                    (Geometry[]) result.toArray(new Geometry[result.size()]), geom.getFactory());
        }

        /*
         * For a geometry, return only polygons. For example, for a GeometryCollection containing points, lines, and polygons only the polygons would
         * be return - the others are removed.
         */
        private Geometry onlyPolygon(Geometry result) {
            if ((result instanceof Polygon) || (result instanceof MultiPolygon)) {
                return result;
            }
            List polys = org.locationtech.jts.geom.util.PolygonExtracter.getPolygons(result);
            if (polys.size() == 0) {
                return null;
            }
            if (polys.size() == 1) {
                return (Polygon) polys.get(0);
            }
            // this could, theoretically, produce invalid MULTIPOLYGONS since polygons cannot share
            // edges. Taking
            // 2 polygons and putting them in a multipolygon is not always valid. However, many
            // systems will not correctly
            // deal with a GeometryCollection with multiple polygons in them.
            // The best strategy is to just create a (potentially) invalid multipolygon.
            return new MultiPolygon(
                    (Polygon[]) polys.toArray(new Polygon[polys.size()]), result.getFactory());
        }

        private Geometry onlyLines(Geometry result) {
            if ((result instanceof LineString) || (result instanceof MultiLineString)) {
                return result;
            }
            List lines = org.locationtech.jts.geom.util.LineStringExtracter.getLines(result);
            if (lines.size() == 0) {
                return null;
            }
            if (lines.size() == 1) {
                return (LineString) lines.get(0);
            }
            return new MultiLineString(
                    (LineString[]) lines.toArray(new LineString[lines.size()]),
                    result.getFactory());
        }

        private Geometry onlyPoints(Geometry result) {
            if ((result instanceof Point) || (result instanceof MultiPoint)) {
                return result;
            }
            List pts = org.locationtech.jts.geom.util.PointExtracter.getPoints(result);
            if (pts.size() == 0) {
                return null;
            }
            if (pts.size() == 1) {
                return (Point) pts.get(0);
            }
            return new MultiPoint(
                    (Point[]) pts.toArray(new Point[pts.size()]), result.getFactory());
        }
    }
}
