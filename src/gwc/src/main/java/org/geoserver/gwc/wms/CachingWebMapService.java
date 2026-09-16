/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RawMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;

/**
 * {@link WebMapService#getMap(GetMapRequest)} Spring's AOP method interceptor to serve cached tiles whenever the
 * request matches a GeoWebCache tile.
 *
 * @author Gabriel Roldan
 */
public class CachingWebMapService implements MethodInterceptor {

    private static final Logger LOGGER = Logging.getLogger(CachingWebMapService.class);

    private GWC gwc;

    public CachingWebMapService(GWC gwc) {
        this.gwc = gwc;
    }

    /**
     * Wraps {@link WebMapService#getMap(GetMapRequest)}, called by the {@link Dispatcher}
     *
     * @see WebMapService#getMap(GetMapRequest)
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    public WebMap invoke(MethodInvocation invocation) throws Throwable {
        GWCConfig config = gwc.getConfig();
        if (!config.isDirectWMSIntegrationEnabled()) {
            return (WebMap) invocation.proceed();
        }

        final GetMapRequest request = getRequest(invocation);
        boolean tiled = request.isTiled() || !config.isRequireTiledParameter();
        final Map<String, String> rawKvp = request.getRawKvp();
        // skipping seeding requests to avoid meta tile recursion
        boolean isSeedingRequest = rawKvp != null && rawKvp.containsKey(GeoServerTileLayer.GWC_SEED_INTERCEPT_TOKEN);
        if (!tiled || isSeedingRequest) {
            return (WebMap) invocation.proceed();
        }

        final StringBuilder requestMistmatchTarget = new StringBuilder();
        ConveyorTile cachedTile = gwc.dispatch(request, requestMistmatchTarget);

        if (cachedTile == null) {
            WebMap dynamicResult = (WebMap) invocation.proceed();
            dynamicResult.setResponseHeader("geowebcache-cache-result", MISS.toString());
            dynamicResult.setResponseHeader("geowebcache-miss-reason", requestMistmatchTarget.toString());
            return dynamicResult;
        }
        checkState(cachedTile.getTileLayer() != null);
        final TileLayer layer = cachedTile.getTileLayer();

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("GetMap request intercepted, serving cached content: " + request);
        }

        final byte[] tileBytes;
        {
            final Resource mapContents = cachedTile.getBlob();
            if (mapContents instanceof ByteArrayResource resource) {
                tileBytes = resource.getContents();
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                mapContents.transferTo(Channels.newChannel(out));
                tileBytes = out.toByteArray();
            }
        }

        // Handle Etags
        final String ifNoneMatch = request.getHttpRequestHeader("If-None-Match");
        final String etag = GWC.getETag(tileBytes);
        if (etag.equals(ifNoneMatch)) {
            // Client already has the current version
            LOGGER.finer("ETag matches, returning 304");
            throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
        }

        LOGGER.finer("No matching ETag, returning cached tile");
        final String mimeType = cachedTile.getMimeType().getMimeType();

        RawMap map = new RawMap(null, tileBytes, mimeType);

        map.setContentDispositionHeader(null, "." + cachedTile.getMimeType().getFileExtension(), false);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        boolean coalesced = GWC.isCoalescedLayers(request);
        if (coalesced) {
            // GWC.setCacheControlHeaders is per-single-layer (driven by the GWC layer's getExpireClients); match
            // exactly what a live multi-layer GetMap produces instead, so a cached and uncached response agree
            WMS.cacheMaxAge(request.isGet(), request.getLayers())
                    .ifPresent(maxAge -> headers.putAll(WMS.cacheControlHeaders(maxAge)));
        } else {
            GWC.setCacheControlHeaders(headers, layer, (int) cachedTile.getTileIndex()[2]);
        }
        GWC.setConditionalGetHeaders(headers, cachedTile, etag, request.getHttpRequestHeader("If-Modified-Since"));
        GWC.setCacheMetadataHeaders(headers, cachedTile, layer);
        // a coalesced multi-layer tile's real per-member cache result (HIT/MISS/PARTIAL n/N) can't be expressed as
        // a single Conveyor.CacheResult, so GWC.dispatchCoalesced smuggles it through the tile's own filtering
        // parameters (never used for anything else on a tile that is never itself persisted or looked up). Removed
        // right after reading: filteringParameters is otherwise a cache-key-derivation map (see
        // GeoServerTileLayer#getModifiableParameters), so leaving the extra key behind would surprise any future
        // code that iterates cachedTile's parameters expecting only real KVP-derived entries
        String coalescedCacheResult = cachedTile.getFilteringParameters().remove(GWC.COALESCED_CACHE_RESULT_KEY);
        if (coalescedCacheResult != null) {
            headers.put("geowebcache-cache-result", coalescedCacheResult);
        }
        headers.forEach((k, v) -> map.setResponseHeader(k, v));

        return map;
    }

    private GetMapRequest getRequest(MethodInvocation invocation) {
        final Method method = invocation.getMethod();
        checkArgument(method.getDeclaringClass().equals(WebMapService.class));
        checkArgument("getMap".equals(method.getName()));

        final Object[] arguments = invocation.getArguments();

        checkArgument(arguments.length == 1);
        checkArgument(arguments[0] instanceof GetMapRequest);

        final GetMapRequest request = (GetMapRequest) arguments[0];
        return request;
    }
}
