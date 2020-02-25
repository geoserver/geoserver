/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RawMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;

/**
 * {@link WebMapService#getMap(GetMapRequest)} Spring's AOP method interceptor to serve cached tiles
 * whenever the request matches a GeoWebCache tile.
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
     * @see
     *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public WebMap invoke(MethodInvocation invocation) throws Throwable {
        GWCConfig config = gwc.getConfig();
        if (!config.isDirectWMSIntegrationEnabled()) {
            return (WebMap) invocation.proceed();
        }

        final GetMapRequest request = getRequest(invocation);
        boolean tiled = request.isTiled();
        if (!tiled) {
            return (WebMap) invocation.proceed();
        }

        final StringBuilder requestMistmatchTarget = new StringBuilder();
        ConveyorTile cachedTile = gwc.dispatch(request, requestMistmatchTarget);

        if (cachedTile == null) {
            WebMap dynamicResult = (WebMap) invocation.proceed();
            dynamicResult.setResponseHeader("geowebcache-cache-result", MISS.toString());
            dynamicResult.setResponseHeader(
                    "geowebcache-miss-reason", requestMistmatchTarget.toString());
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
            if (mapContents instanceof ByteArrayResource) {
                tileBytes = ((ByteArrayResource) mapContents).getContents();
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

        map.setContentDispositionHeader(
                null, "." + cachedTile.getMimeType().getFileExtension(), false);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        GWC.setCacheControlHeaders(headers, layer);
        GWC.setConditionalGetHeaders(
                headers, cachedTile, etag, request.getHttpRequestHeader("If-Modified-Since"));
        GWC.setCacheMetadataHeaders(headers, cachedTile, layer);
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
