/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geotools.renderer.lite.VectorMapRenderUtils.getStyleQuery;

import java.awt.Rectangle;
import java.io.IOException;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Stopwatch;
import com.vividsolutions.jts.geom.Geometry;

public class VectorTileMapOutputFormat extends AbstractMapOutputFormat {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(VectorTileMapOutputFormat.class);

    /** WMS Service configuration * */
    private final WMS wms;

    private final VectorTileBuilderFactory tileBuilderFactory;

    private boolean clipToMapBounds;

    private double overSamplingFactor = 2.0; // 1=no oversampling, 4=four time oversample (generialization will be 1/4 pixel)

    private boolean transformToScreenCoordinates;

    public VectorTileMapOutputFormat(WMS wms, VectorTileBuilderFactory tileBuilderFactory) {
        super(tileBuilderFactory.getMimeType(), tileBuilderFactory.getOutputFormats());
        this.wms = wms;
        this.tileBuilderFactory = tileBuilderFactory;
    }

    public void setOverSamplingFactor(double factor) {
        this.overSamplingFactor = factor;
    }

    public void setClipToMapBounds(boolean clip) {
        this.clipToMapBounds = clip;
    }

    public void setTransformToScreenCoordinates(boolean useScreenCoords) {
        this.transformToScreenCoordinates = useScreenCoords;
    }

    @Override
    public WebMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {
        checkNotNull(mapContent);
        checkNotNull(mapContent.getRenderingArea());
        checkArgument(mapContent.getMapWidth() > 0);
        checkArgument(mapContent.getMapHeight() > 0);

        // mapContent.setMapWidth(5 * mapContent.getMapWidth());
        // mapContent.setMapHeight(5 * mapContent.getMapHeight());

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
                builder = PipelineBuilder.newBuilder(renderingArea, paintArea, sourceCrs,
                        overSamplingFactor);
            } catch (FactoryException e) {
                throw new ServiceException(e);
            }

            Pipeline pipeline = builder.preprocess().transform(transformToScreenCoordinates)
                    .simplify(transformToScreenCoordinates)
                    .clip(clipToMapBounds, transformToScreenCoordinates).collapseCollections()
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

    /**
     * @return {@code null}, not a raster format.
     */
    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return null;
    }

}
