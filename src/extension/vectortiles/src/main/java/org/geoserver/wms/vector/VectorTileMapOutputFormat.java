/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Stopwatch;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.MetatileContextHolder;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.MetaTilingOutputFormat;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.StyleQueryUtil;
import org.geoserver.wms.vector.PipelineBuilder.ClipRemoveDegenerateGeometries;
import org.geoserver.wms.vector.iterator.VTFeature;
import org.geoserver.wms.vector.iterator.VTIterator;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.process.geometry.PolygonLabelProcess;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

public class VectorTileMapOutputFormat extends AbstractMapOutputFormat implements MetaTilingOutputFormat {

    private static final class MetatileBuilders {
        final VectorTileBuilder[] builders;
        final int metaX;
        final int metaY;

        MetatileBuilders(VectorTileBuilder[] builders, int metaX, int metaY) {
            this.builders = builders;
            this.metaX = metaX;
            this.metaY = metaY;
        }
    }

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(VectorTileMapOutputFormat.class);

    private final VectorTileBuilderFactory tileBuilderFactory;

    static final int CLIP_BBOX_SIZE_INCREASE_PIXELS = 12;

    private volatile boolean clipToMapBounds;

    private volatile double overSamplingFactor =
            2.0; // 1=no oversampling, 4=four time oversample (generialization will be 1/4 pixel)

    private volatile boolean transformToScreenCoordinates;

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

    @FunctionalInterface
    /** Accepts features for addition to the tile builders */
    private interface FeatureSink {
        void accept(
                String layerName, String featureId, String geometryName, Geometry geom, Map<String, Object> properties);
    }

    /** Creates a feature sink that adds features into a single tile / tileBuilder */
    private static FeatureSink singleSink(VectorTileBuilder builder) {
        return builder::addFeature;
    }

    /** Creates a feature sink that distributes features into multiple tiles */
    private static FeatureSink tiledSink(
            VectorTileBuilder[] builders, int metaX, int metaY, double subtileW, double subtileH, double bufferPx) {

        // Precompute clip polygons in METATILE screen coords (one per subtile)
        final int numTiles = metaX * metaY;
        ClipRemoveDegenerateGeometries[] clippers = new ClipRemoveDegenerateGeometries[numTiles];
        AffineTransformation[] transforms = new AffineTransformation[numTiles];
        for (int ty = 0; ty < metaY; ty++) {
            for (int tx = 0; tx < metaX; tx++) {
                double x0 = tx * subtileW;
                double y0 = (metaY - 1 - ty) * subtileH;

                // Expand clip envelope by buffer
                Envelope env =
                        new Envelope(x0 - bufferPx, x0 + subtileW + bufferPx, y0 - bufferPx, y0 + subtileH + bufferPx);

                // define geometry clipper and transform to relocate into tile-local coords
                clippers[ty * metaX + tx] = new ClipRemoveDegenerateGeometries(env);
                transforms[ty * metaX + tx] = AffineTransformation.translationInstance(-x0, -y0);
            }
        }
        return (layerName, fid, geomName, geom, props) -> {
            addFeaturesToBuilders(
                    builders,
                    metaX,
                    metaY,
                    subtileW,
                    subtileH,
                    bufferPx,
                    clippers,
                    transforms,
                    layerName,
                    fid,
                    geomName,
                    geom,
                    props);
        };
    }

    private static void addFeaturesToBuilders(
            VectorTileBuilder[] builders,
            int metaX,
            int metaY,
            double subtileW,
            double subtileH,
            double bufferPx,
            ClipRemoveDegenerateGeometries[] clippers,
            AffineTransformation[] transforms,
            String layerName,
            String fid,
            String geomName,
            Geometry geom,
            Map<String, Object> props) {

        if (geom == null || geom.isEmpty()) return;

        Envelope e = geom.getEnvelopeInternal();

        // Compute overlapping tiles in METATILE screen coords
        int minTx = (int) Math.floor((e.getMinX() - bufferPx) / subtileW);
        int maxTx = (int) Math.floor((e.getMaxX() + bufferPx) / subtileW);
        int minTy = (int) Math.floor((subtileH * metaY - (e.getMaxY() + bufferPx)) / subtileH);
        int maxTy = (int) Math.floor((subtileH * metaY - (e.getMinY() - bufferPx)) / subtileH);

        minTx = Math.max(0, minTx);
        maxTx = Math.min(metaX - 1, maxTx);
        minTy = Math.max(0, minTy);
        maxTy = Math.min(metaY - 1, maxTy);

        for (int ty = minTy; ty <= maxTy; ty++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                int idx = ty * metaX + tx;

                Geometry clipped;
                try {
                    clipped = clippers[idx]._run(geom);
                } catch (Exception ignored) {
                    continue;
                }
                if (clipped == null || clipped.isEmpty()) continue;

                Geometry local = transforms[idx].transform(clipped);
                if (local == null || local.isEmpty()) continue;

                builders[idx].addFeature(layerName, fid, geomName, local, props);
            }
        }
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
            paintArea = new Rectangle(
                    this.tileBuilderFactory.getOversampleX() * mapWidth,
                    this.tileBuilderFactory.getOversampleY() * mapHeight);
        }

        MetatileContextHolder.MetaInfo mi = MetatileContextHolder.get();
        final boolean metatiled = (mi != null);

        final VectorTileBuilder[] builders;
        final int metaX;
        final int metaY;
        if (!metatiled) {
            metaX = 1;
            metaY = 1;
            builders = new VectorTileBuilder[] {tileBuilderFactory.newBuilder(paintArea, renderingArea)};
        } else {
            MetatileBuilders mb = createMetatileBuilders(mi, paintArea, renderingArea);
            metaX = mb.metaX;
            metaY = mb.metaY;
            builders = mb.builders;
        }

        CoordinateReferenceSystem sourceCrs;
        for (Layer layer : mapContent.layers()) {
            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
            FeatureType schema = featureSource.getSchema();
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            if (null == geometryDescriptor) {
                continue;
            }

            sourceCrs = geometryDescriptor.getType().getCoordinateReferenceSystem();
            int buffer = StyleQueryUtil.getComputedBuffer(
                    mapContent.getBuffer(),
                    StyleQueryUtil.getFeatureStyles(
                            layer, StyleQueryUtil.getMapScale(mapContent, renderingArea), schema));
            if (this.tileBuilderFactory.shouldOversampleScale()) {
                // buffer is in pixels (style pixels), need to convert to paint area pixels
                buffer *= Math.max(
                        Math.max(this.tileBuilderFactory.getOversampleX(), this.tileBuilderFactory.getOversampleY()),
                        1); // if 0 (i.e. test case), don't expand
            }
            VectorTileOptions vectorTileOptions = new VectorTileOptions(layer, mapContent);
            Query query = StyleQueryUtil.getStyleQuery(layer, mapContent);
            vectorTileOptions.customizeQuery(query);
            Hints hints = query.getHints();
            Pipeline pipeline = getPipeline(
                    mapContent, renderingArea, paintArea, sourceCrs, featureSource.getSupportedHints(), hints, buffer);
            hints.remove(Hints.SCREENMAP);
            FeatureCollection<?, ?> features = featureSource.getFeatures(query);
            final FeatureSink sink;
            if (!metatiled) {
                // Single tile -> single sink
                sink = singleSink(builders[0]);
            } else {
                // geometries are in screen coords of 'paintArea'
                final double subtileScreenW = paintArea.getWidth() / metaX;
                final double subtileScreenH = paintArea.getHeight() / metaY;
                sink = tiledSink(
                        builders,
                        metaX,
                        metaY,
                        subtileScreenW,
                        subtileScreenH,
                        buffer + CLIP_BBOX_SIZE_INCREASE_PIXELS);
            }

            String layerName = schema.getName().getLocalPart();
            boolean coalesceEnabled = vectorTileOptions.isCoalesceEnabled();
            run(features, pipeline, geometryDescriptor, layer, false, layerName, coalesceEnabled, sink);

            if (vectorTileOptions.generateLabelLayer()) {
                vectorTileOptions.customizeLabelQuery(query);
                features = featureSource.getFeatures(query);
                layerName = layerName + "_labels";
                run(
                        features,
                        pipeline,
                        geometryDescriptor,
                        layer,
                        vectorTileOptions.isPolygonLabelEnabled(),
                        layerName,
                        coalesceEnabled,
                        sink);
            }
        }
        if (builders.length == 1) {
            return builders[0].build(mapContent);
        } else {
            byte[][] tiles = new byte[builders.length][];
            int idx = 0;
            for (VectorTileBuilder builder : builders) {
                // Build each tile (side effect on the builder)
                WebMap built = builder.build(mapContent);
                if (!(built instanceof RawMap r)) {
                    throw new ServiceException("Expected a RawMap");
                }
                tiles[idx++] = r.getMapContents();
            }
            return new VectorTileMetatilingWebMap(mapContent, metaX, tiles);
        }
    }

    private MetatileBuilders createMetatileBuilders(
            MetatileContextHolder.MetaInfo mi, Rectangle paintArea, ReferencedEnvelope renderingArea) {

        final int tileSizePx = mi.getTileSize();
        final int metaX = mi.getWidth() / tileSizePx;
        final int metaY = mi.getHeight() / tileSizePx;

        Rectangle tilePaintArea = new Rectangle(tileSizePx, tileSizePx);
        if (tileBuilderFactory.shouldOversampleScale()) {
            tilePaintArea = new Rectangle(
                    tileBuilderFactory.getOversampleX() * tileSizePx, tileBuilderFactory.getOversampleY() * tileSizePx);
        }

        VectorTileBuilder[] builders = new VectorTileBuilder[metaX * metaY];

        double tileWorldW = renderingArea.getWidth() / metaX;
        double tileWorldH = renderingArea.getHeight() / metaY;

        CoordinateReferenceSystem crs = renderingArea.getCoordinateReferenceSystem();
        LongAdder sharedMetaBytes = new LongAdder();

        WMS wms = GeoServerExtensions.bean(WMS.class);
        int memoryKbLimit = wms.getMaxRequestMemory() * 1024;

        for (int ty = 0; ty < metaY; ty++) {
            for (int tx = 0; tx < metaX; tx++) {
                double minX = renderingArea.getMinX() + tx * tileWorldW;
                double maxX = minX + tileWorldW;
                double maxY = renderingArea.getMaxY() - ty * tileWorldH;
                double minY = maxY - tileWorldH;

                ReferencedEnvelope tileEnv = new ReferencedEnvelope(crs);
                tileEnv.init(minX, maxX, minY, maxY);

                int idx = ty * metaX + tx;

                VectorTileBuilder builder = tileBuilderFactory.newBuilder(tilePaintArea, tileEnv);

                builders[idx] = memoryKbLimit > 0
                        ? new MemoryGuardedVectorTileBuilder(builder, memoryKbLimit, sharedMetaBytes)
                        : builder;
            }
        }

        return new MetatileBuilders(builders, metaX, metaY);
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
                    PipelineBuilder.newBuilder(renderingArea, paintArea, sourceCrs, overSamplingFactor, buffer);

            pipeline = builder.preprocess()
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

    void run(
            FeatureCollection<?, ?> features,
            Pipeline pipeline,
            GeometryDescriptor geometryDescriptor,
            Layer layer,
            boolean labelPoint,
            String layerName,
            boolean coalesce,
            FeatureSink sink) {
        Stopwatch sw = Stopwatch.createStarted();
        int count = 0;
        int total = 0;

        final String geometryName = geometryDescriptor.getName().getLocalPart();
        try (VTIterator it = VTIterator.getIterator(features.features(), coalesce)) {
            while (it.hasNext()) {
                VTFeature feature = it.next();
                total++;

                Geometry originalGeom = feature.getGeometry();
                if (labelPoint) originalGeom = getLabelPoint(originalGeom);
                Geometry finalGeom;
                try {
                    finalGeom = pipeline.execute(originalGeom);
                } catch (Exception processingException) {
                    LOGGER.log(Level.WARNING, processingException.getLocalizedMessage(), processingException);
                    continue;
                }
                if (finalGeom.isEmpty()) {
                    continue;
                }
                sink.accept(layerName, feature.getFeatureId(), geometryName, finalGeom, feature.getProperties());
                count++;
            }
        }
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            String msg = "Added %,d out of %,d features of '%s' in %s".formatted(count, total, layer.getTitle(), sw);
            LOGGER.fine(msg);
        }
    }

    /** Computes a label point for the geometry, using the "poly label" algorithm for polygons */
    private Geometry getLabelPoint(Geometry originalGeom) {
        if (originalGeom instanceof Polygon || originalGeom instanceof MultiPolygon) {
            return PolygonLabelProcess.PolyLabeller(originalGeom, null);
        }
        return originalGeom;
    }

    /** @return {@code null}, not a raster format. */
    @Override
    public MapProducerCapabilities getCapabilities(String format) {
        return null;
    }
}
