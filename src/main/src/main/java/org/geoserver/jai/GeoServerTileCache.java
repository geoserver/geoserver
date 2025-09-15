/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import static java.util.logging.Level.FINE;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.eclipse.imagen.media.cache.ConcurrentTileCacheMultiMap;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/**
 * A tile cache implementation based on ConcurrentTileCacheMultiMap, which integrates with the OGC request lifecycle
 * ensuring that tiles are properly disposed of at the end of the request.
 *
 * <p>In addition, it provides improved logging.
 */
public class GeoServerTileCache extends ConcurrentTileCacheMultiMap implements DispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(GeoServerTileCache.class);

    final Cache<Request, Set<RenderedImage>> images =
            CacheBuilder.newBuilder().weakKeys().build();

    @Override
    public void add(RenderedImage owner, int tileX, int tileY, Raster data) {
        recordImage(owner);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine(
                    "Adding tile (%d,%d) for image %s[%d]".formatted(tileX, tileY, owner.getClass(), owner.hashCode()));
        }
        super.add(owner, tileX, tileY, data);
    }

    @Override
    public void add(RenderedImage owner, int tileX, int tileY, Raster data, Object tileCacheMetric) {
        recordImage(owner);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine(
                    "Adding tile (%d,%d) for image %s[%d]".formatted(tileX, tileY, owner.getClass(), owner.hashCode()));
        }
        super.add(owner, tileX, tileY, data, tileCacheMetric);
    }

    @Override
    public void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles, Object tileCacheMetric) {
        recordImage(owner);
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Adding tiles (%s) for image %s[%d]"
                    .formatted(Arrays.toString(tileIndices), owner.getClass(), owner.hashCode()));
        }
        super.addTiles(owner, tileIndices, tiles, tileCacheMetric);
    }

    @Override
    public Raster getTile(RenderedImage owner, int tileX, int tileY) {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Getting tile (%d,%d) for image %s[%d]"
                    .formatted(tileX, tileY, owner.getClass(), owner.hashCode()));
        }
        return super.getTile(owner, tileX, tileY);
    }

    @Override
    public Raster[] getTiles(RenderedImage owner) {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Getting all tiles for image %s[%d]".formatted(owner.getClass(), owner.hashCode()));
        }
        return super.getTiles(owner);
    }

    @Override
    public Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Getting tiles (%s) for image %s[%d]"
                    .formatted(Arrays.toString(tileIndices), owner.getClass(), owner.hashCode()));
        }
        return super.getTiles(owner, tileIndices);
    }

    @Override
    public void remove(RenderedImage owner, int tileX, int tileY) {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Removing tile (%d,%d) for image %s[%d]"
                    .formatted(tileX, tileY, owner.getClass(), owner.hashCode()));
        }
        super.remove(owner, tileX, tileY);
    }

    /**
     * Records the image as associated to the current request, so that we can clear its tiles at the end of the request.
     */
    private void recordImage(RenderedImage owner) {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) return;

        Set<RenderedImage> images = this.images.asMap().computeIfAbsent(request, r -> ConcurrentHashMap.newKeySet());
        images.add(owner);
    }

    @Override
    public void removeTiles(RenderedImage owner) {
        if (LOGGER.isLoggable(FINE) && super.getTiles(owner) != null) {
            LOGGER.fine("Removing all tiles for image %s[%d]".formatted(owner.getClass(), owner.hashCode()));
        }
        super.removeTiles(owner);
    }

    // ------------------------- DispatcherCallback methods -------------------------
    // These methods ensure that the tile cache is cleared at the end of each request
    // ------------------------------------------------------------------------------

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {
        Set<RenderedImage> images = this.images.getIfPresent(request);
        if (images != null) {
            for (RenderedImage image : images) {
                removeTiles(image);
            }
        }
        this.images.invalidate(request);
    }
}
