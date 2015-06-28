package org.geoserver.wms.vector;

import static org.geotools.renderer.lite.VectorMapRenderUtils.buildTransform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import javax.annotation.Nullable;

import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.ScreenMap;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.lite.RendererUtilities;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

class PipelineBuilder {

    private static class Context {
        boolean clipToMapBounds;

        boolean transformToScreenCoordinates;

        @Nullable
        ProjectionHandler projectionHandler;

        MathTransform sourceToTargetCrs;

        MathTransform targetToScreen;

        MathTransform sourceToScreen;

        ReferencedEnvelope renderingArea;

        Rectangle paintArea;

        public ScreenMap screenMap;

        public CoordinateReferenceSystem sourceCrs;

        public AffineTransform worldToScreen;

        public double pixelDistance;

        public double simplificationDistance;
    }

    private Context context;

    private Pipeline first = Pipeline.END, last = Pipeline.END;

    private PipelineBuilder(Context context) {
        this.context = context;
    }

    public static PipelineBuilder newBuilder(ReferencedEnvelope renderingArea, Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs) throws FactoryException {

        Context context = createContext(renderingArea, paintArea, sourceCrs);
        return new PipelineBuilder(context);
    }

    private static Context createContext(ReferencedEnvelope mapArea, Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs) throws FactoryException {

        Context context = new Context();
        context.renderingArea = mapArea;
        context.paintArea = paintArea;
        context.sourceCrs = sourceCrs;
        context.worldToScreen = RendererUtilities.worldToScreenTransform(mapArea, paintArea);
        context.pixelDistance = 0.25;

        final boolean wrap = false;
        context.projectionHandler = ProjectionHandlerFinder.getHandler(mapArea, sourceCrs, wrap);

        CoordinateReferenceSystem mapCrs = context.renderingArea.getCoordinateReferenceSystem();
        context.sourceToTargetCrs = buildTransform(sourceCrs, mapCrs);
        context.targetToScreen = ProjectiveTransform.create(context.worldToScreen);
        context.sourceToScreen = ConcatenatedTransform.create(context.sourceToTargetCrs,
                context.targetToScreen);

        double[] spans;
        try {
            spans = Decimator.computeGeneralizationDistances(context.sourceToScreen.inverse(),
                    context.paintArea, context.pixelDistance);
        } catch (TransformException e) {
            throw Throwables.propagate(e);
        }
        context.simplificationDistance = Math.max(spans[0], spans[1]);
        context.screenMap = new ScreenMap(0, 0, paintArea.width, paintArea.height);
        context.screenMap.setSpans(spans[0], spans[1]);
        context.screenMap.setTransform(context.sourceToScreen);

        return context;
    }

    public PipelineBuilder transformToScreenCoordinates(boolean transformToScreenCoordinates) {
        context.transformToScreenCoordinates = transformToScreenCoordinates;
        return this;
    }

    public PipelineBuilder clipToMapBounds(boolean clipToMapBounds) {
        context.clipToMapBounds = clipToMapBounds;
        return this;
    }

    public PipelineBuilder preprocess() {
        addLast(new PreProcess(context.projectionHandler, context.screenMap));
        return this;
    }

    public PipelineBuilder collapseCollections() {
        addLast(new CollapseCollections());
        return this;
    }

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
                        preProcessed = screenMap.getSimplifiedShape(env.getMinX(), env.getMinY(),
                                env.getMaxX(), env.getMaxY(), preProcessed.getFactory(),
                                preProcessed.getClass());
                    }
            }
            return preProcessed;
        }

    }

    public PipelineBuilder transform() {
        final boolean transformToScreenCoordinates = context.transformToScreenCoordinates;
        final MathTransform sourceToScreen = context.sourceToScreen;
        final MathTransform sourceToTargetCrs = context.sourceToTargetCrs;

        final MathTransform tx = transformToScreenCoordinates ? sourceToScreen : sourceToTargetCrs;

        addLast(new Transform(tx));
        return this;
    }

    public PipelineBuilder simplify() {
        boolean transformToScreenCoordinates = context.transformToScreenCoordinates;
        double pixelDistance = context.pixelDistance;
        double simplificationDistance = context.simplificationDistance;

        double distanceTolerance = transformToScreenCoordinates ? pixelDistance
                : simplificationDistance;

        addLast(new Simplify(distanceTolerance));
        return this;
    }

    public PipelineBuilder clip() {
        if (context.clipToMapBounds) {

            Geometry clippingPolygon;

            if (context.transformToScreenCoordinates) {
                Rectangle paintArea = context.paintArea;
                clippingPolygon = JTS.toGeometry(paintArea);
            } else {
                ReferencedEnvelope renderingArea = context.renderingArea;
                clippingPolygon = JTS.toGeometry(renderingArea);
            }

            addLast(new Clip(clippingPolygon));
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
            if (geom.getDimension() == 0) {
                return geom;
            }
            Geometry simplified = DouglasPeuckerSimplifier.simplify(geom, this.distanceTolerance);
            return simplified;
        }
    }

    private static final class Clip extends Pipeline {

        private final Geometry clippingPolygon;

        Clip(Geometry clippingPolygon) {
            this.clippingPolygon = clippingPolygon;
        }

        @Override
        protected Geometry _run(Geometry geom) throws Exception {
            Geometry clipped = geom.intersection(clippingPolygon);
            return clipped;
        }
    }

}