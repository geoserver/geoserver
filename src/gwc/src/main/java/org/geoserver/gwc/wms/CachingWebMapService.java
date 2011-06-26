/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
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
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.springframework.util.Assert;

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
        Assert.isTrue(method.getDeclaringClass().equals(WebMapService.class));
        Assert.isTrue("getMap".equals(method.getName()));

        final Object[] arguments = invocation.getArguments();

        Assert.isTrue(arguments.length == 1);
        Assert.isInstanceOf(GetMapRequest.class, arguments[0]);

        final GetMapRequest request = (GetMapRequest) arguments[0];
        boolean tiled = request.isTiled();
        if (!tiled) {
            return (WebMap) invocation.proceed();
        }
        ConveyorTile cachedTile = gwc.dispatch(request);
        if (cachedTile != null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("GetMap request intercepted, serving cached content: " + request);
            }
            // Handle Etags
            final String ifNoneMatch = request.getHttpRequestHeader("If-None-Match");
            final String hexTag = Long.toHexString(cachedTile.getTSCreated());

            if (hexTag.equals(ifNoneMatch)) {
                // Client already has the current version
                LOGGER.finer("ETag matches, returning 304");
                throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
            }

            LOGGER.finer("No matching ETag, returning cached tile");
            final String mimeType = cachedTile.getMimeType().getMimeType();

            RawMap map;
            final Resource mapContents = cachedTile.getBlob();
            if (mapContents instanceof ByteArrayResource) {
                byte[] mapBytes;
                mapBytes = ((ByteArrayResource) mapContents).getContents();
                map = new RawMap(null, mapBytes, mimeType);
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                mapContents.transferTo(Channels.newChannel(out));
                map = new RawMap(null, out, mimeType);
            }
            map.setResponseHeader("Cache-Control", "no-cache");
            map.setResponseHeader("ETag", Long.toHexString(cachedTile.getTSCreated()));
            map.setResponseHeader("geowebcache-tile-index",
                    Arrays.toString(cachedTile.getTileIndex()));
            return map;
        }

        return (WebMap) invocation.proceed();
    }

}
