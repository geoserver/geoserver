/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.mime.MimeType;

/**
 * Assembles one coalesced tile by fetching each {@code LAYERS} member's own tile and stacking them alpha-over in
 * request order. Single-use, single-threaded: build one instance per tile request.
 */
class TileStackAssembler {

    private static final Logger LOGGER = Logging.getLogger(TileStackAssembler.class);

    private final ImageDecoderContainer decoders;

    private final ImageEncoderContainer encoders;

    TileStackAssembler(ImageDecoderContainer decoders, ImageEncoderContainer encoders) {
        this.decoders = decoders;
        this.encoders = encoders;
    }

    /**
     * Fetches every cached segment's tile, rendering on a cache miss exactly like a single-layer request, and
     * live-renders every non-cacheable run as a single sub-request; draws each segment's image onto a shared canvas in
     * {@code LAYERS} order, then encodes the result.
     *
     * @param deadline wall-clock time (as per {@link System#currentTimeMillis()}) by which encoding must start, or
     *     {@code <= 0} for no deadline; matches the WMS {@code maxRenderingTime} contract, which no single member's own
     *     render can enforce on its own since it only sees its own elapsed time, not this whole operation's
     * @return the assembled tile, encoded as {@code outputFormat}
     * @throws ServiceException if {@code deadline} has already passed on entry, or by the time encoding starts
     */
    byte[] assemble(
            CoalescedRequestSplitter splitter,
            GWC gwc,
            GetMapRequest request,
            List<CoalescedRequestSplitter.Segment> segments,
            MimeType outputFormat,
            long deadline)
            throws Exception {
        // before the peek, not just after the draw loop: classifyCoalescedMembers can burn the whole budget on a
        // wide stack, and an all-cached assembly has no other entry check (renderLiveSegment guards its own)
        if (deadline > 0 && System.currentTimeMillis() > deadline) {
            throw new ServiceException("This request used more time than allowed and has been forcefully stopped.");
        }
        peekCachedSegments(splitter, gwc, segments);

        BufferedImage canvas = null;
        Graphics2D graphics = null;
        try {
            for (CoalescedRequestSplitter.Segment segment : segments) {
                BufferedImage segmentImage = segment instanceof CoalescedRequestSplitter.CachedSegment cached
                        ? decodeCachedSegment(cached)
                        : splitter.renderLiveSegment(request, (CoalescedRequestSplitter.LiveSegment) segment, deadline);

                if (canvas == null) {
                    canvas = new BufferedImage(
                            segmentImage.getWidth(), segmentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    graphics = canvas.createGraphics();
                } else if (segmentImage.getWidth() != canvas.getWidth()
                        || segmentImage.getHeight() != canvas.getHeight()) {
                    // members share one footprint, gridloc and zoom (enforced in
                    // CoalescedRequestSplitter.classifyCoalescedMembers); a
                    // live segment's own render is the one segment kind that isn't grid-checked, so a mismatch here
                    // would otherwise silently misalign the stack instead of failing loudly
                    throw new IllegalStateException(
                            "Coalesced segment image size " + segmentImage.getWidth() + "x" + segmentImage.getHeight()
                                    + " does not match the tile size " + canvas.getWidth() + "x" + canvas.getHeight());
                }
                graphics.drawImage(segmentImage, 0, 0, null);
                // drops cached/accelerated surface copies now rather than waiting for GC; does not free the raster
                // itself, so it's no substitute for letting segmentImage go out of scope after each segment
                segmentImage.flush();
            }
        } finally {
            if (graphics != null) {
                graphics.dispose();
            }
        }

        if (deadline > 0 && System.currentTimeMillis() > deadline) {
            throw new ServiceException("This request used more time than allowed and has been forcefully stopped.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoders.encode(canvas, outputFormat, out, false, null);
        return out.toByteArray();
    }

    private BufferedImage decodeCachedSegment(CoalescedRequestSplitter.CachedSegment cached) throws Exception {
        ConveyorTile tile = cached.member().tile();
        if (tile.getBlob() == null) {
            // a peek miss, or a member whose layer doesn't support the cache-only peek: render like a single-layer
            // request, on this thread, exactly as before the peek phase existed
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(CoalescedRequestSplitter.MULTI_LAYER_LOG_PREFIX + "Rendering cached segment " + tile
                        + ", peek missed or was skipped");
            }
            cached.member().tileLayer().getTile(tile);
        } else if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(CoalescedRequestSplitter.MULTI_LAYER_LOG_PREFIX + "Cached segment " + tile
                    + " already served by the peek");
        }
        String mimeType = tile.getMimeType().getMimeType();
        return decoders.decode(mimeType, tile.getBlob(), decoders.isAggressiveInputStreamSupported(mimeType), null);
    }

    /** Cache-only peek for every cached segment's tile, in parallel; a hit leaves the tile's blob populated. */
    private void peekCachedSegments(
            CoalescedRequestSplitter splitter, GWC gwc, List<CoalescedRequestSplitter.Segment> segments) {
        // live segments have nothing to peek; a member on a plain TileLayer (no GeoServerTileLayer) can't either,
        // since peekCache isn't part of that interface
        List<CoalescedRequestSplitter.CachedSegment> peekable = segments.stream()
                .filter(CoalescedRequestSplitter.CachedSegment.class::isInstance)
                .map(CoalescedRequestSplitter.CachedSegment.class::cast)
                .filter(cached -> cached.member().tileLayer() instanceof GeoServerTileLayer)
                .toList();
        if (peekable.size() < 2) {
            // 0 or 1 segment: nothing to parallelize, a sequential getTile() in the draw loop is just as fast
            return;
        }
        // outside an interactive request (seeding), there's no CoalescedRequestSplitter.getMetaTilingExecutor()
        // round trip to pay for
        Executor executor = splitter.getMetaTilingExecutor(gwc);
        if (executor == null || Dispatcher.REQUEST.get() == null) {
            return;
        }
        // each peek runs concurrently via GeoServerTileLayer#peekCache (lock-free reads, so no contention with
        // the serial render loop in decodeCachedSegment) and populates its own tile's blob on a hit; allOf().join()
        // blocks this (request) thread until every peek is done, so no render below can start while one is in flight
        List<CompletableFuture<Void>> peeks = peekable.stream()
                .map(cached -> CompletableFuture.runAsync(() -> peekOne(cached), executor))
                .toList();
        CompletableFuture.allOf(peeks.toArray(new CompletableFuture[0])).join();
    }

    /**
     * A peek is a best-effort optimization: any failure (e.g. a storage backend that isn't safe against a concurrent
     * read racing a save) must fall back to treating the member as a miss, exactly like it would without the peek
     * phase, never abort the whole coalesced tile over one member's read hiccup.
     */
    private void peekOne(CoalescedRequestSplitter.CachedSegment cached) {
        ConveyorTile tile = cached.member().tile();
        try {
            GeoServerTileLayer tileLayer = (GeoServerTileLayer) cached.member().tileLayer();
            boolean hit = tileLayer.peekCache(tile);
            if (hit) {
                // peekCache is a raw cache read and, unlike getTile, never fires this itself; a peek hit still
                // needs to count towards disk quota's LFU tile usage tracking
                tileLayer.sendTileRequestedEvent(tile);
            }
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(CoalescedRequestSplitter.MULTI_LAYER_LOG_PREFIX + "Cache peek " + (hit ? "hit" : "miss")
                        + " for " + tile);
            }
        } catch (RuntimeException e) {
            LOGGER.log(
                    Level.FINE,
                    e,
                    () -> CoalescedRequestSplitter.MULTI_LAYER_LOG_PREFIX + "Cache peek failed for " + tile
                            + ", will render it");
        }
    }
}
