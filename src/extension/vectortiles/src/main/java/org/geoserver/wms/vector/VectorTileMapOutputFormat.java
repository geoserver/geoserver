/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geotools.renderer.lite.VectorMapRenderUtils.getStyleQuery;

import com.google.common.base.Stopwatch;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.ComplexAttribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.VectorMapRenderUtils;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

public class VectorTileMapOutputFormat extends AbstractMapOutputFormat {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(VectorTileMapOutputFormat.class);

    private final VectorTileBuilderFactory tileBuilderFactory;

    private boolean clipToMapBounds;

    private double overSamplingFactor =
            2.0; // 1=no oversampling, 4=four time oversample (generialization will be 1/4 pixel)

    private boolean transformToScreenCoordinates;

    public VectorTileMapOutputFormat(VectorTileBuilderFactory tileBuilderFactory) {
        super(tileBuilderFactory.getMimeType(), tileBuilderFactory.getOutputFormats());
        this.tileBuilderFactory = tileBuilderFactory;
    }

    /** Multiplies density of simplification from its base value. */
    public void setOverSamplingFactor(double factor) {
        this.overSamplingFactor = factor;
    }

    /** Does this format use features clipped to the extent of the tile instead of whole features */
    public void setClipToMapBounds(boolean clip) {
        this.clipToMapBounds = clip;
    }

    /** Does this format use screen coordinates */
    public void setTransformToScreenCoordinates(boolean useScreenCoords) {
        this.transformToScreenCoordinates = useScreenCoords;
    }

    @Override
    public WebMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {
        checkNotNull(mapContent);
        checkNotNull(mapContent.getRenderingArea());
        checkArgument(mapContent.getMapWidth() > 0);
        checkArgument(mapContent.getMapHeight() > 0);

        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        int mapWidth = mapContent.getMapWidth();
        int mapHeight = mapContent.getMapHeight();
        Rectangle paintArea = new Rectangle(mapWidth, mapHeight);
        if (this.tileBuilderFactory.shouldOversampleScale()) {
            paintArea =
                    new Rectangle(
                            this.tileBuilderFactory.getOversampleX() * mapWidth,
                            this.tileBuilderFactory.getOversampleY() * mapHeight);
        }

        final VectorTileBuilder vectorTileBuilder =
                this.tileBuilderFactory.newBuilder(paintArea, renderingArea);

        CoordinateReferenceSystem sourceCrs;
        for (Layer layer : mapContent.layers()) {

            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
            GeometryDescriptor geometryDescriptor =
                    featureSource.getSchema().getGeometryDescriptor();
            if (null == geometryDescriptor) {
                continue;
            }

            sourceCrs = geometryDescriptor.getType().getCoordinateReferenceSystem();
            int buffer =
                    VectorMapRenderUtils.getComputedBuffer(
                            mapContent.getBuffer(),
                            VectorMapRenderUtils.getFeatureStyles(
                                    layer,
                                    paintArea,
                                    VectorMapRenderUtils.getMapScale(mapContent, renderingArea),
                                    featureSource.getSchema()));
            if (this.tileBuilderFactory.shouldOversampleScale()) {
                // buffer is in pixels (style pixels), need to convert to paint area pixels
                buffer *=
                        Math.max(
                                Math.max(
                                        this.tileBuilderFactory.getOversampleX(),
                                        this.tileBuilderFactory.getOversampleY()),
                                1); // if 0 (i.e. test case), don't expand
            }

            Query query = getStyleQuery(layer, mapContent);
            Hints hints = query.getHints();

            Pipeline pipeline =
                    getPipeline(
                            mapContent,
                            renderingArea,
                            paintArea,
                            sourceCrs,
                            featureSource.getSupportedHints(),
                            hints,
                            buffer);

            hints.remove(Hints.SCREENMAP);

            FeatureCollection<?, ?> features = featureSource.getFeatures(query);

            run(features, pipeline, geometryDescriptor, vectorTileBuilder, layer);
        }

        WebMap map = vectorTileBuilder.build(mapContent);
        return map;
    }

    protected Pipeline getPipeline(
            final WMSMapContent mapContent,
            final ReferencedEnvelope renderingArea,
            final Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs,
            final Set<RenderingHints.Key> fsHints,
            final Hints qHints,
            int buffer) {
        final Pipeline pipeline;
        try {
            final PipelineBuilder builder =
                    PipelineBuilder.newBuilder(
                            renderingArea, paintArea, sourceCrs, overSamplingFactor, buffer);

            pipeline =
                    builder.preprocess()
                            .transform(transformToScreenCoordinates)
                            .clip(clipToMapBounds, transformToScreenCoordinates)
                            .simplify(transformToScreenCoordinates, fsHints, qHints)
                            .collapseCollections()
                            .build();

        } catch (FactoryException e) {
            throw new ServiceException(e);
        }
        return pipeline;
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

    void run(
            FeatureCollection<?, ?> features,
            Pipeline pipeline,
            GeometryDescriptor geometryDescriptor,
            VectorTileBuilder vectorTileBuilder,
            Layer layer) {
        Stopwatch sw = Stopwatch.createStarted();
        int count = 0;
        int total = 0;
        Feature feature;

        try (FeatureIterator<?> it = features.features()) {
            while (it.hasNext()) {
                feature = it.next();
                total++;
                Geometry finalGeom;

                Geometry originalGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                try {
                    finalGeom = pipeline.execute(originalGeom);
                } catch (Exception processingException) {
                    LOGGER.log(
                            Level.WARNING,
                            processingException.getLocalizedMessage(),
                            processingException);
                    continue;
                }
                if (finalGeom.isEmpty()) {
                    continue;
                }

                final String layerName = feature.getType().getName().getLocalPart();
                final String featureId = feature.getIdentifier().toString();
                final String geometryName = geometryDescriptor.getName().getLocalPart();

                final Map<String, Object> properties = getProperties(feature);

                vectorTileBuilder.addFeature(
                        layerName, featureId, geometryName, finalGeom, properties);
                count++;
            }
        }
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            String msg =
                    String.format(
                            "Added %,d out of %,d features of '%s' in %s",
                            count, total, layer.getTitle(), sw);
            // System.err.println(msg);
            LOGGER.fine(msg);
        }
    }

    /** @return {@code null}, not a raster format. */
    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return null;
    }
}
