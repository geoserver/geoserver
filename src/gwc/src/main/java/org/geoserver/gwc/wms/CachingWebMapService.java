/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geowebcache.conveyor.Conveyor.CacheResult.MISS;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RawMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;

/**
 * {@link WebMapService#getMap(GetMapRequest)} Spring's AOP method interceptor to serve cached tiles
 * whenever the request matches a GeoWebCache tile.
 * 
 * @author Gabriel Roldan
 * 
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
    public WebMap invoke(MethodInvocation invocation) throws Throwable {
        if (!gwc.getConfig().isDirectWMSIntegrationEnabled()) {
            return (WebMap) invocation.proceed();
        }

        final Method method = invocation.getMethod();
        checkArgument(method.getDeclaringClass().equals(WebMapService.class));
        checkArgument("getMap".equals(method.getName()));

        final Object[] arguments = invocation.getArguments();

        checkArgument(arguments.length == 1);
        checkArgument(arguments[0] instanceof GetMapRequest);

        final GetMapRequest request = (GetMapRequest) arguments[0];
        boolean tiled = request.isTiled();
        if (!tiled) {
            return (WebMap) invocation.proceed();
        }
        final StringBuilder requestMistmatchTarget = new StringBuilder();
        ConveyorTile cachedTile = gwc.dispatch(request, requestMistmatchTarget);

        if (cachedTile == null) {
            WebMap dynamicResult = (WebMap) invocation.proceed();
            dynamicResult.setResponseHeader("geowebcache-cache-result", MISS.toString());
            dynamicResult.setResponseHeader("geowebcache-miss-reason",
                    requestMistmatchTarget.toString());
            return dynamicResult;
        }

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
        final byte[] hash = MessageDigest.getInstance("MD5").digest(tileBytes);
        final String etag = toHexString(hash);
        if (etag.equals(ifNoneMatch)) {
            // Client already has the current version
            LOGGER.finer("ETag matches, returning 304");
            throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
        }

        LOGGER.finer("No matching ETag, returning cached tile");
        final String mimeType = cachedTile.getMimeType().getMimeType();

        RawMap map = new RawMap(null, tileBytes, mimeType);

        map.setResponseHeader("Cache-Control", "no-cache");
        map.setResponseHeader("ETag", etag);

        CacheResult cacheResult = cachedTile.getCacheResult();
        String cacheResultHeader = cacheResult == null ? "UNKNOWN" : cacheResult.toString();
        map.setResponseHeader("geowebcache-cache-result", cacheResultHeader);
        map.setResponseHeader("geowebcache-tile-index", Arrays.toString(cachedTile.getTileIndex()));
        map.setContentDispositionHeader(null, "." + cachedTile.getMimeType().getFileExtension());
        return map;

    }

    private String toHexString(byte[] hash) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < hash.length; i += 4) {
            int c1 = 0xFF & hash[i];
            int c2 = 0xFF & hash[i + 1];
            int c3 = 0xFF & hash[i + 2];
            int c4 = 0xFF & hash[i + 3];
            int integer = ((c1 << 24) + (c2 << 16) + (c3 << 8) + (c4 << 0));
            sb.append(Integer.toHexString(integer));
        }
        return sb.toString();
    }

}
