/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.awt.Rectangle;
import java.util.Map;
import org.geoserver.wms.vector.PipelineBuilder.ClipRemoveDegenerateGeometries;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Writes GeoJSON tiles. Unlike the other vector tile formats this one keeps map coordinates, so the meta-tile split has
 * to happen in map coordinates too.
 */
public class GeoJsonVectorTileMapOutputFormat extends VectorTileMapOutputFormat {

    public GeoJsonVectorTileMapOutputFormat(VectorTileBuilderFactory tileBuilderFactory) {
        super(tileBuilderFactory);
    }

    @Override
    protected FeatureSink tiledSink(
            VectorTileBuilder[] builders,
            int metaX,
            int metaY,
            Rectangle paintArea,
            ReferencedEnvelope renderingArea,
            double bufferPx) {

        final double subtileW = renderingArea.getWidth() / metaX;
        final double subtileH = renderingArea.getHeight() / metaY;
        final double buffer = bufferPx * renderingArea.getWidth() / paintArea.getWidth();

        // Precompute clip polygons in map coordinates (one per sub-tile)
        ClipRemoveDegenerateGeometries[] clippers = new ClipRemoveDegenerateGeometries[metaX * metaY];
        for (int ty = 0; ty < metaY; ty++) {
            for (int tx = 0; tx < metaX; tx++) {
                double x0 = renderingArea.getMinX() + tx * subtileW;
                double y0 = renderingArea.getMinY() + ty * subtileH;
                Envelope env = new Envelope(x0 - buffer, x0 + subtileW + buffer, y0 - buffer, y0 + subtileH + buffer);
                clippers[ty * metaX + tx] = new ClipRemoveDegenerateGeometries(env);
            }
        }
        return (layerName, fid, geomName, geom, props) -> addFeatures(
                builders,
                metaX,
                metaY,
                renderingArea,
                subtileW,
                subtileH,
                buffer,
                clippers,
                layerName,
                fid,
                geomName,
                geom,
                props);
    }

    /**
     * Adds the feature to every sub-tile its geometry touches, clipped to the sub-tile. Sub-tiles are numbered as
     * GeoWebCache numbers them, row zero at the bottom, the same way map coordinates grow.
     */
    private static void addFeatures(
            VectorTileBuilder[] builders,
            int metaX,
            int metaY,
            ReferencedEnvelope renderingArea,
            double subtileW,
            double subtileH,
            double buffer,
            ClipRemoveDegenerateGeometries[] clippers,
            String layerName,
            String fid,
            String geomName,
            Geometry geom,
            Map<String, Object> props) {

        if (geom == null || geom.isEmpty()) return;

        Envelope e = geom.getEnvelopeInternal();

        int minTx = (int) Math.floor((e.getMinX() - buffer - renderingArea.getMinX()) / subtileW);
        int maxTx = (int) Math.floor((e.getMaxX() + buffer - renderingArea.getMinX()) / subtileW);
        int minTy = (int) Math.floor((e.getMinY() - buffer - renderingArea.getMinY()) / subtileH);
        int maxTy = (int) Math.floor((e.getMaxY() + buffer - renderingArea.getMinY()) / subtileH);

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

                builders[idx].addFeature(layerName, fid, geomName, clipped, props);
            }
        }
    }
}
