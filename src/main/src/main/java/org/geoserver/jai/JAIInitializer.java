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
import org.geoserver.config.JAIInfo;

/**
 * Initializes ImageN functionality from configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class JAIInitializer implements GeoServerInitializer {

    private final GeoServerTileCache tileCache;

    public JAIInitializer(GeoServerTileCache tileCache) {
        this.tileCache = tileCache;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        initJAI(geoServer.getGlobal().getJAI());

        geoServer.addListener(new ConfigurationListenerAdapter() {

            @Override
            public void handleGlobalChange(
                    GeoServerInfo global, List<String> propertyNames, List<Object> oldValues, List<Object> newValues) {

                if (propertyNames.contains("jAI")) { // TODO: check why the propertyname is reported as jAI
                    // instead of JAI
                    // Make sure to proceed with ImageN init
                    // only in case the global change involved that section
                    initJAI(global.getJAI());
                }
            }
        });
    }

    @SuppressWarnings("PMD.CloseResource")
    void initJAI(JAIInfo jai) {
        ImageN jaiDef = ImageN.getDefaultInstance();
        jai.setJAI(jaiDef);

        // setting ImageN wide hints
        jaiDef.setRenderingHint(ImageN.KEY_CACHED_TILE_RECYCLING_ENABLED, jai.isRecycling());

        // force the tile cache to be the one provided by GeoServer
        TileCache oldTileCache = jai.getTileCache();
        if (oldTileCache != tileCache) {
            jaiDef.setTileCache(tileCache);
            oldTileCache.flush();
        }

        // tile factory and recycler
        if (jai.isRecycling() && !(jaiDef.getRenderingHint(ImageN.KEY_TILE_FACTORY) instanceof ConcurrentTileFactory)) {
            final ConcurrentTileFactory recyclingFactory = new ConcurrentTileFactory();
            jaiDef.setRenderingHint(ImageN.KEY_TILE_FACTORY, recyclingFactory);
            jaiDef.setRenderingHint(ImageN.KEY_TILE_RECYCLER, recyclingFactory);
        } else {
            if (!jai.isRecycling()) {
                final PassThroughTileFactory passThroughFactory = new PassThroughTileFactory();
                jaiDef.setRenderingHint(ImageN.KEY_TILE_FACTORY, passThroughFactory);
                jaiDef.setRenderingHint(ImageN.KEY_TILE_RECYCLER, passThroughFactory);
            }
        }

        // Setting up Cache Capacity
        TileCache jaiCache = jaiDef.getTileCache();
        jai.setTileCache(jaiCache);

        long jaiMemory = (long) (jai.getMemoryCapacity() * Runtime.getRuntime().maxMemory());
        jaiCache.setMemoryCapacity(jaiMemory);

        // Setting up Cache Threshold
        jaiCache.setMemoryThreshold((float) jai.getMemoryThreshold());

        jaiDef.getTileScheduler().setParallelism(jai.getTileThreads());
        jaiDef.getTileScheduler().setPrefetchParallelism(jai.getTileThreads());
        jaiDef.getTileScheduler().setPriority(jai.getTilePriority());
        jaiDef.getTileScheduler().setPrefetchPriority(jai.getTilePriority());
    }
}
