/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.geotools.renderer.lite.VectorMapRenderUtils.createLiteFeatureTypeStyles;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.LiteFeatureTypeStyle;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.VectorMapRenderUtils;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Geometry;

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

        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        final Rectangle paintArea = new Rectangle(mapContent.getMapWidth(),
                mapContent.getMapHeight());

        VectorTileBuilder vectorTileBuilder;
        vectorTileBuilder = this.tileBuilderFactory.newBuilder(paintArea, renderingArea);

        CoordinateReferenceSystem sourceCrs;
        for (Layer layer : mapContent.layers()) {

            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
            GeometryDescriptor geometryDescriptor = featureSource.getSchema()
                    .getGeometryDescriptor();
            if (null == geometryDescriptor) {
                continue;
            }

            sourceCrs = geometryDescriptor.getType().getCoordinateReferenceSystem();

            PipelineBuilder builder;
            try {
                builder = PipelineBuilder.newBuilder(renderingArea, paintArea, sourceCrs);
            } catch (FactoryException e) {
                throw new ServiceException(e);
            }
            Pipeline pipeline = builder//
                    .transformToScreenCoordinates(transformToScreenCoordinates)//
                    .clipToMapBounds(clipToMapBounds)//
                    .preprocess()//
                    .transform()//
                    .simplify()//
                    .clip()//
                    .collapseCollections()//
                    .build();

            Query query = getStyleQuery(layer, mapContent);
            query.getHints().remove(Hints.SCREENMAP);

            FeatureCollection<?, ?> features = featureSource.getFeatures(query);
            Feature feature;
            Stopwatch sw = Stopwatch.createStarted();
            int count = 0;
            int total = 0;

            try (FeatureIterator<?> it = features.features()) {
                while (it.hasNext()) {
                    feature = it.next();
                    total++;
                    Geometry originalGeom;
                    Geometry finalGeom;

                    originalGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                    try {
                        finalGeom = pipeline.execute(originalGeom);
                    } catch (Exception processingException) {
                        processingException.printStackTrace();
                        continue;
                    }
                    if (finalGeom.isEmpty()) {
                        continue;
                    }

                    final String layerName = feature.getName().getLocalPart();
                    final String featureId = feature.getIdentifier().toString();
                    final String geometryName = geometryDescriptor.getName().getLocalPart();

                    final Map<String, Object> properties = getProperties(feature);

                    vectorTileBuilder.addFeature(layerName, featureId, geometryName, finalGeom,
                            properties);
                    count++;
                }
            }
            sw.stop();
            if (LOGGER.isLoggable(Level.FINE)) {
                String msg = String.format("Added %,d out of %,d features of '%s' in %s", count,
                        total, layer.getTitle(), sw);
                // System.err.println(msg);
                LOGGER.fine(msg);
            }
        }

        WebMap map = vectorTileBuilder.build(mapContent);
        return map;
    }

    private Map<String, Object> getProperties(ComplexAttribute feature) {
        Map<String, Object> props = new TreeMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value;
            if (p instanceof ComplexAttribute) {
                value = getProperties((ComplexAttribute) p);
            } else {
                value = p.getValue();
            }
            if (value != null) {
                props.put(name, value);
            }
        }
        return props;
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
