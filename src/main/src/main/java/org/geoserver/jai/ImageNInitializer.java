/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import java.util.List;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.TileCache;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.ImageProcessingInfo;

/**
 * Initializes ImageN functionality from configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class ImageNInitializer implements GeoServerInitializer {

    private final GeoServerTileCache tileCache;

    public ImageNInitializer(GeoServerTileCache tileCache) {
        this.tileCache = tileCache;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        initJAI(geoServer.getGlobal().getImageProcessing());

        geoServer.addListener(new ConfigurationListenerAdapter() {

            @Override
            public void handleGlobalChange(
                    GeoServerInfo global, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {

                if (propertyNames.contains("jAI")) { // TODO: check why the propertyname is reported as jAI
                    // instead of JAI
                    // Make sure to proceed with ImageN init
                    // only in case the global change involved that section
                    initJAI(global.getImageProcessing());
                }
            }
        });
    }

    @SuppressWarnings("PMD.CloseResource")
    void initJAI(ImageProcessingInfo imageProcessing) {
        ImageN imageN = ImageN.getDefaultInstance();
        imageProcessing.setJAI(imageN);

        // setting ImageN wide hints
        imageN.setRenderingHint(ImageN.KEY_CACHED_TILE_RECYCLING_ENABLED, imageProcessing.isRecycling());

        // force the tile cache to be the one provided by GeoServer
        TileCache oldTileCache = imageProcessing.getTileCache();
        if (oldTileCache != tileCache) {
            imageProcessing.setTileCache(tileCache);
            oldTileCache.flush();
        }

        // tile factory and recycler
        if (imageProcessing.isRecycling()
                && !(imageN.getRenderingHint(ImageN.KEY_TILE_FACTORY) instanceof ConcurrentTileFactory)) {
            final ConcurrentTileFactory recyclingFactory = new ConcurrentTileFactory();
            imageN.setRenderingHint(ImageN.KEY_TILE_FACTORY, recyclingFactory);
            imageN.setRenderingHint(ImageN.KEY_TILE_RECYCLER, recyclingFactory);
        } else {
            if (!imageProcessing.isRecycling()) {
                final PassThroughTileFactory passThroughFactory = new PassThroughTileFactory();
                imageN.setRenderingHint(ImageN.KEY_TILE_FACTORY, passThroughFactory);
                imageN.setRenderingHint(ImageN.KEY_TILE_RECYCLER, passThroughFactory);
            }
        }

        // Setting up Cache Capacity
        TileCache jaiCache = imageN.getTileCache();
        imageProcessing.setTileCache(jaiCache);

        long jaiMemory = (long)
                (imageProcessing.getMemoryCapacity() * Runtime.getRuntime().maxMemory());
        jaiCache.setMemoryCapacity(jaiMemory);

        // Setting up Cache Threshold
        jaiCache.setMemoryThreshold((float) imageProcessing.getMemoryThreshold());

        imageN.getTileScheduler().setParallelism(imageProcessing.getTileThreads());
        imageN.getTileScheduler().setPrefetchParallelism(imageProcessing.getTileThreads());
        imageN.getTileScheduler().setPriority(imageProcessing.getTilePriority());
        imageN.getTileScheduler().setPrefetchPriority(imageProcessing.getTilePriority());
    }
}
