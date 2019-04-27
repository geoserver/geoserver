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
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RawMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
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

        map.setContentDispositionHeader(
                null, "." + cachedTile.getMimeType().getFileExtension(), false);

        Integer cacheAgeMax = getCacheAge(layer);
        LOGGER.log(Level.FINE, "Using cacheAgeMax {0}", cacheAgeMax);
        if (cacheAgeMax != null) {
            map.setResponseHeader("Cache-Control", "max-age=" + cacheAgeMax);
        } else {
            map.setResponseHeader("Cache-Control", "no-cache");
        }

        setConditionalGetHeaders(map, cachedTile, request, etag);
        setCacheMetadataHeaders(map, cachedTile, layer);

        return map;
    }

    private void setConditionalGetHeaders(
            RawMap map, ConveyorTile cachedTile, GetMapRequest request, String etag) {
        map.setResponseHeader("ETag", etag);

        final long tileTimeStamp = cachedTile.getTSCreated();
        final String ifModSinceHeader = request.getHttpRequestHeader("If-Modified-Since");
        // commons-httpclient's DateUtil can encode and decode timestamps formatted as per RFC-1123,
        // which is one of the three formats allowed for Last-Modified and If-Modified-Since headers
        // (e.g. 'Sun, 06 Nov 1994 08:49:37 GMT'). See
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1

        final String lastModified =
                org.apache.commons.httpclient.util.DateUtil.formatDate(new Date(tileTimeStamp));
        map.setResponseHeader("Last-Modified", lastModified);

        final Date ifModifiedSince;
        if (ifModSinceHeader != null && ifModSinceHeader.length() > 0) {
            try {
                ifModifiedSince = DateUtil.parseDate(ifModSinceHeader);
                // the HTTP header has second precision
                long ifModSinceSeconds = 1000 * (ifModifiedSince.getTime() / 1000);
                long tileTimeStampSeconds = 1000 * (tileTimeStamp / 1000);
                if (ifModSinceSeconds >= tileTimeStampSeconds) {
                    throw new HttpErrorCodeException(HttpServletResponse.SC_NOT_MODIFIED);
                }
            } catch (DateParseException e) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer(
                            "Can't parse client's If-Modified-Since header: '"
                                    + ifModSinceHeader
                                    + "'");
                }
            }
        }
    }

    private void setCacheMetadataHeaders(RawMap map, ConveyorTile cachedTile, TileLayer layer) {
        long[] tileIndex = cachedTile.getTileIndex();
        CacheResult cacheResult = cachedTile.getCacheResult();
        GridSubset gridSubset = layer.getGridSubset(cachedTile.getGridSetId());
        BoundingBox tileBounds = gridSubset.boundsFromIndex(tileIndex);

        String cacheResultHeader = cacheResult == null ? "UNKNOWN" : cacheResult.toString();
        map.setResponseHeader("geowebcache-layer", layer.getName());
        map.setResponseHeader("geowebcache-cache-result", cacheResultHeader);
        map.setResponseHeader("geowebcache-tile-index", Arrays.toString(tileIndex));
        map.setResponseHeader("geowebcache-tile-bounds", tileBounds.toString());
        map.setResponseHeader("geowebcache-gridset", gridSubset.getName());
        map.setResponseHeader("geowebcache-crs", gridSubset.getSRS().toString());
    }

    private Integer getCacheAge(TileLayer layer) {
        Integer cacheAge = null;
        if (layer instanceof GeoServerTileLayer) {
            PublishedInfo published = ((GeoServerTileLayer) layer).getPublishedInfo();
            // configuring caching does not appear possible for layergroup
            if (published instanceof LayerInfo) {
                LayerInfo layerInfo = (LayerInfo) published;
                MetadataMap metadata = layerInfo.getResource().getMetadata();
                Boolean enabled = metadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class);
                if (enabled != null && enabled) {
                    cacheAge =
                            layerInfo
                                    .getResource()
                                    .getMetadata()
                                    .get(ResourceInfo.CACHE_AGE_MAX, Integer.class);
                }
            }
        }
        return cacheAge;
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
