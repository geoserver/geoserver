/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.geotools.renderer.lite.VectorMapRenderUtils.buildTransform;
import static org.geotools.renderer.lite.VectorMapRenderUtils.createLiteFeatureTypeStyles;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.ScreenMap;
import org.geotools.renderer.crs.ProjectionHandler;
import org.geotools.renderer.crs.ProjectionHandlerFinder;
import org.geotools.renderer.lite.LiteFeatureTypeStyle;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.VectorMapRenderUtils;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class VectorTileMapOutputFormat extends AbstractMapOutputFormat {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(VectorTileMapOutputFormat.class);

    /** WMS Service configuration * */
    private final WMS wms;

    private final VectorTileBuilderFactory tileBuilderFactory;

    private boolean clipToMapBounds;

    private boolean transformToScreenCoordinates;

    public VectorTileMapOutputFormat(WMS wms, VectorTileBuilderFactory tileBuilderFactory) {
        super(tileBuilderFactory.getMimeType(), tileBuilderFactory.getOutputFormats());
        this.wms = wms;
        this.tileBuilderFactory = tileBuilderFactory;
    }

    public void setClipToMapBounds(boolean clip) {
        this.clipToMapBounds = clip;
    }

    public void setTransformToScreenCoordinates(boolean useScreenCoords) {
        this.transformToScreenCoordinates = useScreenCoords;
    }

    @Override
    public WebMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {

        mapContent.setMapWidth(5 * mapContent.getMapWidth());
        mapContent.setMapHeight(5 * mapContent.getMapHeight());
        
        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        final CoordinateReferenceSystem mapCrs = renderingArea.getCoordinateReferenceSystem();
        final AffineTransform worldToScreen = mapContent.getRenderingTransform();
        final Rectangle paintArea = new Rectangle(mapContent.getMapWidth(),
                mapContent.getMapHeight());

        final Polygon mapBounds = JTS.toGeometry(renderingArea);
        final Polygon screenBounds = JTS.toGeometry(new Envelope(0, mapContent.getMapWidth(), 0,
                mapContent.getMapHeight()));

        VectorTileBuilder vectorTileBuilder;
        vectorTileBuilder = this.tileBuilderFactory.newBuilder(paintArea, renderingArea);

        for (Layer layer : mapContent.layers()) {

            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
            GeometryDescriptor geometryDescriptor = featureSource.getSchema()
                    .getGeometryDescriptor();
            if (null == geometryDescriptor) {
                continue;
            }

            CoordinateReferenceSystem sourceCrs = geometryDescriptor.getType()
                    .getCoordinateReferenceSystem();

            ProjectionHandler projectionHandler;
            MathTransform sourceToTargetCrs;
            MathTransform targetToScreen;
            MathTransform sourceToScreen;
            try {
                final boolean wrap = false;
                projectionHandler = ProjectionHandlerFinder.getHandler(renderingArea, sourceCrs,
                        wrap);
                sourceToTargetCrs = buildTransform(sourceCrs, mapCrs);
                targetToScreen = ProjectiveTransform.create(worldToScreen);
                sourceToScreen = ConcatenatedTransform.create(sourceToTargetCrs, targetToScreen);
            } catch (FactoryException e) {
                throw new ServiceException(e);
            }

            Query query = getStyleQuery(layer, mapContent);
            ScreenMap screenMap = (ScreenMap) query.getHints().get(Hints.SCREENMAP);
            query.getHints().remove(Hints.SCREENMAP);

            FeatureCollection<?, ?> features = featureSource.getFeatures(query);
            Feature next;
            Stopwatch sw = Stopwatch.createStarted();
            int count = 0;
            int total = 0;

            final boolean transformToScreenCoordinates = this.transformToScreenCoordinates;
            final boolean clipToMapBounds = this.clipToMapBounds;

            final MathTransform tx = transformToScreenCoordinates ? sourceToScreen
                    : sourceToTargetCrs;
            final double pixelDistance = 1;
            final double simplificationDistance = getSimplificationDistance(sourceToScreen,
                    paintArea, pixelDistance);
            final double distanceTolerance = transformToScreenCoordinates ? pixelDistance
                    : simplificationDistance;
            final Polygon clippingPolygon = transformToScreenCoordinates ? screenBounds : mapBounds;

            try (FeatureIterator<?> it = features.features()) {
                while (it.hasNext()) {
                    next = it.next();
                    total++;
                    Geometry originalGeom;
                    Geometry preProcessed;
                    Geometry finalGeom;

                    originalGeom = (Geometry) next.getDefaultGeometryProperty().getValue();
                    try {
                        preProcessed = preprocess(originalGeom, projectionHandler, screenMap);
                    } catch (TransformException | FactoryException e) {
                        continue;
                    }
                    if (preProcessed == null) {
                        continue;
                    }

                    try {
                        finalGeom = JTS.transform(preProcessed, tx);
                    } catch (MismatchedDimensionException | TransformException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (finalGeom.getDimension() > 0) {
                        finalGeom = DouglasPeuckerSimplifier.simplify(finalGeom, distanceTolerance);
                    }

                    Geometry clipped = finalGeom;
                    if (clipToMapBounds) {
                        try {
                            clipped = finalGeom.intersection(clippingPolygon);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }

                    if (clipped.isEmpty()) {
                        continue;
                    }

                    if (clipped instanceof GeometryCollection && clipped.getNumGeometries() == 1) {
                        clipped = clipped.getGeometryN(0);
                    }

                    // Can't do this, SimpleFeatureImpl is broken
                    // next.getDefaultGeometryProperty().setValue(screenGeom);
                    ((SimpleFeature) next).setDefaultGeometry(clipped);

                    vectorTileBuilder.addFeature((SimpleFeature) next);
                    count++;
                }
            }
            sw.stop();
            if (LOGGER.isLoggable(Level.FINE)) {
                String msg = String.format("Added %,d out of %,d features of '%s' in %s", count,
                        total, layer.getTitle(), sw);
                System.err.println(msg);
                LOGGER.fine(msg);
            }
        }

        WebMap map = vectorTileBuilder.build(mapContent);
        return map;
    }

    private double getSimplificationDistance(MathTransform worldToScreen, Rectangle paintArea,
            double pixelDistance) {

        double[] spans;
        try {
            MathTransform screenToWorld = worldToScreen.inverse();
            spans = Decimator.computeGeneralizationDistances(screenToWorld, paintArea,
                    pixelDistance);
        } catch (TransformException e) {
            throw Throwables.propagate(e);
        }
        return Math.min(spans[0], spans[1]);
    }

    private Geometry preprocess(Geometry originalGeom, ProjectionHandler projectionHandler,
            ScreenMap screenMap) throws TransformException, FactoryException {

        if (projectionHandler == null) {
            return originalGeom;
        }
        if (originalGeom == null || originalGeom.isEmpty()) {
            return null;
        }
        Geometry preProcessed = projectionHandler.preProcess(originalGeom);
        if (preProcessed == null || preProcessed.isEmpty()) {
            return null;
        }
        if (preProcessed instanceof GeometryCollection && preProcessed.getNumGeometries() == 1) {
            preProcessed = preProcessed.getGeometryN(0);
        }

        if (preProcessed.getDimension() > 0 && screenMap != null) {
            Envelope env = preProcessed.getEnvelopeInternal();
            if (screenMap.canSimplify(env))
                if (screenMap.checkAndSet(env)) {
                    return null;
                } else {
                    preProcessed = screenMap.getSimplifiedShape(env.getMinX(), env.getMinY(),
                            env.getMaxX(), env.getMaxY(), preProcessed.getFactory(),
                            preProcessed.getClass());
                }
        }
        return preProcessed;
    }

    private Query getStyleQuery(Layer layer, WMSMapContent mapContent) throws IOException {

        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        final Rectangle screenSize = new Rectangle(mapContent.getMapWidth(),
                mapContent.getMapHeight());
        final double mapScale;
        try {
            mapScale = RendererUtilities.calculateScale(renderingArea, mapContent.getMapWidth(),
                    mapContent.getMapHeight(), null);
        } catch (TransformException | FactoryException e) {
            throw Throwables.propagate(e);
        }

        FeatureSource<?, ?> featureSource = layer.getFeatureSource();
        FeatureType schema = featureSource.getSchema();
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        AffineTransform worldToScreen = mapContent.getRenderingTransform();

        Style style = layer.getStyle();
        List<FeatureTypeStyle> featureStyles = style.featureTypeStyles();
        List<LiteFeatureTypeStyle> styleList = createLiteFeatureTypeStyles(featureStyles, schema,
                mapScale, screenSize);
        Query styleQuery;
        CoordinateReferenceSystem mapCRS;
        CoordinateReferenceSystem sourceCrs;
        try {
            mapCRS = renderingArea.getCoordinateReferenceSystem();
            sourceCrs = geometryDescriptor.getCoordinateReferenceSystem();
            styleQuery = VectorMapRenderUtils
                    .getStyleQuery(featureSource, schema, styleList, renderingArea, mapCRS,
                            sourceCrs, screenSize, geometryDescriptor, worldToScreen);
        } catch (IllegalFilterException | FactoryException e1) {
            throw Throwables.propagate(e1);
        }
        Query query = styleQuery;
        // query.setProperties(ImmutableList.of(FF.property(geometryDescriptor.getName())));
        query.setProperties(Query.ALL_PROPERTIES);
        query.setCoordinateSystem(renderingArea.getCoordinateReferenceSystem());
        // query.setCoordinateSystemReproject(renderingArea.getCoordinateReferenceSystem());
        // query.setMaxFeatures(1_000);

        // CoordinateSequenceFactory coordSeq = PackedCoordinateSequenceFactory.FLOAT_FACTORY;
        // PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING_SINGLE);
        // GeometryFactory geomFact = new GeometryFactory(precisionModel, 0, coordSeq);

        Hints hints = query.getHints();
        // hints.put(Hints.JTS_COORDINATE_SEQUENCE_FACTORY, coordSeq);
        // hints.put(Hints.JTS_GEOMETRY_FACTORY, geomFact);
        hints.put(Hints.FEATURE_2D, Boolean.TRUE);

        return query;
    }

    /**
     * Timeout on the smallest nonzero value of the WMS timeout and the timeout format option If
     * both are zero then there is no timeout
     * 
     * @param localMaxRenderingTime
     * @return
     */
    public int getMaxRenderingTime(int localMaxRenderingTime) {

        int maxRenderingTime = wms.getMaxRenderingTime() * 1000;

        if (maxRenderingTime == 0) {
            maxRenderingTime = localMaxRenderingTime;
        } else if (localMaxRenderingTime != 0) {
            maxRenderingTime = Math.min(maxRenderingTime, localMaxRenderingTime);
        }

        return maxRenderingTime;
    }

    /**
     * @return {@code null}, not a raster format.
     */
    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return null;
    }

}
