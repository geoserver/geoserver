/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/** Wraps a VectorTileBuilder and enforces: shared metatile estimated memory cap (across subtiles) */
public final class MemoryGuardedVectorTileBuilder implements VectorTileBuilder {

    private final VectorTileBuilder delegate;
    private final long metaCapBytes;
    private final LongAdder metaBytes; // shared across all subtiles in same metatile

    public MemoryGuardedVectorTileBuilder(VectorTileBuilder delegate, long metaCapBytes, LongAdder sharedMetaBytes) {

        this.delegate = delegate;
        this.metaCapBytes = metaCapBytes;
        this.metaBytes = sharedMetaBytes;
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties) {

        long inc = estimateBytes(layerName, featureId, geometryName, geometry, properties);

        long metaNext = metaBytes.sum() + inc;
        if (metaNext > metaCapBytes) {
            throw new ServiceException("MVT metatile memory cap exceeded (" + metaNext + " > " + metaCapBytes + ")");
        }

        metaBytes.add(inc);

        delegate.addFeature(layerName, featureId, geometryName, geometry, properties);
    }

    @Override
    public WebMap build(WMSMapContent mapContent) throws IOException {
        return delegate.build(mapContent);
    }

    /** Heuristic estimate. Tune the constants based on profiling of your workload. */
    static long estimateBytes(
            String layerName, String featureId, String geometryName, Geometry geometry, Map<String, Object> props) {

        long size = 64; // base object / bookkeeping

        // Some string-ish overhead
        size += estimateString(layerName);
        size += estimateString(featureId);
        size += estimateString(geometryName);

        if (geometry != null && !geometry.isEmpty()) {
            final int points = geometry.getNumPoints();
            final int dim = geometry.getDimension(); // 0=points,1=lines,2=polygons
            final int rings = (dim == 2) ? fastRingCount(geometry) : 0;

            // These constants aim to approximate:
            // - command integer stream (MoveTo/LineTo/ClosePath + dx/dy params)
            // - varint/protobuf overhead
            // - temporary int buffers used by the encoder
            long bytesPerPoint;
            long bytesPerRing;

            if (dim == 0) { // points
                bytesPerPoint = 16; // MoveTo + dx/dy params (+ overhead)
                bytesPerRing = 0;
            } else if (dim == 1) { // lines
                bytesPerPoint = 20; // MoveTo + LineTo params (+ overhead)
                bytesPerRing = 0;
            } else { // polygons
                bytesPerPoint = 22; // params dominate; includes some buffering
                bytesPerRing = 32; // ClosePath + ring bookkeeping
            }

            // Multi-geom / collection bookkeeping (very small)
            size += geometry.getNumGeometries() * 16L;
            size += points * bytesPerPoint;
            size += rings * bytesPerRing;
        }

        if (props != null && !props.isEmpty()) {
            size += 24; // iterate overhead-ish

            // In MVT tags are pairs of indices; real cost is mostly strings + value table entries.
            for (Map.Entry<String, Object> e : props.entrySet()) {
                size += 8; // tag pair varints / small overhead

                // key: usually de-duplicated in layer keys table; count lightly
                size += estimateKeyString(e.getKey());

                // value: strings dominate; numbers/bools are cheap
                Object v = e.getValue();
                if (v instanceof CharSequence) {
                    CharSequence cs = (CharSequence) v;
                    size += 16 + cs.length(); // compact string assumption (Java11)
                } else if (v instanceof Number || v instanceof Boolean) {
                    size += 8;
                } else if (v == null) {
                    size += 4;
                } else {
                    size += 16; // unknown object bucket;
                }
            }
        }

        return size;
    }

    private static long estimateString(String s) {
        if (s == null) return 8;
        // Java11 compact strings typical: ~1 byte/char; keep it light
        return 16L + s.length();
    }

    private static long estimateKeyString(String s) {
        if (s == null) return 4;
        // keys table entry (light): overhead + bytes
        return 8L + s.length();
    }

    /** Fast ring count for polygons without walking coordinates. */
    private static int fastRingCount(Geometry g) {
        int rings = 0;
        if (g instanceof Polygon) {
            Polygon p = (Polygon) g;
            rings += 1 + p.getNumInteriorRing();
        } else if (g instanceof MultiPolygon) {
            MultiPolygon mp = (MultiPolygon) g;
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Polygon p = (Polygon) mp.getGeometryN(i);
                rings += 1 + p.getNumInteriorRing();
            }
        } else {
            // geometry collections: scan only top-level children
            int n = g.getNumGeometries();
            for (int i = 0; i < n; i++) {
                rings += fastRingCount(g.getGeometryN(i));
            }
        }
        return rings;
    }
}
