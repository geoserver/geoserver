/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.eclipse.imagen.PlanarImage;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.layer.TileLayer;

/**
 * Splits a coalesced (comma-separated {@code LAYERS}) tiled {@code GetMap} request into per-member cached tiles and
 * contiguous live-render runs, and assembles the result. A Spring singleton with no per-request state: the {@link GWC}
 * mediator it delegates security/tile-layer lookups to is passed in by each call instead.
 */
class CoalescedRequestSplitter {

    /** Log message prefix for multi-layer tile coalescing, so these lines are easy to grep out on their own. */
    static final String MULTI_LAYER_LOG_PREFIX = "GWC MultiLayer >> ";

    /**
     * Request formats a coalesced tile can be assembled for. Members are decoded to ARGB and stacked, so png8 costs one
     * extra quantization of the finished canvas (its member tiles were already quantized when they were cached) and its
     * palette alpha is 1-bit, unlike {@code image/png}.
     */
    private static final Set<String> CACHEABLE_FORMATS = Set.of("image/png", "image/png8");

    private final ImageDecoderContainer decoders;

    private final ImageEncoderContainer encoders;

    private final GetMap getMap;

    CoalescedRequestSplitter(ImageDecoderContainer decoders, ImageEncoderContainer encoders, GetMap getMap) {
        this.decoders = decoders;
        this.encoders = encoders;
        this.getMap = getMap;
    }

    /**
     * Splits {@code request} into per-member cached/live segments, assembles them into one tile, and returns it.
     *
     * @return the assembled tile, or {@code null} if the request is not eligible for multi-layer caching, with the
     *     reason appended to {@code requestMismatchTarget}
     */
    ConveyorTile dispatchCoalesced(GWC gwc, GetMapRequest request, StringBuilder requestMismatchTarget) {
        long deadline = computeRenderingDeadline();

        List<Segment> segments = classifyCoalescedMembers(gwc, request, requestMismatchTarget);
        if (segments == null) {
            return null;
        }
        CachedSegment firstCached = segments.stream()
                .filter(CachedSegment.class::isInstance)
                .map(CachedSegment.class::cast)
                .findFirst()
                .orElse(null);
        if (firstCached == null) {
            // nothing in the request is actually cacheable: no per-member tile to key the assembled result on,
            // and nothing gained over just falling all the way back to a plain live render
            requestMismatchTarget.append("no member of the coalesced request is cacheable");
            return null;
        }

        if (exceedsMaxRequestMemory(request)) {
            requestMismatchTarget.append("coalesced tile would exceed max request memory");
            return null;
        }

        byte[] assembled;
        try {
            TileStackAssembler assembler = new TileStackAssembler(decoders, encoders);
            assembled = assembler.assemble(
                    this, gwc, request, segments, firstCached.member().tile().getMimeType(), deadline);
        } catch (Exception e) {
            // Re-checking our own deadline, rather than inspecting the exception, tells if it has genuinely passed
            // In that case, a live re-render would just hit the same wall, so fail hard with no fallback.
            // anything else falls back like any other member fetch/render error
            if (deadline > 0 && System.currentTimeMillis() > deadline) {
                String msg = "This request used more time than allowed and has been forcefully stopped.";
                throw e instanceof ServiceException se ? se : new ServiceException(msg, e);
            }
            if (e instanceof OutsideCoverageException) {
                // routine for a sparse member layer, not an error: classifyCoalescedMembers already skips the
                // members this tile is outside of, so getting here means the coverage changed underneath us
                final String msg = e.getMessage() != null ? e.getMessage() : "tile outside coverage";
                GWC.log.log(Level.FINE, msg);
                requestMismatchTarget.append(msg);
                return null;
            }
            GWC.log.log(Level.INFO, "Error assembling coalesced tile", e);
            requestMismatchTarget.append("error assembling coalesced tile: ").append(e.getMessage());
            return null;
        } finally {
            // renderLiveSegment hands its coverages to RasterCleaner, which normally drains as a dispatcher
            // callback; with no request on this thread nothing would ever free them (same reason as
            // GeoServerTileLayer#withRasterCleaner). Only once assemble() has encoded: a segment image may share a
            // coverage's raster, so disposing any earlier would pull it out from under the canvas
            if (Dispatcher.REQUEST.get() == null) {
                RasterCleaner.cleanup();
            }
        }

        ConveyorTile firstMemberTile = firstCached.member().tile();
        // Cache-Control/Expires are computed separately by CachingWebMapService via WMS#cacheMaxAge over every
        // member's MapLayerInfo, matching the live multi-layer path exactly, deliberately not
        // GWC#setCacheControlHeaders, which is per-single-layer and driven by the GWC layer's getExpireClients.
        // geowebcache-cache-result is signaled here (see GWC#COALESCED_CACHE_RESULT_KEY); the secondary
        // geowebcache-layer/-gridset/-tile-bounds headers still describe only the first member. Mutable, not
        // Collections.singletonMap: TileObject.createQueryTileObject stores this reference as-is (no defensive
        // copy), and CachingWebMapService removes the key again once it has read it
        Map<String, String> signaling = new HashMap<>();
        signaling.put(GWC.COALESCED_CACHE_RESULT_KEY, coalescedCacheResultHeader(segments));
        // never persisted itself: only the per-member tiles it stacks are cached
        ConveyorTile assembledTile = new ConveyorTile(
                null,
                request.getRawKvp().get("LAYERS"),
                firstMemberTile.getGridSetId(),
                firstMemberTile.getTileIndex(),
                firstMemberTile.getMimeType(),
                signaling,
                null,
                null);
        assembledTile.setBlob(new ByteArrayResource(assembled));
        assembledTile.setTileLayer(firstCached.member().tileLayer());
        // GWC.setConditionalGetHeaders reads this to answer If-Modified-Since; TileObject.createQueryTileObject
        // (used by the ConveyorTile constructor above) leaves it at 0, which would report the tile as unchanged
        // since 1970. A live-rendered segment makes the assembled image only as fresh as this render; an
        // all-cached assembly changes whenever its newest constituent member does
        assembledTile.getStorageObject().setCreated(assembledTileCreated(segments));
        return assembledTile;
    }

    /**
     * @return the wall-clock time now, if any segment was live-rendered; otherwise the most recent {@code TSCreated}
     *     among the cached segments, since the assembled image changes whenever its newest constituent member does
     */
    private static long assembledTileCreated(List<Segment> segments) {
        if (segments.stream().anyMatch(LiveSegment.class::isInstance)) {
            return System.currentTimeMillis();
        }
        return segments.stream()
                .filter(CachedSegment.class::isInstance)
                .map(CachedSegment.class::cast)
                .mapToLong(s -> s.member().tile().getTSCreated())
                .max()
                .orElse(0L);
    }

    /**
     * Wall-clock deadline for {@link TileStackAssembler#assemble}, mirroring the WMS {@code maxRenderingTime} contract.
     *
     * @return {@code System.currentTimeMillis()} plus the configured {@code maxRenderingTime}, or {@code -1} if
     *     unlimited
     */
    long computeRenderingDeadline() {
        int maxRenderingTime = WMS.get().getServiceInfo().getMaxRenderingTime();
        return maxRenderingTime > 0 ? System.currentTimeMillis() + maxRenderingTime * 1000L : -1;
    }

    /**
     * Whether assembling a coalesced tile at {@code request}'s size would exceed WMS's configured
     * {@code maxRequestMemory}; always {@code false} when unlimited.
     *
     * <p>Covers the assembly itself only. A {@link LiveSegment} renders through the normal WMS pipeline
     * ({@link #renderLiveSegment}, via {@code GetMap.run}), which enforces {@code maxRequestMemory} against the full
     * budget on its own (see {@code RenderedImageMapOutputFormat.produceMap}) with no way to tell it that the canvas is
     * already held - {@code maxRequestMemory} has no per-request override the way {@code maxRenderingTime} does. So a
     * request mixing cached and live segments can peak at {@code maxRequestMemory} plus one canvas ({@code width *
     * height * 4} bytes), and the overshoot is not reported here: it surfaces, if at all, as the live segment's own
     * render failing.
     */
    boolean exceedsMaxRequestMemory(GetMapRequest request) {
        int maxRequestMemoryKB = WMS.get().getServiceInfo().getMaxRequestMemory();
        if (maxRequestMemoryKB <= 0) {
            return false;
        }
        // the output canvas plus one cached segment's decode buffer, whatever the member count: the caller only gets
        // here with at least one cached segment, and TileStackAssembler.assemble decodes and draws them one at a
        // time, flushing each before decoding the next, so one decoded raster at most is ever live beside the canvas
        long peakBytes = 2L * request.getWidth() * request.getHeight() * 4;
        return peakBytes > (long) maxRequestMemoryKB * 1024;
    }

    /**
     * {@code HIT} if every original {@code LAYERS} member is a cache hit, {@code MISS} if none is, else {@code PARTIAL
     * n/N}; every member of a {@link LiveSegment} run counts as a miss, since none of them was served from its own
     * cache.
     */
    private static String coalescedCacheResultHeader(List<Segment> segments) {
        int hits = 0;
        int total = 0;
        for (Segment segment : segments) {
            if (segment instanceof CachedSegment cached) {
                total++;
                if (cached.member().tile().getCacheResult() == Conveyor.CacheResult.HIT) {
                    hits++;
                }
            } else {
                total += ((LiveSegment) segment).memberIndices().size();
            }
        }
        if (hits == total) {
            return Conveyor.CacheResult.HIT.toString();
        }
        if (hits == 0) {
            return Conveyor.CacheResult.MISS.toString();
        }
        return "PARTIAL " + hits + "/" + total;
    }

    /** A {@code LAYERS} member of a coalesced request, split into its own single-layer prepared tile request. */
    record TileLayerMember(TileLayer tileLayer, ConveyorTile tile) {}

    /**
     * One drawing-order segment of a coalesced request: either a single cacheable member ({@link CachedSegment}), or a
     * maximal run of consecutive members that must be rendered live ({@link LiveSegment}) because none of them can be
     * served from their own cache for this request (see {@link #classifyCoalescedMembers}).
     */
    interface Segment {}

    record CachedSegment(TileLayerMember member) implements Segment {}

    /**
     * @param memberIndices positions (into the original {@code LAYERS} list) of this run's members, in order
     * @param reason why the run's first member could not be served from cache; representative of the whole run
     */
    record LiveSegment(List<Integer> memberIndices, String reason) implements Segment {}

    /**
     * Renders a {@link LiveSegment}'s members as a single in-process {@code GetMap}, bypassing GWC's own
     * {@code WebMapService} interceptor so a live segment cannot recurse back into tile caching.
     *
     * @param deadline the coalesced request's own deadline (see {@link #computeRenderingDeadline}), shared with this
     *     live render via the {@code timeout} format option so a slow segment can't each get its own full
     *     {@code maxRenderingTime} budget
     * @throws ServiceException if {@code deadline} has already passed before this segment's render could even start
     */
    BufferedImage renderLiveSegment(GetMapRequest request, LiveSegment segment, long deadline) throws Exception {
        if (GWC.log.isLoggable(Level.FINER)) {
            GWC.log.finer(MULTI_LAYER_LOG_PREFIX + "Live-rendering coalesced segment members " + segment.memberIndices()
                    + ": " + segment.reason());
        }
        long remainingMillis = 0;
        if (deadline > 0) {
            remainingMillis = deadline - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                throw new ServiceException("This request used more time than allowed and has been forcefully stopped.");
            }
        }
        GetMapRequest subRequest = sliceLiveSegment(request, segment.memberIndices(), remainingMillis);
        WebMap webMap = getMap.run(subRequest);
        try {
            if (!(webMap instanceof RenderedImageMap renderedImageMap)) {
                throw new IllegalStateException(
                        "Live render of a coalesced segment did not produce a raster: " + webMap);
            }
            RenderedImage image = renderedImageMap.getImage();
            if (image instanceof BufferedImage bufferedImage) {
                return bufferedImage;
            }
            return PlanarImage.wrapRenderedImage(image).getAsBufferedImage();
        } finally {
            // webMap.dispose() does not release renderedCoverages (see RenderedImageMap.disposeInternal), so hand
            // them to RasterCleaner ourselves, same as RenderedImageMapResponse.write does for a normal GetMap
            if (webMap instanceof RenderedImageMap renderedImageMap) {
                renderedImageMap.getRenderedCoverages().forEach(RasterCleaner::addCoverage);
            }
            webMap.dispose();
        }
    }

    /**
     * @param remainingMillis this segment's share of the coalesced request's own deadline, or {@code <= 0} for no
     *     deadline; passed through as the {@code timeout} format option, the only way to shrink a single in-process
     *     {@code GetMap}'s own {@code maxRenderingTime} below the server-wide setting
     */
    private static GetMapRequest sliceLiveSegment(
            GetMapRequest request, List<Integer> memberIndices, long remainingMillis) {
        GetMapRequest subRequest = (GetMapRequest) request.clone();
        subRequest.setLayers(pickByIndex(request.getLayers(), memberIndices));
        subRequest.setStyles(pickByIndex(request.getStyles(), memberIndices));
        List<Filter> cqlFilters = request.getCQLFilter();
        if (cqlFilters != null) {
            subRequest.setCQLFilter(pickByIndex(cqlFilters, memberIndices));
        }
        List<Filter> filters = request.getFilter();
        if (filters != null) {
            subRequest.setFilter(pickByIndex(filters, memberIndices));
        }
        List<List<SortBy>> sortBys = request.getSortBy();
        if (sortBys != null) {
            subRequest.setSortBy(pickByIndex(sortBys, memberIndices));
        }
        List<Map<String, String>> viewParams = request.getViewParams();
        if (viewParams != null) {
            subRequest.setViewParams(pickByIndex(viewParams, memberIndices));
        }
        if (remainingMillis > 0) {
            // GetMapRequest.clone() shares the formatOptions map with the original request; copy it before writing
            Map<String, Object> formatOptions = new CaseInsensitiveMap<>(new HashMap<>(request.getFormatOptions()));
            formatOptions.put("timeout", remainingMillis);
            subRequest.setFormatOptions(formatOptions);
        }
        return subRequest;
    }

    private static <T> List<T> pickByIndex(List<T> list, List<Integer> indices) {
        List<T> picked = new ArrayList<>(indices.size());
        for (int index : indices) {
            picked.add(list.get(index));
        }
        return picked;
    }

    /**
     * Classifies each {@code LAYERS} member as cacheable or as needing a live render, grouping consecutive
     * non-cacheable members into one {@link LiveSegment} per member run.
     *
     * @return the ordered segments, or {@code null} if the whole request is ineligible, with the reason appended to
     *     {@code requestMismatchTarget}
     */
    List<Segment> classifyCoalescedMembers(GWC gwc, GetMapRequest request, StringBuilder requestMismatchTarget) {
        if (!gwc.getConfig().isMultiLayerCachingEnabled()) {
            requestMismatchTarget.append("multi-layer tile caching disabled");
            return null;
        }
        String format = request.getFormat();
        // null-safe on purpose: FORMAT is only made mandatory later, in GetMap#assertMandatory, and Set#contains
        // would throw on a null element
        if (format == null || !CACHEABLE_FORMATS.contains(format) || !request.isTransparent()) {
            requestMismatchTarget.append("multi-layer tile caching requires transparent image/png or image/png8");
            return null;
        }
        if (request.getRawKvp().get("FEATUREID") != null) {
            // FEATUREID fids are matched to a layer by their "typename.fid" prefix, not by LAYERS position (see
            // GetMapKvpRequestReader.parseFilters/getFilter), so it can't be positionally sliced per member like the
            // other per-layer KVPs; request-wide gate is the safe first cut
            requestMismatchTarget.append("multi-layer tile caching does not support FEATUREID");
            return null;
        }

        final Map<String, String> rawKvp = request.getRawKvp();
        // KNOWN LIMITATION: LAYERS=groupA (a single layer group, no comma) never reaches this method at all -
        // GWC.dispatch checks the raw LAYERS KVP for a comma before any group expansion happens, so that request
        // takes the ordinary single-layer path and hits groupA's own tile cache exactly as always.
        // LAYERS=groupA,layerB DOES reach this method, but request.getLayers() has ALREADY been flattened by
        // GetMapKvpRequestReader by then: groupA is expanded into its own members (say g1, g2), so this method
        // sees three independent layers - g1, g2, layerB - with no marker that g1/g2 came from a group. The
        // per-member slicing below is positional over the raw KVP, so it cannot survive that: the guard right
        // after this bails out to a live render when the expansion changed the member count AND the request
        // carries a per-layer KVP to slice. Without any such KVP the expanded members are cached individually as
        // usual, and so is a group holding exactly one layer (it expands 1:1, keeping the mapping intact). Either
        // way the group's own tile cache is not consulted, only its members'.
        final List<MapLayerInfo> layers = request.getLayers();
        if (KvpUtils.readFlat(rawKvp.get("LAYERS")).size() != layers.size() && hasPerLayerKvp(rawKvp)) {
            // a LAYERS slot was expanded (a layer group), so raw token index i no longer lines up with member i and
            // the per-layer KVPs sliced below would be assigned to the wrong member, drawing and caching each of
            // them with someone else's value. Not recoverable from raw text alone (see the group limitation
            // above), so drop to the live render. Only when such a KVP is actually present: with none of them
            // there is nothing to misalign, and the expanded members can be cached individually as usual
            requestMismatchTarget.append("a LAYERS entry was expanded, per-member parameters cannot be sliced");
            return null;
        }
        final List<Style> styles = request.getStyles();
        if (styles == null || styles.size() != layers.size()) {
            // GetMapKvpRequestReader pads STYLES out to one entry per layer; anything else means the positional
            // slicing below would hand a member someone else's style instead of failing
            requestMismatchTarget.append("STYLES does not line up with LAYERS");
            return null;
        }
        final List<Filter> cqlFilters = request.getCQLFilter();
        final List<Filter> filters = request.getFilter();
        final List<List<SortBy>> sortBys = request.getSortBy();
        final List<Map<String, String>> viewParams = request.getViewParams();
        final List<String> viewParamTokens;
        {
            String rawViewParams = rawKvp.get("VIEWPARAMS");
            if (rawViewParams == null) {
                viewParamTokens = null;
            } else {
                List<String> tokens = KvpUtils.escapedTokens(rawViewParams, ',');
                if (tokens.size() == layers.size()) {
                    viewParamTokens = tokens;
                } else if (tokens.size() == 1 && layers.size() > 1) {
                    // GetMapKvpRequestReader.applyViewParams replicates a single group over every layer when the
                    // client supplies just one; mirror that here so the raw-KVP mirror used for cache-key
                    // derivation matches what was actually rendered, instead of leaving members 1..N keyed as if
                    // VIEWPARAMS were absent while they were in fact rendered with the replicated value
                    viewParamTokens = Collections.nCopies(layers.size(), tokens.get(0));
                } else {
                    // any other count (e.g. one group per originally-requested layer-group slot, expanded
                    // differently than LAYERS) can't be reliably realigned to per-member positions from raw text
                    // alone - same class of problem as the LAYERS/layer-group limitation above, so bail out
                    // rather than risk a silently wrong cache key
                    requestMismatchTarget.append("VIEWPARAMS text cannot be sliced per member");
                    return null;
                }
            }
        }
        final List<String> cqlFilterTokens;
        {
            String rawCql = rawKvp.get("CQL_FILTER");
            if (rawCql == null) {
                cqlFilterTokens = null;
            } else {
                List<String> tokens = splitCqlFilters(rawCql);
                // the ';' scanner above is an approximation of the CQL grammar (it does not implement a full CQL
                // parser); if its token count disagrees with the already-parsed filter count, the tokens cannot
                // be trusted to line up with the members, so bail out rather than risk a silently wrong cache key
                if (tokens.size() != layers.size()) {
                    requestMismatchTarget.append("CQL_FILTER text cannot be sliced per member");
                    return null;
                }
                cqlFilterTokens = tokens;
            }
        }
        final double scaleDenominator = computeScaleDenominator(request);
        // the raw multi-layer request is authorized nowhere else, so each member needs its own coarse gate
        final ReferencedEnvelope securityBbox;
        try {
            securityBbox =
                    gwc.getConfig().isSecurityEnabled() ? gwc.parseRequestBbox(request, rawKvp.get("LAYERS")) : null;
        } catch (RuntimeException e) {
            requestMismatchTarget.append("invalid request for access check: ").append(e.getMessage());
            return null;
        }

        final List<Segment> segments = new ArrayList<>();
        List<Integer> liveRunIndices = null;
        String liveRunReason = null;
        String gridSetId = null;
        long[] tileIndex = null;

        for (int i = 0; i < layers.size(); i++) {
            String layerName = layers.get(i).getName();

            // cross-stack correctness gates: apply regardless of this member's own cacheability, and abort the
            // whole request, since a live-rendered member can't be composited to fix a label/compositing conflict
            // with a DIFFERENT, already-cached member
            if (gwc.getConfig().isSecurityEnabled()) {
                try {
                    gwc.verifyAccessLayer(layerName, securityBbox);
                } catch (org.geotools.ows.ServiceException | SecurityException e) {
                    requestMismatchTarget.append('\'').append(layerName).append("' access denied");
                    return null;
                }
            }
            if (isIneligibleAtScale(styles.get(i), scaleDenominator)) {
                requestMismatchTarget
                        .append('\'')
                        .append(layerName)
                        .append("' draws labels or composites with layers beneath it at this scale");
                return null;
            }

            String liveReason = null;
            TileLayer tileLayer = null;
            if (!gwc.tld.layerExists(layerName)) {
                liveReason = "'" + layerName + "' is not a tile layer";
            } else {
                try {
                    tileLayer = gwc.tld.getTileLayer(layerName);
                } catch (GeoWebCacheException e) {
                    throw new RuntimeException(e);
                }
                if (!tileLayer.isEnabled()) {
                    liveReason = "'" + layerName + "' tile layer disabled";
                    tileLayer = null;
                }
            }

            ConveyorTile memberTile = null;
            if (tileLayer != null) {
                GetMapRequest memberRequest = (GetMapRequest) request.clone();
                memberRequest.setLayers(Collections.singletonList(layers.get(i)));
                memberRequest.setStyles(Collections.singletonList(styles.get(i)));
                Map<String, String> memberKvp = new CaseInsensitiveMap<>(new HashMap<>(rawKvp));
                memberKvp.put("LAYERS", layerName);
                // forward the client's own text unchanged: getModifiableParameters derives the cache key from
                // memberKvp and GWC.filterApplies matches parameter filters against it
                putRawMemberToken(memberKvp, "STYLES", rawKvp.get("STYLES"), i, KvpUtils.INNER_DELIMETER);
                putRawMemberToken(
                        memberKvp, "INTERPOLATIONS", rawKvp.get("INTERPOLATIONS"), i, KvpUtils.INNER_DELIMETER);
                if (cqlFilterTokens != null) {
                    memberKvp.put("CQL_FILTER", cqlFilterTokens.get(i));
                }
                putRawMemberToken(memberKvp, "FILTER", rawKvp.get("FILTER"), i, KvpUtils.OUTER_DELIMETER);
                putRawMemberToken(memberKvp, "SORTBY", rawKvp.get("SORTBY"), i, KvpUtils.OUTER_DELIMETER);
                if (viewParamTokens != null) {
                    memberKvp.put("VIEWPARAMS", viewParamTokens.get(i));
                }
                memberRequest.setRawKvp(memberKvp);
                if (cqlFilters != null && i < cqlFilters.size()) {
                    memberRequest.setCQLFilter(Collections.singletonList(cqlFilters.get(i)));
                }
                if (filters != null && i < filters.size()) {
                    memberRequest.setFilter(Collections.singletonList(filters.get(i)));
                }
                if (sortBys != null && i < sortBys.size()) {
                    memberRequest.setSortBy(Collections.singletonList(sortBys.get(i)));
                }
                if (viewParams != null && i < viewParams.size()) {
                    memberRequest.setViewParams(Collections.singletonList(viewParams.get(i)));
                }

                // isolated: a failure here must not leak into requestMismatchTarget unless this run ends up being
                // the one reported, since this member may simply join a live segment rather than abort anything
                StringBuilder memberMismatch = new StringBuilder();
                memberTile = gwc.prepareRequest(tileLayer, memberRequest, memberMismatch);
                if (memberTile == null) {
                    liveReason = "'" + layerName + "': " + memberMismatch;
                } else if (!tileLayer.getGridSubset(memberTile.getGridSetId()).covers(memberTile.getTileIndex())) {
                    // sparse member: this tile is outside the layer's cached coverage, so asking its cache for it
                    // would throw OutsideCoverageException and drop the whole stack to a live combined render. Only
                    // this member needs the live render, and it does need one: a coverage can be narrower than the
                    // data behind it (explicit cache bounds, or a zoom outside the subset's range), so an empty
                    // tile is not a safe substitute
                    liveReason = "'" + layerName + "' tile is outside its cached coverage";
                    memberTile = null;
                } else if (gridSetId == null) {
                    gridSetId = memberTile.getGridSetId();
                    tileIndex = memberTile.getTileIndex();
                } else if (!gridSetId.equals(memberTile.getGridSetId())
                        || !Arrays.equals(tileIndex, memberTile.getTileIndex())) {
                    // members share one footprint, gridloc and zoom: TileStackAssembler stacks raw pixels with no
                    // knowledge of gridset identity, so a mismatch here would silently misalign the assembled tile
                    liveReason = "'" + layerName + "' resolved to a different gridset/tile than the other members";
                    memberTile = null;
                }
            }

            if (memberTile != null) {
                if (liveRunIndices != null) {
                    segments.add(new LiveSegment(liveRunIndices, liveRunReason));
                    liveRunIndices = null;
                }
                segments.add(new CachedSegment(new TileLayerMember(tileLayer, memberTile)));
            } else {
                if (liveRunIndices == null) {
                    liveRunIndices = new ArrayList<>();
                    liveRunReason = liveReason;
                }
                liveRunIndices.add(i);
            }
        }
        if (liveRunIndices != null) {
            segments.add(new LiveSegment(liveRunIndices, liveRunReason));
        }
        return segments;
    }

    /**
     * Computes the request's scale denominator the way a live render would; mirrors
     * {@link org.geoserver.wms.FeatureInfoRequestParameters}'s equivalent computation.
     */
    private static double computeScaleDenominator(GetMapRequest request) {
        CoordinateReferenceSystem crs = request.getCrs() != null ? request.getCrs() : DefaultGeographicCRS.WGS84;
        WMSMapContent mapContent = new WMSMapContent(request);
        try {
            mapContent.getViewport().setBounds(new ReferencedEnvelope(request.getBbox(), crs));
            mapContent.setMapWidth(request.getWidth());
            mapContent.setMapHeight(request.getHeight());
            mapContent.setAngle(request.getAngle());
            return mapContent.getScaleDenominator(true);
        } finally {
            mapContent.dispose();
        }
    }

    /** The {@code LAYERS}-aligned KVPs that {@link #classifyCoalescedMembers} slices positionally per member. */
    private static final List<String> PER_LAYER_KVPS =
            List.of("STYLES", "FILTER", "SORTBY", "INTERPOLATIONS", "CQL_FILTER", "VIEWPARAMS");

    /** Whether {@code rawKvp} carries any {@link #PER_LAYER_KVPS} entry with a value to slice. */
    private static boolean hasPerLayerKvp(Map<String, String> rawKvp) {
        return PER_LAYER_KVPS.stream().map(rawKvp::get).anyMatch(value -> value != null && !value.isEmpty());
    }

    /**
     * Writes {@code memberKvp[key]} to the {@code index}-th token of {@code raw}, tokenized with {@code tokenizer};
     * writes nothing when {@code raw} itself is {@code null}, so a per-member value absent from the client's request
     * stays absent from the member's own KVP view.
     */
    private static void putRawMemberToken(
            Map<String, String> memberKvp, String key, String raw, int index, KvpUtils.Tokenizer tokenizer) {
        if (raw == null) {
            return;
        }
        putMemberToken(memberKvp, key, index, KvpUtils.readFlat(raw, tokenizer));
    }

    /**
     * Writes {@code memberKvp[key]} to {@code tokens[index]}, or to {@code ""} if {@code index} has no token. Written
     * even when the token is empty (e.g. {@code STYLES=,labeled} leaves member 0 with the default style):
     * {@code memberKvp} starts as a copy of the whole combined {@code rawKvp}, so skipping an empty token would leave
     * the OTHER members' combined text on this one instead, keying this member's cache entry partly on their settings.
     */
    private static void putMemberToken(Map<String, String> memberKvp, String key, int index, List<String> tokens) {
        memberKvp.put(key, index < tokens.size() ? tokens.get(index) : "");
    }

    /**
     * Splits a {@code CQL_FILTER} value on {@code ;} outside string literals and double-quoted property names, so a
     * {@code ;} embedded in a filter's own text (e.g. {@code NAME='a;b'}) does not get mistaken for the separator
     * between two members' clauses. An approximation of the CQL grammar, not a full parser: callers must still
     * cross-check the resulting token count against the already-parsed filter count before trusting it.
     */
    private static List<String> splitCqlFilters(String raw) {
        List<String> tokens = new ArrayList<>();
        int start = 0;
        char quote = 0; // 0 outside a quoted region, otherwise the quote character that opened it
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0; // a doubled '' closes and immediately reopens, which is the right outcome
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == ';') {
                tokens.add(raw.substring(start, i));
                start = i + 1;
            }
        }
        tokens.add(raw.substring(start));
        return tokens;
    }

    /** Mirrors the private rule scale-range check in {@code org.geotools.renderer.lite.StreamingRenderer}. */
    private static final double SCALE_TOLERANCE = 1e-6;

    private static boolean isWithinScale(Rule rule, double scaleDenominator) {
        return rule.getMinScaleDenominator() - SCALE_TOLERANCE <= scaleDenominator
                && rule.getMaxScaleDenominator() + SCALE_TOLERANCE > scaleDenominator;
    }

    /**
     * Whether {@code style} draws labels, or blends with the layers beneath it, in any {@code FeatureTypeStyle} active
     * at {@code scaleDenominator}: a per-layer cached tile bakes either in without the rest of the stack, so it can't
     * reproduce what a live render of the whole stack would draw.
     */
    private static boolean isIneligibleAtScale(Style style, double scaleDenominator) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            boolean active = false;
            boolean labels = false;
            for (Rule rule : fts.rules()) {
                if (!isWithinScale(rule, scaleDenominator)) {
                    continue;
                }
                active = true;
                for (Symbolizer symbolizer : rule.symbolizers()) {
                    if (symbolizer instanceof TextSymbolizer) {
                        labels = true;
                    }
                }
            }
            if (!active) {
                continue;
            }
            if (fts.getOptions().containsKey(FeatureTypeStyle.COMPOSITE)
                    || fts.getOptions().containsKey(FeatureTypeStyle.COMPOSITE_BASE)) {
                return true; // ineligible: composites with the layers beneath it at this scale
            }
            if (labels) {
                return true; // ineligible: draws labels at this scale
            }
        }
        return false;
    }

    Executor getMetaTilingExecutor(GWC gwc) {
        return gwc.getMetaTilingExecutor();
    }
}
