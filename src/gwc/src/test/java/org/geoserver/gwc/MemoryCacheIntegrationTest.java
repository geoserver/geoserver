/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.LAKES;
import static org.geowebcache.storage.blobstore.file.FilePathUtils.filteredLayerName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.memorycache.MemoryCacheInfo;
import org.junit.After;
import org.junit.Test;

/** Serves memory cached layers through the in-memory tile cache, with the heap backend. */
public class MemoryCacheIntegrationTest extends GeoServerSystemTestSupport {

    private static final String CACHE_RESULT = "geowebcache-cache-result";

    @After
    public void disableCache() throws IOException {
        GeoServerExtensions.bean(XMLConfiguration.class).setMemoryCache(new MemoryCacheInfo());
    }

    @Test
    public void testCacheOnly() throws Exception {
        String layer = getLayerId(BASIC_POLYGONS);
        String url = cacheLayer(layer, false);

        assertEquals("MISS", getAsServletResponse(url).getHeader(CACHE_RESULT));
        awaitHit(url);
        assertThat(storedTiles(layer), empty());
    }

    @Test
    public void testPassthrough() throws Exception {
        String layer = getLayerId(LAKES);
        String url = cacheLayer(layer, true);

        assertEquals("MISS", getAsServletResponse(url).getHeader(CACHE_RESULT));
        awaitHit(url);
        await().atMost(5, SECONDS).until(() -> !storedTiles(layer).isEmpty());
    }

    /** Turns the cache on and memory caches {@code layer}, returning the URL of one of its tiles. */
    private static String cacheLayer(String layer, boolean passthrough) throws IOException {
        MemoryCacheInfo settings = new MemoryCacheInfo();
        settings.setEnabled(true);
        settings.setPassthrough(passthrough);
        GeoServerExtensions.bean(XMLConfiguration.class).setMemoryCache(settings);

        GeoServerTileLayer tileLayer = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer);
        tileLayer.getInfo().setMemoryCached(true);
        GWC.get().save(tileLayer);
        return "gwc/service/wmts?request=GetTile&layer=" + layer
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";
    }

    /** Waits for the tile save, which runs in the background and may still be in flight after the first response. */
    private void awaitHit(String url) {
        await().atMost(5, SECONDS)
                .until(() -> "HIT".equals(getAsServletResponse(url).getHeader(CACHE_RESULT)));
    }

    /** The tiles of {@code layer} in the default file blob store, its metadata left out. */
    private static Collection<File> storedTiles(String layer) throws ConfigurationException {
        String root = GeoServerExtensions.bean(DefaultStorageFinder.class).getDefaultPath();
        File dir = new File(root, filteredLayerName(layer));
        return dir.isDirectory() ? FileUtils.listFiles(dir, new String[] {"png"}, true) : List.of();
    }
}
